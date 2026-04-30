# 07. Admin Service (9007)

> 인증 범례 및 공통 응답 형식 → [API_SPEC.md](../API_SPEC.md)
>
> 모든 어드민 API는 `✅ JWT` + ADMIN 권한 필요. 게이트웨이에서 `/api/v1/admins/**` → `:9007` 라우팅.

---

## 대시보드 (Dashboard)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/v1/admins/dashboard` | ✅ JWT | 통계 대시보드 조회 |

---

### GET `/api/v1/admins/dashboard`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `year` | int | X | 조회 연도 (미입력 시 현재 연도) |

**Response** `200 OK`
```json
{
  "data": {
    "overview": {
      "totalUsers": 1500,
      "activeProducts": 320,
      "pendingOrders": 45,
      "currentMonthSettlement": 8500000
    },
    "monthlyOrderStats": [
      { "month": "2026-01", "orderCount": 120, "totalRevenue": 3600000 }
    ],
    "monthlyNewUserStats": [
      { "month": "2026-01", "newUserCount": 85 }
    ]
  }
}
```

---

## 유저 관리 (User)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/v1/admins/users` | ✅ JWT | 전체 유저 목록 조회 (필터링/검색) |
| GET | `/api/v1/admins/users/{userId}` | ✅ JWT | 특정 유저 상세 조회 |
| PATCH | `/api/v1/admins/users/{userId}/approve-seller` | ✅ JWT | 셀러 승인 |

---

### GET `/api/v1/admins/users`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `role` | String | X | 역할 필터 (USER/SELLER/ADMIN) |
| `name` | String | X | 이름 검색 (부분 일치) |
| `email` | String | X | 이메일 검색 (부분 일치) |
| `page` | int | X | 페이지 번호 (Spring Pageable) |
| `size` | int | X | 페이지 크기 |

**Response** `200 OK`
```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "name": "홍길동",
        "email": "user@example.com",
        "phone": "010-1234-5678",
        "role": "SELLER",
        "createdAt": "2024-01-01T12:00:00"
      }
    ],
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

### GET `/api/v1/admins/users/{userId}`

**Response** `200 OK` — 단건 `UserAdminResponseDto` 반환 (위 목록 항목과 동일 구조)

---

### PATCH `/api/v1/admins/users/{userId}/approve-seller`

유저를 SELLER로 승인합니다. 승인 시 `USER_SELLER_APPROVED` Kafka 이벤트가 발행됩니다.

**Response** `200 OK`

---

## 상품 관리 (Product)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/v1/admins/products` | ✅ JWT | 전체 상품 목록 조회 (필터링/검색) |
| PATCH | `/api/v1/admins/products/{productId}/force-down` | ✅ JWT | 상품 강제 내리기 (DISABLE 처리) |

---

### GET `/api/v1/admins/products`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `status` | String | X | 상태 필터 (ENABLE/DISABLE) |
| `sellerId` | UUID | X | 판매자 ID 필터 |
| `title` | String | X | 제목 검색 (부분 일치) |
| `page` | int | X | 페이지 번호 |
| `size` | int | X | 페이지 크기 |

**Response** `200 OK`
```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "sellerId": "uuid",
        "title": "제주도 여행 패키지",
        "maxCapacity": 10,
        "price": 150000,
        "status": "ENABLE",
        "createdAt": "2024-01-01T12:00:00"
      }
    ],
    "totalElements": 50,
    "totalPages": 3
  }
}
```

---

## 주문 조회 (Order)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/v1/admins/orders` | ✅ JWT | 전체 주문 조회 (필터링/검색) |

---

### GET `/api/v1/admins/orders`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `status` | String | X | 상태 필터 (PENDING/PAID/FAILED/REFUNDED/EXPIRED) |
| `sellerId` | UUID | X | 판매자 ID 필터 |
| `startDate` | LocalDateTime | X | 시작 날짜 (createdAt 기준) |
| `endDate` | LocalDateTime | X | 종료 날짜 (createdAt 기준) |
| `page` | int | X | 페이지 번호 |
| `size` | int | X | 페이지 크기 |

**Response** `200 OK`
```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "productScheduleId": "uuid",
        "userId": "uuid",
        "sellerId": "uuid",
        "quantity": 2,
        "price": 300000,
        "status": "PAID",
        "createdAt": "2024-01-01T12:00:00"
      }
    ],
    "totalElements": 200,
    "totalPages": 10
  }
}
```

---

## 정산 조회 (Settlement)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/v1/admins/settlements` | ✅ JWT | 정산 현황 조회 (필터링/검색) |

---

### GET `/api/v1/admins/settlements`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `status` | String | X | 상태 필터 (READY/TRANSFERRING/SENT/FAILED/HOLD) |
| `sellerId` | UUID | X | 판매자 ID 필터 |
| `settlementMonth` | String | X | 정산월 필터 (예: 2026-01) |
| `page` | int | X | 페이지 번호 |
| `size` | int | X | 페이지 크기 |

**Response** `200 OK`
```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "sellerId": "uuid",
        "settlementMonth": "2026-03",
        "originalAmount": 150000,
        "feeAmount": 12000,
        "feeRate": 0.08,
        "settlementAmount": 138000,
        "status": "READY",
        "transferredAt": null,
        "failReason": null
      }
    ],
    "totalElements": 30,
    "totalPages": 2
  }
}
```

---

## 리뷰 관리 (Review)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/v1/admins/reviews` | ✅ JWT | 리뷰 목록 조회 |
| DELETE | `/api/v1/admins/reviews/{reviewId}` | ✅ JWT | 부적절한 리뷰 삭제 |

---

### GET `/api/v1/admins/reviews`

**Query Parameters**: Spring Pageable (`page`, `size`, `sort`)

**Response** `200 OK`
```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "productId": "uuid",
        "userId": "uuid",
        "userEmail": "user@example.com",
        "rating": 1,
        "content": "불량 리뷰 내용",
        "createdAt": "2024-01-01T12:00:00"
      }
    ],
    "totalElements": 10,
    "totalPages": 1
  }
}
```
