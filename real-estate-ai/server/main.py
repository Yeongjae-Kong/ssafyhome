import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

# [기존] 문서 로더 및 분할기
from langchain_community.document_loaders import PyPDFLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_chroma import Chroma
from langchain_core.prompts import ChatPromptTemplate

# LangChain v1.0 호환성 처리
try:
    from langchain_classic.chains import create_retrieval_chain
    from langchain_classic.chains.combine_documents import create_stuff_documents_chain
except ImportError:
    from langchain.chains import create_retrieval_chain
    from langchain.chains.combine_documents import create_stuff_documents_chain

# 환경변수 로드 (.env 파일에 GMS KEY가 OPENAI_API_KEY로 저장되어 있어야 함)
load_dotenv()

app = FastAPI()

# 1. CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 2. 전역 변수
vector_store = None
rag_chain = None 

# [★핵심 수정 1] SSAFY GMS Base URL 정의
SSAFY_GMS_BASE_URL = "https://gms.ssafy.io/gmsapi/api.openai.com/v1"

@app.on_event("startup")
async def startup_event():
    global vector_store, rag_chain
    
    pdf_path = "report.pdf"
    
    if not os.path.exists(pdf_path):
        print(f"⚠️  파일 없음: {pdf_path} (RAG 기능 불가)")
        return

    print("📄 PDF 로딩 및 벡터 DB 구축 시작...")
    
    # PDF 로드 & 분할
    loader = PyPDFLoader(pdf_path)
    documents = loader.load()
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
    splits = text_splitter.split_documents(documents)
    
    # [핵심 수정] chunk_size=10 추가
    # 이렇게 하면 문서를 10개씩 끊어서 GMS로 보내므로 413 에러가 발생하지 않습니다.
    embedding_model = OpenAIEmbeddings(
        model="text-embedding-3-small", # 혹은 사용 가능한 모델명
        base_url=SSAFY_GMS_BASE_URL,
        chunk_size=10  # ★ 중요: 한 번의 요청에 포함할 텍스트 청크 개수 제한
    )

    vector_store = Chroma.from_documents(
        documents=splits, 
        embedding=embedding_model
    )
    # [★핵심 수정 3] LLM에 GMS Base URL 적용
    # model_name은 GMS에서 지원하는 모델명(예: gpt-4o, gpt-4.1 등)으로 맞춰주세요.
    llm = ChatOpenAI(
        model_name="gpt-4o", 
        temperature=0,
        base_url=SSAFY_GMS_BASE_URL
    )
    
    retriever = vector_store.as_retriever(search_kwargs={"k": 3})
    
    # 1) 질문-답변 프롬프트 정의
    system_prompt = (
        "당신은 부동산 전문가입니다. 아래의 [Context]를 참고하여 질문에 답하세요."
        "문서에 없는 내용은 지어내지 말고 모른다고 답하세요."
        "\n\n"
        "[Context]:\n{context}"
    )
    prompt = ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        ("human", "{input}"),
    ])

    # 2) 문서 결합 체인 생성
    question_answer_chain = create_stuff_documents_chain(llm, prompt)
    
    # 3) 최종 검색 체인 생성
    rag_chain = create_retrieval_chain(retriever, question_answer_chain)
    
    print("✅ RAG 시스템 준비 완료! (SSAFY GMS Connected)")

class AnalyzeRequest(BaseModel):
    region: str
    query: str = ""

@app.post("/api/rag/analyze")
async def analyze_real_estate(req: AnalyzeRequest):
    if not rag_chain:
        raise HTTPException(status_code=500, detail="RAG 서버 초기화 실패 (PDF 확인 요망)")
    
    # 질문 구성
    user_input = f"{req.region} 지역의 2025년 부동산 시장 전망에 대해 보고서를 바탕으로 자세히 분석해줘. {req.query}"
    
    try:
        # 실행
        response = rag_chain.invoke({"input": user_input})
        return {"result": response["answer"]}
        
    except Exception as e:
        print(f"Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    # 로컬 테스트용 실행
    uvicorn.run(app, host="0.0.0.0", port=8000)