# 잡아클래스 (Jaba Class) — 문서 인덱스

이 문서는 `docs/` 하위 전체 문서에 대한 안내서입니다.

---

## 서비스 구성

| # | 서비스         | 포트   | 역할                             |
|---|-------------|------|--------------------------------|
| 0 | API Gateway | 8080 | JWT 인증, RBAC, 라우팅, CB Fallback |
| 1 | User        | 9003 | 회원가입, 로그인, 예치금, 이메일 인증         |
| 2 | Product     | 9004 | 상품/스케줄 관리, 파일 업로드, ES 검색       |
| 3 | Order       | 9005 | 주문 생성, 결제 연동, 환불               |
| 4 | Payment     | 9001 | Toss 결제 승인/취소                  |
| 5 | Settlement  | 9002 | 정산 타겟 적재, 배치 계산/송금             |
| 6 | Admin       | 9007 | 사용자/상품/정산 관리, 판매자 승인           |
| 7 | AI          | 9009 | 상품 임베딩 동기화, 사용자 맞춤 추천          |

---

## 문서 목록

### 00. API 명세 (`00_api-spec/`)

전체 REST API 엔드포인트 명세. API Gateway 기준 경로.

| 문서                                                 | 설명                                   |
|----------------------------------------------------|--------------------------------------|
| [00-API_SPEC.md](./00_api-spec/00-API_SPEC.md)     | 공통 응답 형식, 인증 범례, 게이트웨이 라우팅 테이블       |
| [01-user.md](./00_api-spec/01-user.md)             | User 서비스 API (인증, 회원가입, 이메일 인증, 예치금) |
| [02-product.md](./00_api-spec/02-product.md)       | Product 서비스 API (상품, 스케줄, 찜, 검색)     |
| [03-file.md](./00_api-spec/03-file.md)             | File API (Product 서비스 통합, 9004)      |
| [04-order.md](./00_api-spec/04-order.md)           | Order 서비스 API (주문 생성, 환불)            |
| [05-payment.md](./00_api-spec/05-payment.md)       | Payment 서비스 API (Toss 결제 승인/실패)      |
| [06-settlement.md](./00_api-spec/06-settlement.md) | Settlement 서비스 API (정산 조회, 배치 실행)    |
| [07-admin.md](./00_api-spec/07-admin.md)           | Admin 서비스 API (관리자 전용)               |
| [08-ai.md](./00_api-spec/08-ai.md)                 | AI 서비스 API (추천 조회)                   |

---

### 00. 인증 / API Gateway (`00_auth/`)

| 문서                                         | 설명                                                            |
|--------------------------------------------|---------------------------------------------------------------|
| [api-gateway.md](./00_auth/api-gateway.md) | Spring Cloud Gateway 구조, JWT 검증 필터, RBAC, CB Fallback, 라우팅 규칙 |
| [user-auth.md](./00_auth/user-auth.md)     | JWT 발급 흐름, Access/Refresh Token 구조, 토큰 헤더 전달 방식               |

---

### 01. User 서비스 (`01_user/`)

| 문서                                                       | 설명                                                           |
|----------------------------------------------------------|--------------------------------------------------------------|
| [user.md](./01_user/user.md)                             | 서비스 개요, 패키지 구조, 도메인 모델 전체, 에러 코드                             |
| [auth.md](./01_user/auth.md)                             | JWT 로그인/로그아웃/재발급, RTR, 블랙리스트, OAuth2 소셜 로그인, Redis CB, 보안 알림 |
| [deposit.md](./01_user/deposit.md)                       | 예치금 충전/사용/환불, Kafka 이벤트 소비, 멱등성 처리                           |
| [email-verification.md](./01_user/email-verification.md) | 인증코드 발송, 코드 검증, verifiedToken 생명주기                           |

---

### 02. Product 서비스 (`02_product/`)

| 문서                                                  | 설명                         |
|-----------------------------------------------------|----------------------------|
| [product.md](./02_product/product.md)               | 상품/스케줄 도메인, 비즈니스 로직, 재고 관리 |
| [product-schema.md](./02_product/product-schema.md) | 상품 관련 DB 스키마               |

