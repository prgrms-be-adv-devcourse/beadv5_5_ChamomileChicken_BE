# 05. Payment Service (9001)

> 인증 범례 및 공통 응답 형식 → [API_SPEC.md](../API_SPEC.md)

---

## 상품 결제 (Product Payment)

> ⚠️ `/api/v1/refunds`는 게이트웨이 미등록 경로

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/payments/prepare` | ✅ JWT | 결제 준비 |
| POST | `/api/v1/payments/confirm` | ✅ JWT | 결제 승인 (Toss) |
| POST | `/api/v1/refunds` | ❌ 미등록 | 환불 요청 |

---

### POST `/api/v1/payments/prepare`

**Request Body**
```json
{
  "productId": "uuid",
  "orderId": "uuid",
  "userId": "uuid",
  "productUserId": "uuid",
  "paymentMethod": "CARD | DEPOSIT | HYBRID",
  "paymentAmount": 30000
}
```

**Response** `201 Created`
```json
{
  "data": {
    "paymentId": "uuid",
    "orderId": "uuid",
    "totalAmount": 40000
  }
}
```

---

### POST `/api/v1/payments/confirm`

**Request Body**
```json
{
  "orderId": "uuid",
  "paymentKey": "toss_payment_key",
  "amount": 40000
}
```

**Response** `200 OK`
```json
{
  "data": {
    "paymentId": "uuid",
    "orderId": "uuid",
    "totalAmount": 40000
  }
}
```

---

### POST `/api/v1/refunds`

**Request Body**
```json
{
  "orderId": "uuid",
  "reason": "단순 변심"
}
```

**Response** `200 OK`
```json
{
  "data": {
    "refundId": "uuid",
    "orderId": "uuid",
    "refundAmount": 40000
  }
}
```

---

## 예치금 충전 결제 (Deposit Charge Payment)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/payments/deposits/prepare` | ✅ JWT | 충전 결제 준비 |
| POST | `/api/v1/payments/deposits/confirm` | ✅ JWT | 충전 결제 승인 |

---

### POST `/api/v1/payments/deposits/prepare`

**Request Body**
```json
{
  "userId": "uuid",
  "paymentMethod": "CARD",
  "amount": 50000
}
```

**Response** `201 Created`
```json
{
  "data": {
    "paymentId": "uuid",
    "amount": 50000
  }
}
```

---

### POST `/api/v1/payments/deposits/confirm`

**Request Body**
```json
{
  "paymentId": "uuid",
  "paymentKey": "toss_payment_key",
  "amount": 50000
}
```

**Response** `200 OK`
```json
{
  "data": {
    "paymentId": "uuid",
    "userId": "uuid",
    "amount": 50000
  }
}
```

---

## 내부 API (Internal)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/v1/payments/settlement-targets` | ⚙️ Internal | 결제 정산 대상 조회 (커서 페이징) |
| GET | `/api/v1/payments/refunds/settlement-targets` | ⚙️ Internal | 환불 정산 대상 조회 (커서 페이징) |

**Query Parameters** (공통)

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| from | LocalDateTime | ✅ | 조회 시작 시각 |
| to | LocalDateTime | ✅ | 조회 종료 시각 |
| cursorUpdatedAt | LocalDateTime | ❌ | 커서 (마지막 updatedAt) |
| cursorId | UUID | ❌ | 커서 (마지막 ID) |
| size | int | ❌ | 페이지 크기 (기본 1000) |