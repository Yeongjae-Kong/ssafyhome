import os
import re
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

# LangChain 관련 임포트
from langchain_community.document_loaders import PyPDFLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_chroma import Chroma
from langchain_core.prompts import ChatPromptTemplate

try:
    from langchain_classic.chains import create_retrieval_chain
    from langchain_classic.chains.combine_documents import create_stuff_documents_chain
except ImportError:
    from langchain.chains import create_retrieval_chain
    from langchain.chains.combine_documents import create_stuff_documents_chain

load_dotenv()

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

vector_store = None
retriever = None 
llm = None

SSAFY_GMS_BASE_URL = "https://gms.ssafy.io/gmsapi/api.openai.com/v1"

@app.on_event("startup")
async def startup_event():
    global vector_store, retriever, llm
    
    pdf_files = ["report.pdf", "KB주택시장리뷰_2025년 12월호.pdf", "GTX.pdf", "2026년 한국 경제 및 부동산 시장 전망 통합 보고서.pdf"]
    all_splits = [] 
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)

    print("📄 PDF 파일 로딩 및 통합 시작...")

    for pdf_path in pdf_files:
        if os.path.exists(pdf_path):
            print(f"   Reading: {pdf_path}...")
            try:
                loader = PyPDFLoader(pdf_path)
                documents = loader.load()
                splits = text_splitter.split_documents(documents)
                all_splits.extend(splits)
                print(f"   ✅ {pdf_path} 로드 완료 ({len(splits)} chunks)")
            except Exception as e:
                print(f"   ❌ {pdf_path} 로드 중 에러 발생: {e}")
        else:
            print(f"   ⚠️ 파일 없음: {pdf_path} (건너뜀)")

    if not all_splits:
        print("❌ 로드된 문서가 없습니다. RAG 기능을 사용할 수 없습니다.")
        return

    print(f"📊 총 {len(all_splits)}개의 텍스트 청크를 벡터 DB에 저장합니다...")

    embedding_model = OpenAIEmbeddings(
        model="text-embedding-3-small", 
        base_url=SSAFY_GMS_BASE_URL,
        chunk_size=10 
    )

    vector_store = Chroma.from_documents(
        documents=all_splits, 
        embedding=embedding_model
    )
    
    llm = ChatOpenAI(
        model_name="gpt-4o", 
        temperature=0.3, 
        base_url=SSAFY_GMS_BASE_URL
    )
    
    # 검색 범위를 넉넉하게 잡아서 문맥 부족 현상 완화
    retriever = vector_store.as_retriever(search_kwargs={"k": 6})
    print("✅ RAG 시스템 준비 완료! (SSAFY GMS Connected)")


class AnalyzeRequest(BaseModel):
    region: str
    query: str = ""
    analysis_type: str = "detailed" 

@app.post("/api/rag/analyze")
async def analyze_real_estate(req: AnalyzeRequest):
    if not retriever or not llm:
        raise HTTPException(status_code=500, detail="RAG 서버 초기화 실패")
    
    # ------------------------------------------------------------------
    # [1] 점수 가이드라인
    # ------------------------------------------------------------------
    score_rules = (
        "### 🚨 점수 책정 규칙 (1~7점 척도):"
        "당신은 적극적인 투자 전략가입니다. '보류'나 '판단 불가'는 최대한 피하세요."
        "작은 힌트라도 찾아서 반드시 매수/매도 방향성을 제시하세요."
        "\n"
        "- **[SCORE:1]** (적극 매도 추천): 시장 붕괴, 심각한 악재."
        "- **[SCORE:2]** (매도 추천): 하락 추세."
        "- **[SCORE:3]** (비중 축소 추천): 호재보다 악재 우위."
        "- **[SCORE:4]** (관망): (가급적 사용 금지) 방향성 불분명."
        "- **[SCORE:5]** (소극적 매수 추천): 바닥 다지기, 긍정 신호."
        "- **[SCORE:6]** (매수 추천): 상승 추세, 호재 명확."
        "- **[SCORE:7]** (적극적 매수 추천): 저평가 + 대형 호재."
        "\n"
        "**답변의 맨 첫 줄은 반드시 [SCORE:점수] 태그로 시작하세요.**"
    )

    # ------------------------------------------------------------------
    # [2] 분석 및 작성 가이드
    # ------------------------------------------------------------------
    analysis_rules = (
        "### 📝 분석 및 작성 원칙 (필수 준수):"
        "1. **문서 인용:** [Context]에 해당 지역 내용이 있다면 적극 인용하여 구체적으로 분석하세요."
        "2. **적극적 추론:** 문서에 특정 동/구 단위 정보가 없더라도 절대로 '정보가 없다'고 끝내지 마세요."
        "   - [Context]의 **'서울/수도권 전체 흐름'**, **'거시 경제'** 내용을 바탕으로 논리적으로 추론하세요."
        "   - 일반적인 부동산 지식(입지, 학군, 교통)을 결합하여 분석을 완성하세요."
        "3. **서술 방식:**"
        "   - 가장 첫 줄 [SCORE] 태그를 제외하고, **모든 본문은 자연스러운 줄글(Prose)** 형태로 작성하세요."
        "   - **'#', '**', '-', '1.' 등의 마크다운/특수문자 사용을 금지합니다.**"
        "   - 전문가가 옆에서 말해주는 것처럼 편안하고 전문적인 문체로 작성하세요."
    )

    # ------------------------------------------------------------------
    # [3] 프롬프트 분기 (요약 vs 상세)
    # ------------------------------------------------------------------
    if req.analysis_type == "summary":
        system_instructions = (
            f"당신은 부동산 요약 전문가입니다. Context를 바탕으로 분석하세요.\n"
            f"{score_rules}\n"
            f"{analysis_rules}\n"
            "**분량:** 전체 내용을 **5~6줄 내외**의 두개의 문단으로 작성하세요."
        )
    else:
        system_instructions = (
            f"당신은 대한민국 최고의 부동산 애널리스트입니다. Context를 바탕으로 심층 분석하세요.\n"
            f"{score_rules}\n"
            f"{analysis_rules}\n"
            "**분량 및 구성:** 시장 현황, 입지 분석, 리스크, 투자 전략 순으로 흐름을 잡아 **충분히 상세하게(6~7 문단), 지역의 입지를 중점으로** 서술하세요."
        )

    # [✅ 핵심 수정] 전체를 f-string 하나로 묶고 context만 이중 중괄호 처리
    # 이렇게 하면 Python이 {{context}}를 {context}로 변환해주고, 
    # LangChain은 {context}를 보고 변수 위치를 인식합니다.
    system_prompt = f"{system_instructions}\n\n[Context]:\n{{context}}"

    prompt_template = ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        ("human", "{input}"),
    ])
    
    # 문서 결합 체인
    chain = create_stuff_documents_chain(llm, prompt_template)
    rag_chain = create_retrieval_chain(retriever, chain)
    
    user_input = f"{req.region} 지역의 2025년 부동산 시장 전망. {req.query}"
    
    try:
        response = rag_chain.invoke({"input": user_input})
        raw_answer = response["answer"]
        
        # 점수 파싱
        score_match = re.search(r'\[SCORE:(\d)\]', raw_answer)
        score = 4 
        clean_answer = raw_answer

        if score_match:
            score = int(score_match.group(1))
            clean_answer = raw_answer.replace(score_match.group(0), "").strip()

        return {
            "score": score,
            "result": clean_answer
        }
        
    except Exception as e:
        print(f"Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)