# Kafka 토픽 설계

## 설계 원칙

- 토픽은 **Producer 서비스 기준**으로 1개씩 운영 (총 4개)
- 하나의 토픽 안에서 Kafka **header `eventType`** 으로 이벤트 종류 구분
- **토픽 생성은 Producer 서비스의 `KafkaTopicConfig`에서만** 담당
- Consumer는 토픽을 생성하지 않음

---

## 토픽 명 컨벤션

```
{domain}.events
```

| 도메인 | 토픽명 | Producer |
|--------|--------|----------|
| 결제 | `payment.events` | payment-service |
| 주문 | `order.events` | order-service |
| 상품 | `product.events` | product-service |
| 예치금 | `deposit.events` | user-service |
| 정산 | `settlement.events` | order-service |

---

## 토픽별 이벤트 정의

### `payment.events` (payment-service → 여러 서비스)

| eventType | 설명 | Consumer |
|-----------|------|----------|
| `PAYMENT_COMPLETED` | 결제 완료 | order-service |
| `PAYMENT_FAILED` | 결제 실패 | order-service |
| `PAYMENT_EXPIRED` | 결제 만료 | order-service |
| `PAYMENT_REFUNDED` | 환불 완료 | order-service |

### `order.events` (order-service → 여러 서비스)

| eventType | 설명 | Consumer |
|-----------|------|----------|
| `ORDER_RESERVATION_CONFIRMED` | 주문 확정 (재고 차감 확정) | product-service |
| `ORDER_RESERVATION_RELEASED` | 주문 취소 (재고 복구) | product-service |
| `ORDER_EXPIRED` | 주문 만료 (예치금 환불 요청) | user-service |
| `ORDER_DEPOSIT_REFUND_REQUESTED` | 결제 실패/환불 시 예치금 환불 요청 | user-service |
| `SETTLEMENT_PAYMENT_COMPLETED` | 결제 정산 대상 생성 | settlement-service |
| `SETTLEMENT_REFUND_COMPLETED` | 환불 정산 대상 생성 | settlement-service |

### `settlement.events` (order-service → settlement-service)

| eventType | 설명 | Consumer |
|-----------|------|----------|
| `SETTLEMENT_PAYMENT_COMPLETED` | 결제 정산 대상 적재 | settlement-service |
| `SETTLEMENT_REFUND_COMPLETED` | 환불 정산 대상 적재 | settlement-service |

### `product.events` (product-service → 여러 서비스)

| eventType | 설명 | Consumer |
|-----------|------|----------|
| `PRODUCT_STOCK_RELEASED` | 재고 복구 완료 | (추후 정의) |
| `PRODUCT_STATUS_CHANGED` | 상품 상태 변경 | (추후 정의) |

### `deposit.events` (user-service → 여러 서비스)

| eventType | 설명 | Consumer |
|-----------|------|----------|
| (추후 정의) | | |

---

## 이벤트 전달 방식

- Kafka `value`: 이벤트 JSON payload
- Kafka `header`: `eventType`
- Consumer는 header의 `eventType`으로 먼저 분기한 뒤, 해당 payload를 맞는 DTO 클래스로 역직렬화한다.

예시:

```text
topic   = payment.events
key     = p1
header  = eventType: PAYMENT_COMPLETED
value   = {"eventId":"...","paymentId":"p1","orderId":"o1","productId":"prd1","totalAmount":10000,"occurredAt":"2026-04-20T12:00:00"}
```

---

## 현재 → 변경 매핑

| 기존 토픽 | 새 토픽 | eventType |
|----------|---------|-----------|
| `payment.completed` | `payment.events` | `PAYMENT_COMPLETED` |
| `payment.failed` | `payment.events` | `PAYMENT_FAILED` |
| `payment.expired` | `payment.events` | `PAYMENT_EXPIRED` |
| `payment.refund.completed` | `payment.events` | `PAYMENT_REFUNDED` |
| `order.reservation.confirmed` | `order.events` | `ORDER_RESERVATION_CONFIRMED` |
| `order.reservation.released` | `order.events` | `ORDER_RESERVATION_RELEASED` |
| `order.expired` | `order.events` | `ORDER_EXPIRED` |
| `order.deposit.refund-requested` | `order.events` | `ORDER_DEPOSIT_REFUND_REQUESTED` |
| `settlement.payment.completed` | `settlement.events` | `SETTLEMENT_PAYMENT_COMPLETED` |
| `settlement.refund.completed` | `settlement.events` | `SETTLEMENT_REFUND_COMPLETED` |

---

## 파티션 키 설계

### 원칙

Kafka는 같은 파티션 키를 가진 메시지만 순서를 보장한다. 파티션 키가 다르면 서로 다른 파티션에 들어갈 수 있고, 순서 보장이 깨진다.

### order.events 파티션 키: `orderId`

모든 `order.events` 및 `settlement.events` 이벤트는 **`orderId`를 파티션 키**로 사용한다.

**이유**: 같은 주문의 이벤트(CONFIRMED → REFUNDED 등)가 항상 같은 파티션에 들어가야 Consumer가 라이프사이클 순서를 보장받을 수 있다.

**개선 배경**: 초기 구현에서 `ORDER_RESERVATION_CONFIRMED` / `ORDER_RESERVATION_RELEASED`는 `productUserId`를, `ORDER_REFUNDED` / `ORDER_DEPOSIT_REFUND_REQUESTED`는 `orderId`를 파티션 키로 사용했다. 같은 주문의 이벤트가 서로 다른 파티션에 흩어지면 일관성 추론이 어렵고, 컨슈머가 적체 상황에서 처리 순서를 보장받기 어렵다. 유지보수 명확성과 파티션 일관성을 위해 `orderId`로 통일했다.

**해결**: 모든 order/settlement 관련 이벤트를 `orderId`로 통일.

### payment.events 파티션 키: `paymentId`

모든 `payment.events` 이벤트는 `paymentId`를 파티션 키로 사용한다. 같은 결제의 이벤트(COMPLETED, REFUNDED 등)가 항상 같은 파티션에서 처리된다.

### Eventual Consistency — 결제 완료 후 즉시 환불 시도 문제

Payment 서비스가 결제를 완료하고 `PAYMENT_COMPLETED`를 Kafka에 발행한 뒤, Order 서비스 컨슈머가 이를 처리해 Order 상태를 `PAID`로 바꾸기까지 짧은 지연이 존재한다.

이 지연 동안 사용자가 환불 API를 호출하면 Order 상태가 아직 `PENDING`이므로 요청이 거절된다.

이는 토픽 설계의 문제가 아니라 **Kafka 비동기 구조의 eventual consistency 특성**이다. 해결은 애플리케이션/UX 레벨에서 처리한다.

- 프론트엔드는 결제 완료 후 Order 상태를 폴링하여 `PAID`로 전환된 것을 확인한 뒤 환불 버튼을 활성화한다.
- 또는 WebSocket / SSE로 Order 상태 변경 이벤트를 수신해 즉시 반영한다.

---

## 독립 토픽 (별도 관리)

아래 토픽은 서비스 내부 목적으로 사용되며 위 설계 원칙과 별개로 운영됨.

| 토픽명 | 용도 | 비고 |
|--------|------|------|
| `product.es.index` | product-service 내부 ES 색인 | 외부 서비스 미소비, 독립 운영 |
