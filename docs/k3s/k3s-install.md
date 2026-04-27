# k3s 설치 및 이미지 관리 가이드 (실전 구축 기준)

## 1. 개요

이 문서는 k3s(Kubernetes 경량 배포판)를 설치하고
컨테이너 이미지 상태를 확인 및 정리하는 과정까지를 정리한 가이드이다.

---

# 2. k3s 설치 과정

## 2.1 설치 실행

```bash
curl -sfL https://get.k3s.io | sh -
```

설치 시 자동으로 다음이 구성된다:

* Kubernetes control-plane
* containerd (컨테이너 런타임)
* k3s 서비스 등록

---

## 2.2 설치 확인

```bash
sudo kubectl get nodes
```

---

## 2.3 kubectl 설정

```bash
mkdir -p /home/ubuntu/.kube
sudo cp /etc/rancher/k3s/k3s.yaml /home/ubuntu/.kube/config
sudo chown ubuntu:ubuntu /home/ubuntu/.kube/config
```

---

## 2.4 환경 변수 설정

```bash
export KUBECONFIG=/home/ubuntu/.kube/config
```

---

## 2.5 서비스 상태 확인

```bash
systemctl status k3s
```

---

# 3. containerd 및 crictl 간단 개념

k3s는 Docker 대신 **containerd**를 사용한다.

이때 `crictl`은 다음과 같은 역할을 한다:

> Kubernetes 노드에서 containerd에 저장된 이미지 및 컨테이너 상태를 확인하기 위한 CLI 도구

👉 쉽게 말하면
**“Docker 대신 내부 컨테이너 상태를 확인하는 도구”**라고 이해하면 된다.

---

# 4. crictl 설정

## 4.1 설정 파일 생성

```bash
sudo tee /etc/crictl.yaml > /dev/null <<'EOF'
runtime-endpoint: unix:///run/k3s/containerd/containerd.sock
image-endpoint: unix:///run/k3s/containerd/containerd.sock
timeout: 10
debug: false
EOF
```

---

## 4.2 확인

```bash
cat /etc/crictl.yaml
```

---

# 5. 이미지 목록 확인

## 5.1 전체 이미지 조회

```bash
sudo crictl images
```

---

## 5.2 특정 서비스 이미지 조회

```bash
sudo crictl images | grep '^user'
```

---

# 6. 이미지 누적 문제

이미지는 다음과 같은 이유로 계속 쌓일 수 있다:

* CI/CD로 새로운 버전 계속 배포
* 이전 이미지 자동 삭제되지 않음

---

# 7. 이미지 정리

## 7.1 사용하지 않는 이미지 제거

```bash
sudo crictl rmi --prune
```

---

# 8. 자동 정리 (추천)

## 8.1 스크립트 작성

```bash
#!/bin/bash
/usr/bin/crictl rmi --prune || true
```

---

## 8.2 cron 등록

```bash
crontab -e
```

```cron
0 3 * * * /home/ubuntu/scripts/prune-images.sh
```

---

# 9. 디스크 관리

## 기본 경로

```text
/var/lib/rancher/k3s
```

이미지 및 컨테이너 데이터 저장 위치

---

# 10. 한 줄 정리

**k3s에서는 containerd + crictl을 사용하여 이미지 상태를 확인하고, 주기적으로 정리해야 한다**
