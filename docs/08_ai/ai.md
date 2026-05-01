# AI 서비스 구조 및 흐름

## 배경 및 목적

`ai` 서비스는 사용자 행동 이력과 상품 임베딩을 기반으로 개인화 추천을 생성하는 서비스입니다.

현재 `ai` 서비스의 역할:

| 항목 | 역할 |
|------|------|
| 추천 API | 사용자별 추천 목록 조회 |
| 사용자 행동 수집 | 조회/찜/주문 이벤트를 사용자 활동 이력으로 저장 |
| 사용자 벡터 생성 | 행동 이력과 상품 임베딩을 합성해 사용자 취향 벡터 생성 |
| 상품 임베딩 관리 | 상품 이벤트를 받아 OpenAI 임베딩 생성 및 갱신 |
| 후보 탐색 | pgvector 코사인 유사도 기반 Top-K 후보 검색 |
| 추천 이유 생성 | OpenAI Chat Completions로 추천 이유를 비동기 생성 |
| 캐시 | userVector 상태와 recommendation snapshot을 Redis에 저장 |

## 현재 구현 상태

- 추천 API는 `@CurrentUser`로 전달된 `X-User-Id` 헤더를 기준으로 동작합니다.
- 사용자 벡터는 상품 임베딩 768차원 벡터의 가중합으로 계산합니다.
- 행동 가중치는 `VIEW=1.0`, `WISHLIST=3.0`, `ORDER=5.0`입니다.
- 조회한 상품은 추천 후보에서 제외하지 않습니다.
- 찜/주문한 상품만 추천 후보에서 제외합니다.
- recommendation snapshot은 Redis에 20분, user profile / exclude는 Redis에 6시간 저장합니다.
- 추천 응답 상태는 `PENDING`, `COMPLETED`, `FAILED`입니다.
- 추천 후보가 없으면 인기 상품 fallback 추천으로 내려갑니다.

## 주요 도메인 구조

| 엔티티 | 설명 | 핵심 필드 |
|--------|------|-----------|
| `UserActivity` | 사용자의 상품 행동 이력 | `userId`, `productId`, `actionType`, `createdAt` |
| `UserVector` | 사용자 취향 벡터 값 객체 | `vector` |
| `UserVectorProfile` | Redis에 저장하는 사용자 벡터 상태 | `userVector`, `lastUpdatedAt`, `version` |
| `UserPreferenceState` | 추천 계산용 사용자 상태 | `userVector`, `excludedProductIds` |

행동 타입:

| 타입 | 값 |
|------|----|
| `ActionType` | `VIEW`, `WISHLIST`, `ORDER` |

## 패키지 구조

```text
presentation/controller/
  RecommendationController.java

application/service/
  RecommendationService.java
  RecommendationReasonAsyncService.java
  UserVectorService.java
  UserActivityService.java
  ProductEmbeddingSyncService.java

domain/model/
  UserActivity.java
  UserVector.java
  UserVectorProfile.java
  UserPreferenceState.java
  ActionType.java

domain/repository/
  UserActivityRepository.java
  ProductEmbeddingRepository.java
  CandidateSearchRepository.java
  UserVectorCacheRepository.java
  RecommendationCacheRepository.java

infrastructure/persistence/
  UserActivityJpaRepository.java
  UserActivityRepositoryAdapter.java
  ProductEmbeddingRepositoryImpl.java
  CandidateSearchRepositoryImpl.java
  UserVectorRedisRepository.java
  RecommendationRedisRepository.java
```

## 서비스별 책임

### RecommendationService

- recommendation snapshot 조회
- snapshot miss 시 userVector 상태 확보
- pgvector 후보 검색
- `PENDING` snapshot 생성
- 비동기 추천 이유 생성 워커 호출
- 후보 없을 때 fallback 응답 생성

### RecommendationReasonAsyncService

- OpenAI Chat Completions 비동기 호출
- 성공 시 snapshot을 `COMPLETED`로 갱신
- 실패 시 snapshot을 `FAILED`로 갱신

### UserVectorService

- Redis user profile / exclude 조회
- cache miss 또는 version mismatch 시 full rebuild
- 행동 이벤트 기반 증분 업데이트
- 행동 가중치/최근성 반영
- exclude 상품 집합 구성

### UserActivityService

- 조회/찜/주문 행동 로그 저장
- 해당 유저 recommendation snapshot 삭제
- userVector 증분 업데이트 호출

### ProductEmbeddingSyncService

- 상품 임베딩 생성/갱신/삭제
- 전체 user profile / recommendation snapshot 캐시 무효화

## 주요 흐름

### 추천 조회 흐름

