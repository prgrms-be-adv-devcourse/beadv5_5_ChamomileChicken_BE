# 결제 실패

> PG 승인 요청이 거절된 경우.

## 흐름

```
PaymentService: PG 승인 실패
  Payment.status: READY → FAILED
  Outbox 저장 (PAYMENT_FAILED)
  └─ OutboxPublisher → Kafka: payment.events (PAYMENT_FAILED)
       └─ OrderService (PaymentEventsConsumer)
            eventId 중복 체크 → order.failPayment() → PENDING → FAILED
            Outbox 저장 (ORDER_RESERVATION_RELEASED)
            Outbox 저장 (ORDER_DEPOSIT_REFUND_REQUESTED) [예치금 사용 시]
            processed_events 저장
            └─ OutboxPublisher → Kafka: order.events
                 ├─ ORDER_RESERVATION_RELEASED
                 │    └─ ProductService (OrderEventsConsumer)
                 │         eventId 중복 체크 → ProductUser.status: RESERVED → RELEASED
                 │         Schedule 잔여 인원 복구, processed_events 저장
                 └─ ORDER_DEPOSIT_REFUND_REQUESTED [예치금 사용 시]
                      └─ UserService (OrderEventsConsumer)
                           eventId 중복 체크 → 예치금 복구, processed_events 저장
```

```mermaid
sequenceDiagram
    participant Payment as PaymentService
    participant PG as PG (Toss)
    participant Kafka
    participant Order as OrderService
    participant Product as ProductService
    participant UserSvc as UserService (Deposit)

    Payment->>PG: 승인 요청
    PG-->>Payment: 승인 실패
    Payment->>Payment: Payment.status: READY → FAILED
    Payment->>Payment: Outbox 저장 (PAYMENT_FAILED)
    Payment->>Kafka: payment.events (PAYMENT_FAILED)

    Kafka->>Order: PAYMENT_FAILED 수신 (PaymentEventsConsumer)
    Order->>Order: eventId 중복 체크
    Order->>Order: order.failPayment() → PENDING → FAILED
    Order->>Order: Outbox 저장 (ORDER_RESERVATION_RELEASED)
    alt depositAmount > 0
        Order->>Order: Outbox 저장 (ORDER_DEPOSIT_REFUND_REQUESTED)
    end
    Order->>Order: processed_events 저장
    Order->>Kafka: order.events (ORDER_RESERVATION_RELEASED)
    Order->>Kafka: order.events (ORDER_DEPOSIT_REFUND_REQUESTED)

    Kafka->>Product: ORDER_RESERVATION_RELEASED 수신 (OrderEventsConsumer)
    Product->>Product: eventId 중복 체크
    Product->>Product: ProductUser.status: RESERVED → RELEASED
    Product->>Product: Schedule 잔여 인원 복구
    Product->>Product: processed_events 저장

    Kafka->>UserSvc: ORDER_DEPOSIT_REFUND_REQUESTED 수신 (OrderEventsConsumer)
    UserSvc->>UserSvc: eventId 중복 체크
    UserSvc->>UserSvc: 낙관적 락으로 user.chargeDeposit() — 잔액 복구
    UserSvc->>UserSvc: DepositHistory 저장 (REFUND)
    UserSvc->>UserSvc: processed_events 저장
```

---

## 각 모듈 처리

### Payment — `PaymentService.confirm()` 예외 처리

1. PG 승인 실패
2. `PaymentConfirmHandler.onFailure()` — 하나의 트랜잭션
   - `Payment.status → FAILED`
   - Outbox 저장 (`PAYMENT_FAILED`)
3. `PaymentException` throw → 클라이언트에 에러 반환

Kafka 이벤트:
```
Topic:  payment.events
Header: eventType = PAYMENT_FAILED
Body:   { "eventId": "UUID", "orderId": "UUID", "depositAmount": 5000 }
```

### Order — `PaymentEventsConsumer` → `OrderPaymentResultHandler.onFailed()`

1. `PAYMENT_FAILED` 수신
2. `eventId` 기준 `processed_events` 조회 → 이미 존재하면 즉시 return
3. `order.getStatus() == FAILED || EXPIRED`이면 return (상태 가드)
4. 하나의 트랜잭션으로 처리:
   - `order.failPayment()` → `PENDING → FAILED`
   - Outbox 저장 (`ORDER_RESERVATION_RELEASED`)
   - `depositAmount > 0`이면 Outbox 저장 (`ORDER_DEPOSIT_REFUND_REQUESTED`)
   - `processed_events` 저장

Kafka 이벤트:
```
Topic:  order.events
Header: eventType = ORDER_RESERVATION_RELEASED
Body:   { "eventId": "UUID", "orderId": "UUID", "productUserId": "UUID" }

Topic:  order.events
Header: eventType = ORDER_DEPOSIT_REFUND_REQUESTED
Body:   { "eventId": "UUID", "orderId": "UUID", "userId": "UUID", "depositAmount": 5000 }
```

### Product — `OrderEventsConsumer` → `OrderEventHandler.handleReservationReleased()`

1. `ORDER_RESERVATION_RELEASED` 수신
2. `eventId` 기준 `processed_events` 조회 → 이미 존재하면 즉시 return
3. 하나의 트랜잭션으로 처리:
   - `scheduleUseCase.restoringInventory(RELEASED)` → Schedule 잔여 인원 복구, `ProductUser.status → RELEASED`
   - `processed_events` 저장

### User (Deposit) — `OrderEventsConsumer` → `RefundDepositService.refund()`

1. `ORDER_DEPOSIT_REFUND_REQUESTED` 수신
2. `eventId` 기준 `processed_events` 조회 → 이미 존재하면 즉시 return
3. 하나의 트랜잭션으로 처리:
   - 낙관적 락으로 `user.chargeDeposit(depositAmount)` — 잔액 복구
   - `DepositHistory` 저장 (type: `REFUND`)
   - `processed_events` 저장

---

## 결과 상태

| 도메인 | 상태 |
|--------|------|
| `Payment.status` | `FAILED` |
| `Order.status` | `FAILED` |
| `ProductUser.status` | `RELEASED` |
| Schedule 잔여 인원 | 복구됨 |
| 예치금 잔액 | 복구됨 (사용한 경우) |
