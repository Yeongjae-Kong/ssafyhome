# 🏠 구집
> **아파트 실거래 데이터 기반 AI 시세 종합 분석 서비스**

**"부동산 투자를 고민할 때는? 구집"**

급변하는 부동산 시장에서 단순히 범용 LLM(Foundation Model)을 연동하는 것만으로는 실시간 이슈와 세부적인 지역별 동향을 파악하는 데 한계가 있었습니다. 
**구집**은 이러한 한계를 극복하기 위해 금융그룹의 전문 연구보고서와 다양한 도메인 데이터를 학습시킨 **RAG 파이프라인**을 구축하여, 사용자에게 전문성 있는 분석과 정교한 부동산 인사이트를 제공합니다.

👉 **[시연 영상](https://www.youtube.com/watch?v=EpvmYui2DAQ)**

<br/>

## 주요 기능

* **AI 부동산 비서 (RAG)**
    * RAG 기반 AI를 활용하여 특정 지역 및 단지의 향후 전망과 투자 브리핑을 제공합니다.
    * 단순 통계가 아닌 전문 보고서 기반의 심층 분석 정보를 답변합니다.
* **다음 달 시세 예측**
    * 선형 회귀 분석(Linear Regression) 모델을 통해 지역별 익월 아파트 매매가를 예측합니다.
* **전국 시세 지도**
    * 매매가 기준 전월/전년 대비 등락률을 시각적인 히트맵(Heatmap) 형태로 제공합니다.
* **apt 실거래 데이터 조회**
    * 공공데이터포털 API를 연동하여 최근 1년간의 아파트 실거래 내역을 투명하게 보여줍니다.

<br/>

## Data Pipeline & AI Optimization

전문 도메인 정보를 효율적으로 RAG에 활용하기 위해 다음과 같은 **데이터 파이프라인 자동화**를 설계했습니다.

* **데이터 수집 자동화 (Automation)**
    * **n8n (RSS Feed Trigger)**을 활용하여 부동산 관련 주요 뉴스 및 정보 수집 과정을 자동화, 실시간성을 확보했습니다.
* **비정형 데이터 구조화 (Structuring)**
    * **NotebookLM**을 활용해 수백 개의 부동산 분석 영상 데이터를 요약 및 구조화하여 텍스트 데이터로 변환했습니다.
* **도메인 특화 RAG 구축**
    * 자동화된 파이프라인을 통해 수집된 고품질의 도메인 데이터를 벡터 DB에 적재하여, 범용 모델 대비 높은 전문성과 정확도를 가진 답변을 생성합니다.

<br/>

## 🛠 Tech Stack

| 분류 | 기술 스택 |
| :--- | :--- |
| **Frontend** | React, Vite, Tailwind CSS |
| **Backend** | Spring Boot, Spring Security (JWT), MyBatis |
| **AI Serving** | FastAPI, LangChain, ChromaDB |
| **Pipeline** | n8n (RSS Feed), NotebookLM |
| **Database** | MySQL |
| **Open API** | 공공데이터포털 API, Kakao Maps API, Kakao Local API |

<br/>

## 📂 프로젝트 구조

* **root** : React Frontend Application
* **/backend** : Spring Boot Application
* **/ai_server** : FastAPI Application

<br/>

## 👥 팀원 및 역할

| 이름 | 담당 역할 |
| :--- | :--- |
| **공영재** | AI 모델링 및 RAG 파이프라인 설계, Frontend 개발 |
| **김세웅** | Backend API 개발, 일정 관리 및 문서화 |
