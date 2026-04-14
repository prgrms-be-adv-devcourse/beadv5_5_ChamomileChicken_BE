# 결제 성공

## 흐름

```
User → PaymentService: POST /api/v1/payments/confirm
  │
  PaymentService
  ├─ HTTP → OrderService: 금액 검증
  ├─ PG 승인 요청
  ├─ Payment.status: READY → DONE
  └─ Outbox 저장 (PAYMENT_COMPLETED)
       └─ OutboxPublisher → Kafka: payment.events (PAYMENT_COMPLETED)
            └─ OrderService (PaymentEventsConsumer)
                 Order.status: PENDING → PAID
                 └─ Kafka: order.events (ORDER_RESERVATION_CONFIRMED)
                      └─ ProductService (OrderEventsConsumer)
                           ProductUser.status: RESERVED → CONFIRMED
```

---

## 각 모듈 처리

### Payment — `PaymentService.confirm()`

1. Payment 조회 (orderId 기준)
2. 멱등성 처리: 이미 `DONE`이면 그대로 반환
3. OrderService HTTP 호출 — 금액 검증 (`GET /api/v1/orders/{orderId}/validate?amount=...`)
4. 내부 금액 검증: `paymentAmount == request.amount`
5. PG 승인 요청 (`paymentGatewayPort.confirm`)
6. `Payment.status → DONE`, `paymentKey` 저장
7. Outbox 저장 (`PAYMENT_COMPLETED`)

Kafka 이벤트:
```
Topic:  payment.events
Header: eventType = PAYMENT_COMPLETED
Body:   { "eventId": "UUID", "paymentId": "UUID", "orderId": "UUID" }
```

### Order — `PaymentEventsConsumer` → `OrderService.updatePaymentStatus(SUCCESS)`

1. `PAYMENT_COMPLETED` 수신
2. `order.pay()` → `PENDING → PAID`
3. `ORDER_RESERVATION_CONFIRMED` 발행

Kafka 이벤트:
```
Topic:  order.events
Header: eventType = ORDER_RESERVATION_CONFIRMED
Body:   { "eventId": "UUID", "productUserId": "UUID" }
```

### Product — `OrderEventsConsumer` → `ScheduleService.reservationCompleted()`

1. `ORDER_RESERVATION_CONFIRMED` 수신
2. `ProductUser.status: RESERVED → CONFIRMED` (이미 CONFIRMED이면 무시)

---

## 결과 상태

| 도메인 | 상태 |
|--------|------|
| `Payment.status` | `DONE` |
| `Order.status` | `PAID` |
| `ProductUser.status` | `CONFIRMED` |
| Schedule 잔여 인원 | 차감 유지 |