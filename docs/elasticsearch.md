# Elasticsearch 도입 — 상품 검색 고도화

## 배경 및 목적

데브코스 파이널 프로젝트 요구사항으로 Elasticsearch 도입이 포함되어 있다.
기존 상품 검색은 PostgreSQL JPA 쿼리(`LIKE %keyword%`)로 구현되어 있었으며, 다음 한계가 있었다.

| 항목 | 기존 (JPA LIKE) | 현재 (Elasticsearch) |
|------|----------------|----------------------|
| 검색 방식 | `title` 컬럼 부분 일치만 지원 | `title` + `description` 전문 검색 |
| 한국어 지원 | 형태소 분석 없음 ("노트북"으로 "노트북 거치대" 검색 불가) | nori 형태소 분석기 적용 |
| 오타 허용 | 오타 시 검색 결과 없음 | `title`에 `fuzziness: AUTO + prefixLength: 1` 적용 |
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
| `sellerId` | keyword | — | 판매자 UUID — 토크나이즈 불필요 |
| `sellerName` | keyword | — | 판매자 이름 (비정규화 저장) — 필터용 |
| `title` | text | nori | 상품명 — 형태소 분석 |
| `description` | text | nori | 상품 설명 — 형태소 분석 |
| `status` | keyword | — | ENABLE / DISABLE |
| `price` | double | — | 가격 |
| `maxCapacity` | integer | — | 최대 수용 인원 |
| `thumbnailPath` | keyword | — | 썸네일 경로 — URL, 토크나이즈 불필요 |
| `deleted` | boolean | — | `deleteDt != null` 여부 (소프트 삭제 필터용) |
| `regDt` | date (`date_hour_minute_second`) | — | 등록일시 — 명시적 타입 지정으로 정렬 보장 |

> **비정규화 전략:** `sellerName`을 ES에 직접 저장해 검색 시 user 서비스 REST 호출을 제거한다.
> 판매자 이름이 변경되면 user-service가 `user.events` 토픽에 `USER_NAME_CHANGED` 이벤트를 발행하고,
> product-service의 `UserEventsConsumer`가 해당 sellerId의 모든 ES 문서를 일괄 업데이트한다.

> **@Field 명시 이유:** Spring Data ES는 `@Field` 없는 String 필드를 `text + keyword` 멀티필드로 기본 매핑한다.
> UUID, URL, boolean, date 등 토크나이즈가 필요 없거나 정렬/필터 전용인 필드는 반드시 명시적 타입을 지정해야 한다.

### 인덱스 설정 (`product-settings.json`)

```json
{
  "index": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "refresh_interval": "1s",
    "max_result_window": 10000
  },
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

| 설정 | 값 | 이유 |
|------|---|------|
| `number_of_shards` | 1 | 소규모 데이터셋, 생성 후 변경 불가이므로 명시 |
| `number_of_replicas` | 0 | 단일 노드 dev 환경 — 복제본 설정 시 `yellow` 상태 방지 |
| `refresh_interval` | 1s | 기본값 명시 — 근실시간 검색 반영 |
| `max_result_window` | 10000 | 무한 페이징 방지 |

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

infrastructure/outbox/
  OutboxEvent.java                      ← product_outbox_events 테이블 엔티티
  OutboxRepository.java                 ← FOR UPDATE SKIP LOCKED 폴링 쿼리
  OutboxService.java                    ← 상태 전환 (PENDING → SENDING → PUBLISHED/FAILED)
  OutboxPublisher.java                  ← @Scheduled(fixedDelay=1000) relay

resources/elasticsearch/
  product-settings.json                 ← nori 분석기 + 인덱스 설정
```

> Spring Data Elasticsearch 인터페이스(`ElasticsearchRepository`) 대신 `ElasticsearchOperations`를 직접 사용한다.
> `CriteriaQuery` or() 체이닝의 우선순위 버그를 피하기 위해 `searchByKeyword`는 `NativeQuery` bool 쿼리로 구현했다.

**fuzziness 적용 (`searchByKeyword`):**

