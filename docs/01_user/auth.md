# 인증 (Auth) — User 서비스

## 목차

1. [개요](#1-개요)
2. [JWT 구조](#2-jwt-구조)
3. [로그인 흐름](#3-로그인-흐름)
4. [토큰 재발급 (RTR)](#4-토큰-재발급-rtr)
5. [로그아웃 + 블랙리스트](#5-로그아웃--블랙리스트)
6. [RTR 재사용 감지 (토큰 탈취 방지)](#6-rtr-재사용-감지-토큰-탈취-방지)
7. [토큰 도난 신고 흐름](#7-토큰-도난-신고-흐름)
8. [토큰 상태 검증 (Gateway 연동)](#8-토큰-상태-검증-gateway-연동)
9. [OAuth2 소셜 로그인](#9-oauth2-소셜-로그인)
10. [Redis 서킷 브레이커](#10-redis-서킷-브레이커)
11. [새 기기 로그인 보안 알림](#11-새-기기-로그인-보안-알림)

---

## 1. 개요

User 서비스는 **JWT 기반 Stateless 인증**을 사용한다.
Access Token은 API Gateway에서 검증되며, User 서비스는 로그인/로그아웃/재발급 등 토큰 생명주기를 담당한다.

인증 흐름 전체는 `AuthService` 단일 클래스가 `LoginUseCase`, `LogoutUseCase`, `ReissueUseCase`, `ReportTheftUseCase`, `TokenStatusUseCase`를 모두 구현한다.

---

## 2. JWT 구조

| 항목 | Access Token | Refresh Token |
|------|-------------|---------------|
| 유효시간 | `${jwt.access-token-validity}` ms | `${jwt.refresh-token-validity}` ms |
| 저장 위치 | 클라이언트 메모리 (Pinia) | HttpOnly Cookie (`refresh_token`) + Redis + DB |
| 클레임 | `sub` (userId), `role`, `type=ACCESS` | `sub` (userId), `role`, `type=REFRESH` |
| 전달 방식 | `Authorization: Bearer {token}` | `Cookie: refresh_token={token}` |

**Redis 키 구조:**

| 키 | 값 | TTL | 용도 |
|----|-----|-----|------|
| `refresh:{userId}` | refreshToken | refreshTokenValidity | 토큰 재발급 검증용 |
| `blacklist:{accessToken}` | `"logout"` | 토큰 잔여 유효기간 | 로그아웃된 토큰 식별 |
| `force_logout:{userId}` | `LocalDateTime` | accessTokenValidity | RTR 재사용 감지 시 강제 로그아웃 표시 |
| `theft_report:{token}` | `userId` | refreshTokenValidity | 도난 신고 링크 유효 여부 확인 |

---

## 3. 로그인 흐름

```
POST /api/v1/auth/login
  { email, password }

AuthController.login()
  → clientIp/userAgent 추출
  → AuthService.login()
      1. UserRepository.findByEmailAndSocialType(email, SYSTEM)
      2. BCrypt 비밀번호 검증
      3. handleLoginSecurity()  ← 새 기기 감지 + lastLogin 갱신
      4. Access Token + Refresh Token 발급
      5. user.updateRefreshToken(refreshToken)  ← DB 저장
      6. Redis: SET refresh:{userId} = refreshToken  ← CB 경유
  ← Access Token: Response Body
  ← Refresh Token: HttpOnly Cookie (Set-Cookie)
```

**응답:**
```json
{
  "status": "OK",
  "message": "로그인 성공",
  "data": { "accessToken": "eyJ..." }
}
```

---

## 4. 토큰 재발급 (RTR)

**RTR(Refresh Token Rotation):** 재발급 시 Refresh Token도 함께 교체한다.
이전 Refresh Token이 다시 사용되면 탈취로 간주하고 강제 로그아웃 처리한다.

```
POST /api/v1/auth/reissue
  Cookie: refresh_token={token}

AuthService.reissue()
  1. refreshToken JWT 파싱 + REFRESH 타입 검증
  2. Redis에서 stored = GET refresh:{userId}
     └─ Redis 장애 시 → DB fallback: user.getRefreshToken()
  3. stored != 요청 토큰 → RTR 재사용 감지
       user.forceLogout()
       SET force_logout:{userId}
       DELETE refresh:{userId}
       throw SUSPECTED_TOKEN_THEFT (401)
  4. 새 Access Token + 새 Refresh Token 발급
  5. user.updateRefreshToken(newRefreshToken)
     Redis: SET refresh:{userId} = newRefreshToken
  ← 새 Access Token: Response Body
  ← 새 Refresh Token: HttpOnly Cookie 갱신
```

**Redis 장애 시 fallback:**
Redis read가 실패하면 CB가 OPEN 상태로 전환되고, DB에 저장된 `refreshToken` 컬럼으로 검증을 이어간다.
이중 저장 구조(Redis + DB)로 Redis 장애에도 재발급 가용성을 유지한다.

---

## 5. 로그아웃 + 블랙리스트

```
POST /api/v1/auth/logout
  Authorization: Bearer {accessToken}

AuthService.logout()
  1. user.updateRefreshToken(null)  ← DB에서 Refresh Token 제거
  2. Redis: DELETE refresh:{userId}
  3. Access Token 파싱 → 잔여 유효시간 계산
  4. 잔여 유효시간 > 0:
       a. SHA-256(accessToken) → DB: TokenBlacklist.save()
       b. Redis: SET blacklist:{accessToken} = "logout" TTL=잔여시간
  ← 204 No Content (로그아웃 성공)
  ← Set-Cookie: refresh_token= (maxAge=0, 쿠키 만료)
```

**블랙리스트 이중화:** DB는 영속성을 보장하고, Redis는 API Gateway의 빠른 조회를 지원한다.
`TokenBlacklistCleanupScheduler`가 만료된 DB 블랙리스트를 주기적으로 정리한다.

Gateway가 `GET /api/v1/auth/internal/token-status`를 호출할 때 `@Cacheable(cacheNames = "tokenStatus", key = "#token")`으로 캐싱해 중복 조회를 줄인다.

---

## 6. RTR 재사용 감지 (토큰 탈취 방지)

저장된 Refresh Token과 요청 토큰이 불일치하면 **이미 토큰이 교체된 이후 재사용** 시도로 판단한다.

```
저장된 토큰 ≠ 요청 토큰
  → user.forceLogout()          (forceLogoutAt = now())
  → user.updateRefreshToken(null)
  → Redis: SET force_logout:{userId} = now  TTL=accessTokenValidity
  → Redis: DELETE refresh:{userId}
  → 401 SUSPECTED_TOKEN_THEFT
```

이후 해당 userId의 Access Token은 `checkTokenStatus`에서 `forceLogoutAt` 이전 발급 토큰으로 판별되어 Gateway에서 차단된다.

---

## 7. 토큰 도난 신고 흐름

새 기기 로그인이 감지되면 이메일로 보안 알림을 발송한다.
수신자가 "본인 아님" 링크를 클릭하면 계정을 즉시 보호 처리한다.

```
GET /api/v1/auth/report-theft?token={theftReportToken}

AuthService.reportTheft()
  1. Redis: GET theft_report:{token} → userId 조회
     └─ Redis CB OPEN 또는 키 없음 → THEFT_REPORT_TOKEN_EXPIRED (401)
  2. user.forceLogout()
  3. user.updateRefreshToken(null)
  4. Redis: SET force_logout:{userId}  TTL=accessTokenValidity
  5. Redis: DELETE refresh:{userId}
  6. Redis: DELETE theft_report:{token}
  ← 200 "계정 보호 조치가 완료되었습니다."
```

> `theft_report` 링크의 TTL은 `refreshTokenValidity`와 동일하다.

---

## 8. 토큰 상태 검증 (Gateway 연동)

API Gateway의 `JwtAuthenticationFilter`가 인증된 요청마다 호출한다.

```
GET /api/v1/auth/internal/token-status
  Headers:
    X-Token: {accessToken}
    X-User-Id: {userId}
    X-Token-Iat: {issuedAtMillis}
    X-Internal-Secret: {secret}

AuthController.checkTokenStatus()
  1. X-Internal-Secret 검증 (불일치 → 401)
  2. AuthService.checkTokenStatus()
       a. SHA-256(token) → DB TokenBlacklist 조회
          블랙리스트 존재 → { status: BLACKLISTED }
       b. user.forceLogoutAt != null
          && tokenIssuedAtMillis <= forceLogoutMillis
          → { status: FORCE_LOGOUT }
       c. 정상 → { status: VALID }
```

**응답 (TokenStatusResult):**

| status | 의미 | Gateway 처리 |
|--------|------|-------------|
| `VALID` | 정상 토큰 | 요청 통과 |
| `BLACKLISTED` | 로그아웃된 토큰 | 401 반환 |
| `FORCE_LOGOUT` | RTR 재사용 감지 또는 계정 보호 | 401 반환 |

`@Cacheable(cacheNames = "tokenStatus", key = "#token")` 로 캐싱 (로그아웃/도난신고 시 `@CacheEvict`로 무효화).

---

## 9. OAuth2 소셜 로그인

지원 공급자: **Google, Kakao, Naver**

```
브라우저
  → GET /oauth2/authorization/{provider}
  → Spring Security가 공급자 인가 URL로 리다이렉트
     (state, redirect_uri를 HttpOnly Cookie에 저장)
  → 공급자 로그인 완료 → callback URL 호출
  → CustomOAuth2UserService.loadUser()
       1. 공급자 응답에서 email/socialId/name 추출 (OAuthAttributes)
       2. UserRepository에서 (email, socialType) 조회
          - 없으면: 신규 User 생성 (role=USER)
          - 있으면: 기존 User 반환
  → OAuth2SuccessHandler.onAuthenticationSuccess()
       1. handleLoginSecurity() — 새 기기 감지 + lastLogin 갱신
       2. issueOAuth2Tokens() — Access + Refresh Token 발급
       3. Refresh Token → HttpOnly Cookie
       4. Access Token → redirect URI의 URL fragment (#token=...)
          ex) https://front.example.com/callback#token=eyJ...
  → 프론트엔드가 window.location.hash에서 토큰 읽어 Pinia에 저장 후 URL 정리
```

**소셜 계정 이메일 충돌 방지:**
동일 이메일이 이미 일반(SYSTEM) 계정으로 가입되어 있으면 `INVALID_EMAIL` (409) 예외.
`users` 테이블의 유니크 제약 `(email, social_type)` 덕분에 같은 이메일의 Google/Kakao 계정은 별도 행으로 공존 가능.

---

## 10. Redis 서킷 브레이커

Redis 장애 시 인증 전체가 중단되지 않도록 Resilience4j 서킷 브레이커 2개를 적용한다.

| CB 이름 | 대상 연산 | 설정 |
|---------|-----------|------|
| `redis-read` | `GET` (refresh token, theft_report 조회) | slidingWindow:1, threshold:60%, wait:20s |
| `redis-write` | `SET` / `DELETE` (토큰 저장, 블랙리스트 등록) | slidingWindow:1, threshold:80%, wait:30s |

**장애 시 동작:**

| 연산 | Redis 장애 시 |
|------|--------------|
| Refresh Token 조회 (`reissue`) | DB fallback (`user.getRefreshToken()`) |
| Blacklist 등록 (`logout`) | DB에만 저장, Redis 스킵 (로그 경고) |
| Refresh 저장 (`login`, `reissue`) | DB에만 저장, Redis 스킵 |
| Force logout 저장 (`reissue` 재사용 감지) | DB `forceLogoutAt` 저장으로 대체 |
| Theft report 조회 (`reportTheft`) | CB OPEN → `THEFT_REPORT_TOKEN_EXPIRED` (401) |

> **redis-write는 fail-open** — Redis 쓰기 실패 시 예외를 삼키고 요청은 계속 처리된다.
> **redis-read는 fallback** — 읽기 실패 시 DB로 대체한다. (theft_report 조회 제외: Redis가 유일한 저장소)

---

## 11. 새 기기 로그인 보안 알림

로그인 시 `lastLoginIp` / `lastLoginUserAgent`와 현재 요청을 비교해 새 기기 여부를 판단한다.

```
handleLoginSecurity()
  1. user.lastLoginIp != null
     && (ip 다름 || userAgent 다름)
     → 새 기기로 판단

  2. theftReportToken = UUID.randomUUID()
     Redis: SET theft_report:{theftReportToken} = userId  TTL=refreshTokenValidity

  3. CompletableFuture.runAsync():
       이메일 발송: "[보안 알림] 새로운 기기에서 로그인되었습니다."
       링크: {baseUrl}/api/v1/auth/report-theft?token={theftReportToken}

  4. user.updateLastLogin(ip, userAgent)  ← 항상 갱신
```

보안 알림 이메일 발송 또는 Redis 저장 실패는 로그인 자체를 차단하지 않는다 (CB OPEN 시 스킵).
