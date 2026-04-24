# 추천 로직

## 전체 흐름

```mermaid
flowchart TD
    A["GET /api/v1/recommendations"] --> B{"추천 캐시 있음?"}
    B -- 예 --> C["캐시 응답 반환"]
    B -- 아니오 --> D{"사용자 벡터 캐시 있음?"}
    D -- 예 --> E["벡터 사용"]
    D -- 아니오 --> F["행동 로그 조회"]
    F --> G["사용자 벡터 생성"]
    G --> H["벡터 캐시 저장"]
    H --> E
    E --> I["pgvector 후보 5개 조회"]
    I --> J{"후보 있음?"}
    J -- 아니오 --> K["인기 상품 fallback 반환"]
    J -- 예 --> L["OpenAI 추천 이유 생성"]
    L --> M["추천 캐시 저장"]
    M --> N["추천 응답 반환"]
```

## 빠른 요약

| 항목 | 현재 구현 |
|------|-----------|
| 추천 개수 | 5개 |
| 추천 캐시 TTL | 20분 |
| 사용자 벡터 TTL | 1시간 |
| 벡터 차원 | 768 |
| 행동 가중치 | `VIEW=1.0`, `WISHLIST=2.0`, `ORDER=3.0` |
| 최근성 기준 | 반감기 14일 |
| VIEW 제한 | 같은 상품 최신 3회까지만 반영 |
| fallback | 인기 상품 추천 |

## 1. 캐시 동작

- 추천 결과 캐시: Redis 20분
- 사용자 벡터 캐시: Redis 1시간
- 추천 캐시가 있으면 그대로 반환합니다.
- 추천 캐시가 없으면 사용자 벡터를 조회하고, 그것도 없을 때만 새로 계산합니다.

## 2. 사용자 벡터 생성

사용자 벡터는 `user_activities` 행동 로그와 상품 임베딩을 합쳐 만듭니다.

```text
user_vector = normalize(
  Σ(product_embedding * action_weight * recency_weight)
)
```

### 반영 규칙

- `VIEW = 1.0`
- `WISHLIST = 2.0`
- `ORDER = 3.0`
- 최근 행동일수록 더 크게 반영합니다.
- 반감기는 14일입니다.
- 오래된 `WISHLIST`, `ORDER`도 완전히 제외되지는 않고 점점 약하게 반영됩니다.
- 같은 상품의 `VIEW`는 최신 3회까지만 반영합니다.
- `WISHLIST`, `ORDER`는 이 횟수 제한을 받지 않습니다.
- 상품 임베딩이 없거나 길이가 768이 아니면 그 행동은 건너뜁니다.
- 마지막에 L2 정규화합니다.

### 행동 반영 이미지

```mermaid
flowchart LR
    A["UserActivity"] --> B{"ActionType"}
    B -->|VIEW| C["기본 가중치 1.0"]
    B -->|WISHLIST| D["기본 가중치 2.0"]
    B -->|ORDER| E["기본 가중치 3.0"]
    C --> F["최근성 가중치 곱셈"]
    D --> F
    E --> F
    F --> G["상품 임베딩에 누적"]
    G --> H["최종 L2 정규화"]
```

## 3. 후보 검색

- 테이블: `product_embeddings`
- 대상 조건: `status = 'ENABLE'`
- 방식: pgvector 코사인 유사도 Top-K 검색
- 개수: 5개

SQL 개념은 아래와 같습니다.

```sql
SELECT id, title, description, price, road_address
FROM product_embeddings
WHERE status = 'ENABLE'
ORDER BY embedding <=> (?::vector)
LIMIT 5
```

## 4. 추천 이유 생성

- 후보가 잡히면 OpenAI로 상품별 추천 이유를 생성합니다.
- 최종 응답은 `productId`, `title`, `reason` 목록입니다.

## 5. fallback 추천

- 개인화 후보가 없으면 인기 상품 추천으로 대체합니다.
- 조건: `status = 'ENABLE'` 그리고 `popularity > 0`
- 정렬: `popularity DESC`
- 현재 fallback 결과는 별도 캐시하지 않습니다.

## 6. 행동 로그와 캐시 무효화

### 이벤트 -> 행동 타입

- `PRODUCT_VIEWED` -> `VIEW`
- `PRODUCT_WISHLISTED` -> `WISHLIST`
- `ORDER_COMPLETED` -> `ORDER`

### 캐시 정책

- `WISHLIST`, `ORDER`는 사용자 벡터 캐시와 추천 캐시를 즉시 무효화합니다.
- `VIEW`는 발생 빈도가 높아서 즉시 무효화하지 않고 TTL 만료에 맡깁니다.

```mermaid
flowchart LR
    A["행동 로그 적재"] --> B{"행동 타입"}
    B -->|VIEW| C["캐시 유지"]
    B -->|WISHLIST| D["벡터/추천 캐시 삭제"]
    B -->|ORDER| E["벡터/추천 캐시 삭제"]
```