```java
// title: fuzziness AUTO + prefixLength(1) — 오타 교정, 첫 글자 일치 필수로 후보 수 제한
.should(s -> s.match(m -> m.field("title").query(keyword).fuzziness("AUTO").prefixLength(1)))
// description: 일반 match — nori 형태소 분석으로 충분, fuzzy 비용 제거
.should(s -> s.match(m -> m.field("description").query(keyword)))
```

> **description에 fuzziness를 적용하지 않는 이유:** description은 긴 텍스트로 nori 분석 후 토큰 수가 많다.
> fuzzy 연산은 각 토큰에 편집 거리를 계산하므로 description에 적용 시 비용이 크고 실효성은 낮다.
> nori 형태소 분석기가 이미 한국어 부분 매칭을 처리하므로 fuzzy 없이도 충분하다.

`fuzziness: AUTO` 기준 — 완성형 음절 블록(Unicode code point) 단위 편집 거리 계산:

| 토큰 길이 | 허용 편집 거리 | 예시 |
|-----------|---------------|------|
| 1~2글자 | 0 (exact) | "향수" → "향소" 매칭 안 됨 |
| 3~5글자 | 1 | "도재기" → "도자기" 매칭됨 |
| 6글자 이상 | 2 | — |

> nori가 형태소 분석 후 짧은 토큰(1~2글자)으로 분리되면 해당 토큰에는 fuzziness가 적용되지 않는다.

---

## ES 색인 신뢰성 설계

### 배경 — 기존 방식의 문제

초기 구현은 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`로 ES 색인 이벤트를 발행했다.
이 방식에는 두 가지 근본적인 문제가 있다.

| 문제 | 원인 | 결과 |
|------|------|------|
| 이벤트 유실 | Kafka 장애 시 catch에서 로그만 남기고 버림 | DB 커밋 후 ES 미반영 — 영구 불일치 |
| 순서 역전 | Kafka key 없이 발행 → RR 파티셔닝 | SAVE/DELETE가 다른 파티션 → 순서 보장 불가 |

### 1. Producer — Outbox 패턴

이벤트를 Kafka에 직접 발행하지 않고, **같은 DB 트랜잭션 안에** `product_outbox_events` 테이블에 먼저 저장한다.
별도 스케줄러(`OutboxPublisher`)가 1초 간격으로 폴링하여 Kafka에 발행하고, 성공하면 PUBLISHED로 표시한다.

```
[ProductService - @Transactional]
  DB 저장 (product) + outbox 저장 (PENDING) → 원자적 커밋

[OutboxPublisher - @Scheduled(fixedDelay=1000)]
  product_outbox_events 폴링 (FOR UPDATE SKIP LOCKED)
  → PENDING 이벤트 SENDING으로 전환
  → kafkaTemplate.send(record).get()  ← 동기 전송
  → 성공: PUBLISHED
  → 실패: retryCount++ → PENDING (재시도)
  → retryCount >= 5: FAILED (무한 재시도 방지)
```

Kafka가 다운되어 있어도 outbox에 PENDING 상태로 남아 복구 후 반드시 발행된다.

### 2. Producer — 파티션 순서 보장

`productId`를 Kafka 메시지 key로 사용한다.
Kafka는 동일 key를 동일 파티션으로 라우팅하므로, 같은 상품의 SAVE/DELETE는 항상 같은 파티션 내에서 순서가 보장된다.

```java
ProducerRecord<String, String> record = new ProducerRecord<>(
    event.getEventType().getTopic(),
    event.getAggregateId(),  // productId → 파티션 결정 key
    event.getPayload()
);
```

### 3. Producer — Worker 중복 처리 방지

`OutboxPublisher` 스케줄러가 여러 인스턴스에서 동시에 실행될 경우를 대비해
outbox 조회 쿼리에 `FOR UPDATE SKIP LOCKED`를 적용한다.
한 인스턴스가 row를 점유하면 다른 인스턴스는 해당 row를 건너뛰고 다음 row를 처리한다.

```sql
SELECT * FROM product_outbox_events
WHERE status = 'PENDING'
   OR (status = 'SENDING' AND last_attempt_at < :threshold)
