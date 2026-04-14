# 결제 흐름 개요

---

## 시나리오 목록

| 파일 | 시나리오 | 상태 |
|------|---------|------|
| [01-order-create.md](01-order-create.md) | 주문 생성 | ✅ |
| [02-payment-success.md](02-payment-success.md) | 결제 성공 | ✅ |
| [03-payment-failed.md](03-payment-failed.md) | 결제 실패 (PG 거절) | ✅ |
| [04-payment-not-completed.md](04-payment-not-completed.md) | 결제 미완료 (만료) | ✅ |

---

## Kafka 토픽 구조

서비스마다 토픽 1개, `eventType` 헤더로 이벤트 종류 구분.

| 토픽 | Producer | eventType 목록 |
|------|----------|----------------|
| `payment.events` | payment-service | `PAYMENT_COMPLETED`, `PAYMENT_FAILED`, `PAYMENT_EXPIRED` |
| `order.events` | order-service | `ORDER_RESERVATION_CONFIRMED`, `ORDER_RESERVATION_RELEASED`, `ORDER_DEPOSIT_REFUND_REQUESTED`, `ORDER_EXPIRED` |

### 이벤트 공통 페이로드 규칙

모든 이벤트 바디에 `eventId` (UUID) 포함, `eventType`은 Kafka 헤더에 설정.

```
Header: eventType = PAYMENT_COMPLETED
Body:   { "eventId": "UUID", ... }
```

---

## 서비스별 Consumer

| 서비스 | 구독 토픽 | Consumer 클래스 | groupId |
|--------|----------|----------------|---------|
| order-service | `payment.events` | `PaymentEventsConsumer` | `order-service` |
| product-service | `order.events` | `OrderEventsConsumer` | `product-service` |
| deposit-service (user) | `order.events` | `OrderEventsConsumer` | `deposit-service` |

---

## 도메인 상태값

### Order — `OrderStatus`

| 상태 | 진입 조건 |
|------|-----------|
| `PENDING` | 주문 생성 직후 |
| `PAID` | `PAYMENT_COMPLETED` 수신 |
| `FAILED` | `PAYMENT_FAILED` 수신 |
| `EXPIRED` | `PAYMENT_EXPIRED` 수신 |

### ProductUser — `ReservationStatus`

| 상태 | 진입 조건 |
|------|-----------|
| `RESERVED` | 주문 생성 시 재고 차감과 동시에 생성 |
| `CONFIRMED` | `ORDER_RESERVATION_CONFIRMED` 수신 |
| `RELEASED` | `ORDER_RESERVATION_RELEASED` 수신 |

### Schedule — `ReservedStatus`

| 상태 | 진입 조건 |
|------|-----------|
| `AVAILABLE` | 기본값, 또는 재고 복구 후 잔여 > 0 |
| `FULL` | 잔여 인원 = 0 |
| `CLOSED` | Schedule 삭제 시 |