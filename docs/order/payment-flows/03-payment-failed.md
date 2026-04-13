# 결제 실패

## 흐름

```
Payment → Kafka: payment.failed { orderId }
  └─ OrderService (PaymentFailedConsumer)
       Order.status: PENDING → FAILED
       ├─ Kafka: order.reservation.released { productUserId }
       │    └─ ProductService (OrderReservationReleasedConsumer)
       │         ProductUser.status: RESERVED → RELEASED
       │         Schedule 잔여 인원 복구
       └─ Kafka: order.deposit.refund-requested { orderId, userId, depositAmount }
            └─ UserService: 예치금 복구
               (depositAmount == 0 이면 발행 안 함)
```

## Order 처리 — `PaymentFailedConsumer`

| 항목 | 값 |
|------|----|
| 수신 토픽 | `payment.failed` |
| groupId | `order-service` |
| 페이로드 | `{ "orderId": "UUID" }` |

1. `payment.failed` 수신
2. `updatePaymentStatus(orderId, FAILED)` 호출
3. `order.failPayment()` → `PENDING → FAILED`
4. `order.reservation.released` 발행
   ```json
   { "productUserId": "..." }
   ```
5. `order.deposit.refund-requested` 발행 (예치금 > 0인 경우만)
   ```json
   { "orderId": "...", "userId": "...", "depositAmount": 5000 }
   ```

## Product 처리 — `OrderReservationReleasedConsumer`

| 항목 | 값 |
|------|----|
| 수신 토픽 | `order.reservation.released` |
| groupId | `product-service` |
| 페이로드 | `{ "productUserId": "UUID" }` |

1. `order.reservation.released` 수신
2. `releaseReservation(productUserId)` 호출
3. **중복 수신 방지:** 이미 `RELEASED` 또는 `REFUNDED`이면 무시
4. Schedule 재고 복구 (`restoreCapacity`)
5. `ProductUser.status` → `RESERVED → RELEASED`
6. Schedule 상태 갱신: 잔여 인원 > 0이면 `FULL → AVAILABLE`

## 결과 상태

| 도메인 | 상태 |
|--------|------|
| `Order.status` | `FAILED` |
| `ProductUser.status` | `RELEASED` |
| Schedule 잔여 인원 | 복구됨 |