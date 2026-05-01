# K3s YAML 가이드

## 1. 문서 목적

이 문서는 `.github/k3s/` 아래에 있는 Kubernetes manifest가 현재 어떤 구조로 작성되어 있는지 설명하는 문서입니다.

주요 목적은 아래와 같습니다.

- 서비스 manifest가 어떤 리소스로 구성되는지 설명합니다.
- 공통 규칙과 서비스별 차이를 정리합니다.
- 수정할 때 어떤 규칙을 맞춰야 하는지 정리합니다.

## 2. 현재 manifest 위치

현재 서비스 manifest는 아래 경로에 있습니다.

- `.github/k3s/api-gateway-service.yml`
- `.github/k3s/admin-service.yml`
- `.github/k3s/ai-service.yml`
- `.github/k3s/order-service.yml`
- `.github/k3s/payment-service.yml`
- `.github/k3s/product-service.yml`
- `.github/k3s/settlement-service.yml`
- `.github/k3s/user-service.yml`

공통 인프라 manifest도 같은 경로 아래에 있습니다.

- `.github/k3s/postgres.yml`
- `.github/k3s/kafka.yml`
- `.github/k3s/redis.yml`
- `.github/k3s/elasticsearch.yml`

외부 진입, 인증서, 게이트웨이 정책 관련 manifest도 같은 경로에서 관리합니다.

- `.github/k3s/main-ingress.yml`
- `.github/k3s/cluster-issuer.yml`
- `.github/k3s/gateway-rules-config.yml`

SSL과 도메인 연결은 `cluster-issuer.yml`과 `main-ingress.yml`이 함께 담당합니다. 자세한 내용은 `k3s-ssl-ingress.md`에서 다룹니다.

모니터링 관련 값과 대시보드는 아래 경로에서 관리합니다.

- `.github/k3s/monitoring/monitoring-values.yaml`
- `.github/k3s/monitoring/prometheus-scrape-config.yaml`
- `.github/k3s/monitoring/grafana-dashboards.yml`
- `.github/k3s/monitoring/grafana-official-dashboards.yml`

서비스별 CD 배포 시 `*-service.yml` 파일은 서버의 아래 경로로 복사됩니다.

- `/home/ubuntu/apps/data/k3s-service/`

이후 `deploy-app.sh`가 해당 파일을 읽어 K3s에 적용합니다.

`.github/scripts/deploy-app.sh`와 `.github/scripts/apply-env.sh`는 YAML 자체는 아니지만 같은 K3s 배포 흐름에 포함되는 실행 스크립트입니다. 자세한 동작은 `k3s-sh.md`에서 다룹니다.

모니터링 파일과 인증서/Ingress 관련 manifest는 서비스별 `deploy-app.sh <service>` 흐름과 적용 방식이 다를 수 있으므로, 실제 적용 시 해당 워크플로 또는 운영 명령을 함께 확인해야 합니다.

## 3. 서비스 manifest 공통 구조

대부분의 서비스 manifest는 아래 구조를 따릅니다.

- `Deployment`
- `Service`

단, `api-gateway-service.yml`은 외부 진입점이기 때문에 아래 리소스가 추가됩니다.

- `NodePort Service`
- `Ingress`

## 4. Deployment 공통 규칙

현재 서비스 Deployment는 대부분 아래 규칙을 사용합니다.

```yaml
spec:
  replicas: 1
  revisionHistoryLimit: 3
  strategy:
    type: Recreate
```

즉, 현재는 단일 replica 기준이며, 롤링 업데이트보다 `Recreate` 전략을 사용합니다.

공통 label 규칙도 아래처럼 맞춰져 있습니다.

```yaml
metadata:
  name: user-service
  labels:
    app: user-service
spec:
  selector:
    matchLabels:
      app: user-service
template:
  metadata:
    labels:
      app: user-service
```

즉, 아래 이름이 서로 맞아야 합니다.

- Deployment 이름
- Service selector
- Pod label
- 배포 스크립트에서 사용하는 서비스 이름 규칙

## 5. 이미지 치환 규칙

서비스 manifest는 이미지 값을 고정 문자열로 적지 않고 아래 변수를 사용합니다.

```yaml
image: ${DOCKERHUB_USERNAME}/chamomile-user:${IMAGE_TAG}
```

이 값은 배포 시 `deploy-app.sh`에서 `envsubst`로 치환됩니다.

즉, 현재 manifest 수정 시 아래 변수 이름은 유지해야 합니다.

- `${DOCKERHUB_USERNAME}`
- `${IMAGE_TAG}`

