# Jaba class API 명세서

> **Base URL 패턴**: `http://localhost:{port}/api/v1/...`
> **인증**: `Authorization: Bearer <access_token>` (JWT)
> **응답 공통 래퍼**:
> ```json
> {
>   "status": "SUCCESS | FAILURE",
>   "code": 200,
>   "message": "메시지",
>   "data": { }
> }
> ```

---

## 인증 범례

| 표시 | 의미 |
|------|------|
| `❌ 공개` | 게이트웨이 화이트리스트 — JWT 불필요 |
| `✅ JWT` | 게이트웨이에서 JWT 검증 필수. `X-User-Id` 헤더로 서비스에 전달 |
| `❌ 미등록` | 게이트웨이 라우팅 없음 — 직접 포트 호출 시 인증 없음 |
| `⚙️ Internal` | 서비스 간 RestTemplate 직접 호출 (게이트웨이 우회) |

**게이트웨이 화이트리스트 (JWT 불필요)**
```
POST /api/v1/auth/login
POST /api/v1/auth/reissue
POST /api/v1/users/register
POST /api/v1/users/email-check
POST /api/v1/email/**
GET  /api/v1/products/**
```

**게이트웨이 라우팅 목록 (미등록 경로는 JWT 미적용)**
```
/api/v1/files/**       → :9000 (File)
/api/v1/payments/**    → :9001 (Payment)
/api/v1/auth/**        → :9003 (User)
/api/v1/users/**       → :9003 (User)
/api/v1/email/**       → :9003 (User)
/api/v1/products/**    → :9004 (Product)
/api/v1/orders/**      → :9005 (Order)
/api/v1/admins/**      → :9007 (Admin)
```
> `/api/v1/deposits/**`, `/api/v1/refunds`, `/settlements/**`, `/internal-batch/**`는 게이트웨이 미등록

---

## 목차

