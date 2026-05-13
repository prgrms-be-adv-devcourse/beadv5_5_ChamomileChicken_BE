# Gateway Rules Config Deploy Workflow

## 개요

Gateway Rules ConfigMap을 Kubernetes 클러스터에 배포하기 위한 GitHub Actions Workflow입니다.

`gateway-rules-config.yml` 파일이 수정되면 자동으로 ConfigMap을 적용하고,  
`api-gateway-service` Deployment를 재시작합니다.

---

# 추천 파일 이름

```text
gateway-rules-config-deploy.md
```

---

# Workflow 파일 위치

## Workflow

```text
.github/workflows/deploy-gateway-rules-config.yml
```

## Kubernetes Config

```text
.github/k3s/gateway-rules-config.yml
```

---

# Workflow 전체 흐름

```text
dev 브랜치 Push
        ↓
GitHub Actions 실행
        ↓
Config YAML Checkout
        ↓
서버 내부 경로로 파일 복사
        ↓
kubectl apply 실행
        ↓
api-gateway-service 재시작
        ↓
ConfigMap 적용 여부 확인
```

---

# YAML 설정 설명

## name

```yaml
name: Deploy Gateway Rules Config
```

GitHub Actions 화면에 표시되는 Workflow 이름입니다.

Actions 탭에서 아래 이름으로 표시됩니다.

```text
Deploy Gateway Rules Config
```

---

## on

```yaml
on:
  workflow_dispatch:

  push:
    branches:
      - dev
```

Workflow 실행 조건을 정의합니다.

### workflow_dispatch

GitHub Actions 화면에서 수동 실행할 수 있습니다.

### push

특정 브랜치 Push 시 자동 실행됩니다.

현재는 `dev` 브랜치 Push 시 실행됩니다.

---

## paths

```yaml
paths:
  - '.github/k3s/gateway-rules-config.yml'
  - '.github/workflows/deploy-gateway-rules-config.yml'
```

특정 파일이 변경되었을 때만 Workflow를 실행합니다.

즉 아래 파일 수정 시에만 배포가 수행됩니다.

- gateway-rules-config.yml
- deploy-gateway-rules-config.yml

불필요한 배포 실행을 방지하기 위한 설정입니다.

---

## permissions

```yaml
permissions:
  contents: read
```

Repository 내용을 읽기 전용으로 사용합니다.

현재 Workflow에서는 코드 Checkout만 필요하기 때문에  
최소 권한만 사용하도록 설정되어 있습니다.

---

## concurrency

```yaml
concurrency:
  group: deploy-gateway-rules-config-${{ github.ref }}
  cancel-in-progress: true
```

동일 브랜치에서 Workflow가 여러 번 실행되는 상황을 제어합니다.

### group

같은 브랜치 Workflow를 하나의 그룹으로 묶습니다.

### cancel-in-progress

이전에 실행 중인 Workflow가 있으면 취소하고  
최신 Workflow만 실행합니다.

중복 배포 방지를 위한 설정입니다.

---

# Job 설정

## jobs

```yaml
jobs:
  deploy-gateway-rules-config:
```

실제 배포 작업(Job)을 정의합니다.

현재 Workflow에서는  
`deploy-gateway-rules-config` Job 하나만 사용합니다.

---

## runs-on

```yaml
runs-on: [ self-hosted, ec2, docker ]
```

Workflow를 실행할 Runner 환경입니다.

현재는 Self Hosted Runner 기반으로 실행됩니다.

### self-hosted

GitHub 기본 Runner가 아닌 직접 운영하는 Runner 사용

### ec2

AWS EC2 서버에서 실행

### docker

Docker 환경이 구성된 Runner 사용

---

## env

```yaml
env:
  KUBECONFIG: /home/ubuntu/.kube/config
```

Workflow 전체에서 공통으로 사용할 환경 변수입니다.

현재는 Kubernetes 접근을 위한 kubeconfig 경로를 지정합니다.

---

# Steps 설명

## 1. Checkout

```yaml
- name: Checkout
  uses: actions/checkout@v4
```

Repository 코드를 Runner 서버로 가져옵니다.

---

## sparse-checkout

```yaml
sparse-checkout: |
  .github/k3s/gateway-rules-config.yml
```

전체 Repository가 아닌  
필요한 파일만 Checkout 합니다.

불필요한 파일 다운로드를 줄이기 위한 설정입니다.

---

## 2. 디렉토리 생성

```yaml
mkdir -p /home/ubuntu/apps/data/k3s-service
```

Kubernetes YAML 저장 디렉토리가 없을 경우 생성합니다.

---

## 3. YAML 파일 복사

```yaml
cp .github/k3s/gateway-rules-config.yml \
/home/ubuntu/apps/data/k3s-service/gateway-rules-config.yml
```

Repository 내부 YAML 파일을 실제 배포 경로로 복사합니다.

---

## 4. 파일 확인

```yaml
ls -l /home/ubuntu/apps/data/k3s-service/gateway-rules-config.yml
```

배포 파일이 정상적으로 존재하는지 확인합니다.

배포 전 검증 단계입니다.

---

## 5. ConfigMap 적용

```yaml
kubectl apply -f /home/ubuntu/apps/data/k3s-service/gateway-rules-config.yml
```

Kubernetes 클러스터에 ConfigMap을 적용합니다.

기존 ConfigMap이 존재하면 Update 됩니다.

---

## 6. Deployment 재시작

```yaml
kubectl rollout restart deployment/api-gateway-service
```

ConfigMap 변경 사항 반영을 위해 Gateway Deployment를 재시작합니다.

---

## rollout status

```yaml
kubectl rollout status deployment/api-gateway-service --timeout=300s
```

Deployment가 정상적으로 올라올 때까지 대기합니다.

최대 대기 시간은 300초입니다.

---

## 7. ConfigMap 검증

```yaml
kubectl get configmap gateway-rules-config -o yaml
```

최종적으로 적용된 ConfigMap 정보를 확인합니다.

배포 검증 단계입니다.

---

# 주의사항

- ConfigMap 수정 시 Gateway Pod가 재시작됩니다.
- kubeconfig 경로 변경 시 Workflow 수정이 필요합니다.
- Self Hosted Runner 상태가 비정상일 경우 배포가 실패할 수 있습니다.
- Deployment 재시작 중 일시적인 응답 지연이 발생할 수 있습니다.
