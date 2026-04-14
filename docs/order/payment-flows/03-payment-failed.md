# 결제 실패

> PG 승인 요청이 거절된 경우.

## 흐름

```
PaymentService: PG 승인 실패
  Payment.status: READY → FAILED
  └─ Outbox 저장 (PAYMENT_FAILED)
       └─ OutboxPublisher → Kafka: payment.events (PAYMENT_FAILED)
            └─ OrderService (PaymentEventsConsumer)
                 Order.status: PENDING → FAILED
                 ├─ Kafka: order.events (ORDER_RESERVATION_RELEASED)
                 │    └─ ProductService (OrderEventsConsumer)
                 │         ProductUser.status: RESERVED → RELEASED
                 │         Schedule 잔여 인원 복구
                 └─ (depositAmount > 0) Kafka: order.events (ORDER_DEPOSIT_REFUND_REQUESTED)
                      └─ UserService (OrderEventsConsumer)
                           예치금 복구
```

---

## 각 모듈 처리

### Payment — `PaymentService.confirm()` 예외 처리

1. PG 승인 실패 시 `Payment.status → FAILED`
2. Outbox 저장 (`PAYMENT_FAILED`)
3. `PaymentException` throw → 클라이언트에 에러 반환

Kafka 이벤트:
```
Topic:  payment.events
Header: eventType = PAYMENT_FAILED
Body:   { "eventId": "UUID", "paymentId": "UUID", "orderId": "UUID", "depositAmount": 5000 }
```

### Order — `PaymentEventsConsumer` → `OrderService.updatePaymentStatus(FAILED)`

1. `PAYMENT_FAILED` 수신
2. 이미 `FAILED` 또는 `EXPIRED`이면 무시
3. `order.failPayment()` → `PENDING → FAILED`
4. `ORDER_RESERVATION_RELEASED` 발행
5. `depositAmount > 0`이면 `ORDER_DEPOSIT_REFUND_REQUESTED` 발행

Kafka 이벤트:
```
Topic:  order.events
Header: eventType = ORDER_RESERVATION_RELEASED
Body:   { "eventId": "UUID", "productUserId": "UUID" }

Topic:  order.events
Header: eventType = ORDER_DEPOSIT_REFUND_REQUESTED
Body:   { "eventId": "UUID", "orderId": "UUID", "userId": "UUID", "depositAmount": 5000 }
```

### Product — `OrderEventsConsumer` → `ScheduleService.restoringInventory(RELEASED)`

1. `ORDER_RESERVATION_RELEASED` 수신
2. 이미 `RELEASED`이면 무시
3. Schedule 잔여 인원 복구 (`restoreCapacity`)
4. `ProductUser.status → RELEASED`

### User (Deposit) — `OrderEventsConsumer` → `DepositService.refund()`

1. `ORDER_DEPOSIT_REFUND_REQUESTED` 수신
2. `user.chargeDeposit(depositAmount)` — 잔액 복구
3. `DepositHistory` 저장 (type: `REFUND`)

---

## 결과 상태

| 도메인 | 상태 |
|--------|------|
| `Payment.status` | `FAILED` |
| `Order.status` | `FAILED` |
| `ProductUser.status` | `RELEASED` |
| Schedule 잔여 인원 | 복구됨 |
| 예치금 잔액 | 복구됨 (사용한 경우) |