# User 서비스 문서

User 서비스 문서는 아래 4개 문서로 분리한다.

| 문서 | 설명 |
|------|------|
| [개요 (이 문서)](./user.md) | 서비스 개요, 패키지 구조, 도메인 모델, 에러 코드 |
| [인증 (auth)](./auth.md) | JWT 로그인/로그아웃/재발급, OAuth2 소셜 로그인, Redis 서킷 브레이커, RTR, 토큰 도난 신고 |
| [예치금 (deposit)](./deposit.md) | 예치금 충전/사용/환불, Kafka 이벤트 소비, 멱등성 처리 |
| [이메일 인증 (email-verification)](./email-verification.md) | 인증코드 발송, 코드 검증, verifiedToken 발급 |

전체 API 상세 명세는 [01. User Service API 명세](../00_api-spec/01-user.md)를 함께 참고한다.

---

## 서비스 개요

| 항목 | 값 |
|------|-----|
| 포트 | 9003 |
| 베이스 패키지 | `jabaclass.user` |
| DB | PostgreSQL (prod) / H2 in-memory (test) |
| 캐시/토큰 저장소 | Redis |
| 메시지 브로커 | Kafka |
| 인증 방식 | JWT (Access + Refresh) + OAuth2 소셜 로그인 |

---

## 패키지 구조

```
jabaclass.user
├── auth/                          # 인증 도메인
│   ├── application/
│   │   ├── exception/             # AuthErrorCode, AuthException
│   │   ├── scheduler/             # TokenBlacklistCleanupScheduler
│   │   ├── service/               # AuthService (login/logout/reissue/theft/tokenStatus)
│   │   └── usecase/               # LoginUseCase, LogoutUseCase, ReissueUseCase,
│   │                              #   ReportTheftUseCase, TokenStatusUseCase
│   ├── domain/
│   │   ├── model/                 # TokenBlacklist
│   │   └── repository/            # TokenBlacklistRepository
│   ├── infrastructure/
│   │   ├── jwt/                   # JwtProvider, TokenProvider, TokenType
│   │   └── oauth2/                # CustomOAuth2User, CustomOAuth2UserService,
│   │                              #   OAuth2SuccessHandler, OAuthAttributes,
│   │                              #   HttpCookieOAuth2AuthorizationRequestRepository
│   └── presentation/
│       ├── controller/            # AuthController
│       └── dto/                   # LoginRequestDto, TokenResponseDto, TokenResult, TokenStatusResult
│
├── deposit/                       # 예치금 도메인
│   ├── application/
│   │   ├── service/               # DepositService (facade), DepositChargeService,
│   │   │                          #   DepositQueryService, RefundDepositService,
│   │   │                          #   UseDepositService, ValidateDepositService
│   │   └── usecase/               # DepositUseCase
│   ├── domain/
│   │   ├── DepositHistory         # 예치금 이력 엔티티
│   │   ├── DepositStatus          # PENDING / COMPLETED / FAILED
│   │   ├── DepositType            # CHARGE / PAYMENT / REFUND
│   │   ├── exception/             # DepositErrorCode, DepositException
│   │   └── repository/            # DepositHistoryRepository
│   ├── infrastructure/
│   │   ├── client/                # PaymentClient (외부 결제 서비스 연동)
│   │   ├── idempotency/           # ProcessedEvent, ProcessedEventRepository
│   │   ├── kafka/                 # OrderEventsConsumer
│   │   └── persistence/           # DepositHistoryJpaRepository, DepositHistoryRepositoryAdapter
│   └── presentation/
│       ├── controller/            # DepositController (API/Internal), DepositRestController
│       └── dto/                   # request/response DTOs
│
├── mail/                          # 이메일 인증 도메인
│   ├── application/
│   │   ├── exception/             # MailErrorCode
│   │   ├── service/               # EmailVerificationService, MailService
│   │   └── usecase/               # EmailVerificationUseCase
│   ├── domain/
│   │   ├── model/                 # EmailVerification, MailMessage
│   │   ├── repository/            # EmailVerificationRepository
│   │   └── sender/                # MailSender (인터페이스)
│   └── infrastructure/
│       ├── config/                # MailConfig, MailProperties
│       ├── sender/                # SmtpMailSender, ConsoleMailSender
│       └── storage/               # InMemoryEmailVerificationRepository
│
├── user/                          # 사용자 도메인
│   ├── application/
│   │   ├── exception/             # UserErrorCode
│   │   ├── service/               # UserService
│   │   └── usercase/              # UserUseCase
│   ├── domain/
│   │   ├── model/                 # User, SellerSettlementAccount, UserRole, SocialType
│   │   └── repository/            # UserRepository, SellerSettlementAccountRepository
│   ├── infrastructure/
│   │   ├── kafka/                 # UserEventsPublisher
│   │   └── persistence/           # UserJpaRepository, UserRepositoryAdapter,
│   │                              #   SellerSettlementAccountJpaRepository,
│   │                              #   SellerSettlementAccountRepositoryAdapter
│   └── presentation/
│       ├── controller/            # UserController, UserApi,
│       │                          #   UserInternalController, UserInternalApi
│       └── dto/                   # request/response DTOs
│
└── common/                        # 공통
    ├── auth/                      # @CurrentUser, @CurrentUserRole (ArgumentResolver)
    ├── config/                    # SecurityConfig, RedisCircuitBreakerConfig,
    │                              #   KafkaConfig, RestTemplateConfig, CacheConfig
    ├── dto/                       # ApiResponseDto
    ├── error/                     # GlobalExceptionHandler, BusinessException, ErrorCode
    └── model/                     # BaseEntity (id/UUID, regDt, modDt)
```

