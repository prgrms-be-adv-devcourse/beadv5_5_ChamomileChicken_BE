# K3s Monitoring 가이드

## 1. 문서 목적

이 문서는 K3s 환경의 Prometheus, Grafana, scrape 설정, dashboard 파일을 정리한 문서입니다.

대상 파일은 아래와 같습니다.

- `.github/k3s/monitoring/monitoring-values.yaml`
- `.github/k3s/monitoring/prometheus-scrape-config.yaml`
- `.github/k3s/monitoring/grafana-dashboards.yml`
- `.github/k3s/monitoring/grafana-official-dashboards.yml`
- `.github/k3s/monitoring/apply-dashboards.ps1`
- `.github/k3s/monitoring/dashboards-upstream/`

모니터링은 서비스 배포 manifest와 역할이 다르므로, 인프라 데이터 YAML 문서와 분리해서 관리합니다.

## 2. 현재 모니터링 구성

현재 모니터링 파일은 크게 세 종류입니다.

### Prometheus / Grafana values

파일:

- `.github/k3s/monitoring/monitoring-values.yaml`

주요 내용:

- Grafana `NodePort`: `30300`
- Grafana resource request/limit
- Grafana dashboard sidecar 설정
- Prometheus resource request/limit
- Prometheus scrape interval / evaluation interval
- `additionalScrapeConfigs`

### Prometheus scrape config

파일:

- `.github/k3s/monitoring/prometheus-scrape-config.yaml`

주요 내용:

- Kubernetes service discovery 기반 Spring 서비스 scrape 설정
- Pod annotation 기반 scrape 대상 선별
- `/actuator/prometheus` 경로 수집

### Grafana dashboard

파일:

- `.github/k3s/monitoring/grafana-dashboards.yml`
- `.github/k3s/monitoring/grafana-official-dashboards.yml`
- `.github/k3s/monitoring/dashboards-upstream/`

주요 내용:

- Grafana dashboard ConfigMap
- JVM, Kafka, Node exporter, Postgres, HikariCP 등 dashboard 정의
- `grafana_dashboard` label 기반 sidecar 로딩

## 3. monitoring-values.yaml

`monitoring-values.yaml`은 Prometheus / Grafana Helm values 성격의 파일입니다.

현재 핵심 설정은 아래와 같습니다.

- Grafana는 `NodePort` `30300`으로 노출합니다.
- Prometheus는 `prometheus.prometheusSpec` 아래에서 scrape 관련 설정을 가집니다.
- `additionalScrapeConfigs`에 Kafka, Postgres, Spring 서비스, node exporter target이 들어 있습니다.

현재 `additionalScrapeConfigs`에는 두 종류의 target이 섞여 있습니다.

### 클러스터 내부 DNS target

예시:

- `kafka-exporter.monitoring.svc.cluster.local:9308`
- `postgres-exporter.monitoring.svc.cluster.local:9187`

이 값들은 Prometheus가 같은 Kubernetes 클러스터 안에서 실행될 때 자연스럽게 해석됩니다.

### Private IP + NodePort target

예시:

- `<MODULE_SERVER_PRIVATE_IP>:30903`
- `<MODULE_SERVER_PRIVATE_IP>:30904`
- `<MODULE_SERVER_PRIVATE_IP>:30100`
- `<MODULE_SERVER_PRIVATE_IP>:30910`

이 방식은 모니터링 서버와 서비스 서버가 달라도 네트워크, 보안 그룹, 방화벽, 라우팅이 열려 있으면 동작할 수 있습니다.

여기서 `<MODULE_SERVER_PRIVATE_IP>`는 해당 모듈 또는 서비스가 배포된 서버의 private IP를 의미합니다.

다만 private IP나 NodePort가 바뀌면 scrape 설정도 같이 바꿔야 합니다.

## 4. prometheus-scrape-config.yaml

`prometheus-scrape-config.yaml`은 Kubernetes service discovery 기반 scrape 설정입니다.

현재 핵심 구조는 아래와 같습니다.

- job 이름: `spring-services`
- 메트릭 경로: `/actuator/prometheus`
- discovery 방식: `kubernetes_sd_configs`
- discovery 대상: `default` namespace의 Pod
- scrape 대상 선별 기준: Pod annotation `prometheus.io/scrape: "true"`
- 실제 scrape port: Pod annotation `prometheus.io/port`
- label 보강: Pod label의 `app`, namespace

이 방식은 Prometheus가 애플리케이션 Pod가 떠 있는 Kubernetes API를 볼 수 있을 때 가장 자연스럽게 동작합니다.

즉, Prometheus와 서비스 Pod가 같은 K3s 클러스터 안에 있거나, Prometheus가 해당 클러스터의 Kubernetes API와 Pod 네트워크를 볼 수 있어야 합니다.

## 5. 현재 분리 서버 구조에서의 한계

현재는 모니터링 서버와 서비스 서버가 서로 다른 서버에 있는 구조입니다.

