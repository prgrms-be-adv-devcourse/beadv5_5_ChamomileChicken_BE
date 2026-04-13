# 결제 미완료 (만료)

> 사용자가 결제를 직접 취소하거나 앱을 나가버린 경우.  
> **PaymentService**가 일정 시간 경과 후 타임아웃을 감지하여 `payment.expired` 이벤트를 발행합니다.

---

## 흐름

```
PaymentService: 타임아웃 감지 → payment.expired 발행 { orderId }
  └─ OrderService (PaymentExpiredEventConsumer)
       Order.status: PENDING → EXPIRED
       ├─ Kafka: order.reservation.released { productUserId }
       │    └─ ProductService (OrderReservationReleasedConsumer)
       │         ProductUser.status: RESERVED → RELEASED
       │         Schedule 잔여 인원 복구
       └─ Kafka: order.expired { orderId, userId, depositAmount }
            └─ UserService: 예치금 복구
```

---

## Order 처리 — `PaymentExpiredEventConsumer`

| 항목 | 값 |
|------|----|
| 수신 토픽 | `payment.expired` |
| groupId | `order-service` |
| 페이로드 | `{ "orderId": "UUID" }` |

1. `payment.expired` 수신
2. **중복 처리 방지:** 이미 `EXPIRED`이면 무시
3. `order.expire()` → `PENDING → EXPIRED`
   - `PENDING`이 아닌 경우 `BusinessException(ORDER_EXPIRE_NOT_ALLOWED)` 발생
4. `order.reservation.released` 발행
   ```json
   { "productUserId": "..." }
   ```
5. `order.expired` 발행
   ```json
   { "orderId": "...", "userId": "...", "depositAmount": 5000 }
   ```

---

## Product 처리 — `OrderReservationReleasedConsumer`

| 항목 | 값 |
|------|----|
| 수신 토픽 | `order.reservation.released` |
| groupId | `product-service` |
| 페이로드 | `{ "productUserId": "UUID" }` |

결제 실패(03)와 동일. `ProductUser.status` → `RELEASED`, 재고 복구.

---

## 결과 상태

| 도메인 | 상태 |
|--------|------|
| `Order.status` | `EXPIRED` |
| `ProductUser.status` | `RELEASED` |
| Schedule 잔여 인원 | 복구됨 |