---

## 도메인 모델

### User

**테이블:** `users`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | UUID (PK) | — | `BaseEntity` 상속 |
| `name` | VARCHAR(50) | NOT NULL | 이름 |
| `email` | VARCHAR(320) | NOT NULL | 이메일 |
| `password` | VARCHAR(255) | nullable | BCrypt 해시 (소셜 로그인 시 null) |
| `phone` | VARCHAR(20) | nullable | 전화번호 |
| `role` | VARCHAR(20) | NOT NULL | `USER` / `SELLER` / `ADMIN` |
| `social_type` | VARCHAR(20) | nullable | `SYSTEM` / `GOOGLE` / `KAKAO` / `NAVER` |
| `social_id` | VARCHAR(255) | nullable | 소셜 공급자의 사용자 ID |
| `version` | BIGINT | — | Optimistic lock (@Version) |
| `deposit` | DECIMAL(19,2) | NOT NULL, default 0 | 예치금 잔액 |
| `refresh_token` | VARCHAR(512) | nullable | 현재 유효한 Refresh Token (DB 이중 저장) |
| `last_login_ip` | VARCHAR(45) | nullable | 마지막 로그인 IP (새 기기 감지용) |
| `last_login_user_agent` | VARCHAR(512) | nullable | 마지막 로그인 User-Agent |
| `force_logout_at` | TIMESTAMP | nullable | 강제 로그아웃 시각 (RTR 재사용 감지 시 설정) |
| `reg_dt` | TIMESTAMP | — | 등록일시 (`BaseEntity`) |
| `mod_dt` | TIMESTAMP | — | 수정일시 (`BaseEntity`) |

**유니크 제약:** `(email, social_type)` — 동일 이메일로 소셜/일반 계정 분리 허용

**도메인 메서드:**

| 메서드 | 설명 |
|--------|------|
| `updateProfile(name, phone)` | 이름 + 전화번호 수정 |
| `changeEmail(email)` | 이메일 변경 |
| `chargeDeposit(amount)` | 예치금 증가 |
| `deductDeposit(amount)` | 예치금 차감 (잔액 부족 시 `INSUFFICIENT_DEPOSIT` 예외) |
| `updateRefreshToken(token)` | Refresh Token DB 저장 |
| `updateLastLogin(ip, userAgent)` | 마지막 로그인 정보 갱신 |
| `forceLogout()` | `forceLogoutAt = now()` 설정 |

---

### SellerSettlementAccount

**테이블:** `seller_settlement_accounts`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | UUID (PK) | — | `BaseEntity` 상속 |
| `user_id` | UUID | NOT NULL, UNIQUE | 판매자 ID |
| `bank_code` | VARCHAR(20) | NOT NULL | 은행 코드 |
| `account_number` | VARCHAR(50) | NOT NULL | 계좌번호 |
| `account_holder` | VARCHAR(100) | NOT NULL | 예금주명 |
| `active` | BOOLEAN | NOT NULL | 활성 여부 (정산 실행 조건) |

판매자당 1개만 허용 (user_id unique). 없으면 `register()`, 있으면 `updateAccount()`로 upsert.

---

### DepositHistory

**테이블:** `deposit_histories`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | UUID (PK) | — | `BaseEntity` 상속 |
| `user_id` | UUID (FK → users) | NOT NULL | 사용자 |
| `payment_id` | UUID | nullable | 연관 결제/주문 ID |
| `amount` | DECIMAL(19,2) | NOT NULL | 금액 |
| `type` | VARCHAR(10) | NOT NULL | `CHARGE` / `PAYMENT` / `REFUND` |
| `status` | VARCHAR(10) | NOT NULL | `PENDING` / `COMPLETED` / `FAILED` |

