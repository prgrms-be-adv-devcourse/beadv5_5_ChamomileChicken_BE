# AI 서비스 구조 및 흐름

## 배경 및 목적

`ai` 서비스는 사용자 행동 이력과 상품 임베딩을 기반으로 개인화 추천을 생성하는 서비스입니다.

현재 프로젝트에서 `ai` 서비스는 아래 역할을 가집니다.

| 항목 | 역할 |
|------|------|
| 추천 API | 사용자별 추천 목록 조회 |
| 사용자 행동 수집 | 상품 조회 이벤트를 받아 사용자 활동 이력 저장 |
| 사용자 벡터 생성 | 행동 이력과 상품 임베딩을 합성해 사용자 취향 벡터 생성 |
| 상품 임베딩 관리 | 상품 이벤트를 받아 OpenAI 임베딩 생성 및 갱신 |
| 후보 탐색 | pgvector 코사인 유사도 기반 Top-K 후보 검색 |
| 추천 이유 생성 | OpenAI Chat Completions로 추천 이유 생성 |
| 캐시 | 사용자 벡터 및 추천 결과를 Redis에 저장 |

---

## 현재 구현 상태

**주요 외부 API**

| 영역 | 엔드포인트 | 설명 |
|------|------------|------|
| 추천 | `GET /api/v1/recommendations` | 현재 사용자 맞춤 추천 조회 |

**현재 서비스 흐름의 축**

```
Controller
  -> UseCase
    -> Application Service
      -> Redis Cache
      -> UserActivity JPA
      -> ProductEmbedding JDBC/pgvector
      -> OpenAI API
      -> Kafka Consumer
```

**현재 도메인 특징**

- 추천 API는 `@CurrentUser`로 전달된 `X-User-Id` 헤더를 기준으로 동작합니다.
- 사용자 벡터는 상품 임베딩 768차원 벡터의 가중합으로 계산합니다.
- 행동 가중치는 `VIEW=1.0`, `WISHLIST=2.0`, `ORDER=3.0`입니다.
- 추천 후보가 없으면 인기 상품 fallback 추천으로 내려갑니다.
- 추천 결과는 Redis에 20분, 사용자 벡터는 Redis에 1시간 캐시됩니다.
- 상품 임베딩은 PostgreSQL `pgvector` 확장을 전제로 `product_embeddings` 테이블에 저장됩니다.
- OpenAI는 임베딩 생성과 추천 이유 생성 두 용도로 사용됩니다.

---

## 주요 도메인 구조

### 엔티티별 역할

| 엔티티 | 설명 | 핵심 필드 |
|--------|------|-----------|
| `UserActivity` | 사용자의 상품 행동 이력 | `userId`, `productId`, `actionType`, `createdAt` |
| `ProductEmbedding` | 추천 대상 상품과 임베딩 | `id`, `title`, `description`, `price`, `roadAddress`, `status`, `popularity`, `embedding` |
| `UserVector` | 사용자 취향 벡터 값 객체 | `vector` |

### 상태/분류 값

| 타입 | 값 |
|------|----|
| `ActionType` | `VIEW`, `WISHLIST`, `ORDER` |

> 현재 실제 적재 로직은 `PRODUCT_VIEWED`, `PRODUCT_WISHLISTED`, `ORDER_COMPLETED` 이벤트를 통해 `VIEW`, `WISHLIST`, `ORDER` 행동을 저장합니다.

---

## 패키지 구조 및 계층 역할

```
presentation/controller/
  RecommendationController.java

application/service/
  RecommendationService.java
  UserVectorService.java
  UserActivityService.java
  ProductEmbeddingSyncService.java

application/port/external/
  AiGatewayPort.java

domain/model/
  UserActivity.java
  ProductEmbedding.java
  UserVector.java
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

infrastructure/external/openai/
  EmbeddingService.java
  OpenAiClient.java

infrastructure/kafka/
  ProductAiEventsConsumer.java
  ProductAiSyncedEvent.java
  ProductDeletedEvent.java
  ProductViewedEvent.java
```

---

## 서비스별 책임

### RecommendationService

| 기능 | 설명 |
|------|------|
| 추천 조회 | Redis 추천 캐시 확인 |
| 사용자 벡터 확보 | 캐시 조회 후 없으면 새로 생성 |
| 후보 검색 | pgvector 유사도 기반 Top-K 추천 후보 조회 |
| 추천 이유 생성 | OpenAI Chat Completions 호출 |
| fallback 추천 | 후보가 없으면 인기 상품 기반 추천 생성 |
| 추천 캐시 저장 | 최종 추천 결과를 Redis에 저장 |

### UserVectorService

| 기능 | 설명 |
|------|------|
| 사용자 벡터 조회 | Redis에서 사용자 벡터 캐시 조회 |
| 사용자 벡터 생성 | 활동 이력 기준 상품 ID를 모아 임베딩을 벌크 조회한 뒤 벡터 생성 |
| 가중치 적용 | 행동 유형별 가중치 반영 |
| 정규화 | 코사인 유사도 안정화를 위해 L2 정규화 |