---

### 04. Order 서비스 (`04_order/`)

| 문서                                                                                                | 설명                     |
|---------------------------------------------------------------------------------------------------|------------------------|
| [order.md](./04_order/order.md)                                                                   | 주문 도메인, 상태 머신, Saga 패턴 |
| [payment-flows/00-overview.md](./04_order/payment-flows/00-overview.md)                           | 결제 흐름 개요               |
| [payment-flows/01-order-create.md](./04_order/payment-flows/01-order-create.md)                   | 주문 생성 흐름               |
| [payment-flows/02-payment-success.md](./04_order/payment-flows/02-payment-success.md)             | 결제 성공 흐름               |
| [payment-flows/03-payment-failed.md](./04_order/payment-flows/03-payment-failed.md)               | 결제 실패 흐름               |
| [payment-flows/04-payment-not-completed.md](./04_order/payment-flows/04-payment-not-completed.md) | 결제 미완료(만료) 흐름          |
| [payment-flows/payment-reliability.md](./04_order/payment-flows/payment-reliability.md)           | 결제 신뢰성 설계              |
| [refund-flows/00-overview.md](./04_order/refund-flows/00-overview.md)                             | 환불 흐름 개요               |
| [refund-flows/01-refund-request.md](./04_order/refund-flows/01-refund-request.md)                 | 환불 요청 흐름               |
| [refund-flows/02-refund-success.md](./04_order/refund-flows/02-refund-success.md)                 | 환불 성공 흐름               |
| [refund-flows/03-refund-failed.md](./04_order/refund-flows/03-refund-failed.md)                   | 환불 실패 흐름               |

---

### 05. Payment 서비스 (`05_payment/`)

| 문서                                    | 설명                                |
|---------------------------------------|-----------------------------------|
| [payment.md](./05_payment/payment.md) | Toss 결제 연동, Outbox 패턴, 결제 만료 스케줄러 |

---

### 06. Settlement 서비스 (`06_settlement/`)

| 문서                                                         | 설명                             |
|------------------------------------------------------------|--------------------------------|
| [settlement.md](./06_settlement/settlement.md)             | 정산 서비스 개요 및 문서 목차              |
| [api.md](./06_settlement/api.md)                           | 정산 조회 API, 내부 배치 실행 API        |
| [entities.md](./06_settlement/entities.md)                 | 정산 도메인 엔티티와 상태값                |
| [target-ingestion.md](./06_settlement/target-ingestion.md) | Kafka 이벤트 → 정산 타겟 적재 흐름, 멱등 처리 |
| [batch-flow.md](./06_settlement/batch-flow.md)             | 정산 계산/송금 배치 흐름, chunk bulk 처리  |

---

### 07. Admin 서비스 (`07_admin/`)

| 문서                                                                | 설명                                       |
|-------------------------------------------------------------------|------------------------------------------|
| [admin.md](./07_admin/admin.md)                                   | 어드민 기능 전체 (대시보드, 사용자/상품/정산/리뷰 관리)        |
| [force-down-reliability.md](./07_admin/force-down-reliability.md) | 강제 중지 신뢰성 설계 (재시도, SKIP LOCKED, 보상 트랜잭션) |

---

### 08. AI 서비스 (`08_ai/`)

| 문서                                                             | 설명                     |
|----------------------------------------------------------------|------------------------|
| [ai.md](./08_ai/ai.md)                                         | AI 서비스 개요, 추천 시스템 구조   |
| [ai-recommendation-flow.md](./08_ai/ai-recommendation-flow.md) | 추천 생성 전체 흐름            |
| [recommendation-logic.md](./08_ai/recommendation-logic.md)     | 추천 알고리즘 및 벡터 유사도 계산 로직 |

---

### 09. ERD (`09_ERD/`)

