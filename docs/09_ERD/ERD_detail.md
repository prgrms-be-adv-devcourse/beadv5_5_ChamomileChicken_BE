# 잡아클래스 ERD 문서

## 목차

1. [ERD Cloud 원본](#1-erd-cloud-원본)
2. [Mermaid ERD 상세](#2-mermaid-erd-상세)
    - [User / Deposit 서비스](#21-user--deposit-서비스)
    - [Product 서비스](#22-product-서비스)
    - [Order / Payment / Refund 서비스](#23-order--payment--refund-서비스)
    - [Settlement 서비스](#24-settlement-서비스)
    - [File 서비스](#25-file-서비스)
    - [Outbox / 멱등성 이벤트 테이블](#26-outbox--멱등성-이벤트-테이블)

---

## 1. ERD 원본

> ERD : [ERD.md](ERD.md)

---

## 2. Mermaid ERD 상세

> **표기 규칙**
> - `PK` : Primary Key
> - `FK` : Foreign Key (논리적 참조, 물리적 FK 미설정)
> - ENUM 값은 컬럼 주석에 명시

---

### 2.1 User / Deposit 서비스

```mermaid
erDiagram
    users {
        UUID id PK
        VARCHAR_50 name "이름"
        VARCHAR_320 email "UNIQUE"
        VARCHAR_255 password "패스워드"
        VARCHAR_20 phone "전화번호"
        VARCHAR_20 role "USER | SELLER | ADMIN, DEFAULT USER"
        VARCHAR_255 social_login_type "소셜로그인종류 (nullable)"
        VARCHAR_255 social_login_id "소셜로그인ID (nullable)"
        BIG_DECIMAL deposit "예치금, DEFAULT 0"
    }

    deposit_histories {
        UUID id PK
        UUID payment_id FK "결제 ID (nullable)"
        UUID user_id FK "유저 ID"
        BIG_DECIMAL amount "금액"
        VARCHAR_10 change_type "CHARGE | PAYMENT | REFUND"
        VARCHAR_10 deposit_status "PENDING | COMPLETED | FAILED"
    }

    deposit_payments {
        UUID id PK
        UUID buyer_id FK "구매자 ID"
        VARCHAR_20 payment_method "결제 수단 (nullable)"
        BIGDECIMAL payment_amount "결제 수단 결제 금액"
        BOOLEAN is_success "결제성공여부"
    }

    user_activities {
        UUID id PK
        UUID user_id FK "사용자 ID"
        UUID product_id FK "상품 ID"
        VARCHAR_20 action_type "VIEW | WISHLIST | ORDER"
        TIMESTAMP_6 created_at "생성 일시"
    }

    user_processed_events {
        UUID id PK
        DATETIME processed_at "처리 일시"
    }

    users ||--o{ deposit_histories: "1:N"
    users ||--o{ deposit_payments: "1:N"
    users ||--o{ user_activities: "1:N"
```

---

### 2.2 Product 서비스

```mermaid
erDiagram
    products {
        UUID id PK
        UUID seller_id FK "판매자 ID"
        VARCHAR_100 title "제목"
        INT total_capacity "총인원"
        VARCHAR description "설명"
        UUID description_image_id FK "설명이미지 ID (nullable)"
        BIG_DECIMAL price "가격"
        VARCHAR_20 status "ENABLE | DISABLE"
        VARCHAR_255 road_address "도로명 주소 (nullable)"
        VARCHAR_255 detail_address "상세 주소 (nullable)"
        VARCHAR_10 zip_code "우편 번호 (nullable)"
        BIG_DECIMAL latitude "위도 (nullable)"
        BIG_DECIMAL longitude "경도 (nullable)"
    }

    products_schedule {
        UUID id PK
        UUID product_id FK "상품ID"
        DATE available_date "예약 가능 날짜"
        TIME start_time "시작 시간"
        TIME end_time "종료 시간"
        VARCHAR_20 schedule_status "FULL | AVAILABLE | PENDING | CLOSED"
        INT stock "재고"
    }

    product_users {
        UUID id PK
        UUID product_schedule_id FK "상품 스케줄 id"
        UUID buyer_id FK "구매자 ID"
        INT total_purchase_count "총 구매 인원수"
        VARCHAR_20 purchase_status "RESERVED | CONFIRMED | RELEASED | REFUNDED"
        INTEGER stock_recovery_status "재고 복구 상태 (nullable)"
    }

    product_reviews {
        UUID id PK
        UUID product_id FK "상품 id"
        UUID author_id FK "작성자"
        TINYINT rating "별점, 1~5"
        VARCHAR_1000 content "내용 (nullable)"
    }

    product_like {
        UUID id PK
        UUID user_id FK "사용자ID"
        UUID product_schedule_id FK "상품 일시ID"
        TINYINT quantity "수량"
    }

    product_embeddings {
        UUID id PK
        VARCHAR_255 title "제목 (nullable)"
        TEXT description "설명 (nullable)"
        NUMERIC_38_2 price "가격 (nullable)"
        VARCHAR_255 road_address "도로명 주소 (nullable)"
        VARCHAR_255 status "상태 (nullable)"
        INTEGER popularity "인기도 (nullable)"
        VECTOR_768 embedding "벡터"
    }

    product_processed_events {
        UUID idempotent_key PK "멱등키"
        UUID saved_at "저장 시간"
    }

    products ||--o{ products_schedule: "1:N"
    products_schedule ||--o{ product_users: "1:N"
    products ||--o{ product_reviews: "1:N"
    products ||--o{ product_like: "1:N"
```

---

### 2.3 Order / Payment / Refund 서비스

```mermaid
erDiagram
    orders {
        UUID id PK "주문 PK"
        UUID product_schedule_id FK "상품 일시 ID"
        UUID seller_id FK "판매자 id (nullable)"
        UUID user_id FK "유저 ID"
        UUID product_participant_user_id FK "상품 참여 유저 ID"
        TINYINT quantity "수량"
        BIGDECIMAL order_price "주문 가격"
        VARCHAR_20 order_status "PENDING | PAID | CANCELLED | REFUNDED | FAILED"
        DATETIME created_at "생성 일시"
        DATETIME updated_at "수정 일시"
    }

    payments {
        UUID id PK
        UUID buyer_id FK "구매자 ID"
        UUID product_id FK "상품 ID"
        UUID order_id FK "주문 ID"
        VARCHAR_20 payment_method "결제 수단 (nullable)"
        BIGDECIMAL payment_amount "결제 수단 결제 금액"
        BIGDECIMAL deposit_amount "예치금 결제 금액"
        BIGDECIMAL total_amount "총 결제 금액 (payment_amount + deposit_amount)"
        VARCHAR_20 payment_status "READY | PAID | CANCELLED | FAILED"
        VARCHAR_100 payment_key "결제 키 (nullable)"
        DATETIME paid_at "결제 완료 시간 (nullable)"
        DATETIME created_at "생성 일시"
        DATETIME updated_at "수정 일시"
    }

    refunds {
        UUID id PK
        UUID payment_id FK "결제 ID"
        UUID deposit_payment_id FK "예치금 결제 ID (nullable, 예치금 100% 결제건)"
        BIG_DECIMAL original_payment_total "원본 결제 총액"
        BIG_DECIMAL original_deposit_total "원본 예치금 총액"
        BIG_DECIMAL refund_ratio "환불 비율"
        BIG_DECIMAL payment_refund_amount "결제 수단 환불 금액 (nullable)"
        BIG_DECIMAL deposit_refund_amount "예치금 환불 금액 (nullable)"
        BIG_DECIMAL total_refund_amount "총 환불 금액 (nullable)"
        DATETIME requested_at "요청 시간/날짜 (nullable)"
        DATETIME processed_at "처리 시간/날짜 (nullable)"
        VARCHAR_200 refund_status "REQUESTED | PROCESSING | COMPLETED | FAILED | CANCELLED"
    }

    order_processed_events {
        UUID idempotent_key PK "멱등키"
        UUID saved_at "저장 시간"
    }

    orders ||--o{ payments: "1:N"
    payments ||--o| refunds: "1:1"
```

---

### 2.4 Settlement 서비스

```mermaid
erDiagram
    settlements {
        UUID id PK
        UUID seller_id FK "판매자 ID"
        VARCHAR_7 settlement_month "정산 월, yyyy-MM"
        NUMERIC_19_2 original_amount "원금액 (정산 원천 합계)"
        VARCHAR_30 seller_grade_code "BASIC | SILVER | GOLD | PLATINUM | DIAMOND"
        UUID seller_grade_policy_id FK "판매자 등급 정책 ID"
        NUMERIC_19_2 grade_base_amount "등급 산정 기준 금액"
        NUMERIC_19_2 fee_amount "수수료 금액"
        NUMERIC_10_4 fee_rate "수수료율"
        NUMERIC_19_2 final_settlement_amount "최종 정산 금액"
        VARCHAR_20 settlement_status "READY | HOLD | TRANSFERRING | SENT | FAILED"
        TIMESTAMP transferred_at "이체 완료 시각 (nullable)"
        VARCHAR_500 failure_reason "실패 사유 (nullable)"
    }

    settlement_transfers {
        UUID id PK
        UUID settlement_id FK "정산 ID"
        VARCHAR_20 transfer_status "REQUESTED | SENT | FAILED | HOLD"
        VARCHAR_20 bank_code "은행 코드 (nullable)"
        VARCHAR_100 account_number_masked "계좌번호 마스킹값 (nullable)"
        NUMERIC_19_2 transfer_amount "송금 금액"
        TIMESTAMP requested_at "송금 요청 시각"
        TIMESTAMP completed_at "송금 완료 시각 (nullable)"
        VARCHAR_500 failure_reason "실패 사유 (nullable)"
    }

    settlement_targets {
        UUID id PK
        UUID source_event_id "원천 이벤트 ID (중복 적재 방지)"
        VARCHAR_7 settlement_month "정산 월, yyyy-MM"
        UUID seller_id FK "판매자 ID"
        UUID order_id FK "주문 ID"
        UUID payment_id FK "결제 ID (nullable, PAYMENT일 때 필수)"
        UUID refund_id FK "환불 ID (nullable, REFUND일 때 필수)"
        UUID product_id FK "상품 ID"
        VARCHAR_20 target_type "PAYMENT | REFUND"
        NUMERIC_19_2 base_amount "정산 기준 금액 (환불은 음수)"
        TIMESTAMP occurred_at "발생 시각"
        VARCHAR_20 calculation_status "PENDING | CALCULATED | FAILED"
        TIMESTAMP calculation_requested_at "계산 요청 시각 (nullable)"
        TIMESTAMP calculation_completed_at "계산 완료 시각 (nullable)"
        VARCHAR_500 calculation_failure_reason "계산 실패 사유 (nullable)"
    }

    settlement_target_calculations {
        UUID id PK
        UUID settlement_target_id FK "정산 대상 ID"
        VARCHAR_7 settlement_month "정산 월, yyyy-MM"
        UUID seller_id FK "판매자 ID"
        NUMERIC_19_2 base_amount "계산 기준 금액"
        UUID applied_promotion_id FK "적용 프로모션 ID (nullable)"
        VARCHAR_50 applied_promotion_type "예: NEW_SELLER (nullable)"
        NUMERIC_10_4 applied_fee_rate "적용 수수료율 (nullable)"
        UUID original_payment_calc_id FK "원결제 계산 ID (환불 계산 시 참조, nullable)"
        TIMESTAMP calculated_at "계산 시각"
    }

    seller_grade_policies {
        UUID id PK
        VARCHAR_30 grade_code "BASIC | SILVER | GOLD | PLATINUM | DIAMOND"
        INTEGER policy_version "정책 버전, 1 이상"
        NUMERIC_19_2 min_sales_amount "최소 판매 금액 (등급 하한)"
        NUMERIC_19_2 max_sales_amount "최대 판매 금액 (nullable, 마지막 구간이면 NULL)"
        NUMERIC_10_4 fee_rate "수수료율, 예: 0.0300"
        BOOLEAN is_active "활성 여부"
        TIMESTAMP applied_start_at "적용 시작 시각"
        TIMESTAMP applied_end_at "적용 종료 시각 (nullable)"
    }

    seller_grades {
        UUID id PK
        UUID seller_id FK "판매자 ID"
        UUID seller_grade_policy_id FK "판매자 등급 정책 ID"
        VARCHAR_7 grade_month "등급 산정 월, yyyy-MM"
    }

    settlement_promotions {
        UUID id PK
        VARCHAR_100 promotion_name "프로모션명"
        VARCHAR_30 promotion_type "NEW_SELLER"
        NUMERIC_10_4 promotion_fee_rate "프로모션 수수료율"
        INTEGER apply_period_days "적용 기간 일수, 1 이상"
        BOOLEAN is_active "활성 여부"
    }

    seller_promotions {
        UUID id PK
        UUID seller_id FK "판매자 ID"
        UUID promotion_id FK "프로모션 ID"
        TIMESTAMP started_at "시작일시"
        TIMESTAMP ended_at "종료일시 (nullable)"
        BOOLEAN is_active "활성 여부"
    }

    seller_settlement_accounts {
        UUID id PK
        UUID user_id FK "유저 ID, UNIQUE"
        VARCHAR_20 bank_code "은행 코드"
        VARCHAR_50 account_number "계좌번호"
        VARCHAR_100 account_holder "예금주명"
        BOOLEAN is_active "사용 중인 계좌 여부"
    }

    settlement_batch_locks {
        UUID id PK
        VARCHAR_100 lock_key "unique, 배치 락 식별 키"
        VARCHAR_100 job_name "작업명"
        VARCHAR_7 settlement_month "정산 월"
        TIMESTAMP expired_at "만료 시각"
    }

    settlements ||--o{ settlement_transfers: "1:N"
    settlements ||--o{ settlement_targets: "1:N (월 집계)"
    settlement_targets ||--o{ settlement_target_calculations: "1:N"
    seller_grade_policies ||--o{ seller_grades: "1:N"
    seller_grade_policies ||--o{ settlements: "1:N"
    settlement_promotions ||--o{ seller_promotions: "1:N"
```

---

### 2.5 File 서비스

```mermaid
erDiagram
    files {
        UUID id PK
        TEXT storage_path "저장 PATH"
        DOUBLE file_size "파일 사이즈"
        TEXT original_file_name "파일 원본명 (nullable)"
        VARCHAR_20 storage_status "성공 | 실패 (nullable)"
    }
```

---

### 2.6 Outbox / 멱등성 이벤트 테이블

> 각 서비스의 Transactional Outbox 패턴 및 Kafka 멱등성 처리용 테이블

```mermaid
erDiagram
    order_outbox_events {
        UUID id PK
        VARCHAR aggregate_type "애그리거트 유형"
        VARCHAR aggregate_id "애그리거트 ID"
        VARCHAR event_type "이벤트 타입"
        TEXT event_body "이벤트 본문"
        VARCHAR outbox_status "PENDING | SENDING | PUBLISHED | FAILED"
        INTEGER retry_count "재시도 횟수"
        DATETIME last_tried_at "마지막 시도 일시"
        DATETIME created_at "생성 일시"
        DATETIME updated_at "수정 일시"
    }

    payment_outbox_events {
        UUID id PK
        VARCHAR aggregate_type "애그리거트 유형"
        VARCHAR aggregate_id "애그리거트 ID"
        VARCHAR event_type "이벤트 타입"
        TEXT event_body "이벤트 본문"
        VARCHAR outbox_status "PENDING | SENDING | PUBLISHED | FAILED"
        INTEGER retry_count "재시도 횟수"
        DATETIME last_tried_at "마지막 시도 일시"
        DATETIME created_at "생성 일시"
        DATETIME updated_at "수정 일시"
    }

    product_outbox_events {
        UUID id PK
        VARCHAR aggregate_type "애그리거트 유형"
        VARCHAR aggregate_id "애그리거트 ID"
        VARCHAR event_type "이벤트 타입 (EsEventType)"
        LOB event_body "이벤트 본문"
        VARCHAR outbox_status "OutboxStatus"
        INTEGER retry_count "재시도 횟수"
        DATETIME last_tried_at "마지막 시도 일시"
        DATETIME created_at "생성 일시"
        DATETIME updated_at "수정 일시"
        DATETIME deleted_at "삭제 일시"
    }

    admin_outbox_events {
        UUID id PK
        VARCHAR aggregate_type "애그리거트 유형"
        VARCHAR aggregate_id "애그리거트 ID"
        VARCHAR event_type "이벤트 타입 (EsEventType)"
        LOB event_body "이벤트 본문"
        VARCHAR outbox_status "OutboxStatus"
        INTEGER retry_count "재시도 횟수"
        DATETIME last_tried_at "마지막 시도 일시"
        DATETIME created_at "생성 일시"
        DATETIME updated_at "수정 일시"
    }

    order_processed_events {
        UUID idempotent_key PK "멱등키"
        UUID saved_at "저장 시간"
    }

    product_processed_events {
        UUID idempotent_key PK "멱등키"
        UUID saved_at "저장 시간"
    }

    user_processed_events {
        UUID id PK
        DATETIME processed_at "처리 일시"
    }
```

---

## 서비스별 테이블 매핑 요약

| 서비스        | 테이블                                                                                                                                                                                                                                           |
|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| user       | `users`, `user_activities`, `user_processed_events`                                                                                                                                                                                           |
| deposit    | `deposit_histories`, `deposit_payments`                                                                                                                                                                                                       |
| product    | `products`, `products_schedule`, `product_users`, `product_reviews`, `product_like`, `product_embeddings`, `product_processed_events`                                                                                                         |
| order      | `orders`, `order_processed_events`                                                                                                                                                                                                            |
| payment    | `payments`, `refunds`                                                                                                                                                                                                                         |
| settlement | `settlements`, `settlement_transfers`, `settlement_targets`, `settlement_target_calculations`, `seller_grade_policies`, `seller_grades`, `settlement_promotions`, `seller_promotions`, `seller_settlement_accounts`, `settlement_batch_locks` |
| file       | `files`                                                                                                                                                                                                                                       |
| outbox     | `order_outbox_events`, `payment_outbox_events`, `product_outbox_events`, `admin_outbox_events`                                                                                                                                                |
