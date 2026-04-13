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

### ProductUser — `ReservationStatus`

| 상태 | 진입 조건 |
|------|-----------|
| `RESERVED` | 주문 생성 시 재고 차감과 동시에 생성 |
| `CONFIRMED` | `order.reservation.confirmed` 수신 후 |
| `RELEASED` | `order.reservation.released` 수신 후 (재고 복구) |
| `REFUNDED` | `payment.refund.completed` 수신 후 |

### Schedule — `ReservedStatus`

| 상태 | 진입 조건 |
|------|-----------|
| `AVAILABLE` | 기본값, 또는 재고 복구 후 잔여 > 0 |
| `FULL` | 잔여 인원 = 0 |
| `CLOSED` | Schedule 삭제 시 |

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

## 4. 시나리오별 흐름

### 4.1 주문 생성

```
User → OrderService: POST /api/v1/orders
  │
  ├─ ProductService HTTP 호출: 재고 차감 + 가격 검증
  │    응답: { price, quantity, valid, productUserId }
  │    valid: "OK" | "PRICE_MISMATCH" | "OUT_OF_STOCK"
  │
  ├─ [재고 차감 성공] 총 금액 계산: totalAmount = price × quantity
  │
  ├─ UserService HTTP 호출: 예치금 차감 (depositAmount > 0인 경우)
  │    실패 시 → Kafka: order.reservation.released → ProductService 재고 복구
  │
  └─ Order 저장 (상태: PENDING) → orderId 반환
```

**OrderService 처리 순서**

1. 요청 검증 (`quantity > 0`, `depositAmount >= 0`)
2. ProductService HTTP 호출 — 재고 차감 + 가격 검증
3. 총 금액 계산 (`totalAmount = price × quantity`)
4. `depositAmount > totalAmount`이면 예외
5. UserService HTTP 호출 — 예치금 차감 (`depositAmount == 0`이면 건너뜀)
   - 차감 실패 시: `order.reservation.released` 발행 → 재고 복구 후 주문 실패
6. Order 저장 → `orderId` 반환

**결과 상태**

| 도메인 | 상태 |
|--------|------|
| `Order.status` | `PENDING` |
| `ProductUser.status` | `RESERVED` |
| Schedule 잔여 인원 | 차감됨 |

---

### 4.2 결제 성공

```
Payment → Kafka: payment.completed { orderId }
  └─ OrderService (PaymentCompletedConsumer)
       Order.status: PENDING → PAID
       └─ Kafka: order.reservation.confirmed { productUserId }
            └─ ProductService (OrderReservationConfirmedConsumer)
                 ProductUser.status: RESERVED → CONFIRMED
```

**OrderService 처리** (`PaymentCompletedConsumer`)

| 항목 | 값 |
|------|----|
| 수신 토픽 | `payment.completed` |
| groupId | `order-service` |
| 페이로드 | `{ "orderId": "UUID" }` |

1. `payment.completed` 수신
2. `updatePaymentStatus(orderId, SUCCESS)` 호출
3. `Order.status`: `PENDING → PAID`
4. `order.reservation.confirmed` 발행

**ProductService 처리** (`OrderReservationConfirmedConsumer`)

| 항목 | 값 |
|------|----|
| 수신 토픽 | `order.reservation.confirmed` |
| groupId | `product-service` |
| 페이로드 | `{ "productUserId": "UUID" }` |

1. `order.reservation.confirmed` 수신
2. 이미 `CONFIRMED`이면 무시 (중복 처리 방지)
3. `ProductUser.status`: `RESERVED → CONFIRMED`

**결과 상태**

| 도메인 | 상태 |
|--------|------|
| `Order.status` | `PAID` |
| `ProductUser.status` | `CONFIRMED` |
| Schedule 잔여 인원 | 차감 유지 |

---

### 4.3 결제 실패 (PG 거절)

```
Payment → Kafka: payment.failed { orderId }
  └─ OrderService (PaymentFailedConsumer)
       Order.status: PENDING → FAILED
       ├─ Kafka: order.reservation.released { productUserId }
       │    └─ ProductService (OrderReservationReleasedConsumer)
       │         ProductUser.status: RESERVED → RELEASED
       │         Schedule 잔여 인원 복구
       └─ Kafka: order.deposit.refund-requested { orderId, userId, depositAmount }
            └─ UserService: 예치금 복구 (depositAmount > 0인 경우만)
```

**OrderService 처리** (`PaymentFailedConsumer`)

| 항목 | 값 |
|------|----|
| 수신 토픽 | `payment.failed` |
| groupId | `order-service` |
| 페이로드 | `{ "orderId": "UUID" }` |

1. `payment.failed` 수신
2. `updatePaymentStatus(orderId, FAILED)` 호출
3. `Order.status`: `PENDING → FAILED`
4. `order.reservation.released` 발행 → 재고 복구
5. `order.deposit.refund-requested` 발행 (예치금 > 0인 경우만) → 예치금 복구

**ProductService 처리** (`OrderReservationReleasedConsumer`)

| 항목 | 값 |
|------|----|
| 수신 토픽 | `order.reservation.released` |
| groupId | `product-service` |
| 페이로드 | `{ "productUserId": "UUID" }` |

1. `order.reservation.released` 수신
2. 이미 `RELEASED` 또는 `REFUNDED`이면 무시 (중복 처리 방지)
3. Schedule 재고 복구 (`restoreCapacity`)
4. `ProductUser.status`: `RESERVED → RELEASED`
5. 잔여 인원 > 0이면 Schedule 상태 `FULL → AVAILABLE` 갱신

