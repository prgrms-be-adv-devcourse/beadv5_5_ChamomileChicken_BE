# Jaba Class API 명세서 — 전체 개요

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

| 표시            | 의미                                         |
|---------------|--------------------------------------------|
| `❌ 공개`        | 게이트웨이 화이트리스트 — JWT 불필요                     |
| `✅ JWT`       | 게이트웨이에서 JWT 검증 필수. `X-User-Id` 헤더로 서비스에 전달 |
| `❌ 미등록`       | 게이트웨이 라우팅 없음 — 직접 포트 호출 시 인증 없음            |
| `⚙️ Internal` | 서비스 간 RestTemplate 직접 호출 (게이트웨이 우회)        |

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
/api/v1/internal-batch/settlements/** → :9002 (Settlement)
/api/v1/admins/**      → :9007 (Admin)
```

> `/api/v1/deposits/**`, `/api/v1/refunds`는 게이트웨이 미등록

---

## 서비스 목록

비즈니스 흐름 순서로 정렬됩니다.

| # | 서비스        | 포트   | 문서                                   |
|---|------------|------|--------------------------------------|
| 1 | User       | 9003 | [01-user.md](01-user.md)             |
| 2 | Product    | 9004 | [02-product.md](02-product.md)       |
| 3 | File       | 9000 | [03-file.md](03-file.md)             |
| 4 | Order      | 9005 | [04-order.md](04-order.md)           |
| 5 | Payment    | 9001 | [05-payment.md](05-payment.md)       |
| 6 | Settlement | 9002 | [06-settlement.md](06-settlement.md) |

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

| HTTP 상태 | 설명                               |
|---------|----------------------------------|
| 400     | 유효성 검증 실패 (`@Valid`)             |
| 401     | JWT 인증 실패 (`JwtAuthException`)   |
| 403     | 권한 없음                            |
| 404     | 리소스 없음                           |
| 409     | 비즈니스 규칙 위반 (`BusinessException`) |

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
    Order      → Product   일정 단건 조회 (/api/v1/products/schedules/{scheduleId})
    Payment    → Order     금액 검증, 상태 업데이트 (/api/v1/orders/**)
    Payment    → User      예치금 차감 (/api/v1/deposits/use)
    Settlement → Payment   정산 대상 조회 (/api/v1/payments/settlement-targets)
    Settlement → Order     주문 정보 조회 (/api/v1/orders/bulk)
    Settlement → User      셀러 정산 계좌 조회 (/api/v1/users/sellers/**/bulk)
    Product    → File      파일 확인/URL 조회 (/api/internal/files/**)
```
