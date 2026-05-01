# User Service — 인증 구조

## 개요

User 서비스는 API Gateway 중앙집중 인증 구조에서 두 가지 역할을 담당한다.

1. **인증 처리** (`/api/v1/auth/**`, `/oauth2/**`, `/login/oauth2/**`) — 일반 로그인, 소셜 로그인, 로그아웃, 토큰 재발급
2. **사용자 기능** (`/api/v1/users/**`, `/api/v1/deposits/**`) — Gateway가 주입한 헤더로 사용자 식별

---

## 인증 흐름

### 일반 로그인

```
POST /api/v1/auth/login
  → AuthController.login()
      - ClientIpUtils.extractIp(): X-Real-IP 헤더 → 없으면 remoteAddr
      - User-Agent 헤더 추출 (null이면 "unknown")
  → AuthService: (email, SocialType.SYSTEM)으로 유저 조회 → 비밀번호 검증
  → AuthService.handleLoginSecurity(): 새 기기 감지 → 보안 알림 이메일 발송 (비동기)
  → JwtProvider: Access Token + Refresh Token 생성
  → DB: User.refreshToken 저장 (DB-first — Redis 장애와 무관하게 항상 기록)
  → Redis: Refresh Token 저장 (key: "refresh:{userId}") — write CB 적용, 실패 시 swallow
  → 응답: Access Token (body) + Refresh Token (HttpOnly Cookie)
```

### 소셜 로그인 (OAuth2)

```
GET /oauth2/authorization/{provider}   (provider: google | kakao | naver)
  → Spring Security: OAuth2AuthorizationRequestRedirectFilter
  → state 생성 (CSRF 방어) + provider 인증 URL 조립 → 브라우저를 provider로 302 리다이렉트

[provider에서 사용자 인증 완료]

GET /login/oauth2/code/{provider}?code=AUTH_CODE&state=...
  → Spring Security: OAuth2LoginAuthenticationFilter
  → state 검증 → provider 토큰 엔드포인트에서 access_token 교환
  → provider 유저정보 엔드포인트 호출 → 유저 정보 Map 수신
  → CustomOAuth2UserService.loadUser()
       - OAuthAttributes로 provider별 응답 정규화
       - (social_type, social_id)로 기존 유저 조회
       - 없으면 신규 가입 ((email, social_type) 복합 unique — 동일 이메일 + 다른 소셜타입은 허용)
       - CustomOAuth2User 반환
  → OAuth2SuccessHandler.onAuthenticationSuccess()
       - ClientIpUtils.extractIp(): X-Real-IP 헤더 → 없으면 remoteAddr
       - AuthService.handleLoginSecurity(): 새 기기 감지 → 보안 알림 이메일 발송 (비동기)
       - AuthService.issueOAuth2Tokens(): 일반 로그인과 동일한 DB-first + write CB 경로
       - Refresh Token → HttpOnly Cookie
       - 브라우저를 프론트엔드로 리다이렉트: {OAUTH2_REDIRECT_URI}#token={accessToken}

[Vue] window.location.hash에서 Access Token 추출 → Pinia 저장 → URL 정리
```

### 토큰 재발급

```
POST /api/v1/auth/reissue
  → Cookie에서 Refresh Token 추출
  → Redis read CB → Redis에서 Refresh Token 조회 (1회 retry, 50ms delay)
       → CB OPEN 또는 Redis 장애 시: DB fallback (User.refreshToken 필드)
  → RTR 감지 (stored ≠ provided):
       - User.forceLogout() + refreshToken = null → DB commit 보장
         (@Transactional(noRollbackFor = AuthException.class) — 예외 발생해도 rollback 없음)
       - force_logout:{userId} → Redis write CB 적용
       - SUSPECTED_TOKEN_THEFT 예외 반환
  → 새 Access Token + Refresh Token 발급
  → DB: User.refreshToken 업데이트 (DB-first)
  → Redis: 새 Refresh Token으로 교체 — write CB 적용
  → 응답: 새 Access Token (body) + 새 Refresh Token (HttpOnly Cookie)
```

### 로그아웃