**결과 상태**

| 도메인 | 상태 |
|--------|------|
| `Order.status` | `FAILED` |
| `ProductUser.status` | `RELEASED` |
| Schedule 잔여 인원 | 복구됨 |

---

### 4.4 결제 미완료 (만료)

> 사용자가 결제를 직접 취소하거나 앱을 이탈한 경우.  
> PaymentService가 타임아웃 감지 후 `payment.expired` 이벤트를 발행합니다.

```
PaymentService: 타임아웃 감지 → payment.expired { orderId }
  └─ OrderService (PaymentExpiredEventConsumer)
       Order.status: PENDING → EXPIRED
       ├─ Kafka: order.reservation.released { productUserId }
       │    └─ ProductService (OrderReservationReleasedConsumer)
       │         ProductUser.status: RESERVED → RELEASED
       │         Schedule 잔여 인원 복구
       └─ Kafka: order.expired { orderId, userId, depositAmount }
            └─ UserService: 예치금 복구
```

**OrderService 처리** (`PaymentExpiredEventConsumer`)

| 항목 | 값 |
|------|----|
| 수신 토픽 | `payment.expired` |
| groupId | `order-service` |
| 페이로드 | `{ "orderId": "UUID" }` |

1. `payment.expired` 수신
2. 이미 `EXPIRED`이면 무시 (중복 처리 방지)
3. `PENDING`이 아닌 경우 `BusinessException(ORDER_EXPIRE_NOT_ALLOWED)` 발생
4. `Order.status`: `PENDING → EXPIRED`
5. `order.reservation.released` 발행 → 재고 복구
6. `order.expired` 발행 → 예치금 복구

> Product 처리는 [결제 실패](#43-결제-실패-pg-거절)의 `OrderReservationReleasedConsumer`와 동일

**결과 상태**

| 도메인 | 상태 |
|--------|------|
| `Order.status` | `EXPIRED` |
| `ProductUser.status` | `RELEASED` |
| Schedule 잔여 인원 | 복구됨 |

---

### 4.5 환불

> 결제 완료(`PAID`) 이후 환불이 처리된 경우.  
> Order와 Product가 Payment로부터 동일 토픽을 **독립적으로** 수신합니다.

```
Payment → Kafka: payment.refund.completed { orderId, productUserId }
  ├─ OrderService (PaymentRefundCompletedConsumer)
  │    Order.status: PAID → REFUNDED
  └─ ProductService (PaymentRefundCompletedConsumer)
       ProductUser.status: CONFIRMED → REFUNDED
       Schedule 잔여 인원 복구
```

**OrderService 처리** (`PaymentRefundCompletedConsumer`)

1. `payment.refund.completed` 수신
2. `orderUseCase.refund(orderId)` 호출
3. `Order.status`: `PAID → REFUNDED`

**ProductService 처리** (`PaymentRefundCompletedConsumer`)

1. `payment.refund.completed` 수신 (Order와 별개로 독립 수신)
2. 이미 `REFUNDED`이면 무시 (중복 처리 방지)
3. Schedule 재고 복구 (`restoreCapacity`)
4. `ProductUser.status`: `CONFIRMED → REFUNDED`
5. 잔여 인원 > 0이면 Schedule 상태 `FULL → AVAILABLE` 갱신

**결과 상태**

| 도메인 | 상태 |
|--------|------|
| `Order.status` | `REFUNDED` |
| `ProductUser.status` | `REFUNDED` |
| Schedule 잔여 인원 | 복구됨 |

---

## 5. 서비스 간 책임 분리

### Order Service

- 주문 생성 및 상태 관리 (`PENDING → PAID / FAILED / EXPIRED / REFUNDED`)
- ProductService, UserService HTTP 동기 호출 (주문 생성 시)
- Kafka 이벤트 소비 (Payment 발행 이벤트)
- Kafka 이벤트 발행 (Product, User 대상 보상 트랜잭션)

### Payment Service (담당자 구현 필요)

아래 Kafka 이벤트를 **발행**하면 됩니다.

| 토픽 | 발행 시점 |
|------|-----------|
| `payment.completed` | PG 결제 승인 성공 |
| `payment.failed` | PG 결제 승인 실패 |
| `payment.expired` | 결제 타임아웃 감지 |
| `payment.refund.completed` | 환불 처리 완료 |

> 결제 취소/앱 이탈은 Order 서비스가 `payment.expired` 수신으로 처리 — Payment 담당자 별도 구현 불필요

### User Service (담당자 구현 필요)

**① HTTP 엔드포인트** — 주문 생성 시 Order가 동기 호출

```
POST /api/v1/deposits/use

Request: { "userId": "UUID", "depositAmount": 5000 }
Response: { "valid": true }   // 잔액 부족 시 false
```

**② Kafka 컨슈머** — Order 발행 이벤트 소비

| 토픽 | 처리 내용 |
|------|-----------|
| `order.deposit.refund-requested` | 결제 실패 시 예치금 복구 |
| `order.expired` | 결제 만료 시 예치금 복구 |

### Product Service

- `order.reservation.confirmed` 수신 → `ProductUser.status: RESERVED → CONFIRMED`
- `order.reservation.released` 수신 → `ProductUser.status: RESERVED → RELEASED`, 재고 복구
- `payment.refund.completed` 수신 → `ProductUser.status: CONFIRMED → REFUNDED`, 재고 복구

> 중복 메시지 수신 방지 로직이 각 컨슈머에 구현되어 있음 (이미 최종 상태면 무시)