1. [USER SERVICE (9003)](#1-user-service-9003)
2. [PAYMENT SERVICE (9001)](#2-payment-service-9001)
3. [PRODUCT SERVICE (9004)](#3-product-service-9004)
4. [ORDER SERVICE (9005)](#4-order-service-9005)
5. [FILE SERVICE (9000)](#5-file-service-9000)
6. [SETTLEMENT SERVICE (9002)](#6-settlement-service-9002)
7. [에러 코드 패턴](#에러-코드-패턴)
8. [서비스 간 통신 흐름](#서비스-간-통신-흐름)

---

## 1. USER SERVICE (9003)

### 인증 (Auth)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/auth/login` | ❌ 공개 | 로그인 |
| POST | `/api/v1/auth/logout` | ✅ JWT | 로그아웃 |
| POST | `/api/v1/auth/reissue` | ❌ 공개 | Access Token 재발급 |

---

#### POST `/api/v1/auth/login`

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** `200 OK`
```json
{
  "data": {
    "access_token": "eyJhbGci..."
  }
}
```
> refresh_token은 HttpOnly 쿠키로 반환됨

---

#### POST `/api/v1/auth/reissue`

**Request**: 쿠키의 `refresh_token` 자동 사용

**Response** `200 OK`
```json
{
  "data": {
    "access_token": "eyJhbGci..."
  }
}
```

---

### 이메일 인증 (Email Verification)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/email/verifications` | ❌ 공개 | 인증 코드 발송 |
| POST | `/api/v1/email/verifications/confirm` | ❌ 공개 | 인증 코드 확인 |

---

#### POST `/api/v1/email/verifications`

**Request Body**
```json
{
  "email": "user@example.com"
}
```

**Response** `204 No Content`

---

#### POST `/api/v1/email/verifications/confirm`

**Request Body**
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

**Response** `200 OK`
```json
{
  "data": {
    "verifiedToken": "uuid-token"
  }
}
```

---

### 회원 관리 (User)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/users/email-check` | ❌ 공개 | 이메일 중복 확인 |
| POST | `/api/v1/users/register` | ❌ 공개 | 회원가입 |
| GET | `/api/v1/users/me` | ✅ JWT | 내 정보 조회 |
| PUT | `/api/v1/users/me` | ✅ JWT | 내 정보 수정 |
| PUT | `/api/v1/users/me/email` | ✅ JWT | 이메일 변경 |
| DELETE | `/api/v1/users/me` | ✅ JWT | 회원 탈퇴 |

---

#### POST `/api/v1/users/email-check`

**Request Body**
```json
{
  "email": "user@example.com"
}
```

**Response** `200 OK`
```json
{
  "data": {
    "available": true
  }
}
```

---

#### POST `/api/v1/users/register`

**Request Body**
```json
{
  "name": "홍길동",
  "email": "user@example.com",
  "password": "password123",
  "phone": "010-1234-5678",
  "verifiedToken": "uuid-verified-token"
}
```

**Response** `201 Created`

---

#### GET `/api/v1/users/me`

**Response** `200 OK`
```json
{
  "data": {
    "userId": "uuid",
    "name": "홍길동",
    "email": "user@example.com",
    "phone": "010-1234-5678",
    "role": "BUYER | SELLER",
    "deposit": 50000
  }
}
```

---

#### PUT `/api/v1/users/me`

**Request Body**
```json
{
  "name": "새이름",
  "phone": "010-9999-8888"
}
```

**Response** `204 No Content`

---

#### PUT `/api/v1/users/me/email`

**Request Body**
```json
{
  "newEmail": "new@example.com",
  "verifiedToken": "uuid-verified-token"
}
```

**Response** `204 No Content`

---

### 예치금 (Deposit)

> ⚠️ `/api/v1/deposits/**`는 게이트웨이 미등록 경로 — 현재 인증 없이 직접 호출 가능

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/v1/deposits` | ❌ 미등록 | 거래 내역 조회 |
| GET | `/api/v1/deposits/me` | ❌ 미등록 | 잔액 조회 |
| GET | `/api/v1/deposits/{depositHistoryId}` | ❌ 미등록 | 거래 상세 조회 |

---

#### GET `/api/v1/deposits/me`

**Response** `200 OK`
```json
{
  "data": {
    "userId": "uuid",
    "balance": 50000
  }
}
```

---

#### GET `/api/v1/deposits/{depositHistoryId}`

**Path Variables**: `depositHistoryId` (UUID)

**Response** `200 OK`
```json
{
  "data": {
    "depositHistoryId": "uuid",
    "amount": 10000,
    "status": "CHARGED | USED | REFUNDED",
    "createdAt": "2024-01-01T12:00:00"
  }
}
```

---

### 내부 API (Internal — 서비스 간 통신용)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/users/bulk` | ⚙️ Internal | 유저 다건 조회 |
| POST | `/api/v1/users/sellers/bulk` | ⚙️ Internal | 셀러 정산 정보 다건 조회 |
| POST | `/api/v1/users/sellers/settlement-accounts/bulk` | ⚙️ Internal | 셀러 정산 계좌 다건 조회 |
| POST | `/api/v1/deposits/validate` | ⚙️ Internal | 예치금 잔액 검증 (게이트웨이 미등록) |
| POST | `/api/v1/deposits/use` | ⚙️ Internal | 예치금 차감 (게이트웨이 미등록) |

---

## 2. PAYMENT SERVICE (9001)

### 상품 결제 (Product Payment)

> ⚠️ `/api/v1/refunds`는 게이트웨이 미등록 경로

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/payments/prepare` | ✅ JWT | 결제 준비 |
| POST | `/api/v1/payments/confirm` | ✅ JWT | 결제 승인 (Toss) |
| POST | `/api/v1/refunds` | ❌ 미등록 | 환불 요청 |

---

#### POST `/api/v1/payments/prepare`

**Request Body**
```json
{
  "productId": "uuid",
  "orderId": "uuid",
  "userId": "uuid",
  "productUserId": "uuid",
  "paymentMethod": "CARD | DEPOSIT | HYBRID",
  "paymentAmount": 30000,
  "depositAmount": 10000
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

#### POST `/api/v1/payments/confirm`

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

#### POST `/api/v1/refunds`

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

### 예치금 충전 결제 (Deposit Charge Payment)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/payments/deposits/prepare` | ✅ JWT | 충전 결제 준비 |
| POST | `/api/v1/payments/deposits/confirm` | ✅ JWT | 충전 결제 승인 |

---

#### POST `/api/v1/payments/deposits/prepare`

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

#### POST `/api/v1/payments/deposits/confirm`

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

### 내부 API (Internal)

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

---

## 3. PRODUCT SERVICE (9004)

### 상품 (Product)

> 게이트웨이 화이트리스트: `GET /api/v1/products/**` → 조회는 전체 공개

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/products` | ✅ JWT | 상품 등록 |
| PUT | `/api/v1/products/{productId}` | ✅ JWT | 상품 수정 |
| DELETE | `/api/v1/products/{productId}` | ✅ JWT | 상품 삭제 |
| GET | `/api/v1/products` | ❌ 공개 | 상품 목록 조회 (페이징) |
| GET | `/api/v1/products/{productId}` | ❌ 공개 | 상품 상세 조회 |

---

#### POST `/api/v1/products`

**Request Body**
```json
{
  "sellerId": "uuid",
  "title": "제주도 여행 패키지",
  "maxCapacity": 10,
  "description": "상품 설명",
  "imageIds": ["uuid1", "uuid2"],
  "price": 150000,
  "status": "ENABLE | DISABLE"
}
```

**Response** `201 Created`
```json
{
  "data": {
    "id": "uuid",
    "sellerName": "홍길동",
    "title": "제주도 여행 패키지",
    "maxCapacity": 10,
    "description": "상품 설명",
    "thumbnailPath": "/images/thumb.jpg",
    "imagePaths": ["/images/1.jpg"],
    "price": 150000,
    "statusName": "판매중",
    "regId": "uuid",
    "regDt": "2024-01-01T12:00:00",
    "modifyId": "uuid",
    "modifyDt": "2024-01-01T12:00:00"
  }
}
```

---

#### GET `/api/v1/products`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| title | String | ❌ | 검색 키워드 |
| thisPage | int | ✅ | 페이지 번호 (0부터) |
| pageSize | int | ✅ | 페이지 크기 |
| status | ProductStatus | ❌ | 상태 필터 (ENABLE/DISABLE) |

**Response** `200 OK`
```json
{
  "data": {
    "items": [ ],
    "totalCount": 100
  }
}
```

---

### 스케줄 (Schedule)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/products/{productId}/schedules` | ✅ JWT | 스케줄 등록 |
| PUT | `/api/v1/products/{productId}/schedules/{scheduleId}` | ✅ JWT | 스케줄 수정 |
| DELETE | `/api/v1/products/{productId}/schedules/{scheduleId}` | ✅ JWT | 스케줄 삭제 |
| GET | `/api/v1/products/{productId}/schedules` | ❌ 공개 | 스케줄 목록 조회 |
| GET | `/api/v1/products/{scheduleId}/availability` | ❌ 공개 | 잔여 수량 조회 |

---

#### POST `/api/v1/products/{productId}/schedules`

**Request Body**
```json
{
  "date": "2024-06-01",
  "quantity": 10,
  "status": "OPEN | CLOSED"
}
```

**Response** `201 Created`
```json
{
  "data": {
    "scheduleId": "uuid",
    "productId": "uuid",
    "date": "2024-06-01",
    "quantity": 10,
    "status": "OPEN"
  }
}
```

---

### 리뷰 (Review)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/products/{productId}/reviews` | ✅ JWT | 리뷰 작성 |
| PUT | `/api/v1/products/{productId}/reviews/{reviewId}` | ✅ JWT | 리뷰 수정 |
| DELETE | `/api/v1/products/{productId}/reviews/{reviewId}` | ✅ JWT | 리뷰 삭제 |
| GET | `/api/v1/products/{productId}/reviews` | ❌ 공개 | 상품 리뷰 목록 |
| GET | `/api/v1/products/{productId}/reviews/{reviewId}` | ❌ 공개 | 리뷰 상세 |
| GET | `/api/v1/products/me/reviews` | ❌ 공개* | 내 리뷰 목록 |

> *`GET /api/v1/products/**` 화이트리스트 적용으로 게이트웨이 JWT 미적용. 서비스 내부에서 별도 처리 필요

---

#### POST `/api/v1/products/{productId}/reviews`

**Request Body**
```json
{
  "rating": 5,
  "content": "정말 좋았어요!"
}
```

**Response** `201 Created`
```json
{
  "data": {
    "reviewId": "uuid",
    "productId": "uuid",
    "rating": 5,
    "content": "정말 좋았어요!",
    "createdAt": "2024-01-01T12:00:00"
  }
}
```

---

### 찜하기 (Favorites)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/products/{scheduleId}/likes` | ✅ JWT | 찜 추가 |
| DELETE | `/api/v1/products/{scheduleId}/likes` | ✅ JWT | 찜 삭제 |
| GET | `/api/v1/products/me/likes` | ❌ 공개* | 내 찜 목록 |

> *리뷰와 동일한 화이트리스트 이슈

---

#### POST `/api/v1/products/{scheduleId}/likes`

**Query Parameters**: `quantity` (int)

**Response** `201 Created`
```json
{
  "data": {
    "likeId": "uuid",
    "scheduleId": "uuid",
    "quantity": 2
  }
}
```

---

#### DELETE `/api/v1/products/{scheduleId}/likes`

**Query Parameters**: `likeId` (UUID)

**Response** `200 OK`

---

### 내부 API (Internal)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/products/reservations` | ⚙️ Internal | 재고 확인 및 예약 처리 |
| POST | `/api/v1/products/reservations/status` | ⚙️ Internal | 재고 복원 (주문 취소 시) |
| GET | `/api/v1/products/{productId}/schedules/{scheduleId}/user` | ⚙️ Internal | 스케줄 참여 유저 조회 |
| POST | `/api/v1/products/bulk` | ⚙️ Internal | 상품 다건 조회 (정산용) |
| POST | `/api/v1/products/es-migrate` | ⚙️ Internal | Elasticsearch 마이그레이션 |

---

## 4. ORDER SERVICE (9005)

### 주문 (Order)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/orders` | ✅ JWT | 주문 생성 |
| GET | `/api/v1/orders` | ✅ JWT | 내 주문 목록 |
| GET | `/api/v1/orders/{orderId}` | ✅ JWT | 주문 상세 조회 |
| PATCH | `/api/v1/orders/{orderId}` | ✅ JWT | 주문 취소 |

---

#### POST `/api/v1/orders`

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
    "status": "PENDING | PAID | CANCELLED"
  }
}
```

---

#### GET `/api/v1/orders`

**Query Parameters**: `status` (OrderStatus, 선택)

**Response** `200 OK`
```json
{
  "data": [ ]
}
```

---

### 내부 API (Internal)

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

## 5. FILE SERVICE (9000)

### 파일 업로드 (File)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/files/upload-request` | ✅ JWT | 업로드 URL 발급 (Presigned URL) |
| PATCH | `/api/v1/files/{fileId}/complete` | ✅ JWT | 업로드 완료 처리 |

---

#### POST `/api/v1/files/upload-request`

**Request Body**
```json
{
  "originalName": "photo.jpg"
}
```

**Response** `200 OK`
```json
{
  "data": {
    "fileId": "uuid",
    "uploadUrl": "https://s3.amazonaws.com/presigned-url...",
    "storagePath": "uploads/uuid/photo.jpg"
  }
}
```

> 발급된 `uploadUrl`로 직접 PUT 요청으로 파일 업로드 후, `/complete` 호출

---

### 내부 API (Internal)

> `/api/internal/files/**`는 게이트웨이 미등록 경로 — 서비스 간 직접 호출

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/internal/files/{fileId}/confirm` | ⚙️ Internal | 단건 파일 확인 |
| POST | `/api/internal/files/confirm/bulk` | ⚙️ Internal | 다건 파일 확인 |
| POST | `/api/internal/files/presigned-urls` | ⚙️ Internal | 다건 조회용 Presigned URL 발급 |

---

## 6. SETTLEMENT SERVICE (9002)

> ⚠️ Settlement 서비스 전체가 게이트웨이 미등록 — 현재 모든 경로 인증 없이 직접 접근 가능

### 정산 조회 (Settlement)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/settlements` | ❌ 미등록 | 월별 정산 목록 조회 |
| GET | `/settlements/ready` | ❌ 미등록 | READY 상태 정산 목록 |
| GET | `/settlements/{settlementId}` | ❌ 미등록 | 정산 상세 조회 |

---

#### GET `/settlements`

**Query Parameters**: `month` (String, 형식: `yyyy-MM`)

**Response** `200 OK`
```json
{
  "data": [ ]
}
```

---

### 배치 API (Internal Batch)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/internal-batch/settlements/load-targets` | ⚙️ Internal | 정산 대상 적재 배치 실행 |
| POST | `/internal-batch/settlements/calculate` | ⚙️ Internal | 정산 계산 배치 실행 |
| POST | `/internal-batch/settlements/transfer` | ⚙️ Internal | 정산 송금 배치 실행 |

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| targetDate | LocalDate | load-targets: 대상 날짜 (미입력 시 전일) |
| settlementMonth | String (yyyy-MM) | calculate/transfer: 정산 월 (미입력 시 전월) |

---

## 에러 코드 패턴

```json
{
  "status": "FAILURE",
  "code": 400,
  "message": "이메일 형식이 올바르지 않습니다.",
  "data": null
}
```

| HTTP 상태 | 설명 |
|---------|------|
| 400 | 유효성 검증 실패 (`@Valid`) |
| 401 | JWT 인증 실패 (`JwtAuthException`) |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 비즈니스 규칙 위반 (`BusinessException`) |

---

## 서비스 간 통신 흐름

```
[Client]
    │
    ▼
[API Gateway :8080]  ← JWT 검증 + Redis 블랙리스트 확인
    │                   인증된 요청에 X-User-Id 헤더 주입
    ├──→ User Service    (:9003) /api/v1/auth/**, /api/v1/users/**, /api/v1/email/**
    ├──→ Product Service (:9004) /api/v1/products/**
    ├──→ Order Service   (:9005) /api/v1/orders/**
    ├──→ Payment Service (:9001) /api/v1/payments/**
    └──→ File Service    (:9000) /api/v1/files/**

[서비스 간 직접 호출 RestTemplate — 게이트웨이 우회]
    Order      → Product   재고 확인 (/api/v1/products/reservations)
    Order      → User      예치금 검증/차감 (/api/v1/deposits/validate, /use)
    Payment    → Order     금액 검증, 상태 업데이트 (/api/v1/orders/**)
    Payment    → User      예치금 차감 (/api/v1/deposits/use)
    Settlement → Payment   정산 대상 조회 (/api/v1/payments/settlement-targets)
    Settlement → Order     주문 정보 조회 (/api/v1/orders/bulk)
    Settlement → User      셀러 정산 계좌 조회 (/api/v1/users/sellers/**/bulk)
    Product    → File      파일 확인/URL 조회 (/api/internal/files/**)
```
