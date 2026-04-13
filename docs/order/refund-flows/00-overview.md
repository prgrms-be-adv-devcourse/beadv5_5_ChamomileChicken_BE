# 환불

> 결제 완료(PAID) 이후 환불이 처리된 경우.  
> Order와 Product가 Payment로부터 동일 토픽을 **독립적으로** 수신.

## 흐름

```
Payment → Kafka: payment.refund.completed { orderId, productUserId }
  ├─ OrderService (PaymentRefundCompletedConsumer)
  │    Order.status: PAID → REFUNDED
  └─ ProductService (PaymentRefundCompletedConsumer)
       ProductUser.status: CONFIRMED → REFUNDED
       Schedule 잔여 인원 복구
```

## Order 처리 — `PaymentRefundCompletedConsumer`

1. `payment.refund.completed` 수신
2. `orderUseCase.refund(orderId)` 호출
3. `order.refund()` → `PAID → REFUNDED`

## Product 처리 — `PaymentRefundCompletedConsumer`

1. `payment.refund.completed` 수신 (Order와 별개로 독립 수신)
2. `refundReservation(productUserId)` 호출
3. **중복 수신 방지:** 이미 `REFUNDED`이면 무시
4. Schedule 재고 복구 (`restoreCapacity`)
5. `ProductUser.status` → `CONFIRMED → REFUNDED`
6. Schedule 상태 갱신: 잔여 인원 > 0이면 `FULL → AVAILABLE`

## 결과 상태

| 도메인 | 상태 |
|--------|------|
| `Order.status` | `REFUNDED` |
| `ProductUser.status` | `REFUNDED` |
| Schedule 잔여 인원 | 복구됨 |