```
POST /api/v1/auth/logout
  → DB: User.refreshToken = null
  → Redis: Refresh Token 삭제 (key: "refresh:{userId}") — write CB 적용
  → DB: TokenBlacklist에 sha256(accessToken) + expiresAt 저장 (DB-first)
  → Redis: blacklist:{accessToken} 등록 (만료 시간까지) — write CB 적용
  → Caffeine: @CacheEvict("tokenStatus", key=accessToken) — 즉시 무효화
  → Refresh Token Cookie 만료 처리
```

### 새 기기 감지 및 토큰 탈취 신고

```
[로그인 시 - 일반/OAuth2 공통]
  → handleLoginSecurity(userId, clientIp, userAgent)
      - lastLoginIp 또는 lastLoginUserAgent 변경 감지 시 새 기기로 판단
      - theft_report 토큰 생성 → Redis 저장 (key: "theft_report:{token}", TTL: 7일)
      - 보안 알림 이메일 비동기 발송 (CompletableFuture.runAsync)
      - lastLoginIp / lastLoginUserAgent 업데이트

GET /api/v1/auth/report-theft?token={token}
  → Redis에서 theft_report 토큰으로 userId 조회
  → force_logout:{userId} 설정 → Redis에서 Refresh Token 삭제
  → 이후 해당 userId의 모든 기존 Access Token 차단
```

---

## Redis 장애 대응 (Circuit Breaker)

Redis 장애 시에도 로그인/재발급/로그아웃 기능이 정상 동작하도록 DB-first 패턴과 Circuit Breaker를 도입했다.

### 설계 원칙

- **DB-first**: Refresh Token은 항상 `User.refreshToken` 필드(DB)에 먼저 저장된다. Redis는 캐시 역할이며 장애 시 swallow.
- **Redis-first + DB fallback**: `reissue()`는 Redis를 우선 조회하고, CB OPEN 또는 장애 시 DB에서 조회한다.
- **read/write Circuit Breaker 분리**: read 장애(가용성 저하)와 write 장애(정합성 저하)는 성격이 다르므로 독립적으로 관리한다.

### Circuit Breaker 설정

| 구분 | 이름 | failureRateThreshold | waitDuration | slidingWindowSize | minimumCalls |
|------|------|---------------------|--------------|-------------------|--------------|
| read | `redis-read` | 60% | 20s | 5 | 3 |
| write | `redis-write` | 80% | 30s | 5 | 3 |

- read CB: OPEN → `reissue()` DB fallback, `reportTheft()` THEFT_REPORT_TOKEN_EXPIRED 반환 (graceful)
- write CB: OPEN → Redis write 스킵, DB는 이미 저장된 상태이므로 기능 영향 없음
- HALF-OPEN: 2회 테스트 요청 → 성공 시 CLOSED 복귀

### retry 전략

read 작업만 `executeWithRedisRetry()`로 1회 재시도 (50ms delay)를 한다.
write 작업은 retry 없이 CB만 적용한다. write 재시도는 멱등성이 보장되지 않는 경우가 있어 배제했다.

### Caffeine 캐시 연동

`checkTokenStatus()` 결과는 Caffeine에 60초 TTL로 캐싱된다 (key: accessToken).
`logout()` 시 `@CacheEvict`로 즉시 무효화하여 로그아웃 직후 요청에서 stale 캐시가 반환되는 것을 방지한다.

### RTR 감지 시 트랜잭션 보장

`reissue()`에서 RTR(Refresh Token Reuse) 감지 시 `AuthException`을 던져 응답은 4xx를 반환하지만,
`@Transactional(noRollbackFor = AuthException.class)`를 적용하여 `forceLogout()` 및 `refreshToken = null` DB write는 rollback되지 않고 커밋된다.

---

## 토큰 전달 방식 비교

| | 일반 로그인 | 소셜 로그인 |
|---|---|---|
| Access Token | 응답 body (`{ accessToken }`) | 리다이렉트 URL fragment (`#token=`) |
| Refresh Token | HttpOnly Cookie (`refresh_token`) | HttpOnly Cookie (`refresh_token`) |

소셜 로그인은 인증 완료 후 반드시 브라우저 리다이렉트가 발생하므로 응답 body를 사용할 수 없다.
Access Token을 URL fragment에 실어 전달하면 서버 로그에 남지 않고, Vue가 `window.location.hash`로 읽은 뒤 URL을 정리한다.