ORDER BY reg_dt ASC
LIMIT 100
FOR UPDATE SKIP LOCKED
```

`SENDING + last_attempt_at < threshold(5분)` 조건은 인스턴스가 죽어서 SENDING 상태에서 멈춘 이벤트를 재처리하기 위함이다.

### 4. Consumer — DLQ (Dead Letter Queue)

Consumer에서 처리 불가능한 메시지(파싱 오류, ES 장애 등)가 무한히 재시도되면 이후 메시지 처리가 지연된다.
`KafkaConsumerConfig`의 `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`로 이를 방지한다.

```
product.es.index 처리 실패
  → FixedBackOff: 1초 간격 3회 재시도
  → 3회 초과 시 → product.es.index.dlq 로 자동 라우팅
```

### 5. Outbox 상태 전이

```
PENDING   → SENDING   (OutboxPublisher가 처리 시작)
SENDING   → PUBLISHED (Kafka 발행 성공)
SENDING   → PENDING   (발행 실패, retryCount < 5 → 재시도 대기)
SENDING   → FAILED    (발행 실패, retryCount >= 5 → 더 이상 재시도 안 함)
SENDING   → PENDING   (인스턴스 장애 후 threshold 초과 시 재처리)
```

### 6. Consumer — Bulk 색인 부분 실패 처리

`elasticsearchOperations.bulkIndex()`는 HTTP 200을 반환해도 내부 일부 문서가 실패할 수 있다.
`BulkFailureException`을 catch해 실패 문서 ID를 로깅한다.

```java
try {
    elasticsearchOperations.bulkIndex(queries, ProductDocument.class);
} catch (BulkFailureException e) {
    log.error("ES bulk 색인 일부 실패. 실패 문서: {}", e.getFailedDocuments());
}
```

---

## ProductService — ES 색인 흐름

```java
// 검색 시 (변경 없음)
ProductSearchRepository.searchByKeyword(keyword, pageable)
ProductSearchRepository.findAllEnabled(pageable)

// CRUD 시 (Outbox 패턴으로 변경)
// 같은 @Transactional 안에서 outbox 저장 → OutboxPublisher가 Kafka 발행
outboxRepository.save(OutboxEvent.create("PRODUCT", productId, EsEventType.ES_SAVE, payload))
outboxRepository.save(OutboxEvent.create("PRODUCT", productId, EsEventType.ES_DELETE, payload))
```

**Kafka 기반 ES 색인 흐름 (Outbox 패턴 적용 후):**

```
[ProductService - create/update/delete]
  @Transactional 내에서:
    DB 저장 (product)
    outboxRepository.save(OutboxEvent - PENDING)  ← 같은 트랜잭션, 원자적

[OutboxPublisher - 1초 주기]
  product_outbox_events 폴링
  → kafkaTemplate.send(topic, productId, payload).get()

[ProductEsKafkaConsumer]
  메시지 수신
  → SAVE: productSearchRepository.save(document)
  → DELETE: productSearchRepository.deleteById(productId)
  → 실패 시 DLQ(product.es.index.dlq) 라우팅
