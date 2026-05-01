# K3s 개념 정리

## 1. 개요

이 문서는 이 저장소의 K3s 관련 문서들을 이해하는 데 필요한 최소 개념을 정리한 문서입니다.

대상 문서는 아래와 같습니다.

- `k3s-install.md`
- `k3s-workflows.md`
- `k3s-yml.md`
- `k3s-sh.md`

즉, 쿠버네티스를 깊게 공부하기 위한 문서가 아니라, 이 프로젝트의 배포 구조를 읽고 따라갈 수 있을 정도의 개념만 정리하는 것이 목적입니다.

---

## 2. K3s가 무엇인지

K3s는 가벼운 Kubernetes 배포판입니다.

쉽게 말씀드리면, Kubernetes의 핵심 기능을 더 간단하게 설치하고 운영할 수 있도록 만든 버전이라고 보시면 됩니다.

이 프로젝트에서는 EC2 안에 K3s를 설치하고, 그 위에 여러 서비스를 배포합니다.

즉, 지금 구조는 아래처럼 이해하시면 됩니다.

```text
EC2 서버
-> K3s 클러스터
-> user, order, payment 같은 서비스 배포
```

---

## 3. Kubernetes를 왜 쓰는지

Kubernetes는 컨테이너를 실행하고 관리하는 플랫폼입니다.

이 프로젝트에서는 Docker 이미지로 애플리케이션을 만들고, Kubernetes가 그 이미지를 실행하도록 합니다.

Kubernetes를 쓰는 이유는 보통 아래와 같습니다.

- 컨테이너를 일관된 방식으로 배포할 수 있습니다.
- 서비스 재시작과 상태 관리를 자동화할 수 있습니다.
- 여러 서비스를 구조적으로 관리할 수 있습니다.
- readiness, liveness 같은 상태 점검 기능을 사용할 수 있습니다.

---

## 4. 이 프로젝트에서 자주 나오는 구성 요소

이 문서들에서 자주 보게 되는 핵심 개념은 아래 정도입니다.

- `kubectl`
- `Deployment`
- `Pod`
- `Service`
- `Ingress`
- `ConfigMap`
- `Secret`
- `Probe`

아래에서 하나씩 간단히 정리하겠습니다.

---

## 5. kubectl

`kubectl`은 Kubernetes를 조작하는 명령어 도구입니다.

쉽게 말씀드리면, Kubernetes에 명령을 내리는 CLI입니다.

예시:

```bash
kubectl get pods
kubectl get deployment
kubectl apply -f my-service.yml
kubectl logs <pod-name>
```

이 프로젝트에서는 배포 스크립트와 GitHub Actions가 `kubectl`을 사용해서 K3s에 리소스를 적용합니다.

---

## 6. Deployment와 Pod

### 6.1 Pod

Pod는 Kubernetes에서 컨테이너를 실행하는 가장 작은 단위입니다.

초심자 기준으로는 아래처럼 이해하시면 충분합니다.

- Docker 컨테이너를 Kubernetes 안에서 실행한 결과물

### 6.2 Deployment

Deployment는 Pod를 어떻게 띄울지 정의하는 리소스입니다.

예를 들면 아래 내용을 Deployment가 관리합니다.

- 어떤 Docker 이미지를 사용할지
- 파드를 몇 개 띄울지
- 어떤 환경 변수를 넣을지
- 어떤 포트를 사용할지

즉, 실제 애플리케이션 실행 계획서 같은 역할입니다.

이 프로젝트의 `.github/k3s/*-service.yml` 파일에서 가장 큰 비중을 차지하는 것이 Deployment입니다.

---

## 7. Service

Service는 Pod 앞에 붙는 고정된 네트워크 주소입니다.

왜 필요한지 간단히 말씀드리면, Pod는 재시작되면 이름이나 IP가 바뀔 수 있기 때문입니다.

그래서 다른 서비스는 Pod를 직접 부르지 않고, 보통 Service 이름으로 접근합니다.

예시:

- `user-service`
- `order-service`
- `payment-service`
- `ai-service`

즉, 애플리케이션끼리 통신할 때는 보통 Pod가 아니라 Service를 바라본다고 생각하시면 됩니다.

---

## 8. Ingress

Ingress는 외부에서 들어오는 HTTP 요청을 어떤 Service로 보낼지 정하는 규칙입니다.

예를 들어 아래와 같은 역할을 합니다.

- `/api` 요청은 `api-gateway-service`로 전달
- `/` 요청은 `frontend-service`로 전달

이 프로젝트에서는 Traefik을 ingress controller로 사용합니다.

즉, Ingress는 “외부 요청 라우팅 규칙”이라고 이해하시면 됩니다.

---

## 9. ConfigMap과 Secret

애플리케이션은 실행될 때 다양한 설정값이 필요합니다.

예를 들면 아래와 같습니다.

- DB 주소
- Redis 주소
- JWT 시크릿
- OAuth 클라이언트 키

이 값을 Kubernetes에서는 보통 아래 두 가지로 나누어 관리합니다.

### 9.1 ConfigMap

민감하지 않은 일반 설정값을 담는 리소스입니다.

예시:

- `SPRING_PROFILES_ACTIVE=prod`
- `POSTGRES_HOST=my-db-host`

### 9.2 Secret

비밀번호, 토큰, 시크릿 키 같은 민감한 값을 담는 리소스입니다.