```text
RecommendationController
  -> RecommendationService.recommend(userId)
    -> RecommendationRedisRepository.get()
    -> snapshot 있으면 즉시 반환
    -> UserVectorService.getOrCreateState()
      -> UserVectorRedisRepository.getProfile()
      -> UserVectorRedisRepository.getExcludedProductIds()
      -> 없으면 UserActivityRepository.findByUserId()
      -> ProductEmbeddingRepository.findAllByProductIds()
      -> userVector / exclude full rebuild
    -> CandidateSearchRepository.findTopK(userVector, excludedProductIds, 5)
    -> RecommendationRedisRepository.save(PENDING)
    -> RecommendationReasonAsyncService.generateAndCache()
    -> RecommendationResponseDto 반환

RecommendationReasonAsyncService
  -> OpenAiClient.generateRecommendationReasons()
  -> 성공 시 RecommendationRedisRepository.save(COMPLETED)
  -> 실패 시 RecommendationRedisRepository.save(FAILED)
```

### 행동 이벤트 흐름

```text
product.events / order.events
  -> UserActivityService.recordActivity()
    -> user_activities INSERT
    -> RecommendationRedisRepository.delete(userId)
    -> UserVectorService.updateOnActivity()
      -> exclude set 갱신
      -> profile miss면 full rebuild
      -> profile hit면 decay + embedding * weight 증분 반영
```

## 추천 알고리즘 개요

### 1. 사용자 벡터 생성

full rebuild:

```text
user_vector = normalize(
  Σ(product_embedding * action_weight * recency_weight)
)
```

증분 업데이트:

```text
updated_vector = normalize(
  decay(previous_vector) + (product_embedding * action_weight)
)
```

행동 가중치:

| 행동 | 가중치 |
|------|--------|
| `VIEW` | `1.0` |
| `WISHLIST` | `3.0` |
| `ORDER` | `5.0` |

추가 규칙:

- 반감기 14일 기준 최근성 가중치를 적용합니다.
- 상품 임베딩이 없거나 길이가 768이 아니면 건너뜁니다.
- 마지막에 L2 정규화합니다.

### 2. 후보 검색

```sql
SELECT id, title, description, price, road_address
FROM product_embeddings
WHERE status = 'ENABLE'
  AND embedding <=> (?::vector) <= ?
  AND id NOT IN (...)
ORDER BY embedding <=> (?::vector)
LIMIT 5
```

### 3. 제외 정책

- `VIEW`는 추천 후보에서 제외하지 않습니다.
- `WISHLIST`, `ORDER`만 추천 후보에서 제외합니다.
- 제외 상품은 Redis `user:exclude:v2:{userId}`에 저장합니다.

### 4. 추천 이유 생성

- 첫 응답은 기본 추천 이유와 함께 `PENDING`
- OpenAI 추천 이유 생성은 비동기 executor에서 수행
- 성공 시 `COMPLETED`, 실패 시 `FAILED`
- 프론트는 같은 API를 polling 하며 상태를 확인

## Redis 연동

| 키 | 의미 | TTL |
|----|------|-----|
| `user:profile:v2:{userId}` | userVector profile | 6시간 |
| `user:exclude:v2:{userId}` | 제외 상품 ID 집합 | 6시간 |
| `user:{userId}:recommendation:v2` | recommendation snapshot (`PENDING`, `COMPLETED`, `FAILED`) | 20분 |

## PostgreSQL 연동

| 테이블 | 용도 |
|--------|------|
| `user_activities` | 사용자 행동 이력 저장 |
| `product_embeddings` | 추천 대상 상품 및 벡터 저장 |

## API 응답 형태

### GET `/api/v1/recommendations`

```json
{
  "status": "COMPLETED",
  "recommendations": [
    {
      "productId": "11111111-1111-1111-1111-111111111111",
      "title": "한강 노을 사진 클래스",
      "reason": "야외 감성과 체험형 취향에 잘 맞는 클래스입니다."
    }
  ]
}
```

클라이언트 연동 규칙:

- `status=PENDING`이면 기본 추천 이유를 먼저 표시합니다.
- 동일한 `GET /api/v1/recommendations` API를 polling 하여 `COMPLETED` 또는 `FAILED`를 확인합니다.
- 현재 프론트는 `1.5초` 간격, 최대 `5회` polling 합니다.
- `FAILED`면 기본 추천 이유를 유지합니다.

## 운영 주의사항

- recommendation snapshot은 polling 동안 같은 추천 세트를 유지하기 위한 용도입니다.
- 행동 이벤트가 들어오면 해당 유저 snapshot을 즉시 삭제합니다.
- 상품 임베딩이 바뀌면 전체 user profile / recommendation snapshot을 삭제합니다.
- Redis key 버전은 현재 `v2`입니다.
- `LocalDateTime`은 Redis에 ISO 문자열로 저장합니다.