## 6. envFrom 주입 규칙

현재 서비스들은 대부분 아래 규칙으로 설정을 주입받습니다.

```yaml
envFrom:
  - configMapRef:
      name: common-config
      optional: true
  - configMapRef:
      name: user-config
      optional: true
  - secretRef:
      name: common-secret
      optional: true
  - secretRef:
      name: db-secret
      optional: false
  - secretRef:
      name: user-secret
      optional: true
```

의미는 아래와 같습니다.

- `common-config`, `common-secret`
  - 모든 서비스가 공통으로 참조하는 설정입니다.
- `<service>-config`, `<service>-secret`
  - 특정 서비스 전용 설정입니다.
- `db-secret`
  - DB 접속 정보입니다.

즉, 서비스 manifest의 env 리소스 이름은 `deploy-env.yml`에서 만드는 ConfigMap / Secret 이름과 정확히 맞아야 합니다.

## 7. 포트 규칙

현재 서비스 포트는 아래와 같습니다.

- `api-gateway`: `8080`
- `payment`: `9001`
- `settlement`: `9002`
- `user`: `9003`
- `product`: `9004`
- `order`: `9005`
- `admin`: `9007`
- `ai`: `9009`

아래 값들은 서로 맞아야 합니다.

- 애플리케이션 실제 포트
- `containerPort`
- `Service.port`
- `Service.targetPort`
- probe 포트
- Prometheus annotation의 `prometheus.io/port`

## 8. 리소스 설정

현재 애플리케이션 서비스는 대부분 아래 리소스 기준을 사용합니다.

```yaml
resources:
  requests:
    cpu: "100m"
    memory: "256Mi"
  limits:
    cpu: "300m"
    memory: "512Mi"
```

현재 `product-service`와 `user-service`도 단일 노드에서 CPU 예약량이 과도하게 잡히지 않도록 `requests.cpu: "100m"` 기준을 사용합니다.

서비스별 limit은 완전히 동일하지 않습니다.

- `user-service`: CPU limit `500m`
- 그 외 서비스: CPU limit `300m`

Kubernetes 스케줄러는 실제 CPU 사용량이 아니라 `requests.cpu`, `requests.memory`를 기준으로 Pod 배치 가능 여부를 판단합니다. 노드의 현재 CPU 사용률이 낮아도 request 합계가 allocatable 용량을 넘으면 Pod가 `Pending` 상태가 될 수 있습니다.

또한 대부분의 서비스는 아래 JVM 옵션을 사용합니다.

```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-Xms128m -Xmx256m"
```

## 9. Health Probe 규칙

현재 서비스는 Spring Boot actuator 기반 probe를 사용합니다.

기본 경로는 아래와 같습니다.

- `startupProbe`: `/actuator/health`
- `readinessProbe`: `/actuator/health/readiness`
- `livenessProbe`: `/actuator/health/liveness`

각 서비스별로 `initialDelaySeconds`, `periodSeconds`, `timeoutSeconds`, `failureThreshold` 값은 조금씩 다를 수 있습니다.

예를 들어:

- `api-gateway-service`는 `8080` 포트를 사용합니다.
- `admin-service`는 `9007` 포트를 사용합니다.
- `product-service`는 다른 서비스보다 startup 지연을 더 길게 두고 있습니다.

즉, 서비스 특성에 따라 probe 수치는 달라도 probe 경로 구조는 통일되어 있습니다.

## 10. Prometheus annotations

현재 모든 애플리케이션 서비스의 pod template에는 Prometheus annotations가 들어가 있습니다.

기본 규칙은 아래와 같습니다.

```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/path: "/actuator/prometheus"
  prometheus.io/port: "<서비스 포트>"
```

예시는 아래와 같습니다.

- `api-gateway-service`: `8080`
- `admin-service`: `9007`
- `ai-service`: `9009`

즉, 메트릭 수집은 각 서비스의 actuator Prometheus endpoint를 기준으로 합니다.

## 11. initContainer 의존성 대기

현재 서비스들은 필요한 인프라가 준비될 때까지 `initContainers`로 대기합니다.

### 공통 예시: Postgres

```yaml
initContainers:
  - name: wait-for-postgres
    image: busybox:1.36
    command:
      - /bin/sh
      - -c
      - until nc -z postgres-headless 5432; do echo waiting for postgres-headless:5432; sleep 2; done
```

이 방식의 의미는 아래와 같습니다.

- DB 접속이 가능해질 때까지 메인 컨테이너 시작을 늦춥니다.
- DNS 이름과 포트만 확인합니다.

