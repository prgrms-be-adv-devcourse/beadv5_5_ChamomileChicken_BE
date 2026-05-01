# K3s 서비스 배포 구조

## 1. 문서 목적

이 문서는 현재 저장소의 K3s 서비스 배포 구조를 서비스 관점에서 정리한 문서입니다.

처음 보는 분도 아래 내용을 바로 이해할 수 있도록 구성했습니다.

- 어떤 서비스가 배포되는지
- 각 서비스가 어떤 인프라에 의존하는지
- 어떤 포트를 사용하는지
- ConfigMap, Secret, Prometheus 수집이 어떤 규칙으로 연결되는지

## 2. 전체 구성

현재 K3s 기준 배포 대상은 크게 두 종류입니다.

### 애플리케이션 서비스

- `api-gateway-service`
- `admin-service`
- `ai-service`
- `order-service`
- `payment-service`
- `product-service`
- `settlement-service`
- `user-service`

### 공통 인프라

- `postgres`
- `kafka`
- `zookeeper`
- `redis`
- `elasticsearch`

## 3. 서비스 모듈 구조

Gradle 기준 서비스 모듈은 아래와 같습니다.

- `api-gateway`
- `service:admin`
- `service:ai`
- `service:order`
- `service:payment`
- `service:product`
- `service:settlement`
- `service:user`

즉, 코드 구조와 K3s 배포 구조가 거의 1:1로 대응합니다.

## 4. 외부 진입 구조

외부 트래픽은 기본적으로 `api-gateway-service`를 통해 들어옵니다.

현재 게이트웨이는 아래 특징을 가집니다.

- `NodePort` 서비스 사용
- 서비스 포트 `8080`
- `nodePort`는 `30100`
- `Ingress` 사용
- `traefik` ingress class 사용
- `main-ingress.yml`에서 `jabaclass.store` 도메인과 TLS를 설정
- `cluster-issuer.yml`에서 Let's Encrypt 인증서 발급자 설정

현재 Ingress 경로는 아래와 같습니다.

- `/api`
- `/swagger-ui`
- `/v3/api-docs`
- `/docs`
- `/oauth2`
- `/login/oauth2`

루트 경로 `/`는 `frontend-service`로 연결합니다.

즉, 외부 사용자는 대부분 게이트웨이를 통해 내부 서비스 기능에 접근하게 됩니다.

SSL / HTTPS 연결 구조는 `k3s-ssl-ingress.md`에서 별도로 다룹니다.

## 5. 서비스별 포트와 선행 의존성

아래 표는 현재 각 서비스의 컨테이너 포트와 initContainer 기준 선행 의존성을 정리한 내용입니다.

| 서비스 | 포트 | 선행 의존성 |
| --- | --- | --- |
| `api-gateway-service` | `8080` | `postgres`, `redis` |
| `admin-service` | `9007` | `postgres`, `kafka`, `redis` |
| `ai-service` | `9009` | `postgres`, `kafka`, `redis` |
| `order-service` | `9005` | `postgres`, `kafka`, `redis` |
| `payment-service` | `9001` | `postgres`, `kafka`, `redis` |
| `product-service` | `9004` | `postgres`, `kafka`, `redis`, `elasticsearch` |
| `settlement-service` | `9002` | `postgres`, `kafka`, `redis` |
| `user-service` | `9003` | `postgres`, `kafka`, `redis` |

대부분의 의존성은 `initContainers`에서 `nc -z`로 확인한 뒤 메인 컨테이너가 시작되도록 구성되어 있습니다.

즉, 필요한 인프라가 준비되지 않으면 애플리케이션 컨테이너가 바로 올라오지 않도록 설계되어 있습니다.

## 6. 공통 인프라의 역할

### Postgres

- 서비스명: `postgres`, `postgres-headless`
- 포트: `5432`
- 형태: `StatefulSet`

역할은 아래와 같습니다.

- 주요 서비스의 기본 데이터 저장소

### Kafka