> 상품 임베딩은 개별 조회하지 않고 벌크 조회하여 추천 벡터 생성 구간의 N+1 쿼리를 방지합니다.

### UserActivityService

| 기능 | 설명 |
|------|------|
| 상품 조회 기록 저장 | `PRODUCT_VIEWED` 이벤트를 `UserActivity`로 저장 |

### ProductEmbeddingSyncService

| 기능 | 설명 |
|------|------|
| 상품 임베딩 생성/갱신 | 상품 텍스트를 OpenAI 임베딩으로 변환 후 upsert |
| 상품 임베딩 삭제 | 상품 삭제 이벤트 수신 시 row 삭제 |

---

## 주요 요청 및 이벤트 흐름

### 추천 조회 흐름

```
RecommendationController
  -> RecommendationService.recommend(userId)
    -> RecommendationRedisRepository.get()
    -> UserVectorService.getOrCreate()
      -> UserVectorRedisRepository.get()
      -> 없으면 UserActivityRepository.findByUserId()
      -> ProductEmbeddingRepository.findAllByProductIds()
      -> 사용자 벡터 생성 및 정규화
      -> UserVectorRedisRepository.save()
    -> CandidateSearchRepository.findTopK()
    -> OpenAiClient.generateRecommendationReasons()
    -> RecommendationRedisRepository.save()
    -> RecommendationResponseDto 반환
```

### 추천 fallback 흐름

```
RecommendationService.recommend()
  -> 후보 없음
  -> CandidateSearchRepository.findPopular()
  -> "현재 인기 있는 클래스입니다." 사유로 응답 생성
  -> Redis 캐시 저장
```

### 상품 임베딩 동기화 흐름

```
product.events
  -> eventType = PRODUCT_AI_SYNCED
  -> ProductAiEventsConsumer.consume()
    -> ProductEmbeddingSyncService.saveOrUpdate()
      -> EmbeddingService.embedProductText()
      -> OpenAI Embeddings API 호출
      -> ProductEmbeddingRepository.upsert()
```

### 상품 삭제 반영 흐름

```
product.events
  -> eventType = PRODUCT_DELETED
  -> ProductAiEventsConsumer.consume()
    -> ProductEmbeddingSyncService.delete(productId)
      -> product_embeddings DELETE
```

### 사용자 행동 기록 흐름

```
product.events
  -> eventType = PRODUCT_VIEWED
  -> ProductAiEventsConsumer.consume()
    -> UserActivityService.recordProductView()
      -> user_activities INSERT
```

---

## 추천 알고리즘 개요

### 1. 사용자 벡터 생성

사용자 벡터는 사용자가 상호작용한 상품 임베딩의 가중합으로 생성합니다.
이때 반영 대상 상품 ID를 먼저 모은 뒤 상품 임베딩을 벌크 조회하여 사용자 벡터를 계산합니다.
이 방식으로 활동 수만큼 임베딩 조회 쿼리가 반복되는 N+1 문제를 방지합니다.

```text
user_vector = normalize(
  Σ(product_embedding * action_weight)
)
```

행동 가중치:

| 행동 | 가중치 |
|------|--------|
| `VIEW` | `1.0` |
| `WISHLIST` | `2.0` |
| `ORDER` | `3.0` |

### 2. 후보 검색

`CandidateSearchRepositoryImpl`는 PostgreSQL `pgvector`의 cosine distance 연산자 `<=>`를 사용합니다.

```sql
SELECT id, title, description, price, road_address
FROM product_embeddings
WHERE status = 'ENABLE'
ORDER BY embedding <=> (?::vector)
LIMIT ?
```

### 3. 추천 이유 생성

후보 목록이 정해지면 OpenAI Chat Completions에 배치 프롬프트를 보내고, 각 상품별 추천 사유를 JSON으로 반환받습니다.

출력 목표:

- 상품별 추천 이유 생성
- 벡터 값 직접 언급 금지
- 사용자 취향이 반영된 문장
- 20~60자
- `~합니다` 형태

### 4. fallback 추천

개인화 후보가 없으면 `popularity DESC` 기준 인기 상품 추천으로 대체합니다.

---

## 외부 연동 구조

### OpenAI 연동

| 용도 | 사용 위치 | 엔드포인트 |
|------|-----------|------------|
| 상품 임베딩 생성 | `EmbeddingService` | `POST /v1/embeddings` |
| 추천 이유 생성 | `OpenAiClient` | `POST /v1/chat/completions` |

임베딩 입력 텍스트는 아래 필드를 이어붙여 생성합니다.

- `상품명: {title}`
- `설명: {description}`
- `주소: {roadAddress}`

### Kafka 연동

