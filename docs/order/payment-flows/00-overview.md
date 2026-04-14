# 이벤트 흐름 개요

> **현재 구현 범위**: 01~04 (주문 생성, 결제 성공/실패, 결제 미완료)  
> 05 (환불)은 미구현

---

## 시나리오 목록

| 파일 | 시나리오 | 상태 |
|------|---------|------|
| [01-order-create.md](01-order-create.md) | 주문 생성 | ✅ 구현 완료 |
| [02-payment-success.md](02-payment-success.md) | 결제 성공 | ✅ 구현 완료 |
| [03-payment-failed.md](03-payment-failed.md) | 결제 실패 (PG 거절) | ✅ 구현 완료 |
| [04-payment-not-completed.md](04-payment-not-completed.md) | 결제 미완료 (취소 / 만료) | ✅ 구현 완료 |
| [05-refund.md](05-refund.md) | 환불 | ⏳ 미구현 |

---

## 담당자별 구현 가이드

### Payment 담당자가 구현해야 할 것

아래 Kafka 이벤트를 **발행(produce)**하면 됩니다.

| 토픽 | 발행 시점 | 페이로드 | Order 결과 상태 |
|------|-----------|----------|----------------|
| `payment.completed` | PG 결제 승인 성공 | `{ "orderId": "UUID" }` | `PAID` |
| `payment.failed` | PG 결제 승인 실패 | `{ "orderId": "UUID" }` | `FAILED` |

> Order 서비스가 이미 이 토픽들을 수신하고 있습니다.  
> 결제 취소 / 앱 이탈은 Order 서비스 스케줄러가 직접 처리하므로 Payment 담당자 구현 불필요.

---

### 예치금(UserService) 담당자가 구현해야 할 것

**① HTTP 엔드포인트 (Order → User 동기 호출)**

```
POST /api/v1/deposits/use
Content-Type: application/json

Request:
{
  "userId": "UUID",
  "depositAmount": 5000
}

Response:
{
  "valid": true   // 잔액 부족 시 false
}
```

주문 생성 시 Order 서비스가 호출합니다. `valid: false` 반환 시 주문이 실패됩니다.

**② Kafka 컨슈머 (Order → User 비동기)**

| 토픽 | 수신 시점 | 페이로드 | 처리 내용 |
|------|-----------|----------|-----------|
| `order.deposit.refund-requested` | 결제 실패 시 | `{ "orderId": "UUID", "userId": "UUID", "depositAmount": 5000 }` | 예치금 복구 |
| `order.expired` | 결제 미완료(만료) 시 | `{ "orderId": "UUID", "userId": "UUID", "depositAmount": 5000 }` | 예치금 복구 |

> 두 토픽 모두 컨슈머 구현이 필요합니다.

---

## Kafka 토픽 전체 목록

### Payment → Order

| 토픽 | 상황 | Order 상태 | 구현 여부 |
|------|------|-----------|----------|
| `payment.completed` | 결제 성공 | `PAID` | Payment 담당자 구현 필요 |
| `payment.failed` | 결제 실패 | `FAILED` | Payment 담당자 구현 필요 |

### Order → Product

| 토픽 | 상황 | 구현 여부 |
|------|------|----------|
| `order.reservation.confirmed` | 결제 성공 시 예약 확정 | ✅ |
| `order.reservation.released` | 결제 실패/만료 시 재고 복구 | ✅ |

### Order → User

| 토픽 | 상황 | 구현 여부 |
|------|------|----------|
| `order.deposit.refund-requested` | 결제 실패 시 예치금 복구 | User 담당자 컨슈머 구현 필요 |
| `order.expired` | 결제 미완료(만료) 시 예치금 복구 | User 담당자 컨슈머 구현 필요 |

---

## 도메인 상태값

### Order - `OrderStatus`

| 상태 | 진입 조건 |
|------|-----------|
| `PENDING` | 주문 생성 직후 |
| `PAID` | `payment.completed` 수신 |
| `FAILED` | `payment.failed` 수신 |
| `EXPIRED` | PaymentService가 타임아웃 감지 후 `payment.expired` 발행 |
| `REFUNDED` | 환불 완료 시 — ⏳ 미구현 |

### ProductUser - `ReservationStatus`

| 상태 | 진입 조건 |
|------|-----------|
| `RESERVED` | 주문 생성 시 재고 차감과 동시에 생성 |
| `CONFIRMED` | `order.reservation.confirmed` 수신 |
| `RELEASED` | `order.reservation.released` 수신 |
| `REFUNDED` | 환불 완료 시 — ⏳ 미구현 |

### Schedule - `ReservedStatus`

| 상태 | 진입 조건 |
|------|-----------|
| `AVAILABLE` | 기본값, 또는 재고 복구 후 잔여 > 0 |
| `FULL` | 잔여 인원 = 0 |
| `CLOSED` | Schedule 삭제 시 |