- 서비스명: `kafka-service`, `kafka-service-headless`
- 포트: `9092`
- 형태: `StatefulSet`

역할은 아래와 같습니다.

- 이벤트 발행 및 구독
- 비동기 처리 기반 서비스 연동

### Zookeeper

- 서비스명: `zookeeper-service`, `zookeeper-service-headless`
- 포트: `2181`
- 형태: `StatefulSet`

역할은 아래와 같습니다.

- 현재 Kafka 구성에 필요한 coordination 계층

### Redis

- 서비스명: `redis`
- 포트: `6379`
- 형태: `StatefulSet`

역할은 아래와 같습니다.

- 캐시
- 세션, 토큰, 임시 데이터 저장

### Elasticsearch

- 서비스명: `elasticsearch`
- 포트: `9200`
- 형태: `Deployment`

역할은 아래와 같습니다.

- 검색 및 색인
- 현재는 `product-service`가 직접 의존합니다.

## 7. 서비스 설정 주입 구조

애플리케이션 서비스는 대부분 같은 방식으로 설정을 주입받습니다.

기본 규칙은 아래와 같습니다.

- 공통 설정: `common-config`
- 서비스별 설정: `<service>-config`
- 공통 비밀값: `common-secret`
- DB 비밀값: `db-secret`
- 서비스별 비밀값: `<service>-secret`

예시는 아래와 같습니다.

- `admin-service`
  - `common-config`
  - `admin-config`
  - `common-secret`
  - `db-secret`
  - `admin-secret`

- `api-gateway-service`
  - `common-config`
  - `api-gateway-config`
  - `common-secret`
  - `db-secret`
  - `api-gateway-secret`

즉, 서비스별 설정 리소스 이름은 대부분 같은 규칙을 따릅니다.

## 8. 서비스 리소스 공통 규칙

대부분의 애플리케이션 서비스는 아래 공통 규칙을 사용합니다.

### 배포 형태

- `Deployment`
- `replicas: 1`
- `strategy.type: Recreate`
- `revisionHistoryLimit: 3`

### 리소스

대체로 아래 기준을 사용합니다.

- CPU request: `100m`
- Memory request: `256Mi`
- Memory limit: `512Mi`

CPU limit은 서비스별로 다를 수 있습니다.

- `user-service`: `500m`
- 그 외 서비스: `300m`

현재 `product-service`와 `user-service`도 CPU request는 `100m`로 맞춰져 있습니다. 단일 노드 K3s에서는 실제 사용량보다 `requests` 예약량 합계가 스케줄링에 더 직접적으로 영향을 주기 때문입니다.

일부 인프라 서비스는 별도 리소스 값을 사용합니다.

### JVM 옵션

대부분의 애플리케이션 서비스는 아래 값을 사용합니다.

- `JAVA_TOOL_OPTIONS = -Xms128m -Xmx256m`

## 9. Health Probe 구조

모든 애플리케이션 서비스에는 Spring Boot actuator 기반 probe가 붙어 있습니다.

기본 경로는 아래와 같습니다.

- `startupProbe`: `/actuator/health`
- `readinessProbe`: `/actuator/health/readiness`
- `livenessProbe`: `/actuator/health/liveness`

서비스마다 초기 지연 시간과 timeout 값은 조금씩 다를 수 있습니다.

즉, 운영 중 상태 판정은 Kubernetes와 애플리케이션 관점이 함께 반영되도록 구성되어 있습니다.

## 10. Prometheus 수집 구조

현재 모든 애플리케이션 서비스의 pod template에는 Prometheus annotations가 붙어 있습니다.

기본 규칙은 아래와 같습니다.

- `prometheus.io/scrape: "true"`
- `prometheus.io/path: "/actuator/prometheus"`
- `prometheus.io/port: "<서비스 포트>"`

예시는 아래와 같습니다.

- `api-gateway-service`: `8080`
- `admin-service`: `9007`
- `user-service`: `9003`

