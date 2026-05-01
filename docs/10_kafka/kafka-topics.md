# Kafka 토픽 설계

## 설계 원칙

- 토픽은 **Producer 서비스 기준**으로 운영 (단, `settlement.events`는 order-service + admin-service 공동 발행)
- 하나의 토픽 안에서 Kafka **header `eventType`** 으로 이벤트 종류 구분 (단, `admin.product`는 JSON body의 `type` 필드로 구분)
- **토픽 생성은 Producer 서비스의 `KafkaTopicConfig`에서만** 담당 (Consumer는 토픽 생성 안 함)
- 에러 처리: 모든 서비스 `FixedBackOff(1000ms, 3회)` + `DeadLetterPublishingRecoverer` → `{topic}.dlq`

---

## 토픽 목록

| 토픽명 | Producer | Consumer | 파티션 | 토픽 생성 서비스 |
|--------|----------|----------|--------|----------------|
| `payment.events` | payment-service | order-service | 3 | payment-service |
| `order.events` | order-service | product-service, user-service, ai-service | 3 | order-service |
| `settlement.events` | order-service, admin-service | settlement-service | 3 | order-service, settlement-service |
| `product.events` | product-service | ai-service | — (자동 생성) | — |
| `user.events` | user-service | product-service | — (자동 생성) | — |
| `admin.product` | admin-service | product-service | — (자동 생성) | — |
| `product.es.index` | product-service | product-service (내부) | 3 | product-service |
| `product.es.index.dlq` | product-service (DLQ) | — | 3 | product-service |
| `payment.events.dlq` | (자동 라우팅) | — | 3 | order-service |
| `order.events.dlq` | (자동 라우팅) | — | 3 | order-service |
| `settlement.events.dlq` | (자동 라우팅) | — | 3 | settlement-service |

---

## 토픽별 이벤트 정의

### `payment.events` — payment-service 발행

**발행 방식:** Outbox 패턴 (동기 발행)
**파티션 키:** `paymentId`

| eventType | 설명 | Consumer | Group ID |
|-----------|------|----------|---------|
| `PAYMENT_COMPLETED` | 결제 승인 완료 | order-service | `order-service` |
| `PAYMENT_FAILED` | 결제 실패 (PG 거절) | order-service | `order-service` |
| `PAYMENT_EXPIRED` | 결제 미완료 만료 | order-service | `order-service` |

**페이로드:**

| eventType | 필드 |
|-----------|------|
| `PAYMENT_COMPLETED` | `eventId`, `paymentId`, `orderId`, `productId`, `totalAmount`, `occurredAt` |
| `PAYMENT_FAILED` | `eventId`, `paymentId`, `orderId`, `depositAmount` |
| `PAYMENT_EXPIRED` | `eventId`, `paymentId`, `orderId`, `depositAmount` |

---

### `order.events` — order-service 발행

**발행 방식:** Outbox 패턴 (동기 발행)
**파티션 키:** `orderId` (전체 이벤트 통일)

| eventType | 설명 | Consumer | Group ID |
|-----------|------|----------|---------|
| `ORDER_COMPLETED` | 결제 완료 → 주문 확정 | ai-service | `ai-order-activity` |
| `ORDER_RESERVATION_CONFIRMED` | 재고 차감 확정 | product-service | `product-service` |
| `ORDER_RESERVATION_RELEASED` | 재고 예약 해제 (결제 실패/만료/환불) | product-service | `product-service` |
| `ORDER_REFUNDED` | 환불 완료 → 재고 복구 | product-service | `product-service` |
| `ORDER_DEPOSIT_REFUND_REQUESTED` | 예치금 환불 요청 (결제 실패/환불 시) | user-service | `deposit-service` |
| `ORDER_EXPIRED` | 주문 만료 → 예치금 반환 | user-service | `deposit-service` |

**페이로드:**

