# 주문 생성

## 흐름

```
User → OrderService: POST /orders
OrderService → ProductService: HTTP 재고 차감 + 가격 검증
ProductService → OrderService: 응답 (price, productUserId)
OrderService → UserService: HTTP 예치금 차감
  └─ 실패 시 → Kafka: order.reservation.released → ProductService: 재고 복구
UserService → OrderService: 응답 (성공/실패)
OrderService → User: orderId 반환
```

## OrderService 처리 순서

1. **요청 검증**
   - `quantity > 0`
   - `depositAmount >= 0`
   - `productPrice >= 0`

2. **ProductService HTTP 호출** — 재고 차감 + 가격 검증
   ```
   요청: productScheduleId, userId, quantity, price
   응답: { price, quantity, valid, productUserId }
   ```
   - Product 내부: 클라이언트가 보낸 `price`와 실제 상품 가격 비교
   - 가격 일치 시 재고 차감 → `ProductUser` 생성 (`valid: "OK"`)
   - 가격 불일치: `valid: "PRICE_MISMATCH"` / 재고 부족: `valid: "OUT_OF_STOCK"`

3. **총 금액 계산** — `totalAmount = price × quantity`

4. **UserService HTTP 호출** — 예치금 차감
   - `depositAmount > totalAmount` 이면 호출 전 예외
   - `depositAmount == 0` 이면 건너뜀
   - **차감 실패 시:** `order.reservation.released` 발행 → Product 재고 복구

5. **Order 저장** — 상태: `PENDING`

6. **orderId 반환**

## Kafka (실패 시 보상)

| 방향 | 토픽 | groupId | 페이로드 | 시점 |
|------|------|---------|---------|------|
| Order → Product | `order.reservation.released` | `product-service` | `{ "productUserId": "UUID" }` | 예치금 차감 실패 시 재고 복구 |

## 결과 상태

| 도메인 | 상태 |
|--------|------|
| `Order.status` | `PENDING` |
| `ProductUser.status` | `RESERVED` |
| Schedule 잔여 인원 | 차감됨 |