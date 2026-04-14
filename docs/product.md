# Product 서비스 구조 및 흐름

## 배경 및 목적

`product` 서비스는 상품 도메인의 중심 서비스로, 단순 상품 CRUD를 넘어서 일정 관리, 예약 선점, 재고 차감/복구, 리뷰/찜, 검색 연동까지 함께 담당한다.

현재 프로젝트에서 `product` 서비스는 다음 문제를 한 곳에서 풀고 있다.

| 항목 | 역할 |
|------|------|
| 상품 관리 | 상품 등록, 수정, 삭제, 단건/목록 조회 |
| 일정 관리 | 상품별 일정 생성, 수정, 삭제 |
| 예약 관리 | 주문 전 재고 검증, 예약 사용자 생성 |
| 결제 후처리 | 결제 성공 시 예약 확정, 실패/취소/환불 시 재고 복구 |
| 사용자 연동 | 판매자/예약 사용자 이름 조회 |
| 파일 연동 | 상품 이미지 업로드 확인 |
| 검색 연동 | Elasticsearch 색인 이벤트 발행 및 조회 |

---

## 현재 구현 상태

**주요 외부 API**

| 영역 | 대표 엔드포인트 | 설명 |
|------|------------------|------|
| 상품 | `GET /api/v1/products` | 상품 검색/목록 조회 |
| 상품 | `POST /api/v1/products` | 상품 생성 |
| 상품 | `PUT /api/v1/products/{productId}` | 상품 수정 |
| 상품 | `DELETE /api/v1/products/{productId}` | 상품 삭제 |
| 일정 | `POST /api/v1/products/{productId}/schedules` | 일정 생성 |
| 일정 | `POST /api/v1/products/schedules/reservations` | 주문 전 예약 검증 |
| 리뷰 | `POST /api/v1/products/{productId}/reviews` | 리뷰 생성 |
| 찜 | `POST /api/v1/products/schedules/{scheduleId}/favorites` | 찜 생성 |

**현재 서비스 흐름의 큰 축**

```
Controller
  -> UseCase
    -> Application Service
      -> Domain Repository
      -> ACL Client (user, file)
      -> Event Publisher / Kafka
      -> Elasticsearch Adapter
```

**현재 핵심 특징**

- 상품 생성/수정 시 파일 서비스와 사용자 서비스 조회가 함께 묶여 있다.
- 일정은 `capacity`를 통해 재고 역할을 수행한다.
- 예약 엔티티는 `ProductUser`로 별도 관리한다.
- 결제 이후 상태 변경은 동기 API가 아니라 이벤트 기반 후처리 성격이 강하다.
- 검색은 RDB보다 Elasticsearch 사용 비중이 높다.

---

## 주요 도메인 설계

### 엔티티별 역할

| 엔티티 | 설명 | 핵심 필드 |
|--------|------|-----------|
| `Product` | 상품 기본 정보 | `sellerId`, `title`, `description`, `price`, `maxCapacity`, `status` |
| `Schedule` | 상품의 날짜/시간 슬롯 | `productId`, `scheduleDt`, `startTime`, `endTime`, `status`, `capacity` |
| `ProductUser` | 특정 일정에 대한 예약 사용자 | `productScheduleId`, `userId`, `guestCount`, `status`, `restoreStatus` |
| `Review` | 상품 리뷰 | `productId`, `userId`, `rating`, `content` |
| `Favorite` | 상품 찜 | `productScheduleId`, `userId`, `quantity` |

### 상태값

| 타입 | 값 |
|------|----|
| `ProductStatus` | `ENABLE`, `DISABLE` |
| `ReservedStatus` | `FULL`, `AVAILABLE`, `PENDING`, `CLOSED` |
| `ReservationStatus` | `RESERVED`, `CONFIRMED`, `RELEASED`, `REFUNDED` |

> `ReservedStatus`는 일정 자체의 상태이고, `ReservationStatus`는 예약 사용자(`ProductUser`)의 상태이다.

---

## 패키지 구조 및 계층 역할

현재 구조는 도메인 인터페이스와 인프라 구현을 분리하는 형태를 유지하고 있다.

