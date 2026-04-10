# Elasticsearch 도입 — 상품 검색 고도화

## 배경 및 목적

데브코스 파이널 프로젝트 요구사항으로 Elasticsearch 도입이 포함되어 있다.
현재 상품 검색은 PostgreSQL JPA 쿼리(`LIKE %keyword%`)로 구현되어 있으며, 다음 한계가 있다.

| 항목 | 현재 (JPA LIKE) | 목표 (Elasticsearch) |
|------|----------------|----------------------|
| 검색 방식 | `title` 컬럼 부분 일치만 지원 | `title` + `description` 전문 검색 |
| 한국어 지원 | 형태소 분석 없음 ("노트북"으로 "노트북 거치대" 검색 불가) | nori 형태소 분석기 적용 |
| 성능 | Full table scan (LIKE 앞 와일드카드) | 역인덱스 기반 빠른 검색 |
| 확장성 | 컬럼 추가 시 쿼리 전체 수정 필요 | 필드 추가만으로 검색 범위 확장 가능 |

---

## 현재 검색 구현 상태

**엔드포인트:** `GET /api/v1/products`

**파라미터:**
- `title` (optional) — 검색 키워드
- `status` — `ENABLE` / `DISABLE`
- `thisPage`, `pageSize` — 페이징

**현재 흐름:**

```
ProductRestController
  → ProductUseCase.searchAll()
    → ProductService.searchAll()
      → ProductJpaRepository.findByStatusAndTitleContainingAndDeleteDtIsNull()  ← 여기를 ES로 교체
      → SellerRepository.findSellerList()  (user 서비스 REST 호출)
      → SearchProductResponseDto 반환
```

**현재 Product 엔티티 주요 필드:**
- `id` (UUID), `sellerId` (UUID), `title`, `description`, `price`, `status`, `deleteDt`

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
    "analyzer": {
      "nori": {
        "type": "custom",
        "tokenizer": "nori_tokenizer",
        "filter": ["lowercase"]
      }
    }
  }
}
```

---

## 아키텍처 — 기존 패턴 유지

기존 `domain/repository` 인터페이스 → `infrastructure` 어댑터 패턴을 그대로 따른다.

```
domain/repository/
  ProductSearchRepository.java          ← 도메인 레이어 인터페이스 (ES 의존 없음)

infrastructure/elasticsearch/
  ProductDocument.java                  ← ES 인덱스 문서
  ProductElasticsearchRepository.java   ← Spring Data Elasticsearch 인터페이스
  ProductSearchRepositoryAdapter.java   ← ProductSearchRepository 구현체

resources/elasticsearch/
  product-settings.json                 ← nori 분석기 인덱스 설정
```

**`ProductService`에서의 의존 관계:**

```java
// 검색 시
ProductSearchRepository.searchByKeyword(keyword, pageable)   // ES 조회
ProductSearchRepository.findAllEnabled(pageable)             // ES 조회

// CRUD 시 (동기화)
ProductSearchRepository.save(ProductDocument.from(product))  // ES 색인
ProductSearchRepository.deleteById(id)                       // ES 삭제
```

---

## 구현 계획

### Phase 1 — 기반 구조 (완료)
- [x] `ProductDocument` 클래스 생성 (ES 인덱스 매핑)
- [x] `ProductSearchRepository` 도메인 인터페이스 정의
- [x] `ProductElasticsearchRepository` Spring Data ES 인터페이스
- [x] `ProductSearchRepositoryAdapter` 어댑터 구현
- [x] nori 분석기 설정 파일 (`product-settings.json`)

### Phase 2 — 서비스 연동
- [ ] `build.gradle`에 `spring-boot-starter-data-elasticsearch` 추가
- [x] `application-dev.yml`에 ES 연결 설정 추가 (`spring.elasticsearch.uris`)
- [x] `ProductService.searchAll()` — ES 검색으로 교체 (user 서비스 REST 호출 제거)
- [x] `ProductService.create()` — 상품 등록 시 ES 색인
- [x] `ProductService.update()` — 상품 수정 시 ES 업데이트
- [x] `ProductService.delete()` — 상품 삭제 시 ES 문서 삭제
- [x] `ProductResponseDto.from(ProductDocument)` 팩토리 메서드 추가
- [x] `SearchProductResponseDto.fromEs()` 오버로드 추가

### Phase 3 — 인프라
- [x] `docker-compose.yml`에 Elasticsearch 8.17.0 컨테이너 추가
- [ ] 기존 PostgreSQL 데이터 → ES 초기 마이그레이션 (배치 또는 API)

### Phase 4 — 테스트
- [x] 기존 `ProductCUDTest` — `ProductSearchRepository` Mock 추가 후 통과
- [x] 기존 `ProductSelectTest` — `전체_상품_조회` ES 기반으로 재작성 후 통과
- [x] 신규 `ProductSearchTest` — ES 검색 5개 케이스 (키워드 유무, sellerName 포함, 빈 결과, 페이징)

---

## application.yml 추가 설정 (예정)

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    # 인증이 필요한 경우
    # username: elastic
    # password: ${ES_PASSWORD}
```

---

## docker-compose 추가 예정

```yaml
elasticsearch:
  image: elasticsearch:8.17.0
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
    - ES_JAVA_OPTS=-Xms512m -Xmx512m
  ports:
    - "9200:9200"
  volumes:
    - es_data:/usr/share/elasticsearch/data

volumes:
  es_data:
```

> `xpack.security.enabled=false` — 로컬 개발 환경 전용. 프로덕션에서는 TLS + 인증 필수.

---

## 고려 사항

### 데이터 정합성
- PostgreSQL이 원본(source of truth), ES는 검색용 읽기 복제본
- 서비스 장애 시 ES 색인 실패가 DB 트랜잭션에 영향을 주지 않도록 ES 저장 실패는 로그만 남기고 예외를 삼키는 방향 검토 필요

### 한국어 nori 플러그인
- Elasticsearch 8.x 이상에서는 `nori` 플러그인이 기본 포함되어 있음
- 도커 이미지 사용 시 별도 설치 불필요

### 검색 범위 확장 가능성
- 현재: `title` + `description`
- 추후: 태그, 카테고리 등 필드 추가 시 `ProductDocument`에 필드만 추가하면 됨