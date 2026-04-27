# API Gateway — 중앙집중 인증 구조

## 배경 및 목적

기존 구조에서는 각 서비스가 `common` 모듈의 `JwtAuthenticationFilter`를 통해 JWT를 직접 파싱하여 인증을 처리했다.
이 방식은 다음 문제가 있었다.

| 항목 | 기존 (서비스 직접 파싱) | 현재 (API Gateway 중앙집중) |
|------|----------------------|---------------------------|
| JWT 파싱 위치 | 각 서비스마다 중복 처리 | Gateway 단일 처리 |
| common 모듈 의존 | 모든 서비스가 JWT 코드 의존 | 의존성 제거 가능 |
| 인증 일관성 | 서비스마다 구현 방식 상이 | Gateway에서 통일 |
| 화이트리스트 관리 | 코드 하드코딩 | DB 관리 (gateway_whitelist) |
| 역할 기반 접근 제어 | 없음 | DB 관리 (gateway_route_policy) |

---

## 인증 흐름

```
클라이언트
  → Authorization: Bearer <access_token>
  → API Gateway (:8080)
      1. X-User-Id / X-User-Role 헤더 위조 방지 (sanitize)
      2. DB(gateway_whitelist) 조회 → 화이트리스트 경로면 바로 통과
      3. JWT 서명 검증 및 Access Token 타입 확인
      4. Redis 블랙리스트 조회 (로그아웃 여부 확인)
      5. DB(gateway_route_policy) 조회 → RBAC 역할 검사
      6. X-User-Id, X-User-Role 헤더 주입
  → 각 서비스 (내부망)
      @CurrentUser UUID userId       ← X-User-Id 헤더 파싱
      @CurrentUserRole String role   ← X-User-Role 헤더 파싱 (필요한 서비스만)
```

---

## Gateway 핵심 구성

### JwtAuthenticationFilter

요청마다 실행되는 `GlobalFilter`. 처리 순서:

1. **헤더 위조 방지**: 요청의 `X-User-Id`, `X-User-Role` 헤더를 제거 (클라이언트 위조 차단)
2. **화이트리스트 확인**: `WhitelistService`에서 DB 조회 → 해당하면 JWT 검증 없이 통과
3. **JWT 검증**: `Authorization` 헤더에서 Access Token 추출, 서명 검증, 토큰 타입 확인
4. **Redis 블랙리스트 조회**: 로그아웃 처리된 토큰 차단
5. **RBAC 검사**: `RbacService`에서 DB 조회 → role이 허용되지 않으면 403 반환
6. **헤더 주입**: claims에서 `userId`, `role` 추출 후 내부 헤더로 주입

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

---

### WhitelistService — 화이트리스트 (JWT 불필요 경로)

DB(`gateway_whitelist`)에서 조회하며, Caffeine 로컬 캐시(TTL 10분)로 매 요청 DB I/O를 방지한다.

