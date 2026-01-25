# 🏠 구집
> **부동산 실거래 데이터 기반 AI 시세 종합 분석 서비스**

**"부동산 투자를 고민할 때는? 구집"** 공공데이터와 AI(RAG)를 활용하여 사용자에게 실질적인 지역별 투자 브리핑과 시세 예측 정보를 제공하는 서비스입니다.

👉 **[시연 영상](https://www.youtube.com/watch?v=EpvmYui2DAQ)**

<br/>

## ✨ 주요 기능

* **🤖 AI 부동산 비서 (RAG)**
    * RAG(Retrieval-Augmented Generation) 기반 AI를 활용하여 특정 지역 및 단지의 향후 전망과 투자 브리핑을 제공합니다.
* **📈 다음 달 시세 예측**
    * 선형 회귀 분석(Linear Regression) 모델을 통해 지역별 익월 아파트 매매가를 예측합니다.
* **🗺️ 전국 시세 지도**
    * 매매가 기준 전월/전년 대비 등락률을 시각적인 히트맵(Heatmap) 형태로 제공합니다.
* **apt 실거래 데이터 조회**
    * 공공데이터포털 API를 연동하여 최근 1년간의 아파트 실거래 내역을 투명하게 보여줍니다.

<br/>

## 🛠 Tech Stack

| 분류 | 기술 스택 |
| :--- | :--- |
| **Frontend** | React, Vite, Tailwind CSS |
| **Backend** | Spring Boot, Spring Security (JWT), MyBatis |
| **AI Serving** | FastAPI, LangChain, ChromaDB |
| **Database** | MySQL |
| **Open API** | 공공데이터포털 API, Kakao Maps API, Kakao Local API |

<br/>

## 📂 프로젝트 구조

* **root**: React Frontend Application
* **/backend** : Spring Boot Application
* **/ai_server** : FastAPI Application

<br/>

## 👥 팀원 및 역할

| 이름 | 담당 역할 |
| :--- | :--- |
| **공영재** | AI 개발, Frontend 개발 |
| **김세웅** | Backend 개발|
