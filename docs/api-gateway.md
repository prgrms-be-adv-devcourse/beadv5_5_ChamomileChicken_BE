# API Gateway — 중앙집중 인증 구조

## 배경 및 목적

기존 구조에서는 각 서비스가 `common` 모듈의 `JwtAuthenticationFilter`를 통해 JWT를 직접 파싱하여 인증을 처리했다.
이 방식은 다음 문제가 있었다.

| 항목 | 기존 (서비스 직접 파싱) | 현재 (API Gateway 중앙집중) |
|------|----------------------|---------------------------|
| JWT 파싱 위치 | 각 서비스마다 중복 처리 | Gateway 단일 처리 |
| common 모듈 의존 | 모든 서비스가 JWT 코드 의존 | 의존성 제거 가능 |
| 인증 일관성 | 서비스마다 구현 방식 상이 | Gateway에서 통일 |
| 사용자 식별 방식 | SecurityUtil / @AuthenticationPrincipal 혼용 | @CurrentUser 통일 |

---

## 인증 흐름

```
클라이언트
  → Authorization: Bearer <access_token>
  → API Gateway (:8080)
      1. 화이트리스트 경로 확인
      2. JWT 서명 검증
      3. Redis 블랙리스트 조회 (로그아웃 여부 확인)
      4. X-User-Id, X-User-Role 헤더 주입
  → 각 서비스 (내부망)
      @CurrentUser UUID userId       ← X-User-Id 헤더 파싱
      @CurrentUserRole String role   ← X-User-Role 헤더 파싱 (필요한 서비스만)
```

---

## Gateway 핵심 구성

### JwtAuthenticationFilter

요청마다 실행되는 `GlobalFilter`. 처리 순서:

1. 화이트리스트 경로 확인 → 해당하면 JWT 검증 없이 통과
2. `Authorization` 헤더에서 Access Token 추출
3. JWT 서명 검증 및 Access Token 타입 확인
4. Redis 블랙리스트 조회 (로그아웃된 토큰 차단)
5. claims에서 `userId`, `role` 추출 → 헤더에 주입

```java
ServerWebExchange mutatedExchange = exchange.mutate()
    .request(r -> {
        r.header("X-User-Id", userId.toString());
        if (role != null) {
            r.header("X-User-Role", role);
        }
    })
    .build();
```

### 화이트리스트 (JWT 불필요 경로)

| Method | Path |
|--------|------|
| POST | /api/v1/auth/login |
| POST | /api/v1/auth/reissue |
| POST | /api/v1/users/register |
| POST | /api/v1/users/email-check |
| POST | /api/v1/email/** |
| GET | /api/v1/products/** |

### RouteConfig — 서비스 라우팅

| 경로 | 서비스 | 포트 |
|------|--------|------|
| /api/v1/files/** | File | 9000 |
| /api/v1/payments/** | Payment | 9001 |
| /api/v1/settlements/** | Settlement | 9002 |
| /api/v1/auth/**, /api/v1/users/**, /api/v1/email/**, /api/v1/deposits/** | User | 9003 |
| /api/v1/products/** | Product | 9004 |
| /api/v1/orders/** | Order | 9005 |
| /api/v1/admins/** | Admin | 9007 |

---

## 토큰 구조

| 토큰 | 전달 방식 | 용도 |
|------|-----------|------|
| Access Token | `Authorization: Bearer` 헤더 | API 인증 (단기) |
| Refresh Token | `HttpOnly Cookie` (refresh_token) | 토큰 재발급 (장기) |

---

## 각 서비스 적용 가이드

### 1. common 모듈 의존성 제거

`build.gradle`에서 제거:

```groovy
implementation project(':common')
```

### 2. SecurityConfig 단순화

JWT 필터 및 관련 빈 전부 제거. `anyRequest().permitAll()` 적용.

> **permitAll() 설정 이유**
>
> `@CurrentUser` 방식은 Spring Security의 `SecurityContext`를 사용하지 않는다.
> `anyRequest().authenticated()` 설정 시 `SecurityContext`가 비어있어 모든 요청이 차단된다.
> 서비스 포트는 내부 네트워크에서만 접근 가능하도록 인프라 레벨에서 보호한다.

```java
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
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().permitAll()
        );
    return http.build();
}
```

### 3. @CurrentUser / @CurrentUserRole 추가

각 서비스 패키지에 맞게 아래 파일들을 추가한다.

**CurrentUser.java**
```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {}
```

**CurrentUserArgumentResolver.java**
```java
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
            && parameter.getParameterType().equals(UUID.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String userId = request.getHeader(USER_ID_HEADER);

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("X-User-Id 헤더가 없습니다. Gateway를 통한 요청인지 확인하세요.");
        }

        return UUID.fromString(userId);
    }
}
```

**CurrentUserRole.java** (role 인가가 필요한 서비스만 추가)
```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserRole {}
```

**CurrentUserRoleArgumentResolver.java** (role 인가가 필요한 서비스만 추가)
```java
@Component
public class CurrentUserRoleArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserRole.class)
            && parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String role = request.getHeader(USER_ROLE_HEADER);

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("X-User-Role 헤더가 없습니다. Gateway를 통한 요청인지 확인하세요.");
        }

        return role;
    }
}
```

**WebMvcConfig.java**
```java
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    // role이 필요한 서비스는 CurrentUserRoleArgumentResolver도 주입

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
```

### 4. 컨트롤러 적용

```java
// userId만 필요한 경우
@GetMapping("/me")
public ResponseEntity<?> getMyInfo(@CurrentUser UUID userId) { ... }

// role 인가가 필요한 경우
@PostMapping
public ResponseEntity<?> create(
    @CurrentUser UUID userId,
    @CurrentUserRole String role,
    @RequestBody ProductRequestDto request
) {
    if (!"SELLER".equals(role)) throw new ForbiddenException();
    ...
}
```

---

## 보안 고려사항

### anyRequest().permitAll() 보완

서비스 포트를 외부에서 직접 접근할 수 없도록 Docker Compose에서 내부 네트워크로만 노출한다.

```yaml
services:
  api-gateway:
    ports:
      - "8080:8080"   # 외부 노출

  user-service:
    expose:
      - "9003"        # 내부 네트워크만 접근 가능
    networks:
      - internal

networks:
  internal:
    driver: bridge
```

### 서비스 간 직접 호출 (Internal API)

Gateway를 거치지 않는 서비스 간 직접 호출은 `X-INTERNAL-KEY` 헤더로 보호한다.
file 서비스의 `InternalApiFilter` 패턴 참고.