| Method | Path | 설명 |
|--------|------|------|
| POST | /api/v1/auth/login | 로그인 |
| POST | /api/v1/auth/reissue | 토큰 재발급 |
| POST | /api/v1/users/register | 회원가입 |
| POST | /api/v1/users/email-check | 이메일 중복 확인 |
| POST | /api/v1/email/** | 이메일 인증 |
| GET | /api/v1/products | 상품 목록 조회 |
| GET | /api/v1/products/* | 상품 상세 조회 |
| GET | /api/v1/products/*/schedules | 상품 일정 목록 조회 |
| GET | /api/v1/products/*/availability | 스케줄 예약 가능 여부 |
| GET | /api/v1/products/*/reviewList | 상품 리뷰 목록 조회 |
| GET | /api/v1/products/*/reviews/* | 리뷰 단건 조회 |
| GET | /oauth2/authorization/** | 소셜 로그인 시작 |
| GET | /login/oauth2/code/** | 소셜 로그인 콜백 |

화이트리스트 정책 변경은 DB 데이터 수정만으로 적용 가능하다. 캐시 TTL(10분) 이내엔 기존 정책이 유지된다.

---

### RbacService — 역할 기반 접근 제어

DB(`gateway_route_policy`)에서 조회하며, Caffeine 로컬 캐시(TTL 3분)를 사용한다.

**매칭 로직**: 경로 패턴이 일치하는 모든 정책 중 **패턴 길이 기준 내림차순 정렬** 후 가장 구체적인 정책 하나를 선택한다.
- 정책이 없는 경로: 통과 (인증만 되면 허용)
- 정책이 있는 경로: `allowed_roles`에 포함된 경우만 허용

| Method | Path | allowed_roles | 설명 |
|--------|------|---------------|------|
| POST | /api/v1/products | SELLER,ADMIN | 상품 등록 |
| PUT | /api/v1/products/* | SELLER,ADMIN | 상품 수정 |
| DELETE | /api/v1/products/* | SELLER,ADMIN | 상품 삭제 |
| POST | /api/v1/products/*/schedules | SELLER,ADMIN | 일정 등록 |
| PUT | /api/v1/products/*/schedules/* | SELLER,ADMIN | 일정 수정 |
| DELETE | /api/v1/products/*/schedules/* | SELLER,ADMIN | 일정 삭제 |
| GET | /api/v1/settlements | SELLER,ADMIN | 정산 목록 조회 |
| GET | /api/v1/settlements/ready | SELLER,ADMIN | READY 정산 목록 |
| GET | /api/v1/settlements/* | SELLER,ADMIN | 정산 단건 조회 |
| GET | /api/v1/settlements/me | SELLER,ADMIN | 판매자 정산 목록 조회 |
| GET | /api/v1/settlements/me/*/details | SELLER,ADMIN | 판매자 정산 상세 항목 조회 |
| POST | /api/v1/internal-batch/settlements/calculate | ADMIN | 정산 계산 배치 수동 실행 |
| POST | /api/v1/internal-batch/settlements/transfer | ADMIN | 정산 송금 배치 수동 실행 |
| GET | /api/v1/admins/** | ADMIN | 어드민 조회 |
| POST | /api/v1/admins/** | ADMIN | 어드민 등록 |
| PATCH | /api/v1/admins/** | ADMIN | 어드민 수정 |
| DELETE | /api/v1/admins/** | ADMIN | 어드민 삭제 |

---

### DB 연동 — R2DBC

Spring Cloud Gateway는 WebFlux(Netty 이벤트 루프) 기반이므로 JDBC(블로킹)을 사용할 수 없다.
`spring-boot-starter-data-r2dbc`를 사용하여 DB 조회를 논블로킹으로 처리한다.

| 환경 | Driver | URL |
|------|--------|-----|
| dev | r2dbc-h2 (in-memory) | `r2dbc:h2:mem:///gateway` |
| prod | r2dbc-postgresql | `r2dbc:postgresql://${POSTGRES_HOST}/...` |

환경별 초기화 방식이 다르다.

- **dev**: `schema.sql` / `data.sql` (resources 루트)을 `spring.sql.init.mode=always` 설정으로 앱 시작 시 자동 실행. H2 인메모리 특성상 재시작마다 초기화되므로 매번 재실행된다.
- **prod**: Flyway가 `db/migration/V1__init_schema.sql` / `V2__insert_data.sql`을 순서대로 실행. 이미 적용된 버전은 `flyway_schema_history` 테이블로 추적하여 재실행하지 않는다.

---

### RouteConfig — 서비스 라우팅

| 경로 | 서비스 | 포트 |
|------|--------|------|
| /api/v1/files/** | File | 9000 |
| /api/v1/payments/** | Payment | 9001 |
| /api/v1/settlements/** | Settlement | 9002 |
| /api/v1/internal-batch/settlements/** | Settlement | 9002 |
| /api/v1/email/** | User (Rate Limit 적용) | 9003 |
| /api/v1/auth/**, /api/v1/users/**, /api/v1/deposits/**, /oauth2/authorization/**, /login/oauth2/code/** | User | 9003 |
| /api/v1/products/** | Product | 9004 |
| /api/v1/orders/** | Order | 9005 |
| /api/v1/admins/** | Admin | 9007 |

---

### GatewayConfig — Rate Limiting

이메일 인증 경로(`/api/v1/email/**`)에 Redis 기반 Rate Limiting이 적용된다.
`KeyResolver`는 `X-Forwarded-For` 헤더를 우선 확인하여 프록시/로드밸런서 환경에서도 실제 클라이언트 IP를 식별한다.

```
replenishRate: 1 req/s
burstCapacity: 3 req
```

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

// role에 따라 비즈니스 로직을 다르게 처리해야 하는 경우
@PostMapping
public ResponseEntity<?> create(
    @CurrentUser UUID userId,
    @CurrentUserRole String role,
    @RequestBody ProductRequestDto request
) {
    // 역할 기반 접근 제어는 Gateway RBAC에서 처리됨
    // @CurrentUserRole은 서비스 내 비즈니스 로직 분기 용도로만 사용
    ...
}
```

---

## 보안 고려사항

### 헤더 위조 방지

클라이언트가 `X-User-Id`, `X-User-Role` 헤더를 임의로 설정해 권한을 위조하는 것을 막기 위해,
`JwtAuthenticationFilter`가 가장 먼저 해당 헤더를 제거(`sanitize`)한 뒤 JWT 검증 결과로만 재주입한다.

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
호출 측에서 헤더에 사전 공유된 키를 포함하고, 수신 측 필터에서 이를 검증한다.
