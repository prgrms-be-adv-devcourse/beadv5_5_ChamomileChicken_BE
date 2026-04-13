# Order Service 문서

> **Base URL**: `http://localhost:9005/api/v1/orders`  
> **인증**: `Authorization: Bearer <access_token>` (JWT)

---

## 목차

1. [API 엔드포인트](#1-api-엔드포인트)
2. [도메인 상태값](#2-도메인-상태값)
3. [Kafka 토픽 전체 목록](#3-kafka-토픽-전체-목록)
4. [시나리오별 흐름](#4-시나리오별-흐름)
   - [주문 생성](#41-주문-생성)
   - [결제 성공](#42-결제-성공)
   - [결제 실패](#43-결제-실패-pg-거절)
   - [결제 미완료 (만료)](#44-결제-미완료-만료)
   - [환불](#45-환불)
5. [서비스 간 책임 분리](#5-서비스-간-책임-분리)

---

## 1. API 엔드포인트

### 주문 (Public)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/orders` | ✅ JWT | 주문 생성 |
| GET | `/api/v1/orders` | ✅ JWT | 내 주문 목록 조회 |
| GET | `/api/v1/orders/{orderId}` | ✅ JWT | 주문 상세 조회 |
| PATCH | `/api/v1/orders/{orderId}` | ✅ JWT | 주문 취소 |

---

#### POST `/api/v1/orders` — 주문 생성

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
    "depositAmount": 10000,
    "paymentAmount": 30000,
    "status": "PENDING"
  }
}
```

> `totalAmount = 상품 단가 × quantity`  
> `paymentAmount = totalAmount - depositAmount` (카드 실결제 금액)

---

#### GET `/api/v1/orders` — 주문 목록 조회

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| status | OrderStatus | ❌ | 상태 필터 |

**Response** `200 OK`
```json
{
  "data": [ ]
}
```

---

### 내부 API (Internal — 서비스 간 직접 호출)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/v1/orders/{orderId}/payment-amount/validate` | ⚙️ Internal | 결제 금액 검증 |
| PUT | `/api/v1/orders/{orderId}/payment-status` | ⚙️ Internal | 결제 상태 업데이트 |
| POST | `/api/v1/orders/bulk` | ⚙️ Internal | 주문 다건 조회 (정산용) |

---

#### GET `/api/v1/orders/{orderId}/payment-amount/validate`

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

#### PUT `/api/v1/orders/{orderId}/payment-status`

**Request Body**
```json
{
  "paymentStatus": "SUCCESS | FAILURE"
}
```

**Response** `204 No Content`

---

## 2. 도메인 상태값

### Order — `OrderStatus`

| 상태 | 진입 조건 |
|------|-----------|
| `PENDING` | 주문 생성 직후 |
| `PAID` | `payment.completed` Kafka 이벤트 수신 |
| `FAILED` | `payment.failed` Kafka 이벤트 수신 |
| `EXPIRED` | `payment.expired` Kafka 이벤트 수신 (결제 타임아웃) |
| `REFUNDED` | `payment.refund.completed` Kafka 이벤트 수신 |


---

## 3. Kafka 토픽 전체 목록

### Payment → Order (Payment 담당자가 발행)

| 토픽 | 발행 시점 | 페이로드 |
|------|-----------|----------|
| `payment.completed` | PG 결제 승인 성공 | `{ "orderId": "UUID" }` |
| `payment.failed` | PG 결제 승인 실패 | `{ "orderId": "UUID" }` |
| `payment.expired` | 결제 타임아웃 감지 | `{ "orderId": "UUID" }` |
| `payment.refund.completed` | 환불 처리 완료 | `{ "orderId": "UUID", "productUserId": "UUID" }` |

### Order → Product (Order 서비스가 발행, Product 서비스가 소비)

| 토픽 | groupId | 발행 시점 | 페이로드 |
|------|---------|-----------|----------|
| `order.reservation.confirmed` | `product-service` | 결제 성공 시 예약 확정 | `{ "productUserId": "UUID" }` |
| `order.reservation.released` | `product-service` | 결제 실패/만료 시 재고 복구 | `{ "productUserId": "UUID" }` |

### Order → User (Order 서비스가 발행, User 서비스가 소비)

| 토픽 | groupId | 발행 시점 | 페이로드 |
|------|---------|-----------|----------|
| `order.deposit.refund-requested` | `user-service` | 결제 실패 시 예치금 복구 | `{ "orderId": "UUID", "userId": "UUID", "depositAmount": 5000 }` |
| `order.expired` | `user-service` | 결제 만료 시 예치금 복구 | `{ "orderId": "UUID", "userId": "UUID", "depositAmount": 5000 }` |

> `depositAmount == 0`인 경우 Order → User 이벤트는 발행하지 않음

---
