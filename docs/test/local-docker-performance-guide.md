# Docker 성능 테스트 환경 구축 가이드

## 목표

```
부하 발생(k6)
→ Prometheus/Grafana로 지표 확인
→ 병목 구간 파악
→ 코드/설정 개선
→ 동일 조건 재측정
→ 개선 전후 수치 비교
```

**측정 대상 지표**

| 지표 | 도구 |
|------|------|
| TPS / RPS | k6 output |
| p95, p99 응답 시간 | k6 + Grafana |
| HTTP 5xx 에러율 | k6 + Grafana |
| CPU 사용률 | Grafana (cAdvisor or host metrics) |
| JVM Heap 사용률 | Grafana (Spring Actuator → Prometheus) |
| GC Pause | Grafana |
| HikariCP Active / Pending | Grafana |
| DB 쿼리 수행 시간 | Grafana |

---

## 빌드 흐름

```
[로컬 Gradle 빌드 → JAR 생성]
        ↓
[docker build → 로컬 이미지]
        ↓
[docker-compose up → 컨테이너 실행]
        ↓
[k6 → Prometheus → Grafana]
```

CI/CD를 구축하지 않아도 된다. 빌드와 이미지 생성을 직접 실행하면 동일한 결과다.

---

## 전체 구성도

```
[k6]
  │  HTTP 요청
  ▼
[api-gateway :8080]
  │  라우팅
  ▼
[payment:9001] [user:9003] [product:9004] [order:9005] ...
  │               │
  └───────────────┘
  /actuator/prometheus (각 서비스)
  │
  ▼
[Prometheus :9090]
  │
  ▼
[Grafana :3000]
```

---

## 사전 확인: 로컬 앱 실행 테스트 (권장)

Docker로 묶기 전에 빌드가 통과하는지 먼저 확인한다.
Docker 안에서 빌드 에러를 디버깅하면 로그가 복잡해진다.

```bash
# 인프라 먼저 (Kafka, Postgres, Redis, Elasticsearch)
docker-compose up -d

# 특정 서비스 단독 빌드 확인
./gradlew :service:user:bootRun

# 빌드만 확인 (실행 없이)
./gradlew :service:user:clean :service:user:bootJar
```

빌드가 통과하면 Docker 환경으로 이동한다.

---

## Step 1: 모든 서비스 JAR 빌드

각 서비스의 Dockerfile은 `build/libs/*.jar`를 컨테이너 안으로 복사한다.
이미지를 빌드하기 전에 반드시 JAR를 먼저 생성해야 한다.

```bash
# 전체 빌드 (테스트 제외 — 속도 우선)
./gradlew clean bootJar -x test

# 또는 서비스별로
./gradlew :service:payment:clean :service:payment:bootJar -x test
./gradlew :service:user:clean :service:user:bootJar -x test
./gradlew :service:product:clean :service:product:bootJar -x test
./gradlew :service:order:clean :service:order:bootJar -x test
./gradlew :service:settlement:clean :service:settlement:bootJar -x test
./gradlew :service:admin:clean :service:admin:bootJar -x test
./gradlew :service:ai:clean :service:ai:bootJar -x test
./gradlew :api-gateway:clean :api-gateway:bootJar -x test
```

---

## Step 2: `.env` 파일 준비

기존 `.env`를 복사해 로컬 테스트용으로 조정한다.

```bash
cp .env .env.local
```

`.env.local` 핵심 값:

```dotenv
SPRING_PROFILES_ACTIVE=prod

# Kafka (docker-compose 네트워크 내부 서비스명 사용)
KAFKA_HOST=kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Postgres
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=backend5

# Redis
APP_REDIS_HOST=redis
APP_REDIS_PORT=6379

# Gateway (컨테이너 내부에서 서로 접근하는 URL)
GATEWAY_SERVICE_URL=http://api-gateway:8080

# 서비스간 URL (필요한 경우)
ORDER_SERVICE_URL=http://order-service:9005

# 외부 연동 (실제 값 입력)
JWT_SECRET=your-jwt-secret
TOSS_SECRET_KEY=your-toss-key
AWS_ACCESS_KEY_ID=your-aws-key
AWS_SECRET_ACCESS_KEY=your-aws-secret
S3_BUCKET_NAME=your-bucket
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email
MAIL_PASSWORD=your-password
```

