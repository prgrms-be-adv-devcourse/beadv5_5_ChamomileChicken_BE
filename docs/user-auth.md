# User Service — 인증 구조

## 개요

User 서비스는 API Gateway 중앙집중 인증 구조에서 두 가지 역할을 담당한다.

1. **인증 처리** (`/api/v1/auth/**`) — 로그인, 로그아웃, 토큰 재발급
2. **사용자 기능** (`/api/v1/users/**`, `/api/v1/deposits/**`) — Gateway가 주입한 헤더로 사용자 식별

---

## 인증 흐름

### 로그인

```
POST /api/v1/auth/login
  → AuthController.login()
  → AuthService: 이메일/비밀번호 검증
  → JwtProvider: Access Token + Refresh Token 생성
  → Redis: Refresh Token 저장 (key: userId)
  → 응답: Access Token (body) + Refresh Token (HttpOnly Cookie)
```

### 토큰 재발급

```
POST /api/v1/auth/reissue
  → Cookie에서 Refresh Token 추출
  → Redis에서 Refresh Token 검증
  → 새 Access Token 발급
  → 응답: 새 Access Token (body)
```

### 로그아웃

```
POST /api/v1/auth/logout
  → Access Token → Redis 블랙리스트 등록 (만료 시간까지 유지)
  → Redis에서 Refresh Token 삭제
  → Refresh Token Cookie 삭제
```

---

## SecurityConfig 구조

```java
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

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
로그인 및 토큰 재발급 시 `JwtProvider`로 Access Token / Refresh Token을 생성한다.

### ObjectMapper 빈을 유지하는 이유

Kafka Consumer (`DepositRefundConsumer` 등)가 메시지 역직렬화에 `ObjectMapper`를 사용한다.
Spring Boot 자동 구성 `ObjectMapper`와 충돌을 방지하기 위해 명시적으로 빈으로 등록한다.

### anyRequest().permitAll() 설정 이유

`@CurrentUser` 방식은 Spring Security `SecurityContext`를 채우지 않는다.
`authenticated()` 설정 시 모든 요청이 401로 차단된다.
보안은 인프라 레벨(Docker 내부 네트워크)에서 보완한다. → [api-gateway.md 보안 고려사항](./api-gateway.md#보안-고려사항) 참고

---

## @CurrentUser 사용법

Gateway가 주입한 `X-User-Id` 헤더를 컨트롤러 파라미터로 직접 받는다.

```java
// 현재 로그인 사용자 정보 조회
@GetMapping("/me")
public ResponseEntity<UserResponseDto> getMyInfo(
    @CurrentUser UUID userId
) {
    return ResponseEntity.ok(userUseCase.getMyInfo(userId));
}

// 현재 로그인 사용자 정보 수정
@PutMapping("/me")
public ResponseEntity<Void> updateMyInfo(
    @CurrentUser UUID userId,
    @Valid @RequestBody UpdateUserRequestDto request
) {
    userUseCase.updateMyInfo(userId, request);
    return ResponseEntity.noContent().build();
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

회원가입 시 role은 항상 `USER`로 설정된다.
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
| `common/config/SecurityConfig.java` | Spring Security 설정 |
| `auth/application/AuthService.java` | 로그인/로그아웃/재발급 비즈니스 로직 |
| `auth/presentation/AuthController.java` | 인증 API 엔드포인트 |