| eventType | 필드 |
|-----------|------|
| `ORDER_COMPLETED` | `eventId`, `orderId`, `userId`, `productId` |
| `ORDER_RESERVATION_CONFIRMED` | `eventId`, `orderId`, `productUserId` |
| `ORDER_RESERVATION_RELEASED` | `eventId`, `orderId`, `productUserId` |
| `ORDER_REFUNDED` | `eventId`, `orderId`, `productUserId` |
| `ORDER_DEPOSIT_REFUND_REQUESTED` | `eventId`, `orderId`, `userId`, `depositAmount` |
| `ORDER_EXPIRED` | `eventId`, `orderId`, `userId`, `depositAmount` |

> **`ORDER_RESERVATION_CONFIRMED` / `ORDER_RESERVATION_RELEASED` / `ORDER_REFUNDED`의 `productUserId`:**
> 상품 스케줄의 실제 사용자 수(또는 예약 상태)를 변경하기 위해 사용. `userId`(구매자)와 다름.

---

### `settlement.events` — order-service + admin-service 발행

**발행 방식:** Outbox 패턴 (동기 발행)
**파티션 키:** `orderId` (`SETTLEMENT_*`), `sellerId` (`USER_SELLER_APPROVED`)

| eventType | 발행 서비스 | 설명 | Consumer | Group ID |
|-----------|------------|------|----------|---------|
| `SETTLEMENT_PAYMENT_COMPLETED` | order-service | 결제 완료 정산 타겟 적재 | settlement-service | `settlement-service` |
| `SETTLEMENT_REFUND_COMPLETED` | order-service | 환불 완료 정산 타겟 적재 | settlement-service | `settlement-service` |
| `USER_SELLER_APPROVED` | admin-service | 신규 판매자 승인 → 프로모션 등록 | settlement-service | `settlement-service` |

**페이로드:**

| eventType | 필드 |
|-----------|------|
| `SETTLEMENT_PAYMENT_COMPLETED` | `eventId`, `orderId`, `paymentId`, `sellerId`, `productId`, `settlementBaseAmount`, `occurredAt` |
| `SETTLEMENT_REFUND_COMPLETED` | `eventId`, `orderId`, `paymentId`, `refundId`, `sellerId`, `productId`, `settlementBaseAmount`, `occurredAt` |
| `USER_SELLER_APPROVED` | `eventId`, `type("SELLER_APPROVED")`, `sellerId`, `approvedAt` |

> **멱등 처리:** settlement-service는 `eventId`를 `SettlementTarget.sourceEventId`(UNIQUE)로 저장해 중복 수신 방어.

---

### `product.events` — product-service 발행

**발행 방식:** `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` (비동기)
**파티션 키:** `productId` (`PRODUCT_AI_SYNCED`, `PRODUCT_DELETED`), `userId` (`PRODUCT_VIEWED`, `PRODUCT_WISHLISTED`)

| eventType | 설명 | Consumer | Group ID |
|-----------|------|----------|---------|
| `PRODUCT_AI_SYNCED` | 상품 생성/수정 → AI 임베딩 동기화 | ai-service | `ai-product-indexer` |
| `PRODUCT_DELETED` | 상품 삭제 → AI 임베딩 제거 | ai-service | `ai-product-indexer` |
| `PRODUCT_VIEWED` | 상품 조회 행동 기록 | ai-service | `ai-product-indexer` |
| `PRODUCT_WISHLISTED` | 상품 찜 행동 기록 | ai-service | `ai-product-indexer` |

**페이로드:**

| eventType | 필드 |
|-----------|------|
| `PRODUCT_AI_SYNCED` | `eventId`, `productId`, `title`, `description`, `price`, `roadAddress`, `status`, `popularity` |
| `PRODUCT_DELETED` | `eventId`, `productId` |
| `PRODUCT_VIEWED` | `eventId`, `userId`, `productId` |
| `PRODUCT_WISHLISTED` | `eventId`, `userId`, `productId` |

> **주의:** `product.events`는 `@TransactionalEventListener + @Async` 방식으로 발행된다.
> Outbox 패턴이 아니므로 발행 실패 시 재시도 없음. AI 데이터 손실 허용 가능(추천 정확도 영향, 서비스 중단 아님).

---

### `user.events` — user-service 발행

**발행 방식:** `KafkaTemplate` 직접 발행 (비동기, `whenComplete` 콜백 로깅)
**파티션 키:** `userId`

