# Elasticsearch 도입 — 상품 검색 고도화

## 배경 및 목적

데브코스 파이널 프로젝트 요구사항으로 Elasticsearch 도입이 포함되어 있다.
기존 상품 검색은 PostgreSQL JPA 쿼리(`LIKE %keyword%`)로 구현되어 있었으며, 다음 한계가 있었다.

| 항목 | 기존 (JPA LIKE) | 현재 (Elasticsearch) |
|------|----------------|----------------------|
| 검색 방식 | `title` 컬럼 부분 일치만 지원 | `title` + `description` 전문 검색 |
| 한국어 지원 | 형태소 분석 없음 ("노트북"으로 "노트북 거치대" 검색 불가) | nori 형태소 분석기 적용 |
| 성능 | Full table scan (LIKE 앞 와일드카드) | 역인덱스 기반 빠른 검색 |
| 확장성 | 컬럼 추가 시 쿼리 전체 수정 필요 | 필드 추가만으로 검색 범위 확장 가능 |
| user 서비스 호출 | 검색마다 sellerName 조회를 위해 REST 호출 | sellerName 비정규화로 호출 제거 |

---

## 현재 검색 구현 상태

**엔드포인트:** `GET /api/v1/products`

**파라미터:**
- `title` (optional) — 검색 키워드
- `status` — `ENABLE` / `DISABLE`
- `thisPage`, `pageSize` — 페이징

**현재 흐름 (ES 도입 후):**

```
ProductRestController
  → ProductUseCase.searchAll()
    → ProductService.searchAll()
      → [keyword 없음] ProductSearchRepository.findAllEnabled()   ← ES 전체 조회
      → [keyword 있음] ProductSearchRepository.searchByKeyword()  ← ES 키워드 검색
      → SearchProductResponseDto 반환 (user 서비스 호출 없음)
```

---

## Elasticsearch 인덱스 설계

### ProductDocument 필드

| 필드 | ES 타입 | 분석기 | 설명 |
|------|---------|--------|------|
| `id` | keyword | — | UUID (PK) |
| `sellerId` | keyword | — | 판매자 UUID |
| `sellerName` | keyword | — | 판매자 이름 (비정규화 저장) |
| `title` | text | nori | 상품명 — 형태소 분석 |
| `description` | text | nori | 상품 설명 — 형태소 분석 |
| `status` | keyword | — | ENABLE / DISABLE |
| `price` | double | — | 가격 |
| `maxCapacity` | integer | — | 최대 수용 인원 |
| `thumbnailPath` | keyword | — | 썸네일 경로 |
| `deleted` | boolean | — | `deleteDt != null` 여부 (소프트 삭제 필터용) |
| `regDt` | date | — | 등록일시 |

> **비정규화 전략:** `sellerName`을 ES에 직접 저장해 검색 시 user 서비스 REST 호출을 제거한다.
> 판매자 이름이 변경되는 경우는 드물기 때문에 허용 가능한 trade-off로 판단.

### nori 분석기 설정 (`product-settings.json`)

```json
{
  "analysis": {
    "tokenizer": {
      "nori_mixed": {
        "type": "nori_tokenizer",
        "decompound_mode": "mixed"
      }
    },
    "filter": {
      "nori_stop": {
        "type": "nori_part_of_speech",
        "stoptags": [
          "JKS", "JKC", "JKG", "JKO", "JKB", "JKV", "JKQ", "JX", "JC",
          "EC", "EF", "EP", "ETN", "ETM",
          "VX",
          "XPN", "XSA", "XSN", "XSV"
        ]
      }
    },
    "analyzer": {
      "nori": {
        "type": "custom",
        "tokenizer": "nori_mixed",
        "filter": ["nori_stop", "lowercase"]
      }
    }
  }
}
```

- `decompound_mode: mixed` — 복합어를 원형과 분리형 모두 색인 (예: "공방클래스" → "공방", "클래스", "공방클래스")
- `nori_stop` — 조사, 어미, 접사 등 불용어 제거로 검색 정확도 향상

---

## 아키텍처 — 기존 패턴 유지

기존 `domain/repository` 인터페이스 → `infrastructure` 어댑터 패턴을 그대로 따른다.

```
domain/repository/
  ProductSearchRepository.java          ← 도메인 레이어 인터페이스 (ES 의존 없음)

infrastructure/elasticsearch/
  ProductDocument.java                  ← ES 인덱스 문서
  ProductSearchRepositoryAdapter.java   ← ElasticsearchOperations 기반 구현체

resources/elasticsearch/
  product-settings.json                 ← nori 분석기 인덱스 설정
```

