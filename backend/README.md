# SSAFY 홈 백엔드 개요 (요약)

- 기술: Spring Boot 3, Java 17, MyBatis, Security(JWT), Swagger, MySQL
- 현재 상태 핵심 변경
  - 거래 조회는 OpenAPI(data.go.kr) 실시간 조회로 전환
  - 아파트 상세는 DB 조회 + 옵션으로 생활권 요약 병합
  - 전 엔드포인트 공개(개발 용). 운영 시 인증 복구 필요

## 주요 엔드포인트
- 아파트(`/api/items`)
  - GET `/{aptSeq}` 아파트 상세 조회
    - 옵션: `withAccess=true` → 생활권 요약(지하철/마트/편의점/학교/병원/약국/카페) 함께 반환
  - GET `/{aptSeq}/transactions?period=YYYYMM[&page&size]` 아파트 거래(월, OpenAPI)
- 지역(`/api/region`)
  - GET `/code?sido&gugun[&dong]` 지역 코드 조회
  - GET `/houses?sido&gugun[&dong]` 지역 내 아파트 목록
  - GET `/deals?sido&gugun[&dong]&period=YYYYMM[&page&size]` 지역 거래(월, OpenAPI)
- 실거래(OpenAPI 전용)
  - GET `/api/live/apt-trades?sgg=11680&period=202501[&page&size]`
- 헬스체크: GET `/api/health`

## 외부 설정
- data.go.kr 실거래 API
  - `molit.base-url=https://apis.data.go.kr/1613000/RTMSDataSvcAptTrade`
  - `molit.paths.apt-trade=/getRTMSDataSvcAptTrade`
  - `molit.service-key=...` (data.go.kr 발급키, 보통 URL 인코딩 형태)
  - `molit.service-key-need-encode=false`
- Kakao 로컬 API: `kakao.local.key=...`

## 유의사항/개선
- 아파트명 매칭: 공백/특수문자/괄호 제거 + 대소문자 무시 + 부분포함(유사도)로 보강
- period(YYYYMM) 파라미터 필수: 거래 조회(`/region/deals`, `/items/{aptSeq}/transactions`)
- HouseDeal는 DTO로 사용(JPA 어노테이션 제거)
- 운영 전 인증 복구 및 키 비밀관리(ENV) 권장