| eventType | 설명 | Consumer | Group ID |
|-----------|------|----------|---------|
| `USER_NAME_CHANGED` | 판매자 이름 변경 → ES `sellerName` 일괄 동기화 | product-service | `product-user-sync` |

**페이로드:**
```json
{ "userId": "UUID", "newName": "string" }
```

> **발행 시점:** `UserService.updateMyInfo()`에서 이름 변경 감지 시 트랜잭션 커밋 후(`afterCommit`) 발행.

---

### `admin.product` — admin-service 발행

**발행 방식:** Outbox 패턴 (동기 발행)
**파티션 키:** `productId`
**헤더 방식 다름:** `eventType` 헤더 없음 — JSON body의 `type` 필드로 구분

| type 값 | 설명 | Consumer | Group ID |
|---------|------|----------|---------|
| `FORCE_DOWN` | 관리자 상품 강제 중지 → DB 상태 변경 + ES 삭제 | product-service | `product-admin-consumer` |

**페이로드:**
```json
{ "type": "FORCE_DOWN", "productId": "UUID" }
```

> **ES 삭제 포함:** product-service의 `AdminProductKafkaConsumer`는 DB 상태 변경 후 ES에서도 해당 문서를 삭제한다.
> ES 삭제 실패 시 예외를 그대로 던져 Kafka 재시도 정책을 따른다.

---

## 이벤트 전달 방식

```
topic   = order.events
key     = {orderId}              ← 파티션 결정 키
header  = eventType: ORDER_RESERVATION_CONFIRMED
value   = {"eventId":"...","orderId":"...","productUserId":"..."}
```

- **Consumer는 header의 `eventType`으로 분기** 후 payload를 해당 DTO로 역직렬화
- `admin.product`는 예외 — header 없음, payload의 `type` 필드로 분기

---

## 파티션 키 설계

### 원칙

같은 파티션 키를 가진 메시지만 Kafka 순서가 보장된다.
관련 이벤트가 같은 파티션에 속해야 Consumer가 라이프사이클 순서를 올바르게 처리할 수 있다.

### payment.events — `paymentId`

같은 결제의 COMPLETED/FAILED/EXPIRED가 항상 같은 파티션에서 처리됨.

### order.events — `orderId` (전체 통일)

모든 order.events 이벤트는 `orderId`를 파티션 키로 통일한다.

> **개선 배경:** 초기 구현에서 `ORDER_RESERVATION_CONFIRMED` / `ORDER_RESERVATION_RELEASED`는 `productUserId`를, 나머지는 `orderId`를 사용했다. 같은 주문의 이벤트가 다른 파티션에 흩어지면 적체 상황에서 순서 보장이 어렵다. 유지보수 명확성과 일관성을 위해 `orderId`로 통일.

### settlement.events — eventType별 분리

| eventType | 파티션 키 | 이유 |
|-----------|---------|------|
| `SETTLEMENT_PAYMENT_COMPLETED` | `orderId` | 같은 주문의 정산 이벤트 순서 보장 |
| `SETTLEMENT_REFUND_COMPLETED` | `orderId` | 같은 주문의 정산 이벤트 순서 보장 |
| `USER_SELLER_APPROVED` | `sellerId` | 같은 판매자의 승인 이벤트 순서 보장 |

### product.events — eventType별 분리

| eventType | 파티션 키 | 이유 |
|-----------|---------|------|
| `PRODUCT_AI_SYNCED` | `productId` | 같은 상품의 임베딩 업데이트 순서 보장 |
| `PRODUCT_DELETED` | `productId` | 동일 상품 SYNCED → DELETED 순서 보장 |
| `PRODUCT_VIEWED` | `userId` | 같은 사용자의 행동 이벤트 순서 보장 |
| `PRODUCT_WISHLISTED` | `userId` | 같은 사용자의 행동 이벤트 순서 보장 |

### user.events — `userId`

`USER_NAME_CHANGED`는 같은 사용자의 이름 변경이 순서대로 ES에 반영됨.

### admin.product — `productId`

같은 상품의 `FORCE_DOWN` 이벤트 중복 발행 시에도 순서가 보장됨.

