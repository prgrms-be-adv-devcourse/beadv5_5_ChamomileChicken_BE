# Admin 서비스 설계

## 개요

Admin 서비스는 운영팀/관리자 전용 백오피스 서비스다. (port: 9007)

일반 서비스와 달리 **여러 서비스의 DB를 직접 참조**하는 방식으로 구현된다.
Admin은 트래픽이 현저히 낮고, 각 서비스에 Internal API를 추가하는 비용보다 직접 참조의 실용성이 높다고 판단했다.

---

## 아키텍처 결정

### ADMIN 역할 검증

Admin은 보안 최상급 영역이므로 이중 검증 구조를 채택한다.

- **1차 (Gateway)**: JWT 검증 + Redis 블랙리스트 체크 → `X-User-Role` 헤더 주입
- **2차 (Admin 서비스)**: `AdminRoleInterceptor`에서 `X-User-Role == ADMIN` 재확인

클라이언트가 `X-User-Role` 헤더를 임의로 조작해도 Gateway가 JWT claim 기반으로 덮어쓰므로 위조가 불가능하다.
별도의 Admin 엔티티는 존재하지 않는다.

```
클라이언트 (ADMIN 유저)
  → Authorization: Bearer <access_token>
  → API Gateway (:8080)
      JWT 검증 → X-User-Id, X-User-Role: ADMIN 헤더 주입
  → Admin 서비스 (:9007)
      AdminRoleInterceptor → X-User-Role == "ADMIN" 검증
      → 이하 비즈니스 로직 처리
```

### 데이터 접근 전략

모든 READ / WRITE는 각 서비스 DB를 직접 참조한다.
단, **상품 강제 내리기**는 직접 DB soft delete 후 Kafka 이벤트를 발행해 연쇄 처리를 위임한다.

| 기능 | 방식 | 이유 |
|------|------|------|
| 전체 유저 목록 조회 | 직접 DB (user) | 조회, 부작용 없음 |
| 특정 유저 상세 조회 | 직접 DB (user) | 조회, 부작용 없음 |
| 셀러 승인 | 직접 DB (user) | role 컬럼만 변경, 연쇄 로직 없음 |
| 전체 상품 조회 | 직접 DB (product) | 조회, 부작용 없음 |
| 상품 강제 내리기 | 직접 DB (product) + Kafka 이벤트 | soft delete 후 schedule 취소 + ES 삭제를 product 서비스에 위임 |
| 전체 주문 조회 | 직접 DB (order) | 조회, 부작용 없음 |
| 정산 현황 조회 | 직접 DB (settlement) | 조회, 부작용 없음 |
| 리뷰 삭제 | 직접 DB (product) | 소프트딜리트 (deleteDt 셋팅), 감사 추적 가능 |

> **상품 강제 내리기를 이벤트 기반으로 처리하는 이유**
>
> 상품 강제 내리기는 product soft delete 이후 연관 schedule 취소, ES 인덱스 삭제까지
> 연쇄 처리가 필요하다. Internal API(동기 호출) 방식은 product 서비스 장애 시 Admin도 실패하는
> 강한 결합이 생긴다.
>
> `@TransactionalEventListener(AFTER_COMMIT)`으로 트랜잭션 커밋 이후에만 Kafka 이벤트를 발행하고,
> product 서비스가 이를 수신해 나머지 연쇄 처리를 담당한다.
> 트랜잭션 롤백 시에는 이벤트가 발행되지 않아 안전하며, 실패 시 보상 트랜잭션으로 복구한다.

### 상품 강제 내리기 흐름

```
Admin 서비스
  1. Product soft delete (DB 직접)
  2. @TransactionalEventListener(AFTER_COMMIT) → Kafka: ProductForceDownEvent 발행

Product 서비스
  3. ProductForceDownEvent 수신
  4. 연관 Schedule 전체 soft delete
  5. ES 인덱스 삭제
  6. 실패 시 → 보상 트랜잭션으로 Product 상태 복구 이벤트 발행
```

### Multi-DataSource 구성

Admin 서비스는 여러 DB에 연결해야 하므로 DataSource를 분리해서 설정한다.

```
AdminApplication
  ├── userDataSource       → user DB
  ├── productDataSource    → product DB
  ├── orderDataSource      → order DB
  └── settlementDataSource → settlement DB
```

각 DataSource는 `@Qualifier`로 구분하며, 도메인 패키지별로 `EntityManagerFactory`와 `TransactionManager`를 분리한다.

---

## 기능 목록

### 유저 관리

| 기능 | Method | Path |
|------|--------|------|
| 전체 유저 목록 조회 | GET | /api/v1/admins/users |
| 특정 유저 상세 조회 | GET | /api/v1/admins/users/{userId} |
| 셀러 승인 | PATCH | /api/v1/admins/users/{userId}/approve-seller |

### 상품 관리

| 기능 | Method | Path |
|------|--------|------|
| 전체 상품 조회 | GET | /api/v1/admins/products |
| 상품 강제 내리기 | PATCH | /api/v1/admins/products/{productId}/force-down |