이 상태에서 Prometheus가 서비스 서버의 Kubernetes API / Pod 네트워크를 직접 볼 수 없다면 `prometheus-scrape-config.yaml`의 `kubernetes_sd_configs` 방식은 그대로 활용하기 어렵습니다.

이 말은 “모니터링이 분리되어 있으면 무조건 수집이 불가능하다”는 뜻은 아닙니다.

정확히는 아래처럼 구분해야 합니다.

- `prometheus-scrape-config.yaml`의 Kubernetes service discovery 방식은 같은 클러스터 또는 remote Kubernetes API 접근이 필요합니다.
- `monitoring-values.yaml`의 private IP + NodePort 방식은 서버가 달라도 네트워크가 열려 있으면 동작할 수 있습니다.

따라서 지금 구조에서는 `prometheus-scrape-config.yaml`보다 `monitoring-values.yaml`의 static target 방식이 더 현실적인 수집 방식입니다.

## 6. 선택 가능한 방향

현재 구조에서 선택지는 아래와 같습니다.

### 1. 모니터링을 서비스와 같은 K3s 클러스터로 이동

장점:

- Kubernetes service discovery를 그대로 사용할 수 있습니다.
- Pod annotation 기반 자동 수집이 쉬워집니다.
- exporter와 service DNS를 내부 주소로 안정적으로 사용할 수 있습니다.

단점:

- 서비스 클러스터 리소스를 더 사용합니다.
- 운영 장애가 같은 클러스터에 묶일 수 있습니다.

### 2. 분리 서버 유지 + NodePort/static scrape 사용

장점:

- 현재 분리 구조를 유지할 수 있습니다.
- Prometheus가 Kubernetes API를 몰라도 target을 수집할 수 있습니다.

단점:

- NodePort를 열어야 합니다.
- 보안 그룹, 방화벽, 라우팅 관리가 필요합니다.
- private IP나 NodePort가 바뀌면 설정을 수정해야 합니다.

### 3. 분리 서버 유지 + remote Kubernetes API 접근 구성

장점:

- Prometheus는 분리된 상태로 두면서 Kubernetes discovery를 사용할 수 있습니다.

단점:

- kubeconfig, RBAC, API server 접근, 네트워크 보안 구성이 필요합니다.
- Pod IP로 직접 scrape하려면 Pod 네트워크 접근성까지 해결해야 합니다.

## 7. 서비스 쪽 준비 조건

Spring 서비스 Pod에는 Prometheus annotation이 들어 있습니다.

기본 규칙:

- `prometheus.io/scrape: "true"`
- `prometheus.io/path: "/actuator/prometheus"`
- `prometheus.io/port: "<서비스 포트>"`

서비스 애플리케이션 쪽에서는 `/actuator/prometheus` endpoint가 실제로 열려 있어야 합니다.

확인 예시:

```bash
kubectl port-forward svc/user-service 9003:9003
curl http://localhost:9003/actuator/prometheus
```

NodePort/static scrape 방식에서는 각 서비스의 NodePort 또는 외부 접근 가능한 endpoint가 필요합니다.

## 8. 확인 명령

Prometheus / Grafana 상태:

```bash
kubectl get pods -n monitoring
kubectl get svc -n monitoring
```

Grafana NodePort:

```bash
kubectl get svc -n monitoring | grep grafana
```

서비스 메트릭 endpoint:

```bash
curl http://<service-host>:<port>/actuator/prometheus
```

Prometheus target 상태는 Grafana 또는 Prometheus UI에서 `Targets` 화면으로 확인합니다.

## 9. 주의할 점

- `monitoring-values.yaml`은 Helm values 성격이므로 일반 manifest처럼 `kubectl apply`로 처리하는 파일이 아닐 수 있습니다.
- `prometheus-scrape-config.yaml`은 Kubernetes service discovery가 가능한 환경을 전제로 합니다.
- 모니터링 서버와 서비스 서버가 분리되어 있으면 Pod annotation만으로는 자동 수집되지 않습니다.
- static target 방식은 private IP, NodePort, 보안 그룹 변화에 취약합니다.
- `<MODULE_SERVER_PRIVATE_IP>` 값이 바뀌면 `monitoring-values.yaml`도 함께 수정해야 합니다.

## 10. 요약

현재 모니터링 설정은 두 방향이 섞여 있습니다.

- `prometheus-scrape-config.yaml`: 같은 클러스터 또는 Kubernetes API 접근이 가능한 Prometheus에 적합합니다.
- `monitoring-values.yaml`의 static target: 다른 서버에서 NodePort로 수집할 때 사용할 수 있습니다.

현재처럼 모니터링과 서비스가 서로 다른 서버에 있다면, `prometheus-scrape-config.yaml`은 그대로 쓰기 어렵고 static target 방식이나 별도 네트워크/RBAC 구성이 필요합니다.