> 소셜 로그인(Google, Kakao, Naver) 키도 필요하면 추가한다.

---

## Step 3: Prometheus 설정 파일 작성

```bash
mkdir -p monitoring/prometheus
```

`monitoring/prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-gateway:8080']

  - job_name: 'payment-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['payment-service:9001']

  - job_name: 'user-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['user-service:9003']

  - job_name: 'product-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['product-service:9004']

  - job_name: 'order-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['order-service:9005']

  - job_name: 'settlement-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['settlement-service:9002']
```

---

## Step 4: Grafana 프로비저닝 설정

```bash
mkdir -p monitoring/grafana/provisioning/datasources
mkdir -p monitoring/grafana/provisioning/dashboards
mkdir -p monitoring/grafana/dashboards
```

`monitoring/grafana/provisioning/datasources/prometheus.yml`:

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

`monitoring/grafana/provisioning/dashboards/default.yml`:

```yaml
apiVersion: 1

providers:
  - name: default
    orgId: 1
    folder: ''
    type: file
    options:
      path: /var/lib/grafana/dashboards
```

> 대시보드 JSON 파일은 Step 7에서 import한다.

---

## Step 5: docker-compose.local.yml 작성

기존 `docker-compose.yml`(인프라)을 base로 extends하는 방식으로 앱 서비스 + 모니터링을 추가한다.

```yaml
# docker-compose.local.yml
# 실행: docker-compose -f docker-compose.yml -f docker-compose.local.yml --env-file .env.local up -d

services:
  # ── 애플리케이션 서비스 ──────────────────────────────────────────
  api-gateway:
    build:
      context: ./api-gateway
    container_name: api-gateway
    ports:
      - "8080:8080"
    env_file: .env.local
    depends_on:
      - postgres
      - redis
    restart: unless-stopped

  payment-service:
    build:
      context: ./service/payment
    container_name: payment-service
    ports:
      - "9001:9001"
    env_file: .env.local
    depends_on:
      - postgres
      - kafka
    restart: unless-stopped

  user-service:
    build:
      context: ./service/user
    container_name: user-service
    ports:
      - "9003:9003"
    env_file: .env.local
    depends_on:
      - postgres
      - kafka
      - redis
    restart: unless-stopped

  product-service:
    build:
      context: ./service/product
    container_name: product-service
    ports:
      - "9004:9004"
    env_file: .env.local
    depends_on:
      - postgres
      - kafka
      - elasticsearch
    restart: unless-stopped

  order-service:
    build:
      context: ./service/order
    container_name: order-service
    ports:
      - "9005:9005"
    env_file: .env.local
    depends_on:
      - postgres
      - kafka
    restart: unless-stopped

  settlement-service:
    build:
      context: ./service/settlement
    container_name: settlement-service
    ports:
      - "9002:9002"
    env_file: .env.local
    depends_on:
      - postgres
      - kafka
    restart: unless-stopped

  # ── 모니터링 스택 ──────────────────────────────────────────────
  prometheus:
    image: prom/prometheus:v2.51.2
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=7d'
    restart: unless-stopped

  grafana:
    image: grafana/grafana:10.4.2
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - ./monitoring/grafana/provisioning:/etc/grafana/provisioning:ro
      - ./monitoring/grafana/dashboards:/var/lib/grafana/dashboards:ro
      - grafana_data:/var/lib/grafana
    depends_on:
      - prometheus
    restart: unless-stopped

volumes:
  prometheus_data:
  grafana_data:
```

---

## Step 6: 실행

```bash
# 1. 인프라 + 앱 + 모니터링 전체 기동
docker-compose -f docker-compose.yml -f docker-compose.local.yml --env-file .env.local up -d

# 2. 컨테이너 상태 확인
docker-compose -f docker-compose.yml -f docker-compose.local.yml ps

# 3. 특정 서비스 로그 확인
docker logs -f user-service

# 4. Prometheus targets 확인 (브라우저)
# http://localhost:9090/targets  → 모든 서비스가 UP 이어야 함

# 5. Grafana 접속
# http://localhost:3000  (admin / admin)
```