예시:

- `JWT_SECRET`
- `MAIL_PASSWORD`
- `GOOGLE_CLIENT_SECRET`

이 프로젝트에서는 `apply-env.sh`가 `.env` 파일을 만들고, `deploy-app.sh`가 그 파일로 ConfigMap과 Secret을 생성하거나 갱신합니다.

---

## 10. Probe

Probe는 애플리케이션 상태를 점검하는 설정입니다.

이 프로젝트에서는 아래 세 가지를 자주 봅니다.

- `startupProbe`
- `readinessProbe`
- `livenessProbe`

### 10.1 startupProbe

애플리케이션이 처음 실행될 때, 부팅이 끝났는지 확인합니다.

### 10.2 readinessProbe

애플리케이션이 실제 요청을 받을 준비가 되었는지 확인합니다.

이 값이 실패하면 파드는 떠 있어도 트래픽을 받지 못할 수 있습니다.

즉, “Running인데 연결이 안 된다”는 문제는 readiness와 관련이 있는 경우가 많습니다.

### 10.3 livenessProbe

애플리케이션이 살아 있는지 확인합니다.

계속 실패하면 Kubernetes가 컨테이너를 재시작할 수 있습니다.

---

## 11. YAML 파일이 무엇인지

Kubernetes 리소스는 보통 YAML 파일로 정의합니다.

예를 들어 아래와 같은 것들이 YAML에 들어 있습니다.

- Deployment
- Service
- Ingress
- 환경 변수 주입 방식
- 포트 정보
- probe 정보

이 프로젝트에서는 `.github/k3s/*.yml` 파일이 실제 배포 매니페스트 역할을 합니다.

즉, YAML은 “Kubernetes에 적용할 설정 문서”라고 보시면 됩니다.

---

## 12. self-hosted runner가 필요한 이유

이 프로젝트의 deploy job은 GitHub 기본 러너가 아니라 EC2 안의 self-hosted runner에서 실행됩니다.

그 이유는 아래와 같습니다.

- K3s 클러스터에 직접 접근해야 합니다.
- `/home/ubuntu/.kube/config` 파일이 필요합니다.
- `kubectl`, `docker`, `envsubst`, `rsync`를 EC2 환경에서 실행해야 합니다.

즉, build는 GitHub 쪽에서 해도 되지만, deploy는 K3s가 있는 서버에서 실행해야 하는 구조입니다.

---

## 13. 이 프로젝트의 배포 흐름을 개념적으로 보면

이 프로젝트의 배포는 개념적으로 아래 순서로 이해하시면 됩니다.

```text
1. Gradle로 애플리케이션 빌드
2. Docker 이미지 생성
3. Docker Hub에 Push
4. EC2 self-hosted runner에서 배포 스크립트 실행
5. ConfigMap / Secret 생성 또는 갱신
6. Kubernetes YAML 적용
7. Deployment rollout 확인
8. readiness 상태 확인
```

즉, 단순히 이미지만 만드는 것이 아니라, 실제 서비스가 준비 상태가 되는지까지 확인하는 구조입니다.

---

## 14. 초심자 기준으로 특히 헷갈리기 쉬운 부분

### 14.1 Pod가 떠 있다고 바로 정상은 아닙니다

파드가 `Running`이어도 readiness가 실패하면 실제 요청은 못 받을 수 있습니다.

### 14.2 Service는 Pod와 다릅니다

Pod는 실제 실행 중인 컨테이너이고, Service는 그 앞의 고정된 접근 지점입니다.

### 14.3 ConfigMap과 Secret은 둘 다 환경 변수 소스입니다

다만 민감도에 따라 나누어 관리한다고 이해하시면 됩니다.

### 14.4 YAML만 있다고 끝나지 않습니다

YAML 파일이 있어도 실제로 `kubectl apply`가 되어야 하고, 필요한 ConfigMap/Secret도 먼저 준비되어야 합니다.

### 14.5 GitHub Actions와 K3s는 역할이 다릅니다

- GitHub Actions: 빌드와 배포 자동화
- K3s: 실제 컨테이너 실행과 운영

---

## 15. 이 문서를 읽고 나서 보면 좋은 순서

개념을 이해하신 뒤에는 아래 순서로 문서를 보시면 흐름이 잘 이어집니다.

1. `k3s-install.md`
   K3s와 기본 도구 설치
2. `k3s-workflows.md`
   GitHub Actions 기반 배포 흐름
3. `k3s-yml.md`
   서비스별 Kubernetes YAML 구조
4. `k3s-sh.md`
   배포 스크립트 동작 방식

---

## 16. 정리

이 프로젝트를 이해하는 데 필요한 최소 개념은 아래 정도입니다.

- K3s는 가벼운 Kubernetes입니다.
- `kubectl`은 Kubernetes를 조작하는 도구입니다.
- Deployment는 파드를 띄우는 설정입니다.
- Service는 파드 앞의 고정 주소입니다.
- Ingress는 외부 요청 진입 규칙입니다.
- ConfigMap과 Secret은 환경 변수 소스입니다.
- Probe는 애플리케이션 상태를 점검합니다.

이 정도 개념만 이해하셔도, 이 저장소의 K3s 설치 문서, 워크플로 문서, YAML 문서, 쉘 스크립트 문서를 읽는 데는 충분합니다.
