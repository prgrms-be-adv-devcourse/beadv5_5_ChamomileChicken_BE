# 04. Order Service (9005)

> 인증 범례 및 공통 응답 형식 → [API_SPEC.md](../API_SPEC.md)

---

## 주문 (Order)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/orders` | ✅ JWT | 주문 생성 |
| GET | `/api/v1/orders` | ✅ JWT | 내 주문 목록 |
| GET | `/api/v1/orders/{orderId}` | ✅ JWT | 주문 상세 조회 |
| PATCH | `/api/v1/orders/{orderId}` | ✅ JWT | 주문 취소 |

---

### POST `/api/v1/orders`

**Request Body**
```json
{
  "productId": "uuid",
  "productScheduleId": "uuid",
  "quantity": 2,
  "depositAmount": 10000
}
```

**Response** `201 Created`
```json
{
  "data": {
    "id": "uuid",
    "buyerId": "uuid",
    "productId": "uuid",
    "productScheduleId": "uuid",
    "productUserId": "uuid",
    "quantity": 2,
    "totalAmount": 40000,
    "paymentAmount": 30000,
    "status": "PENDING | PAID | CANCELLED"
  }
}
```

---

### GET `/api/v1/orders`

**Query Parameters**: `status` (OrderStatus, 선택)

**Response** `200 OK`
```json
{
  "data": [ ]
}
```

---

## 내부 API (Internal)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/v1/orders/{orderId}/payment-amount/validate` | ⚙️ Internal | 결제 금액 검증 |
| PUT | `/api/v1/orders/{orderId}/payment-status` | ⚙️ Internal | 결제 상태 업데이트 |
| POST | `/api/v1/orders/bulk` | ⚙️ Internal | 주문 다건 조회 (정산용) |

---

### GET `/api/v1/orders/{orderId}/payment-amount/validate`

**Query Parameters**: `amount` (BigDecimal)

**Response** `200 OK`
```json
{
  "data": {
    "valid": true
  }
}
```

---

### PUT `/api/v1/orders/{orderId}/payment-status`

**Request Body**
```json
{
  "paymentStatus": "SUCCESS | FAILURE"
}
```

**Response** `204 No Content`