```
presentation/controller/
  ProductRestController.java
  SchdulesRestController.java
  ReviewRestController.java
  FavoritesRestController.java
  ProductInternalController.java

application/service/
  ProductService.java
  ScheduleService.java
  ProductUserService.java
  ReviewService.java
  FavoriteService.java

domain/model/
  Product.java
  Schedule.java
  ProductUser.java
  Review.java
  Favorite.java

domain/repository/
  ProductRepository.java
  ScheduleRepository.java
  ProductUserRepository.java
  ReviewRepository.java
  FavoriteRepository.java
  ProductSearchRepository.java

infrastructure/acl/
  user, file 서비스 호출 클라이언트

infrastructure/persistence/
  JPA Repository Adapter

infrastructure/kafka/
  주문/결제 이벤트 소비
  ES 색인 이벤트 발행/소비

infrastructure/elasticsearch/
  ProductDocument.java
  ProductSearchRepositoryAdapter.java
```

> `application.service`가 실제 유스케이스를 수행하고, 외부 서비스 연동은 `infrastructure.acl`, 검색 연동은 `infrastructure.elasticsearch`, 이벤트 기반 처리는 `infrastructure.kafka`로 분리되어 있다.

---

## 핵심 서비스별 책임

### ProductService

| 기능 | 설명 |
|------|------|
| 상품 생성 | 이미지 확인, 상품 저장, 판매자 조회, ES 저장 이벤트 발행 |
| 상품 수정 | 소유자 검증 후 필드 수정, 이미지 재확인, ES 저장 이벤트 발행 |
| 상품 삭제 | soft delete 처리, ES 삭제 이벤트 발행 |
| 상품 검색 | ES 조회 기반 목록 반환 |
| 상품 단건 조회 | 상품 조회 후 판매자 이름 조합 |
| 정산용 조회 | product id 목록으로 상품 정보 반환 |
| ES 마이그레이션 | DB 데이터를 ES로 재색인 |

### ScheduleService

| 기능 | 설명 |
|------|------|
| 일정 생성 | 날짜/시간 검증, 충돌 검증, 초기 재고 세팅 |
| 일정 수정 | 시간 충돌 재검증, 일정 상태/재고 변경 |
| 일정 삭제 | 일정 상태 종료 및 soft delete |
| 예약 검증 | 주문 전 재고 차감과 `ProductUser` 생성 |
| 예약 확정 | 결제 성공 후 `RESERVED -> CONFIRMED` |
| 재고 복구 | 결제 실패/취소/환불 시 재고 복구 및 상태 변경 |
| 잔여 좌석 조회 | 상품 최대 인원과 남은 재고를 조합해 반환 |

### ProductUserService

| 기능 | 설명 |
|------|------|
| 예약 사용자 생성 | 예약 사용자 엔티티 생성 |
| 예약 사용자 목록 조회 | user 서비스 이름 조회 후 응답 DTO 조합 |
| 내부 조회 | 예약 후속 처리용 엔티티 반환 |

### ReviewService

| 기능 | 설명 |
|------|------|
| 리뷰 생성 | 상품 리뷰 저장 |
| 리뷰 수정 | 본인 리뷰 검증 후 수정 |
| 리뷰 삭제 | soft delete |
| 사용자/상품별 조회 | delete 되지 않은 리뷰만 조회 |

### FavoriteService

| 기능 | 설명 |
|------|------|
| 찜 생성 | 찜 엔티티 저장 |
| 찜 삭제 | 본인 찜 검증 후 soft delete |
| 사용자별 찜 조회 | 활성 찜 목록 반환 |

---

## 주요 요청 흐름

### 상품 생성 흐름

```
ProductRestController
  -> ProductService.create()
    -> FileConfirmClient.confirmBulk()         // 이미지 확인
    -> ProductRepository.save()
    -> SellerApiClient.findSeller()            // 판매자 이름 조회
    -> ProductEsSaveEvent 발행
    -> ProductEventResponseDto 발행
```

### 상품 수정 흐름

```
ProductRestController
  -> ProductService.update()
    -> 상품 존재 여부 확인
    -> 판매자 소유 여부 검증
    -> 필드 수정
    -> 이미지가 있으면 file 서비스 재확인
    -> ProductEsSaveEvent 발행
```

### 일정 생성 흐름