### 주문/정산 조회

| 기능 | Method | Path |
|------|--------|------|
| 전체 주문 조회 | GET | /api/v1/admins/orders |
| 정산 현황 조회 | GET | /api/v1/admins/settlements |

### 리뷰 관리

| 기능 | Method | Path |
|------|--------|------|
| 부적절한 리뷰 삭제 | DELETE | /api/v1/admins/reviews/{reviewId} |

---

## 패키지 구조

```
service/admin/src/main/java/jabaclass/admin/
│
├── AdminApplication.java
│
├── common/
│   ├── auth/
│   │   ├── CurrentUser.java                    # @CurrentUser 어노테이션
│   │   ├── CurrentUserArgumentResolver.java    # X-User-Id 헤더 파싱
│   │   └── AdminRoleInterceptor.java           # X-User-Role: ADMIN 검증
│   ├── config/
│   │   ├── WebMvcConfig.java                   # ArgumentResolver, Interceptor 등록
│   │   ├── KafkaProducerConfig.java            # Kafka Producer 설정
│   │   ├── UserDataSourceConfig.java           # user DB DataSource/EMF/TM
│   │   ├── ProductDataSourceConfig.java        # product + review DB DataSource/EMF/TM
│   │   ├── OrderDataSourceConfig.java          # order DB DataSource/EMF/TM
│   │   ├── SettlementDataSourceConfig.java     # settlement DB DataSource/EMF/TM
│   │   └── SwaggerConfig.java
│   ├── dto/
│   │   └── ApiResponseDto.java
│   └── error/
│       ├── ErrorCode.java
│       ├── AdminErrorCode.java
│       ├── CommonErrorCode.java
│       ├── BusinessException.java
│       └── GlobalExceptionHandler.java
│
├── user/
│   ├── application/
│   │   ├── usecase/
│   │   │   └── UserAdminUseCase.java
│   │   └── service/
│   │       └── UserAdminService.java
│   ├── domain/
│   │   ├── model/
│   │   │   ├── User.java                       # user DB 직접 참조용 엔티티 (필요 필드만)
│   │   │   └── UserRole.java
│   │   └── repository/
│   │       └── UserAdminRepository.java
│   ├── infrastructure/
│   │   └── persistence/
│   │       ├── UserAdminJpaRepository.java
│   │       └── UserAdminRepositoryAdapter.java
│   └── presentation/
│       ├── controller/
│       │   ├── UserAdminApi.java
│       │   └── UserAdminController.java
│       └── dto/
│           └── response/
│               └── UserAdminResponseDto.java
│
├── product/
│   ├── application/
│   │   ├── usecase/
│   │   │   └── ProductAdminUseCase.java
│   │   └── service/
│   │       └── ProductAdminService.java
│   ├── domain/
│   │   ├── model/
│   │   │   └── Product.java                    # product DB 직접 참조용 엔티티 (필요 필드만)
│   │   └── repository/
│   │       └── ProductAdminRepository.java
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── ProductAdminJpaRepository.java
│   │   │   └── ProductAdminRepositoryAdapter.java
│   │   └── kafka/
│   │       ├── AdminProductEvent.java          # 강제 내리기 이벤트 (type + productId)
│   │       └── AdminProductEventPublisher.java # AFTER_COMMIT 이벤트 발행
│   └── presentation/
│       ├── controller/
│       │   ├── ProductAdminApi.java
│       │   └── ProductAdminController.java
│       └── dto/
│           └── response/
│               └── ProductAdminResponseDto.java
│
├── review/
│   ├── application/
│   │   ├── usecase/
│   │   │   └── ReviewAdminUseCase.java
│   │   └── service/
│   │       └── ReviewAdminService.java
│   ├── domain/
│   │   ├── model/
│   │   │   └── Review.java                     # product DB 직접 참조용 엔티티
│   │   └── repository/
│   │       └── ReviewAdminRepository.java
│   ├── infrastructure/
│   │   └── persistence/
│   │       ├── ReviewAdminJpaRepository.java
│   │       └── ReviewAdminRepositoryAdapter.java
│   └── presentation/
│       ├── controller/
│       │   ├── ReviewAdminApi.java
│       │   └── ReviewAdminController.java
│       └── dto/
│           └── response/
│               └── ReviewAdminResponseDto.java
│
├── order/
│   ├── application/
│   │   ├── usecase/
│   │   │   └── OrderAdminUseCase.java
│   │   └── service/
│   │       └── OrderAdminService.java
│   ├── domain/
│   │   ├── model/
│   │   │   └── Order.java                      # order DB 직접 참조용 엔티티
│   │   └── repository/
│   │       └── OrderAdminRepository.java
│   ├── infrastructure/
│   │   └── persistence/
│   │       ├── OrderAdminJpaRepository.java
│   │       └── OrderAdminRepositoryAdapter.java
│   └── presentation/
│       ├── controller/
│       │   ├── OrderAdminApi.java
│       │   └── OrderAdminController.java
│       └── dto/
│           └── response/
│               └── OrderAdminResponseDto.java
│
└── settlement/
    ├── application/
    │   ├── usecase/
    │   │   └── SettlementAdminUseCase.java
    │   └── service/
    │       └── SettlementAdminService.java
    ├── domain/
    │   ├── model/
    │   │   └── Settlement.java                 # settlement DB 직접 참조용 엔티티
    │   └── repository/
    │       └── SettlementAdminRepository.java
    ├── infrastructure/
    │   └── persistence/
    │       ├── SettlementAdminJpaRepository.java
    │       └── SettlementAdminRepositoryAdapter.java
    └── presentation/
        ├── controller/
        │   ├── SettlementAdminApi.java
        │   └── SettlementAdminController.java
        └── dto/
            └── response/
                └── SettlementAdminResponseDto.java
```

