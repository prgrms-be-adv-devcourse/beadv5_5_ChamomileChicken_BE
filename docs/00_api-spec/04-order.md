# 04. Order Service (9005)

> 인증 범례 및 공통 응답 형식 → [API_SPEC.md](00-API_SPEC.md)

---

## 주문 (Order)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/orders` | ✅ JWT | 주문 생성 |
| GET | `/api/v1/orders` | ✅ JWT | 내 주문 목록 |
| GET | `/api/v1/orders/{orderId}` | ✅ JWT | 주문 상세 조회 |
| DELETE | `/api/v1/orders/{orderId}/refund` | ✅ JWT | 주문 환불 요청 |
| GET | `/api/v1/orders/{orderId}/refund-info` | ✅ JWT | 주문 환불 정보 조회 |

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
| POST | `/api/v1/orders/bulk` | ⚙️ Internal | 주문 다건 조회 (정산용) |

---

### DELETE `/api/v1/orders/{orderId}/refund`

주문 환불을 요청합니다. 환불 처리 후 payment 서비스를 통해 실제 환불이 진행됩니다.

**Response** `204 No Content`

---

### GET `/api/v1/orders/{orderId}/refund-info`

환불 완료된 주문의 클래스 시작일, 환불 처리일, 환불 비율을 조회합니다.

**Response** `200 OK`
```json
{
  "data": {
    "orderId": "uuid",
    "classStartDate": "2024-06-01",
    "refundProcessedAt": "2024-05-20T10:00:00",
    "refundRate": 0.5,
    "paymentRefundAmount": 15000,
    "depositRefundAmount": 5000,
    "totalRefundAmount": 20000
  }
}
```

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