```
SchdulesRestController
  -> ScheduleService.create()
    -> 상품 조회
    -> 판매자 소유 여부 검증
    -> 날짜/시간 검증
    -> 충돌 일정 조회
    -> Schedule(capacity = product.maxCapacity) 생성
    -> 저장
```

### 주문 전 예약 검증 흐름

```
외부 주문 요청
  -> ScheduleService.verification()
    -> Schedule 조회
    -> Product 조회
    -> 가격 검증
    -> scheduleRepository.verification()       // 재고 차감
    -> ProductUserService.create()             // 예약 사용자 생성
    -> OrderResponseDto 반환
```

### 결제 성공 후 예약 확정 흐름

```
결제 완료 이벤트
  -> ScheduleService.reservationCompleted()
    -> ProductUser 조회
    -> Schedule 존재 확인
    -> status RESERVED -> CONFIRMED 변경
```

### 결제 실패/취소/환불 후 재고 복구 흐름

```
결제 실패/취소/환불 이벤트
  -> ScheduleService.restoringInventory()
    -> restoreStatus 선점
    -> ProductUser 조회
    -> Schedule / Product 조회
    -> 재고 복구
    -> status RELEASED 또는 REFUNDED 변경
```

### 리뷰 / 찜 흐름

```
ReviewRestController
  -> ReviewService
    -> 저장 / 수정 / soft delete / 목록 조회

FavoritesRestController
  -> FavoriteService
    -> 저장 / soft delete / 사용자별 목록 조회
```

---

## 내부 연동 구조

### user 서비스 연동

| 용도 | 사용 위치 |
|------|-----------|
| 판매자 이름 조회 | 상품 생성, 수정, 단건 조회 |
| 사용자 이름 일괄 조회 | 예약 사용자 목록 조회 |
| seller 정보 포함 | ES 문서 생성 시 사용 |

### file 서비스 연동

| 용도 | 사용 위치 |
|------|-----------|
| 업로드된 이미지 확인 | 상품 생성, 상품 수정 |

### Kafka / 이벤트 연동

| 용도 | 설명 |
|------|------|
| ES 색인 이벤트 | 상품 생성/수정/삭제 후 ES 반영 |
| 주문/결제 이벤트 | 예약 확정 및 재고 복구 |

---

## 데이터 및 운영 주의사항

### 스키마 관점

- `products_schedule.capacity`는 재고 역할이므로 null이면 안 된다.
- `products_users.status`는 현재 `ReservationStatus`와 DB 체크 제약조건이 반드시 일치해야 한다.
- `products_users.reg_dt`는 null이면 안 되며, 마이그레이션 시 백필이 필요하다.
- product 도메인은 `delete_dt` 기반 soft delete를 사용한다.

### 운영 관점

- Hibernate `ddl-auto: update`만으로는 운영 스키마 변경을 안전하게 처리하기 어렵다.
- 상태값 변경(`PENDING -> RESERVED` 등)이 있었다면 DB check constraint도 함께 변경해야 한다.
- 일정 재고(`capacity`)와 상품 최대 인원(`maxCapacity`)은 다른 의미이므로 마이그레이션 시 구분해서 다뤄야 한다.
- ES, file, user 연동이 상품 생성/수정 흐름에 함께 들어 있으므로 실패 원인 추적 시 외부 연동 로그가 중요하다.

---

## 구현 체크리스트

### 상품 도메인
- [x] 상품 생성/수정/삭제 기능 구현
- [x] 이미지 확인 연동 구현
- [x] 판매자 정보 조회 연동 구현
- [x] ES 저장/삭제 이벤트 발행 구현

### 일정/예약 도메인
- [x] 일정 생성/수정/삭제 기능 구현
- [x] 시간 충돌 검증 구현
- [x] 주문 전 재고 검증 구현
- [x] 예약 사용자 생성 구현
- [x] 결제 완료 후 예약 확정 구현
- [x] 결제 실패/취소/환불 후 재고 복구 구현

### 부가 기능
- [x] 리뷰 생성/수정/삭제/조회 구현
- [x] 찜 생성/삭제/조회 구현
- [x] 상품 검색의 ES 연동 구현

### 운영 정비 포인트
- [ ] `products_users.status` 제약조건을 현재 enum 기준으로 정리
- [ ] `products_schedule.capacity` null 데이터 정리 및 not null 고정
- [ ] product 스키마 변경용 수동 migration 절차 안정화