현재 `ai` 서비스는 `product.events` 토픽을 소비합니다.

| eventType | 설명 | 처리 |
|-----------|------|------|
| `PRODUCT_AI_SYNCED` | 상품 추천용 정보 동기화 | 임베딩 생성 후 upsert |
| `PRODUCT_DELETED` | 상품 삭제 | 임베딩 row 삭제 |
| `PRODUCT_VIEWED` | 사용자 상품 조회 | 활동 이력 저장 |

에러 처리:

- Kafka listener는 1초 간격 3회 재시도 후 DLQ로 전송합니다.
- DLQ 토픽명은 `{원본토픽}.dlq` 규칙을 사용합니다.

### Redis 연동

| 키 | 의미 | TTL |
|----|------|-----|
| `user:{userId}:vector` | 사용자 벡터 캐시 | 1시간 |
| `user:{userId}:recommendation` | 추천 결과 캐시 | 20분 |

### PostgreSQL 연동

| 테이블 | 용도 |
|--------|------|
| `user_activities` | 사용자 행동 이력 저장 |
| `product_embeddings` | 추천 대상 상품 및 벡터 저장 |

---

## API 응답 형태

### GET `/api/v1/recommendations`

응답 DTO:

```json
{
  "recommendations": [
    {
      "productId": "11111111-1111-1111-1111-111111111111",
      "title": "한강 노을 사진 클래스",
      "reason": "야외 감성과 체험형 취향에 잘 맞는 클래스입니다."
    }
  ]
}
```

필드 설명:

| 필드 | 설명 |
|------|------|
| `recommendations` | 추천 결과 목록 |
| `productId` | 추천 상품 ID |
| `title` | 추천 상품 제목 |
| `reason` | 생성된 추천 사유 |

---

## 설정 및 실행 포인트

### 주요 설정

| 항목 | 값 |
|------|----|
| 서비스 포트 | `9009` |
| 프로파일 | `SPRING_PROFILES_ACTIVE` |
| Kafka 주소 | `KAFKA_BOOTSTRAP_SERVERS` |
| OpenAI 키 | `OPENAI_API_KEY` |

`application-prod.yml` 기준:

- PostgreSQL datasource 사용
- `spring.jpa.hibernate.ddl-auto=validate`
- 운영에서는 스키마를 애플리케이션이 자동 생성하지 않음

### 의존 인프라

- PostgreSQL
- Redis
- Kafka
- OpenAI API
- PostgreSQL `pgvector` 확장

---

## 데이터 및 운영 주의사항

### 스키마 관련

- `product_embeddings.embedding`은 `vector(768)` 컬럼이어야 합니다.
- OpenAI 임베딩 차원 수와 DB vector 차원 수가 반드시 일치해야 합니다.
- `user_activities.created_at`은 null이면 안 됩니다.
- `product_embeddings.status` 값은 추천 대상 필터 조건(`ENABLE`)과 일치해야 합니다.

### 운영 관련

- 사용자 활동이 추가로 적재돼도 기존 사용자 벡터 캐시는 즉시 무효화되지 않습니다.
- 현재 코드상 `UserVectorRedisRepository.delete()`는 구현돼 있지만 이벤트 기반 무효화 로직은 연결되어 있지 않습니다.
- 추천 이유 생성은 OpenAI 응답이 JSON 형식을 지켜야 하므로 프롬프트/모델 변경 시 파싱 안정성을 함께 점검해야 합니다.
- Kafka 문서(`docs/kafka-topics.md`)에는 현재 `ai` 서비스가 소비하는 `PRODUCT_AI_SYNCED`, `PRODUCT_DELETED`, `PRODUCT_VIEWED` 이벤트가 아직 반영되지 않았습니다.
- `RecommendationService`는 fallback 추천도 캐시하므로 초기 데이터 부족 상황에서 같은 결과가 20분간 유지될 수 있습니다.

---

## 구현 체크리스트

### 추천 기능

- [x] 추천 조회 API 구현
- [x] 추천 결과 Redis 캐시 구현
- [x] 후보 검색 pgvector 쿼리 구현
- [x] 인기 상품 fallback 추천 구현
- [x] OpenAI 추천 이유 생성 구현

### 데이터 파이프라인

- [x] 상품 임베딩 생성 및 upsert 구현
- [x] 상품 삭제 시 임베딩 삭제 구현
- [x] 상품 조회 이벤트 기반 사용자 행동 저장 구현
- [x] 사용자 활동 기반 벡터 생성 구현
- [x] Kafka 재시도 및 DLQ 처리 구현

### 운영 정비 포인트

- [ ] `product.events` 이벤트 문서를 현재 구현 기준으로 갱신
- [ ] 사용자 행동 누적 시 사용자 벡터 캐시 무효화 전략 추가
- [ ] 운영 DB에 `pgvector` 확장 및 관련 인덱스 전략 정리
