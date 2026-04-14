# 01. User Service (9003)

> 인증 범례 및 공통 응답 형식 → [API_SPEC.md](../API_SPEC.md)

---

## 인증 (Auth)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/auth/login` | ❌ 공개 | 로그인 |
| POST | `/api/v1/auth/logout` | ✅ JWT | 로그아웃 |
| POST | `/api/v1/auth/reissue` | ❌ 공개 | Access Token 재발급 |

---

### POST `/api/v1/auth/login`

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

### POST `/api/v1/auth/reissue`

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

## 이메일 인증 (Email Verification)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/email/verifications` | ❌ 공개 | 인증 코드 발송 |
| POST | `/api/v1/email/verifications/confirm` | ❌ 공개 | 인증 코드 확인 |

---

### POST `/api/v1/email/verifications`

**Request Body**
```json
{
  "email": "user@example.com"
}
```

**Response** `204 No Content`

---

### POST `/api/v1/email/verifications/confirm`

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

## 회원 관리 (User)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/users/email-check` | ❌ 공개 | 이메일 중복 확인 |
| POST | `/api/v1/users/register` | ❌ 공개 | 회원가입 |
| GET | `/api/v1/users/me` | ✅ JWT | 내 정보 조회 |
| PUT | `/api/v1/users/me` | ✅ JWT | 내 정보 수정 |
| PUT | `/api/v1/users/me/email` | ✅ JWT | 이메일 변경 |
| DELETE | `/api/v1/users/me` | ✅ JWT | 회원 탈퇴 |

---

### POST `/api/v1/users/email-check`

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

### POST `/api/v1/users/register`

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

### GET `/api/v1/users/me`

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

### PUT `/api/v1/users/me`

**Request Body**
```json
{
  "name": "새이름",
  "phone": "010-9999-8888"
}
```

**Response** `204 No Content`

---

### PUT `/api/v1/users/me/email`

**Request Body**
```json
{
  "newEmail": "new@example.com",
  "verifiedToken": "uuid-verified-token"
}
```

**Response** `204 No Content`

---

## 예치금 (Deposit)

> ⚠️ `/api/v1/deposits/**`는 게이트웨이 미등록 경로 — 현재 인증 없이 직접 호출 가능

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/v1/deposits` | ❌ 미등록 | 거래 내역 조회 |
| GET | `/api/v1/deposits/me` | ❌ 미등록 | 잔액 조회 |
| GET | `/api/v1/deposits/{depositHistoryId}` | ❌ 미등록 | 거래 상세 조회 |

---

### GET `/api/v1/deposits/me`

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

### GET `/api/v1/deposits/{depositHistoryId}`

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

## 내부 API (Internal — 서비스 간 통신용)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/users/bulk` | ⚙️ Internal | 유저 다건 조회 |
| POST | `/api/v1/users/sellers/bulk` | ⚙️ Internal | 셀러 정산 정보 다건 조회 |
| POST | `/api/v1/users/sellers/settlement-accounts/bulk` | ⚙️ Internal | 셀러 정산 계좌 다건 조회 |
| POST | `/api/v1/deposits/validate` | ⚙️ Internal | 예치금 잔액 검증 (게이트웨이 미등록) |
| POST | `/api/v1/deposits/use` | ⚙️ Internal | 예치금 차감 (게이트웨이 미등록) |