### 현재 서비스별 의존성

- `api-gateway-service`
  - Postgres
  - Redis
- `admin-service`
  - Postgres
  - Kafka
  - Redis
- `ai-service`
  - Postgres
  - Kafka
  - Redis
- `order-service`
  - Postgres
  - Kafka
  - Redis
- `payment-service`
  - Postgres
  - Kafka
  - Redis
- `product-service`
  - Postgres
  - Kafka
  - Redis
  - Elasticsearch
- `settlement-service`
  - Postgres
  - Kafka
  - Redis
- `user-service`
  - Postgres
  - Kafka
  - Redis

즉, 서비스별 initContainer 구성이 다를 수 있으므로 manifest 수정 시 의존성 목록도 함께 봐야 합니다.

## 12. api-gateway만 다른 점

`api-gateway-service.yml`은 다른 서비스와 달리 외부 진입점 역할을 합니다.

추가로 아래 리소스를 가집니다.

### 1. NodePort Service

```yaml
spec:
  type: NodePort
  ports:
    - port: 8080
      targetPort: 8080
      nodePort: 30100
```

### 2. Ingress

현재 Ingress는 아래 경로를 게이트웨이로 연결합니다.

- `/api`
- `/swagger-ui`
- `/v3/api-docs`
- `/docs`
- `/oauth2`
- `/login/oauth2`

`/oauth2/callback`과 루트 경로 `/`는 `frontend-service:3000`으로 연결합니다.

`/oauth2/callback`은 `/oauth2`보다 구체적인 경로이므로 `/oauth2`보다 먼저 선언해야 합니다.

HTTPS 인증서 발급과 `main-tls` Secret 생성 흐름은 `k3s-ssl-ingress.md`에서 별도로 설명합니다.

즉, 외부 트래픽은 게이트웨이에서 받아 내부 서비스로 전달하는 구조입니다.

## 13. 인프라 manifest 예시

서비스 manifest 외에도 인프라 manifest가 함께 존재합니다.

예를 들어 `postgres.yml`은 아래 리소스를 포함합니다.

- `postgres-headless` Service
- `postgres` Service
- `postgres` StatefulSet

즉, 서비스 manifest에서 참조하는 DNS 이름인 `postgres-headless` 같은 값은 인프라 manifest와 연결되어 있습니다.

이 점 때문에 서비스 manifest만 따로 수정할 때도 인프라 이름을 함부로 바꾸면 안 됩니다.

## 14. 수정 시 주의할 점

`.github/k3s/*-service.yml`을 수정할 때는 아래 항목을 함께 확인하시는 편이 안전합니다.

### 1. 이름 규칙

- Deployment 이름
- Service 이름
- Pod label
- `deploy-app.sh` 인자 규칙

### 2. env 리소스 이름

- `common-config`
- `common-secret`
- `<service>-config`
- `<service>-secret`
- `db-secret`

### 3. 이미지 변수

- `${DOCKERHUB_USERNAME}`
- `${IMAGE_TAG}`

### 4. 의존성 대상

- `postgres-headless:5432`
- `kafka-service:9092`
- `redis:6379`
- `elasticsearch:9200`

### 5. probe와 annotation 포트

- `containerPort`
- probe 포트
- Service 포트
- Prometheus 포트

## 15. 문제 발생 시 확인 순서

manifest 변경 후 문제가 생기면 아래 순서로 확인하시는 편이 좋습니다.

1. `kubectl get deployment`
2. `kubectl get svc`
3. `kubectl get ingress`
4. `kubectl describe deployment <name>`
5. `kubectl describe pod <pod-name>`
6. `kubectl logs <pod-name> --tail=200`

자주 발생하는 문제는 아래와 같습니다.

- probe 포트가 잘못되었습니다.
- ConfigMap / Secret 이름이 manifest와 다릅니다.
- initContainer가 잘못된 DNS나 포트를 기다립니다.
- Service selector와 Pod label이 다릅니다.
- 외부 노출 경로가 게이트웨이와 맞지 않습니다.

## 16. 요약

현재 `.github/k3s` 아래 서비스 manifest는 공통 규칙을 많이 공유하지만, 서비스별 의존성과 포트, probe 수치, 게이트웨이의 외부 노출 방식은 차이가 있습니다.

즉, manifest 수정 시에는 아래 3가지를 항상 함께 보셔야 합니다.

1. 서비스 이름 규칙
2. env 리소스 이름 규칙
3. 의존성 및 포트 규칙