---

## 에러 처리 / DLQ

모든 Consumer 서비스에 동일한 에러 핸들러가 적용된다.

```
메시지 처리 실패
  → FixedBackOff: 1초 간격 3회 재시도
  → 3회 초과 시 → {topic}.dlq 자동 라우팅
  → 오프셋 정상 커밋 → 다음 메시지 처리 계속
```

**DLQ 토픽 목록:**

| 원본 토픽 | DLQ 토픽 | 생성 서비스 |
|----------|---------|------------|
| `payment.events` | `payment.events.dlq` | order-service |
| `order.events` | `order.events.dlq` | order-service |
| `settlement.events` | `settlement.events.dlq` | settlement-service |
| `product.events` | `product.events.dlq` | ai-service (동적 생성) |
| `product.es.index` | `product.es.index.dlq` | product-service |

---

## Outbox 패턴 적용 현황

| 서비스 | 발행 방식 | 대상 토픽 |
|--------|----------|----------|
| payment-service | Outbox (`payment_outbox_events`) | `payment.events` |
| order-service | Outbox (`order_outbox_events`) | `order.events`, `settlement.events` |
| admin-service | Outbox (`admin_outbox_events`) | `admin.product`, `settlement.events` |
| product-service | `@TransactionalEventListener + @Async` | `product.events` |
| user-service | `KafkaTemplate` 직접 발행 | `user.events` |

> **Outbox 패턴:** DB 트랜잭션과 Kafka 발행을 원자적으로 처리. DB 커밋 성공 시 outbox 테이블에 이벤트 저장 → 별도 스케줄러(`OutboxPublisher`)가 1초 간격 폴링 → Kafka 동기 발행 → PUBLISHED 상태로 갱신.

---

## 멱등성 처리

Consumer가 같은 이벤트를 중복 수신하는 at-least-once 상황에 대비해 `processed_events` 테이블(eventId PK)로 멱등성을 보장한다.

| 서비스 | 멱등성 처리 방식 |
|--------|---------------|
| order-service | `processed_events` 테이블 + 상태 가드(OrderStatus 체크) |
| user-service (deposit) | `processed_events` 테이블 |
| settlement-service | `source_event_id` UNIQUE 제약 (INSERT 중복 시 자연 방어) |
| product-service | `processed_events` 테이블 (재고 변경 핸들러) |

---

## Eventual Consistency — 결제 완료 후 즉시 환불 시도 문제

Payment 서비스가 `PAYMENT_COMPLETED`를 발행한 뒤, order-service 컨슈머가 Order 상태를 `PAID`로 바꾸기까지 짧은 지연이 존재한다.

이 지연 동안 환불 API를 호출하면 Order가 아직 `PENDING`이므로 요청이 거절된다.

이는 **Kafka 비동기 구조의 eventual consistency 특성**이다. 해결은 프론트엔드 레벨에서 처리한다:
- 결제 완료 후 Order 상태를 폴링하여 `PAID` 전환 확인 후 환불 버튼 활성화

---

## 현재 → 변경 매핑 (마이그레이션 이력)

| 기존 토픽 | 새 토픽 | eventType |
|----------|---------|-----------|
| `payment.completed` | `payment.events` | `PAYMENT_COMPLETED` |
| `payment.failed` | `payment.events` | `PAYMENT_FAILED` |
| `payment.expired` | `payment.events` | `PAYMENT_EXPIRED` |
| `order.reservation.confirmed` | `order.events` | `ORDER_RESERVATION_CONFIRMED` |
| `order.reservation.released` | `order.events` | `ORDER_RESERVATION_RELEASED` |
| `order.expired` | `order.events` | `ORDER_EXPIRED` |
| `order.deposit.refund-requested` | `order.events` | `ORDER_DEPOSIT_REFUND_REQUESTED` |
| `settlement.payment.completed` | `settlement.events` | `SETTLEMENT_PAYMENT_COMPLETED` |
| `settlement.refund.completed` | `settlement.events` | `SETTLEMENT_REFUND_COMPLETED` |
| `user.seller-approved` | `settlement.events` | `USER_SELLER_APPROVED` |