---

## 핵심 구현 가이드

### AdminRoleInterceptor

모든 `/api/v1/admins/**` 요청에 적용되는 인터셉터.
`X-User-Role` 헤더가 `ADMIN`이 아니면 403을 반환한다.

```java
@Component
public class AdminRoleInterceptor implements HandlerInterceptor {

	private static final String USER_ROLE_HEADER = "X-User-Role";

	@Override
	public boolean preHandle(HttpServletRequest request,
		HttpServletResponse response,
		Object handler) {
		String role = request.getHeader(USER_ROLE_HEADER);
		if (!"ADMIN".equals(role)) {
			throw new BusinessException(AdminErrorCode.FORBIDDEN);
		}
		return true;
	}
}
```

### WebMvcConfig

```java
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

	private final CurrentUserArgumentResolver currentUserArgumentResolver;
	private final AdminRoleInterceptor adminRoleInterceptor;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentUserArgumentResolver);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(adminRoleInterceptor)
			.addPathPatterns("/api/v1/admins/**");
	}
}
```

### 상품 강제 내리기 — 이벤트 기반 처리

Admin 서비스에서 product soft delete 후 `@TransactionalEventListener(AFTER_COMMIT)`으로
Kafka 이벤트를 발행한다. product 서비스가 이를 수신해 schedule 삭제와 ES 인덱스 삭제를 처리한다.

#### Kafka 토픽 전략

Kafka 토픽은 **모듈(대상 서비스)당 하나**로 관리하고, payload의 `type` 필드로 이벤트 종류를 구분한다.
이벤트가 추가되더라도 토픽은 그대로 유지하고 `type` 값만 늘리면 된다.

```
토픽: admin.product
payload: { "type": "FORCE_DOWN", "productId": "..." }
```

```java
// Admin 서비스 - AdminProductEvent (payload)
public record AdminProductEvent(String type, String productId) {
    public static AdminProductEvent forceDown(UUID productId) {
        return new AdminProductEvent("FORCE_DOWN", productId.toString());
    }
}

// Admin 서비스 - AdminProductEventPublisher
@Component
@RequiredArgsConstructor
public class AdminProductEventPublisher {

    public static final String TOPIC = "admin.product";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(AdminProductEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.productId(), objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new RuntimeException("AdminProductEvent 발행 실패", e);
        }
    }
}

// Admin 서비스 - ProductAdminService
@Transactional
public void forceDownProduct(UUID productId) {
    Product product = productAdminRepository.findById(productId)
        .orElseThrow(() -> new BusinessException(AdminErrorCode.PRODUCT_NOT_FOUND));
    product.forceDown();  // soft delete
    applicationEventPublisher.publishEvent(AdminProductEvent.forceDown(productId));
}
```

```java
// Product 서비스 - AdminProductEventConsumer (product 서비스 담당자 구현)
@KafkaListener(topics = AdminProductEventConsumer.TOPIC)
public void consume(String message) {
    AdminProductEvent event = objectMapper.readValue(message, AdminProductEvent.class);
    switch (event.type()) {
        case "FORCE_DOWN" -> {
            // 1. 연관 Schedule 전체 soft delete
            // 2. ES 인덱스 삭제
            // 실패 시 보상 트랜잭션으로 Product 상태 복구
        }
    }
}
```

### 엔티티 정의 주의사항

Admin 모듈의 엔티티는 각 서비스 DB를 참조하지만, 해당 서비스의 엔티티를 그대로 복사하지 않는다.
Admin 기능에 필요한 필드만 포함한 **읽기 전용에 가까운 가벼운 엔티티**로 정의한다.
`@Table(name = "...")` 으로 실제 테이블명을 명시하고, `ddl-auto: none` 으로 설정해 스키마를 건드리지 않는다.

---

## 협의 필요 사항

| 항목 | 담당 서비스 | 내용 |
|------|------------|------|
| 상품 강제 내리기 Kafka Consumer 구현 | product 서비스 | `admin.product` 토픽 수신 후 type=FORCE_DOWN 처리 → schedule soft delete + ES 삭제 + 보상 트랜잭션 구현 필요 |