---

### TokenBlacklist

**테이블:** `token_blacklists` (논리적 명칭)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | UUID (PK) | — |
| `token_hash` | VARCHAR | Access Token의 SHA-256 해시 |
| `expires_at` | TIMESTAMP | 토큰 만료 시각 |

로그아웃 시 Access Token의 SHA-256 해시를 DB + Redis에 모두 등록한다.
만료된 블랙리스트는 `TokenBlacklistCleanupScheduler`가 주기적으로 삭제한다.

---

## 에러 코드

### UserErrorCode

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없습니다. |
| `EMAIL_ALREADY_EXISTS` | 409 | 이미 가입된 이메일입니다. |

### AuthErrorCode

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `USER_NOT_FOUND` | 404 | 이메일 또는 비밀번호가 올바르지 않습니다. |
| `INVALID_REFRESH_TOKEN` | 401 | 유효하지 않은 refresh token입니다. |
| `REFRESH_TOKEN_MISMATCH` | 401 | refresh token이 일치하지 않습니다. |
| `SUSPECTED_TOKEN_THEFT` | 401 | 비정상적인 인증 시도가 감지되었습니다. 다시 로그인해주세요. |
| `THEFT_REPORT_TOKEN_EXPIRED` | 401 | 유효하지 않거나 만료된 링크입니다. |
| `ALREADY_LOGGED_OUT` | 401 | 이미 로그아웃된 유저입니다. |
| `INVALID_TOKEN` | 401 | 유효하지 않은 인증 정보입니다. |
| `INVALID_REQUEST` | 401 | 인증 정보가 존재하지 않습니다. |
| `INVALID_EMAIL` | 409 | 가입할 수 없는 이메일입니다. 다른 이메일을 사용해주세요. |
| `UNSUPPORTED_PROVIDER` | 400 | 지원하지 않는 소셜 로그인입니다. |

### DepositErrorCode

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `NOT_FOUND_USER` | 404 | 존재하지 않는 회원입니다. |
| `NOT_FOUND_DEPOSIT_HISTORY` | 404 | 존재하지 않는 예치금 이력입니다. |
| `PAYMENT_SERVICE_UNAVAILABLE` | 503 | 결제 서비스에 연결할 수 없습니다. |
| `PAYMENT_FAILED` | 400 | 결제에 실패했습니다. |
| `INSUFFICIENT_DEPOSIT` | 400 | 예치금이 부족합니다. |

### MailErrorCode

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `EMAIL_VERIFICATION_NOT_FOUND` | 404 | 이메일 인증 정보를 찾을 수 없습니다. |
| `EMAIL_VERIFICATION_CODE_EXPIRED` | 400 | 인증코드가 만료되었습니다. |
| `EMAIL_VERIFICATION_CODE_MISMATCH` | 400 | 인증코드가 올바르지 않습니다. |
| `EMAIL_VERIFICATION_TOKEN_NOT_FOUND` | 404 | 이메일 인증 토큰이 존재하지 않습니다. |
| `EMAIL_VERIFICATION_TOKEN_EXPIRED` | 400 | 이메일 인증 토큰이 만료되었습니다. |
| `EMAIL_VERIFICATION_TOKEN_MISMATCH` | 400 | 이메일 인증 토큰이 올바르지 않습니다. |

---

## Kafka 이벤트

### 발행 (Producer)

| 토픽 | 헤더 `eventType` | Key | 발행 시점 |
|------|-----------------|-----|----------|
| `user.events` | `USER_NAME_CHANGED` | `userId` | `updateMyInfo` 에서 이름 변경 감지 시 (트랜잭션 커밋 후) |

**페이로드:**
```json
{ "userId": "UUID", "newName": "string" }
```

소비자: `product-service`의 `UserEventsConsumer` — 해당 판매자의 모든 ES 문서 `sellerName` 일괄 업데이트.

### 소비 (Consumer)

| 토픽 | Group ID | 소비 이벤트 |
|------|----------|------------|
| `order.events` | `deposit-service` | `ORDER_DEPOSIT_REFUND_REQUESTED`, `ORDER_EXPIRED` |

상세는 [deposit.md](./deposit.md) 참고.

---

## 공통 어노테이션

| 어노테이션 | 위치 | 설명 |
|-----------|------|------|
| `@CurrentUser` | 컨트롤러 파라미터 | 요청 헤더 `X-User-Id`에서 UUID 추출 |
| `@CurrentUserRole` | 컨트롤러 파라미터 | 요청 헤더 `X-User-Role`에서 UserRole 추출 |

두 어노테이션 모두 Spring Security Context를 사용하지 않고 API Gateway가 주입한 헤더를 직접 읽는다.
