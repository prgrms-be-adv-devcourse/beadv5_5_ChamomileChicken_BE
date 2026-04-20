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
> **Outbox 패턴**을 적용해 product soft delete와 outbox 이벤트 저장을 같은 트랜잭션으로 묶어 원자적으로 커밋한다.
> 별도 스케줄러(`OutboxEventPoller`)가 Kafka에 발행하며, Kafka 장애 시에도 이벤트 유실 없이 재발행이 보장된다.
> 신뢰성 설계 상세는 [force-down-reliability.md](force-down-reliability.md)를 참고한다.

### 상품 강제 내리기 흐름

```
[Admin 서비스 — 동기, HTTP 응답 전]

PATCH /api/v1/admins/products/{productId}/force-down
  → AdminRoleInterceptor: X-User-Role == ADMIN 검증
  → ProductAdminService.forceDownProduct()  @Transactional(productTransactionManager)
      1. Admin DB products 조회 (없으면 404)
      2. product.forceDown() → Admin DB: status=DISABLE, deleteDt=now()
      3. OutboxEvent.create() 저장 → admin_outbox_events: status=PENDING
      ↓ DB 커밋 (products + outbox 원자적)
  → HTTP 200 응답 반환

[Admin 서비스 — 비동기, OutboxEventPoller]

OutboxEventPoller  @Scheduled(fixedDelay=1000)
  → admin_outbox_events PENDING 조회 (FOR UPDATE SKIP LOCKED)
  → OutboxService.markSending() → status=SENDING
  → kafkaTemplate.send(ProducerRecord).get()  ← 동기 발행
      성공: OutboxService.markPublished() → status=PUBLISHED
      실패: OutboxService.retry() → retryCount++, status=PENDING
            retryCount >= 5: OutboxService.markFailed() → status=FAILED

[Product 서비스 — 비동기, Kafka]

Kafka 토픽: admin.product
  → AdminProductKafkaConsumer.consume()
      ↓ type == "FORCE_DOWN"
  → AdminProductEventHandler.processForceDown()  @Transactional
      4. Product DB products 조회
      5. product.changeStatus(DISABLE) + product.changeDelete()
      6. scheduleRepository.softDeleteByProductId()
      ↓ DB 커밋
  → productSearchRepository.deleteById()  ← ES 삭제 (트랜잭션 밖)
      성공 → 완료
      실패 → RuntimeException 재던짐 → Kafka 재시도 (FixedBackOff 1s × 3회)
```

| 처리 대상 | 시점 | 결과 |
|-----------|------|------|
| Admin DB `products` | HTTP 응답 전 (동기) | status=DISABLE, deleteDt=now() |
| Admin DB `admin_outbox_events` | 위와 같은 트랜잭션 | status=PENDING |
| Product DB `products` | Kafka 소비 후 (비동기) | status=DISABLE, deleteDt=now() |
| Product DB `products_schedule` | 위와 같은 트랜잭션 | delete_dt=now() (soft-delete) |
| Elasticsearch | DB 커밋 직후 | 인덱스 삭제 |

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
│   │   │   ├── ProductAdminRepositoryAdapter.java
│   │   │   ├── OutboxEventJpaRepository.java   # FOR UPDATE SKIP LOCKED 쿼리
│   │   │   └── OutboxEventRepositoryAdapter.java
│   │   ├── kafka/
│   │   │   └── AdminProductEvent.java          # 강제 내리기 이벤트 payload (type + productId)
│   │   └── outbox/
│   │       ├── EventType.java                  # PRODUCT_FORCE_DOWN("admin.product")
│   │       ├── OutboxService.java              # 상태 전환 전용 @Transactional
│   │       └── OutboxEventPoller.java          # @Scheduled 폴러
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

### 상품 강제 내리기 — Outbox 패턴 기반 처리

Admin 서비스에서 product soft delete와 outbox 이벤트 저장을 같은 트랜잭션으로 묶는다.
`OutboxEventPoller`가 주기적으로 폴링하여 Kafka에 발행한다.
신뢰성 설계 상세(재시도, SKIP LOCKED, 보상 트랜잭션 등)는 [force-down-reliability.md](force-down-reliability.md)를 참고한다.

#### Kafka 토픽 전략