> Spring Data Elasticsearch 인터페이스(`ElasticsearchRepository`) 대신 `ElasticsearchOperations`를 직접 사용한다.
> `CriteriaQuery` or() 체이닝의 우선순위 버그를 피하기 위해 `searchByKeyword`는 `NativeQuery` bool 쿼리로 구현했다.

**`ProductService`에서의 의존 관계:**

```java
// 검색 시
ProductSearchRepository.searchByKeyword(keyword, pageable)   // ES 키워드 검색
ProductSearchRepository.findAllEnabled(pageable)             // ES 전체 조회

// CRUD 시 (RDB-ES 동기화)
ProductSearchRepository.save(ProductDocument.from(product, sellerName))  // 등록/수정 시 색인
ProductSearchRepository.deleteById(productId)                             // 삭제 시 ES 문서 제거
```

---

## 초기 마이그레이션

ES 도입 이전에 PostgreSQL에 이미 존재하는 상품 데이터를 ES에 일괄 색인하기 위한 엔드포인트를 제공한다.

**엔드포인트:** `POST /api/v1/products/es-migrate`

- 인증 없이 호출 가능 (internal 엔드포인트, `SecurityConfig`에서 `permitAll` 처리)
- 삭제되지 않은 전체 상품을 ES에 벌크 색인 후 색인 건수 반환
- **일회성 작업** — ES 컨테이너 최초 구동 후 한 번만 호출

```json
// 응답 예시
{ "indexed": 42 }
```

---

## 인프라 설정

### docker-compose.yml

```yaml
elasticsearch:
  build:
    context: .
    dockerfile_inline: |
      FROM elasticsearch:9.0.3
      RUN bin/elasticsearch-plugin install --batch analysis-nori
  container_name: elasticsearch
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
    - ES_JAVA_OPTS=-Xms512m -Xmx512m
  ports:
    - "9200:9200"
  volumes:
    - es_data:/usr/share/elasticsearch/data
```

> nori 플러그인은 ES 9.x 기본 이미지에 포함되어 있지 않아 `dockerfile_inline`으로 빌드 시 설치한다.
> `xpack.security.enabled=false` — 로컬 개발 환경 전용. 프로덕션에서는 TLS + 인증 필수.

### application-dev.yml

```yaml
spring:
  elasticsearch:
    uris: ${ES_URIS:http://localhost:9200}
```

---

## 구현 완료 체크리스트

### 기반 구조
- [x] `ProductDocument` 클래스 생성 (ES 인덱스 매핑)
- [x] `ProductSearchRepository` 도메인 인터페이스 정의
- [x] `ProductSearchRepositoryAdapter` — `ElasticsearchOperations` 기반 구현
- [x] nori 분석기 설정 파일 (`product-settings.json`)

### 서비스 연동
- [x] `build.gradle`에 `spring-boot-starter-data-elasticsearch` 추가
- [x] `application-dev.yml`에 ES 연결 설정 추가
- [x] `ProductService.searchAll()` — ES 검색으로 교체 (user 서비스 REST 호출 제거)
- [x] `ProductService.create()` — 상품 등록 시 ES 색인
- [x] `ProductService.update()` — 상품 수정 시 ES 업데이트
- [x] `ProductService.delete()` — 상품 삭제 시 ES 문서 삭제
- [x] `ProductResponseDto.from(ProductDocument)` 팩토리 메서드 추가
- [x] `SearchProductResponseDto.fromEs()` 오버로드 추가
- [x] `SecurityConfig` — es-migrate 엔드포인트 permitAll 추가

### 인프라
- [x] `docker-compose.yml`에 Elasticsearch 9.0.3 컨테이너 추가 (nori 플러그인 포함)
- [x] 기존 PostgreSQL 데이터 → ES 초기 마이그레이션 API (`POST /api/v1/products/es-migrate`)

### 테스트
- [x] 기존 `ProductCUDTest` — `ProductSearchRepository` Mock 추가 후 통과
- [x] 기존 `ProductSelectTest` — `전체_상품_조회` ES 기반으로 재작성 후 통과
- [x] 신규 `ProductSearchTest` — ES 검색 5개 케이스 (키워드 유무, sellerName 포함, 빈 결과, 페이징)

---

## 고려 사항

### 데이터 정합성
- PostgreSQL이 원본(source of truth), ES는 검색용 읽기 복제본
- 현재 구현은 동기 방식 — DB 저장 후 즉시 ES 색인 시도
- ES 색인 실패 시 DB 트랜잭션은 이미 커밋된 상태이므로 불일치 발생 가능
- 실서비스 수준이라면 `@TransactionalEventListener(AFTER_COMMIT)` 또는 Kafka 기반 비동기 색인 검토 필요

### 검색 범위 확장 가능성
- 현재: `title` + `description`
- 추후: 태그, 카테고리 등 필드 추가 시 `ProductDocument`에 필드만 추가하면 됨