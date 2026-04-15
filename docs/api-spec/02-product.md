# 02. Product Service (9004)

> 인증 범례 및 공통 응답 형식 → [API_SPEC.md](../API_SPEC.md)

---

## 상품 (Product)

> 게이트웨이 화이트리스트: `GET /api/v1/products/**` → 조회는 전체 공개

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/products` | ✅ JWT | 상품 등록 |
| PUT | `/api/v1/products/{productId}` | ✅ JWT | 상품 수정 |
| DELETE | `/api/v1/products/{productId}` | ✅ JWT | 상품 삭제 |
| GET | `/api/v1/products` | ❌ 공개 | 상품 목록 조회 (페이징) |
| GET | `/api/v1/products/{productId}` | ❌ 공개 | 상품 상세 조회 |

---

### POST `/api/v1/products`

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

### GET `/api/v1/products`

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

## 스케줄 (Schedule)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/products/{productId}/schedules` | ✅ JWT | 스케줄 등록 |
| PUT | `/api/v1/products/{productId}/schedules/{scheduleId}` | ✅ JWT | 스케줄 수정 |
| DELETE | `/api/v1/products/{productId}/schedules/{scheduleId}` | ✅ JWT | 스케줄 삭제 |
| GET | `/api/v1/products/{productId}/schedules` | ❌ 공개 | 스케줄 목록 조회 |
| GET | `/api/v1/products/{scheduleId}/availability` | ❌ 공개 | 잔여 수량 조회 |
| POST | `/api/v1/products/schedules/{scheduleId}` | ⚙️ Internal | 스케줄 단건 조회 |

---

### POST `/api/v1/products/{productId}/schedules`

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

### POST `/api/v1/products/schedules/{scheduleId}`

**Response** `200 OK`
```json
{
  "data": {
    "scheduleId": "uuid",
    "productId": "uuid",
    "date": "2024-06-01",
    "quantity": 10,
    "status": "AVAILABLE | FULL | CLOSED"
  }
}
```

---

## 리뷰 (Review)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/products/{productId}/reviews` | ✅ JWT | 리뷰 작성 |
| PUT | `/api/v1/products/{productId}/reviews/{reviewId}` | ✅ JWT | 리뷰 수정 |
| DELETE | `/api/v1/products/{productId}/reviews/{reviewId}` | ✅ JWT | 리뷰 삭제 |
| GET | `/api/v1/products/{productId}/reviewList` | ❌ 공개 | 상품 리뷰 목록 |
| GET | `/api/v1/products/{productId}/reviews/{reviewId}` | ❌ 공개 | 리뷰 상세 |
| GET | `/api/v1/products/me/reviews` | ❌ 공개* | 내 리뷰 목록 |

> *`GET /api/v1/products/**` 화이트리스트 적용으로 게이트웨이 JWT 미적용. 서비스 내부에서 별도 처리 필요

---

### POST `/api/v1/products/{productId}/reviews`

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

## 찜하기 (Favorites)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/products/{scheduleId}/likes` | ✅ JWT | 찜 추가 |
| DELETE | `/api/v1/products/{scheduleId}/likes` | ✅ JWT | 찜 삭제 |
| GET | `/api/v1/products/me/likes` | ❌ 공개* | 내 찜 목록 |

> *리뷰와 동일한 화이트리스트 이슈

---

### POST `/api/v1/products/{scheduleId}/likes`

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

### DELETE `/api/v1/products/{scheduleId}/likes`

**Query Parameters**: `likeId` (UUID)

**Response** `200 OK`

---

## 내부 API (Internal)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/products/reservations` | ⚙️ Internal | 재고 확인 및 예약 처리 |
| POST | `/api/v1/products/reservations/status` | ⚙️ Internal | 재고 복원 (주문 취소 시) |
| POST | `/api/v1/products/schedules/{scheduleId}` | ⚙️ Internal | 스케줄 단건 조회 |
| GET | `/api/v1/products/{productId}/schedules/{scheduleId}/user` | ⚙️ Internal | 스케줄 참여 유저 조회 |
| POST | `/api/v1/products/bulk` | ⚙️ Internal | 상품 다건 조회 (정산용) |
| POST | `/api/v1/products/es-migrate` | ⚙️ Internal | Elasticsearch 마이그레이션 |
