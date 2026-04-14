# Kafka 토픽 설계

## 설계 원칙

- 토픽은 **Producer 서비스 기준**으로 1개씩 운영 (총 4개)
- 하나의 토픽 안에서 `eventType` 필드로 이벤트 종류 구분
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

---

## 토픽별 이벤트 정의

### `payment.events` (payment-service → 여러 서비스)

| eventType | 설명 | Consumer |
|-----------|------|----------|
| `PAYMENT_COMPLETED` | 결제 완료 | order-service |
| `PAYMENT_FAILED` | 결제 실패 | order-service |
| `PAYMENT_EXPIRED` | 결제 만료 | order-service |
| `PAYMENT_REFUND_COMPLETED` | 환불 완료 | order-service, product-service |

### `order.events` (order-service → 여러 서비스)

| eventType | 설명 | Consumer |
|-----------|------|----------|
| `ORDER_RESERVATION_CONFIRMED` | 주문 확정 (재고 차감 확정) | product-service |
| `ORDER_RESERVATION_RELEASED` | 주문 취소 (재고 복구) | product-service |
| `ORDER_EXPIRED` | 주문 만료 (예치금 환불 요청) | user-service |
| `ORDER_DEPOSIT_REFUND_REQUESTED` | 결제 실패 시 예치금 환불 요청 | user-service |

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

## 이벤트 페이로드 공통 구조

```json
{
  "eventId": "uuid",
  "eventType": "PAYMENT_COMPLETED",
  "occurredAt": "2026-04-14T10:00:00",
  "payload": {
    ...
  }
}
```

---

## 현재 → 변경 매핑

| 기존 토픽 | 새 토픽 | eventType |
|----------|---------|-----------|
| `payment.completed` | `payment.events` | `PAYMENT_COMPLETED` |
| `payment.failed` | `payment.events` | `PAYMENT_FAILED` |
| `payment.expired` | `payment.events` | `PAYMENT_EXPIRED` |
| `payment.refund.completed` | `payment.events` | `PAYMENT_REFUND_COMPLETED` |
| `order.reservation.confirmed` | `order.events` | `ORDER_RESERVATION_CONFIRMED` |
| `order.reservation.released` | `order.events` | `ORDER_RESERVATION_RELEASED` |
| `order.expired` | `order.events` | `ORDER_EXPIRED` |
| `order.deposit.refund-requested` | `order.events` | `ORDER_DEPOSIT_REFUND_REQUESTED` |

---

## 독립 토픽 (별도 관리)

아래 토픽은 서비스 내부 목적으로 사용되며 위 설계 원칙과 별개로 운영됨.

| 토픽명 | 용도 | 비고 |
|--------|------|------|
| `product.es.index` | product-service 내부 ES 색인 | 외부 서비스 미소비, 독립 운영 |