---

## Step 7: Grafana 대시보드 설정

### Spring Boot 전용 대시보드

Grafana Labs에서 검증된 대시보드를 import한다.

| 대시보드 | ID | 용도 |
|----------|----|------|
| JVM (Micrometer) | `4701` | Heap, GC, Thread, HikariCP |
| Spring Boot Statistics | `12464` | HTTP, DB, 커넥션 풀 |
| k6 Load Testing Results | `2587` | k6 실시간 결과 |

**Import 방법**: Grafana → Dashboards → Import → ID 입력 → Prometheus 데이터소스 선택

### HikariCP 핵심 패널 쿼리 예시

```promql
# Active 커넥션
hikaricp_connections_active{application="user-service"}

# Pending 커넥션 (이게 올라가면 DB 병목)
hikaricp_connections_pending{application="user-service"}

# p99 응답시간
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[1m]))
```

---

## Step 8: k6 테스트 실행

### 설치

```bash
brew install k6
```

### 기본 테스트 스크립트 예시

`docs/test/scripts/smoke-test.js`:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },   // ramp-up
    { duration: '1m',  target: 10 },   // steady
    { duration: '30s', target: 0 },    // ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // p95 < 500ms
    http_req_failed: ['rate<0.01'],    // 에러율 < 1%
  },
};

export default function () {
  const res = http.get('http://localhost:8080/api/products');
  check(res, {
    'status 200': (r) => r.status === 200,
    'duration < 500ms': (r) => r.timings.duration < 500,
  });
  sleep(1);
}
```

### 실행

```bash
# 기본 실행 (터미널 출력)
k6 run docs/test/scripts/smoke-test.js

# Prometheus remote write로 실시간 연동
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
k6 run --out=experimental-prometheus-rw docs/test/scripts/smoke-test.js
```

> `--out=experimental-prometheus-rw` 사용 시 Grafana 대시보드 ID `2587`에서 실시간으로 확인 가능

---

## Step 9: 개선 사이클

```
1. k6 실행
2. Grafana에서 병목 지점 확인
   - p99 응답시간 spike → 어느 서비스?
   - HikariCP Pending 증가 → 커넥션 풀 부족
   - GC Pause 증가 → Heap 튜닝 필요
3. 코드/설정 수정
4. JAR 재빌드 → 컨테이너 재기동
   docker-compose -f docker-compose.yml -f docker-compose.local.yml build user-service
   docker-compose -f docker-compose.yml -f docker-compose.local.yml up -d user-service
5. 동일 조건으로 k6 재실행 (같은 VU 수, 같은 duration)
6. 개선 전후 스크린샷 비교
```

---

## 자주 쓰는 명령어

```bash
# 전체 기동
docker-compose -f docker-compose.yml -f docker-compose.local.yml --env-file .env.local up -d

# 특정 서비스만 재기동
docker-compose -f docker-compose.local.yml up -d --build user-service

# 전체 종료 (볼륨 유지)
docker-compose -f docker-compose.yml -f docker-compose.local.yml down

# 전체 종료 + 볼륨 삭제 (데이터 초기화)
docker-compose -f docker-compose.yml -f docker-compose.local.yml down -v

# Prometheus 타겟 상태 확인
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job: .labels.job, health: .health}'

# 특정 서비스 메트릭 확인
curl http://localhost:9003/actuator/prometheus | grep hikaricp
```

---

## 체크리스트

- [ ] `./gradlew clean bootJar -x test` 빌드 성공
- [ ] `.env.local` 파일 작성
- [ ] `monitoring/prometheus/prometheus.yml` 작성
- [ ] `docker-compose -f docker-compose.yml -f docker-compose.local.yml up -d` 실행
- [ ] `http://localhost:9090/targets` 에서 모든 서비스 UP 확인
- [ ] `http://localhost:3000` Grafana 접속 확인
- [ ] Grafana 대시보드 4701, 12464 import
- [ ] k6 smoke test 실행 확인