| 문서                                      | 설명                           |
|-----------------------------------------|------------------------------|
| [ERD.md](./09_ERD/ERD.md)               | ERD 원본                       |
| [ERD_detail.md](./09_ERD/ERD_detail.md) | Mermaid ERD 상세 (서비스별 테이블 전체) |

---

### 10. Kafka (`10_kafka/`)

| 문서                                            | 설명                                                          |
|-----------------------------------------------|-------------------------------------------------------------|
| [kafka-topics.md](./10_kafka/kafka-topics.md) | 토픽 설계, 이벤트 정의 및 페이로드, Consumer Group ID, DLQ, 파티션 키, 멱등성 처리 |

---

### 11. Elasticsearch (`11_elasticsearch/`)

| 문서                                                      | 설명                                                              |
|---------------------------------------------------------|-----------------------------------------------------------------|
| [elasticsearch.md](./11_elasticsearch/elasticsearch.md) | ES 도입 배경, 인덱스 설계, nori 분석기, Outbox 패턴 기반 색인, 서킷 브레이커, fuzziness |

---

### 12. k3s 인프라 (`12_k3s/`)

| 문서                                                                                  | 설명                             |
|-------------------------------------------------------------------------------------|--------------------------------|
| [k3s-concept.md](./12_k3s/k3s-concept.md)                                           | k3s 아키텍처 개념                    |
| [k3s-install.md](./12_k3s/k3s-install.md)                                           | k3s 설치 가이드                     |
| [k3s-yml.md](./12_k3s/k3s-yml.md)                                                   | Kubernetes YAML 명세             |
| [k3s-ssl-ingress.md](./12_k3s/k3s-ssl-ingress.md)                                   | SSL, cert-manager, Ingress     |
| [k3s-monitoring.md](./12_k3s/k3s-monitoring.md)                                     | Prometheus, Grafana, scrape 설정 |
| [k3s-infra-data-yml-reference.md](./12_k3s/k3s-infra-data-yml-reference.md)         | 인프라/데이터 YAML 레퍼런스              |
| [k3s-service-deployment-structure.md](./12_k3s/k3s-service-deployment-structure.md) | 서비스 배포 구조                      |
| [k3s-sh.md](./12_k3s/k3s-sh.md)                                                     | 배포 Shell 스크립트                  |
| [k3s-cicd-final-guide.md](./13_CICD/k3s-cicd-final-guide.md)                        | CI/CD 최종 가이드                   |

---

### 13. CI/CD (`13_CICD/`)

| 문서                                                                     | 설명                |
|------------------------------------------------------------------------|-------------------|
| [service-cicd-optimization.md](./13_CICD/service-cicd-optimization.md) | 서비스별 CI/CD 최적화 전략 |
| [deploy-env-workflow.md](./13_CICD/deploy-env-workflow.md)             | 환경변수 배포 워크플로      |

---

## 주요 아키텍처 패턴

| 패턴                           | 적용 서비스                                | 관련 문서                                                                                                                      |
|------------------------------|---------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| Outbox 패턴                    | payment, order, admin, product(ES)    | [kafka-topics.md](./10_kafka/kafka-topics.md)                                                                              |
| Saga (보상 트랜잭션)               | order ↔ payment ↔ product ↔ user      | [order.md](./04_order/order.md)                                                                                            |
| RTR (Refresh Token Rotation) | user                                  | [auth.md](./01_user/auth.md)                                                                                               |
| Redis 서킷 브레이커                | user (auth), product (ES)             | [auth.md](./01_user/auth.md), [elasticsearch.md](./11_elasticsearch/elasticsearch.md)                                      |
| ES 서킷 브레이커 + DB Fallback     | product                               | [elasticsearch.md](./11_elasticsearch/elasticsearch.md)                                                                    |
| 멱등성 처리                       | order, user(deposit), settlement      | [kafka-topics.md](./10_kafka/kafka-topics.md)                                                                              |
| SKIP LOCKED                  | product(ES outbox), admin(force-down) | [elasticsearch.md](./11_elasticsearch/elasticsearch.md), [force-down-reliability.md](./07_admin/force-down-reliability.md) |