```

> **메시지 페이로드 전략:** `ProductDocument` 전체를 Kafka 메시지에 담는다.
> ProductService는 create/update 시점에 이미 product + sellerName을 보유하고 있으므로
> Consumer에서 DB / user 서비스 재조회 없이 바로 ES 색인 가능.
> PostgreSQL이 source of truth이므로 ES 색인 실패 시 `migrateToEs`로 복구 가능.

---

## 초기 마이그레이션

ES 도입 이전에 PostgreSQL에 이미 존재하는 상품 데이터를 ES에 일괄 색인하기 위한 엔드포인트를 제공한다.

**엔드포인트:** `POST /api/v1/products/es-migrate`

- 인증 없이 호출 가능 (internal 엔드포인트, `SecurityConfig`에서 `permitAll` 처리)
- 삭제되지 않은 전체 상품을 ES에 벌크 색인 후 색인 건수 반환
- **일회성 작업** — ES 컨테이너 최초 구동 후 한 번만 호출
- 벌크 실패 시 `BulkFailureException`으로 실패 문서 ID 로깅 후 계속 진행

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
- [x] `ProductDocument` 클래스 생성 (ES 인덱스 매핑 — 전 필드 `@Field` 명시)
- [x] `ProductSearchRepository` 도메인 인터페이스 정의
- [x] `ProductSearchRepositoryAdapter` — `ElasticsearchOperations` 기반 구현
- [x] nori 분석기 + 인덱스 설정 파일 (`product-settings.json`)

### 서비스 연동
- [x] `build.gradle`에 `spring-boot-starter-data-elasticsearch` 추가
- [x] `application-dev.yml`에 ES 연결 설정 추가
- [x] `ProductService.searchAll()` — ES 검색으로 교체 (user 서비스 REST 호출 제거)
- [x] `ProductResponseDto.from(ProductDocument)` 팩토리 메서드 추가
- [x] `SearchProductResponseDto.fromEs()` 오버로드 추가
- [x] `SecurityConfig` — es-migrate 엔드포인트 permitAll 추가

### Kafka 기반 ES 색인 — Outbox 패턴
- [x] `OutboxStatus` — PENDING / SENDING / PUBLISHED / FAILED
- [x] `EsEventType` — ES_SAVE / ES_DELETE (topic: product.es.index)
- [x] `OutboxEvent` — `product_outbox_events` JPA 엔티티
- [x] `OutboxRepository` — `FOR UPDATE SKIP LOCKED` 폴링 쿼리
- [x] `OutboxService` — 상태 전환 메서드
- [x] `OutboxPublisher` — `@Scheduled(fixedDelay=1000)`, 동기 Kafka 발행, 재시도/FAILED 처리
- [x] `ProductService.create/update/delete()` — `publishEvent` 제거 → `outboxRepository.save()`
- [x] `ProductApplication` — `@EnableScheduling` 추가
- [x] `ProductEsKafkaConsumer` — 메시지 수신 후 ES 색인/삭제 처리
- [x] `ProductEsIndexMessage` — `resolveProductId()` 헬퍼 추가 (Kafka key 통일)
- [x] 삭제: `ProductEsKafkaProducer`, `ProductEsSaveEvent`, `ProductEsDeleteEvent`

### 검색 고도화
- [x] `searchByKeyword()` — `title`에 `fuzziness: AUTO + prefixLength(1)`, `description`은 일반 match
- [x] `KafkaConsumerConfig` — `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` (DLQ: `product.es.index.dlq`)
- [x] `KafkaTopicConfig` — `PRODUCT_ES_TOPIC` 상수 + `NewTopic` 빈 (토픽명 단일 관리, `EsEventType` / `ProductEsKafkaConsumer` 참조)

### 판매자 이름 변경 시 ES 동기화
- [x] `UserEventsPublisher` (user-service) — `updateMyInfo`에서 이름 변경 감지 시 `user.events`에 `USER_NAME_CHANGED` 발행
- [x] `UserEventsConsumer` (product-service) — `user.events` 수신 후 `updateSellerNameForAll` 호출
- [x] `ProductSearchRepository.updateSellerNameForAll()` — 인터페이스 추가
- [x] `ProductSearchRepositoryAdapter.updateSellerNameForAll()` — sellerId로 ES 문서 전체 조회 후 sellerName bulkIndex
- [x] `ProductDocument` — `@Builder(toBuilder = true)` 추가 (sellerName 교체 시 불변 복사)

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
- **Outbox 패턴**으로 DB-Kafka 원자성 보장 — DB 커밋과 outbox 저장이 하나의 트랜잭션
- Consumer 실패 시 `FixedBackOff` 3회 재시도 → 초과 시 DLQ(`product.es.index.dlq`) 라우팅
- ES 장애 시 `migrateToEs` API로 전체 재색인 가능

### 검색 범위 확장 가능성
- 현재: `title` (fuzziness) + `description` (일반 match)
- 추후: 태그, 카테고리 등 필드 추가 시 `ProductDocument`에 필드만 추가하면 됨