즉, 서비스 메트릭은 actuator Prometheus 엔드포인트를 통해 수집되는 구조입니다.

단, 실제 수집 방식은 Prometheus가 어디에 떠 있는지에 따라 달라집니다. 같은 클러스터 discovery, NodePort static scrape, 현재 분리 서버 구조의 제약은 `k3s-monitoring.md`에서 별도로 다룹니다.

## 11. 서비스 노출 방식

현재 서비스 노출 방식은 아래처럼 나뉩니다.

### 외부 노출

- `api-gateway-service`
  - `NodePort`
  - `Ingress` 연결

### 내부 통신 전용

- `admin-service`
- `ai-service`
- `order-service`
- `payment-service`
- `product-service`
- `settlement-service`
- `user-service`

이 서비스들은 기본적으로 `ClusterIP`를 사용합니다.

즉, 내부 서비스는 클러스터 내부 통신용이고 외부 진입은 게이트웨이가 담당하는 구조입니다.

## 12. 배포 순서 관점

명시적으로 배포 순서를 강제하는 단일 스크립트는 없지만, 운영 관점에서는 아래 순서로 이해하시는 편이 좋습니다.

1. 인프라 준비
   - `postgres`
   - `redis`
   - `zookeeper`
   - `kafka`
   - `elasticsearch`
2. 내부 서비스 시작
   - `user`
   - `admin`
   - `order`
   - `payment`
   - `product`
   - `settlement`
   - `ai`
3. 게이트웨이 시작

다만 실제 배포는 개별 서비스 단위로도 가능하며, initContainer가 필요한 인프라를 기다리는 방식으로 보완하고 있습니다.

## 13. 운영자가 기억하면 좋은 핵심 규칙

### 규칙 1. 외부 진입은 게이트웨이를 통합니다

외부 요청은 기본적으로 `api-gateway-service`를 통해 들어옵니다.

### 규칙 2. 대부분의 서비스는 공통 인프라 3종에 의존합니다

대부분의 서비스는 아래 3가지를 기본 의존성으로 가집니다.

- Postgres
- Kafka
- Redis

### 규칙 3. product만 Elasticsearch를 추가로 사용합니다

검색 기능 때문에 `product-service`는 `elasticsearch`를 추가 의존성으로 사용합니다.

### 규칙 4. 설정 주입 규칙은 통일되어 있습니다

- 공통: `common-config`, `common-secret`
- 서비스별: `<service>-config`, `<service>-secret`

### 규칙 5. 메트릭 수집은 서비스 포트 기준으로 통일되어 있습니다

각 서비스는 자신의 actuator Prometheus endpoint를 그대로 노출합니다.

## 14. 관련 파일

주요 배포 파일은 아래와 같습니다.

- `.github/k3s/api-gateway-service.yml`
- `.github/k3s/admin-service.yml`
- `.github/k3s/ai-service.yml`
- `.github/k3s/order-service.yml`
- `.github/k3s/payment-service.yml`
- `.github/k3s/product-service.yml`
- `.github/k3s/settlement-service.yml`
- `.github/k3s/user-service.yml`
- `.github/k3s/postgres.yml`
- `.github/k3s/kafka.yml`
- `.github/k3s/redis.yml`
- `.github/k3s/elasticsearch.yml`

## 15. 요약

현재 배포 구조는 `게이트웨이 1개 + 내부 서비스 여러 개 + 공통 인프라` 구조이며, 대부분의 서비스는 공통 규칙으로 배포되고 게이트웨이가 외부 진입을 담당합니다.

처음 구조를 파악하실 때는 아래 순서로 보시면 이해가 쉽습니다.

1. 게이트웨이가 외부 진입을 받습니다.
2. 내부 서비스는 ClusterIP로 통신합니다.
3. ConfigMap / Secret은 공통 규칙으로 주입됩니다.
4. 인프라 의존성은 initContainer가 먼저 확인합니다.
5. Prometheus는 actuator endpoint를 기준으로 메트릭을 수집합니다.
