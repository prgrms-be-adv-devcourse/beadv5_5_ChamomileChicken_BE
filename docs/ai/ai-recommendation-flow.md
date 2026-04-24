# AI 추천 기능 흐름

## 핵심 클래스

- `RecommendationController`
  - 추천 API 진입점
  - 현재 사용자 ID를 받아 추천 유스케이스 호출

- `RecommendationService`
  - 추천 기능의 메인 오케스트레이터
  - 캐시 조회, 사용자 벡터 조회, 후보 검색, 추천 이유 생성, 응답 캐싱 담당

- `ProductAiEventsConsumer`
  - `product.events` Kafka 토픽 소비
  - 상품 임베딩 갱신, 삭제, 사용자 조회 이력 적재 처리

- `ProductEmbeddingSyncService`
  - 상품 정보를 임베딩으로 변환
  - 임베딩 저장/수정/삭제 담당

- `UserActivityService`
  - 상품 조회 이벤트를 사용자 활동 데이터로 저장

- `UserVectorService`
  - 사용자 활동 기반 벡터 조회/생성 담당
  - 반복 VIEW 상한과 최근성 감쇠를 적용해 사용자 벡터 구성

## 추천 요청 흐름

1. `RecommendationController`가 추천 요청을 받음
2. `RecommendationService`가 추천 캐시를 먼저 확인
3. 캐시가 없으면 `UserVectorService`로 사용자 벡터 조회 또는 생성
4. 사용자 벡터 생성 시 행동 가중치, 최신 3회 VIEW 제한, 최근성 감쇠를 적용
5. `CandidateSearchRepository`로 벡터 기반 후보 상품 검색
6. 후보가 있으면 `AiGatewayPort`로 추천 이유 생성
7. 결과를 캐시에 저장하고 응답 반환
8. 후보가 없으면 인기 상품 기반 fallback 반환

## 데이터 적재 흐름

1. product 서비스가 `product.events` 발행
2. `ProductAiEventsConsumer`가 이벤트 수신
3. `PRODUCT_AI_SYNCED`
   - `ProductEmbeddingSyncService`가 상품 임베딩 저장/갱신
4. `PRODUCT_DELETED`
   - 상품 임베딩 삭제
5. `PRODUCT_VIEWED`
   - `UserActivityService`가 사용자 조회 이력 저장
   - 사용자 벡터 캐시와 추천 캐시를 함께 무효화

## 한 줄 요약

이 추천 기능은 이벤트로 상품/사용자 데이터를 쌓아두고, 추천 요청 시 `RecommendationService`가 벡터 검색과 AI 이유 생성을 조합해 결과를 반환하는 구조다.
