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
  → AuthService: 이메일/비밀번호 검증
  → JwtProvider: Access Token + Refresh Token 생성
  → Redis: Refresh Token 저장 (key: "refresh:{userId}")
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
       - 없으면 신규 가입 (이메일 중복 시 에러)
       - CustomOAuth2User 반환
  → OAuth2SuccessHandler.onAuthenticationSuccess()
       - Access Token + Refresh Token 생성
       - Redis: Refresh Token 저장 (key: "refresh:{userId}")
       - Refresh Token → HttpOnly Cookie
       - 브라우저를 프론트엔드로 리다이렉트: {OAUTH2_REDIRECT_URI}#token={accessToken}

[Vue] window.location.hash에서 Access Token 추출 → Pinia 저장 → URL 정리
```

### 토큰 재발급

```
POST /api/v1/auth/reissue
  → Cookie에서 Refresh Token 추출
  → Redis에서 Refresh Token 검증
  → 새 Access Token + Refresh Token 발급
  → Redis: 새 Refresh Token으로 교체
  → 응답: 새 Access Token (body) + 새 Refresh Token (HttpOnly Cookie)
```

### 로그아웃

```
POST /api/v1/auth/logout
  → Access Token → Redis 블랙리스트 등록 (만료 시간까지 유지)
  → Redis에서 Refresh Token 삭제
  → Refresh Token Cookie 만료 처리
```

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
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

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
                .userInfoEndpoint(userInfo ->
                    userInfo.userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
            );
        return http.build();
    }

    @Bean
    public JwtProvider jwtProvider(JwtProperties jwtProperties) {
        return new JwtProvider(jwtProperties);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### JwtProvider 빈을 유지하는 이유

Gateway가 JWT 검증을 담당하지만, User 서비스는 **토큰 생성** 책임이 있다.
일반 로그인(`AuthService`), 소셜 로그인(`OAuth2SuccessHandler`), 토큰 재발급 모두 `TokenProvider`로 토큰을 생성한다.

### ObjectMapper 빈을 유지하는 이유

Kafka Consumer (`DepositRefundConsumer` 등)가 메시지 역직렬화에 `ObjectMapper`를 사용한다.
Spring Boot 자동 구성 `ObjectMapper`와 충돌을 방지하기 위해 명시적으로 빈으로 등록한다.

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
- 신규 가입 시 동일 이메일의 기존 계정이 있으면 `INVALID_EMAIL` 에러 반환

### OAuth2SuccessHandler

`AuthenticationSuccessHandler` 구현체. 일반 로그인의 `AuthController.login()` 후반부와 동일한 역할을 한다.
`authentication.getPrincipal()`에서 `CustomOAuth2User`를 꺼내 JWT 발급 → Redis 저장 → Cookie 세팅 → 프론트 리다이렉트 순으로 처리한다.

---

## 일반 회원 vs 소셜 회원

| | 일반 회원 | 소셜 회원 |
|---|---|---|
| `password` | BCrypt 해시값 | `null` |
| `phone` | 필수 | `null` (선택) |
| `social_type` | `null` | `GOOGLE` / `KAKAO` / `NAVER` |
| `social_id` | `null` | provider가 발급한 고유 ID |
| 로그인 방식 | 이메일 + 비밀번호 | OAuth2 provider 인증 |
| 동일 이메일 중복 가입 | 불가 | 불가 (provider 무관) |

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
| `auth/application/AuthService.java` | 일반 로그인/로그아웃/재발급 비즈니스 로직 |
| `auth/presentation/AuthController.java` | 인증 API 엔드포인트 |
| `auth/infrastructure/oauth2/OAuthAttributes.java` | provider별 응답 정규화 값 객체 |
| `auth/infrastructure/oauth2/CustomOAuth2User.java` | OAuth2User ↔ User 엔티티 브리지 |
| `auth/infrastructure/oauth2/CustomOAuth2UserService.java` | OAuth2 유저 조회/신규 가입 처리 |
| `auth/infrastructure/oauth2/OAuth2SuccessHandler.java` | JWT 발급 + Cookie + 프론트 리다이렉트 |