---

## SecurityConfig 구조

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository)
                )
                .userInfoEndpoint(userInfo ->
                    userInfo.userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### ObjectMapper 빈 위치

`HttpCookieOAuth2AuthorizationRequestRepository`가 OAuth2 state를 Redis에 JSON으로 저장하기 위해 `ObjectMapper`를 주입받는다.
Spring Boot 자동 구성 `ObjectMapper`를 그대로 주입받으며, 별도 빈 등록은 필요 없다.

### anyRequest().permitAll() 설정 이유

`@CurrentUser` 방식은 Spring Security `SecurityContext`를 채우지 않는다.
`authenticated()` 설정 시 모든 요청이 401로 차단된다.
보안은 인프라 레벨(Docker 내부 네트워크)에서 보완한다. → [api-gateway.md 보안 고려사항](./api-gateway.md#보안-고려사항) 참고

---

## OAuth2 구성 요소

OAuth2 관련 클래스는 모두 `auth/infrastructure/oauth2/` 패키지에 위치한다.
Spring Security OAuth2 메커니즘에 직접 의존하는 인프라 관심사이며, 4개 클래스가 하나의 파이프라인으로 순서대로 협력한다.

### OAuthAttributes

provider별로 다른 JSON 응답 구조를 `name / email / socialId / socialType`의 단일 구조로 정규화하는 값 객체.
Spring 컴포넌트가 아니며 `CustomOAuth2UserService` 내부에서만 사용된다.

| provider | id 키 | email 위치 | name 위치 |
|---|---|---|---|
| Google | `sub` | 최상위 | 최상위 |
| Kakao | `id` (최상위) | `kakao_account.email` | `kakao_account.profile.nickname` |
| Naver | `response.id` | `response.email` | `response.name` |

### CustomOAuth2User

Spring Security의 `OAuth2User` 인터페이스 구현체. 우리 `User` 엔티티를 감싸서 `SuccessHandler`에서 꺼낼 수 있게 한다.
Spring Security는 인증 완료 후 이 객체를 `Authentication.getPrincipal()`에 저장한다.

### CustomOAuth2UserService

`DefaultOAuth2UserService`를 상속. `loadUser()`는 `super.loadUser()`로 provider 통신을 위임한 뒤 우리 비즈니스 로직(유저 조회/신규 가입)을 실행한다.

- `(social_type, social_id)` 기준으로 기존 유저 조회 → 있으면 로그인, 없으면 신규 가입
- 동일 이메일이더라도 소셜 타입이 다르면 별개 계정으로 신규 가입 허용

### HttpCookieOAuth2AuthorizationRequestRepository

STATELESS 세션에서는 Spring Security 기본 구현(`HttpSessionOAuth2AuthorizationRequestRepository`)이 동작하지 않는다.
OAuth2 state를 쿠키에 직렬화하는 방식은 Java `SerializationUtils`를 사용하며 역직렬화 RCE 취약점에 노출된다.

이를 해결하기 위해 **Redis 서버사이드 저장** 방식으로 재구현했다.
- state 저장 시: `OAuth2AuthorizationRequest` → `OAuth2AuthorizationRequestDto`로 변환 → JSON으로 Redis 저장 (TTL: 60초), 쿠키에는 state 값(조회 키)만 저장
- state 조회 시: 쿠키에서 state 값 추출 → Redis 조회 → JSON → DTO → `OAuth2AuthorizationRequest` 재구성
- `removeAuthorizationRequest()`는 `loadAuthorizationRequest()`에 위임 (Spring Security 명세 준수)

### OAuth2AuthorizationRequestDto

`OAuth2AuthorizationRequest`는 Jackson 역직렬화가 불가능한 구조라 직접 직렬화할 수 없다.
`from()` 정적 팩토리로 변환하고, `toRequest()`로 다시 `OAuth2AuthorizationRequest`를 재구성하는 DTO.

### OAuth2SuccessHandler

`AuthenticationSuccessHandler` 구현체. 일반 로그인의 `AuthController.login()` 후반부와 동일한 역할을 한다.
`authentication.getPrincipal()`에서 `CustomOAuth2User`를 꺼내 JWT 발급 → Redis 저장 → Cookie 세팅 → 프론트 리다이렉트 순으로 처리한다.

---

## 일반 회원 vs 소셜 회원

| | 일반 회원 | 소셜 회원 |
|---|---|---|
| `password` | BCrypt 해시값 | `null` |
| `phone` | 필수 | `null` (선택) |
| `social_type` | `SYSTEM` | `GOOGLE` / `KAKAO` / `NAVER` |
| `social_id` | `null` | provider가 발급한 고유 ID |
| 로그인 방식 | 이메일 + 비밀번호 | OAuth2 provider 인증 |
| 동일 이메일 중복 가입 | 불가 (`(email, SYSTEM)` 기준) | 허용 (`(email, social_type)` 복합 unique — 동일 이메일 + 다른 소셜타입은 별개 계정) |

---

## @CurrentUser 사용법

Gateway가 주입한 `X-User-Id` 헤더를 컨트롤러 파라미터로 직접 받는다.
일반 로그인, 소셜 로그인 모두 인증 완료 후 동일한 JWT 구조를 사용하므로 컨트롤러 코드 변경 없이 동작한다.

```java
@GetMapping("/me")
public ResponseEntity<UserResponseDto> getMyInfo(
    @CurrentUser UUID userId
) {
    return ResponseEntity.ok(userUseCase.getMyInfo(userId));
}
```

---

## JWT Claims 구조

| Claim | 키 | 타입 | 설명 |
|-------|----|------|------|
| 사용자 ID | `userId` | UUID (String) | 고유 식별자 |
| 사용자 역할 | `role` | String | `USER` / `SELLER` / `ADMIN` |
| 토큰 타입 | `type` | String | `access` / `refresh` |

### role 기본값

회원가입 시 role은 항상 `USER`로 설정된다 (일반 가입, 소셜 가입 모두 동일).
`SELLER` 승급은 별도 관리자 처리를 통해 이루어진다.

---

## 관련 파일

| 파일 | 역할 |
|------|------|
| `common/auth/CurrentUser.java` | @CurrentUser 파라미터 어노테이션 |
| `common/auth/CurrentUserArgumentResolver.java` | X-User-Id 헤더 → UUID 변환 |
| `common/auth/CurrentUserRole.java` | @CurrentUserRole 파라미터 어노테이션 |
| `common/auth/CurrentUserRoleArgumentResolver.java` | X-User-Role 헤더 → String 변환 |
| `common/config/WebMvcConfig.java` | ArgumentResolver 등록 |
| `common/config/SecurityConfig.java` | Spring Security 설정 (OAuth2 로그인 포함) |
| `auth/application/AuthService.java` | 일반 로그인/로그아웃/재발급/토큰탈취신고/새기기감지 비즈니스 로직 |
| `auth/presentation/AuthController.java` | 인증 API 엔드포인트 |
| `common/util/ClientIpUtils.java` | IP 추출 유틸 (X-Real-IP → remoteAddr fallback) |
| `auth/infrastructure/oauth2/OAuthAttributes.java` | provider별 응답 정규화 값 객체 |
| `auth/infrastructure/oauth2/CustomOAuth2User.java` | OAuth2User ↔ User 엔티티 브리지 |
| `auth/infrastructure/oauth2/CustomOAuth2UserService.java` | OAuth2 유저 조회/신규 가입 처리 |
| `auth/infrastructure/oauth2/OAuth2SuccessHandler.java` | JWT 발급 + Cookie + 프론트 리다이렉트 |
| `auth/infrastructure/oauth2/CookieUtils.java` | 쿠키 조작 유틸 (getCookie / addCookie / deleteCookie), static 메서드만 유지 |
| `auth/infrastructure/oauth2/HttpCookieOAuth2AuthorizationRequestRepository.java` | OAuth2 state Redis 서버사이드 저장 + 쿠키 키 관리 |
| `auth/infrastructure/oauth2/OAuth2AuthorizationRequestDto.java` | OAuth2AuthorizationRequest Redis JSON 직렬화용 DTO |
