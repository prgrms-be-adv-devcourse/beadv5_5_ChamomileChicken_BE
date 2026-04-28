# 추천 로직

## 전체 흐름

```mermaid
flowchart TD
    A["조회/찜/주문 이벤트 수신"] --> B["user_activities 저장"]
    B --> C["recommendation snapshot 삭제"]
    C --> D["userVector / exclude 증분 업데이트"]

    E["GET /api/v1/recommendations"] --> F{"snapshot cache 있음?"}
    F -- 예 --> G["캐시 응답 반환<br/>PENDING / COMPLETED / FAILED"]
    F -- 아니오 --> H{"user profile / exclude cache 있음?"}
    H -- 예 --> I["Redis 상태 사용"]
    H -- 아니오 --> J["user_activities 조회"]
    J --> K["userVector / exclude full rebuild"]
    K --> I
    I --> L["pgvector 후보 5개 조회<br/>조회/찜/주문 상품 제외"]
    L --> M{"후보 있음?"}
    M -- 아니오 --> N["인기 상품 fallback 반환<br/>COMPLETED"]
    M -- 예 --> O["기본 추천 이유로 PENDING 응답 생성"]
    O --> P["snapshot cache 저장"]
    P --> Q["즉시 응답 반환"]
    P -. async .-> R["OpenAI 추천 이유 생성"]
    R --> S{"생성 성공?"}
    S -- 예 --> T["COMPLETED로 snapshot 갱신"]
    S -- 아니오 --> U["FAILED로 snapshot 갱신"]
```

## 2-Phase 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller as RecommendationController
    participant Service as RecommendationService
    participant Snapshot as RecommendationRedisRepository
    participant Vector as UserVectorService
    participant Search as CandidateSearchRepository
    participant Async as RecommendationReasonAsyncService
    participant OpenAI as OpenAiClient

    Client->>Controller: GET /api/v1/recommendations
    Controller->>Service: recommend(userId)
    Service->>Snapshot: get(userId)

    alt snapshot 존재
        Snapshot-->>Service: cached response
        Service-->>Controller: cached response
        Controller-->>Client: 200 OK
    else snapshot 없음
        Service->>Vector: getOrCreateState(userId)
        Vector-->>Service: userVector + excludedProductIds
        Service->>Search: findTopK(userVector, excludedProductIds, 5)
        Search-->>Service: candidates

        alt 후보 없음
            Service-->>Controller: fallback response (COMPLETED)
            Controller-->>Client: 200 OK
        else 후보 있음
            Service->>Snapshot: save(PENDING + 기본 추천 이유)
            Service->>Async: generateAndCache(userId, userVector, candidates)
            Service-->>Controller: PENDING response
            Controller-->>Client: 200 OK

            par 백그라운드 작업
                Async->>OpenAI: generateRecommendationReasons(...)
                alt 성공
                    OpenAI-->>Async: reasonMap
                    Async->>Snapshot: save(COMPLETED + GPT 추천 이유)
                else 실패 / timeout / 파싱 오류
                    Async->>Snapshot: save(FAILED + 기본 추천 이유)
                end
            and polling
                Client->>Controller: GET /api/v1/recommendations
                Controller->>Service: recommend(userId)
                Service->>Snapshot: get(userId)
                Snapshot-->>Service: PENDING / COMPLETED / FAILED
                Service-->>Controller: cached response
                Controller-->>Client: 200 OK
            end
        end
    end
```

## 구현 요약

| 항목 | 현재 구현 |
|------|-----------|
| 추천 개수 | 5개 |
| recommendation snapshot TTL | 20분 |
| user profile / exclude TTL | 6시간 |
| user profile version | 2 |
| 벡터 차원 | 768 |
| 행동 가중치 | `VIEW=1.0`, `WISHLIST=3.0`, `ORDER=5.0` |
| 최근성 기준 | 반감기 14일 |
| VIEW 제외 여부 | 제외함 |
| 응답 상태 | `PENDING`, `COMPLETED`, `FAILED` |
| fallback | 인기 상품 추천 |

## 1. 캐시 동작

- recommendation snapshot 캐시: Redis 20분
- user profile / exclude 캐시: Redis 6시간
- 추천 API는 snapshot 캐시가 있으면 그 값을 그대로 반환합니다.
- snapshot 캐시는 polling 동안 같은 추천 세트를 유지하기 위한 용도입니다.
- snapshot 캐시가 없으면 user profile / exclude 캐시를 조회합니다.
- user profile 또는 exclude 캐시가 없거나 버전이 다르면 `user_activities` 기준으로 full rebuild 합니다.

## 2. 사용자 벡터 생성 및 갱신

사용자 벡터는 아래 두 방식으로 유지됩니다.

1. 이벤트 수신 시 증분 업데이트
2. 캐시 miss 또는 버전 불일치 시 full rebuild

```text
user_vector = normalize(
  decay(previous_user_vector) + (product_embedding * action_weight)
)
```

full rebuild 시에는 전체 행동 로그 기준으로 아래 수식을 사용합니다.

```text
user_vector = normalize(
  Σ(product_embedding * action_weight * recency_weight)
)
```

반영 규칙:

- `VIEW = 1.0`
- `WISHLIST = 3.0`
- `ORDER = 5.0`
- 최근 행동일수록 더 크게 반영합니다.
- 반감기는 14일입니다.
- 상품 임베딩이 없거나 길이가 768이 아니면 그 행동은 건너뜁니다.
- 마지막에 L2 정규화합니다.

## 3. 제외 상품 정책

- `VIEW`, `WISHLIST`, `ORDER` 모두 추천 후보에서 제외합니다.
- 제외 상품 ID는 Redis `user:exclude:v2:{userId}`에 저장합니다.
- full rebuild 시에도 `user_activities` 전체에서 다시 구성합니다.

## 4. 후보 검색

- 테이블: `product_embeddings`
- 대상 조건: `status = 'ENABLE'`
- 방식: pgvector 코사인 유사도 Top-K 검색
- 개수: 5개
- 제외 조건: Redis exclude set에 포함된 상품 ID는 `NOT IN (...)`으로 제거

## 5. 추천 이유 생성

- 후보가 잡히면 기본 추천 이유로 먼저 `PENDING` 응답을 반환합니다.
- OpenAI 추천 이유 생성은 `RecommendationReasonAsyncService`에서 비동기로 수행합니다.
- 성공 시 snapshot을 `COMPLETED`로 갱신합니다.
- timeout, 5xx, 파싱 실패가 나면 기본 추천 이유를 유지한 채 `FAILED`로 갱신합니다.
- 비동기 작업 제출 자체가 거절되면 snapshot을 즉시 `FAILED`로 갱신합니다.

## 6. fallback 추천

- 개인화 후보가 없으면 인기 상품 추천으로 대체합니다.
- 조건: `status = 'ENABLE'` 그리고 `popularity > 0`
- 정렬: `popularity DESC`
- fallback 응답 상태는 `COMPLETED`입니다.

## 7. 행동 로그와 캐시 무효화

이벤트 -> 행동 타입:

- `PRODUCT_VIEWED` -> `VIEW`
- `PRODUCT_WISHLISTED` -> `WISHLIST`
- `ORDER_COMPLETED` -> `ORDER`

캐시 정책:

- 모든 행동 이벤트는 해당 유저의 recommendation snapshot을 즉시 삭제합니다.
- 모든 행동 이벤트는 userVector / exclude를 즉시 증분 업데이트합니다.
- 상품 임베딩이 바뀌면 모든 user profile과 recommendation snapshot을 삭제합니다.