Kafka 토픽은 **모듈(대상 서비스)당 하나**로 관리하고, payload의 `type` 필드로 이벤트 종류를 구분한다.
이벤트가 추가되더라도 토픽은 그대로 유지하고 `type` 값만 늘리면 된다.

```
토픽: admin.product
payload: { "type": "FORCE_DOWN", "productId": "..." }
```

```java
// Admin 서비스 - ProductAdminService
@Transactional(transactionManager = "productTransactionManager")
public void forceDownProduct(UUID productId) {
    productAdminRepository.findById(productId)
        .orElseThrow(() -> new BusinessException(AdminErrorCode.PRODUCT_NOT_FOUND))
        .forceDown();

    String payload = objectMapper.writeValueAsString(AdminProductEvent.forceDown(productId));
    outboxEventRepository.save(OutboxEvent.create(
        "product", productId.toString(), EventType.PRODUCT_FORCE_DOWN, payload
    ));
}

// Admin 서비스 - OutboxEventPoller
@Scheduled(fixedDelay = 1000)
public void publish() {
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
    List<OutboxEvent> events = outboxEventRepository.findProcessableEvents(threshold, 100);
    outboxService.markSending(events);

    for (OutboxEvent event : events) {
        if (event.isRetryExceeded()) {
            outboxService.markFailed(event);
            continue;
        }
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                event.getEventType().getTopic(), event.getAggregateId(), event.getPayload()
            );
            kafkaTemplate.send(record).get();
            outboxService.markPublished(event);
        } catch (Exception e) {
            outboxService.retry(event);
        }
    }
}
```

```java
// Product 서비스 - AdminProductEventHandler
@Transactional
public void processForceDown(UUID productId) {
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new BusinessException(CommonErrorCode.PRODUCT_NOT_FOUND));
    product.changeStatus(ProductStatus.DISABLE);
    product.changeDelete();
    scheduleRepository.softDeleteByProductId(productId);
}

```

### 엔티티 정의 주의사항

Admin 모듈의 엔티티는 각 서비스 DB를 참조하지만, 해당 서비스의 엔티티를 그대로 복사하지 않는다.
Admin 기능에 필요한 필드만 포함한 **읽기 전용에 가까운 가벼운 엔티티**로 정의한다.
`@Table(name = "...")` 으로 실제 테이블명을 명시하고, `ddl-auto: none` 으로 설정해 스키마를 건드리지 않는다.

---

## 구현 현황

| 항목 | 상태 | 브랜치 |
|------|------|--------|
| Admin 모듈 전체 구현 (유저/상품/주문/정산/리뷰) | ✅ 완료 | `feature/admin/admin-init/218` |
| 상품 강제 내리기 Kafka Consumer + Outbox 패턴 | ✅ 완료 | `feature/product/product-admin-delete-product/231` |

### Admin 서비스 추가/수정 파일

```
service/admin/.../product/
├── domain/model/
│   ├── OutboxEvent.java              # admin_outbox_events 엔티티 (retryCount, SENDING 상태 등)
│   └── OutboxStatus.java            # PENDING / SENDING / PUBLISHED / FAILED
├── domain/repository/
│   └── OutboxEventRepository.java
└── infrastructure/
    ├── persistence/
    │   ├── OutboxEventJpaRepository.java    # FOR UPDATE SKIP LOCKED 네이티브 쿼리
    │   └── OutboxEventRepositoryAdapter.java
    └── outbox/
        ├── EventType.java                   # PRODUCT_FORCE_DOWN("admin.product")
        ├── OutboxService.java               # 상태 전환 전용 @Transactional
        └── OutboxEventPoller.java           # @Scheduled(fixedDelay=1000) 폴러
```

### Product 서비스 추가/수정 파일

```
service/product/.../infrastructure/kafka/admin/
├── AdminProductMessage.java         # 메시지 역직렬화 레코드
├── AdminProductEventHandler.java    # DB 트랜잭션 처리 (processForceDown)
└── AdminProductKafkaConsumer.java   # @KafkaListener + ES 삭제
```

수정된 파일:
- `ScheduleRepository` — `softDeleteByProductId` 추가
- `ScheduleJpaRepository` — `@Modifying(clearAutomatically=true, flushAutomatically=true)` bulk 쿼리 추가
- `ScheduleRepositoryAdapter` — 위 인터페이스 구현 위임
