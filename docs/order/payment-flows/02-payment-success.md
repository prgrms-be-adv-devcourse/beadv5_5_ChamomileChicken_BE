# 결제 성공

## 흐름

```
Payment → Kafka: payment.completed { orderId }
  └─ OrderService (PaymentCompletedConsumer)
       Order.status: PENDING → PAID
       └─ Kafka: order.reservation.confirmed { productUserId }
            └─ ProductService (OrderReservationConfirmedConsumer)
                 ProductUser.status: RESERVED → CONFIRMED
```

## Order 처리 — `PaymentCompletedConsumer`

| 항목 | 값 |
|------|----|
| 수신 토픽 | `payment.completed` |
| groupId | `order-service` |
| 페이로드 | `{ "orderId": "UUID" }` |

1. `payment.completed` 수신
2. `updatePaymentStatus(orderId, SUCCESS)` 호출
3. `order.pay()` → `PENDING → PAID`
4. `order.reservation.confirmed` 발행
   ```json
   { "productUserId": "..." }
   ```

## Product 처리 — `OrderReservationConfirmedConsumer`

| 항목 | 값 |
|------|----|
| 수신 토픽 | `order.reservation.confirmed` |
| groupId | `product-service` |
| 페이로드 | `{ "productUserId": "UUID" }` |

1. `order.reservation.confirmed` 수신
2. `confirmReservation(productUserId)` 호출
3. **중복 수신 방지:** 이미 `CONFIRMED`이면 무시
4. `ProductUser.status` → `RESERVED → CONFIRMED`

## 결과 상태

| 도메인 | 상태 |
|--------|------|
| `Order.status` | `PAID` |
| `ProductUser.status` | `CONFIRMED` |
| Schedule 잔여 인원 | 차감 유지 |