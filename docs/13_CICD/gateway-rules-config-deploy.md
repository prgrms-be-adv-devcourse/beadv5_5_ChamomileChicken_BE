# Gateway Rules Config Deploy Workflow

## 개요

Gateway Rules ConfigMap을 Kubernetes 클러스터에 배포하기 위한 GitHub Actions Workflow입니다.

`gateway-rules-config.yml` 파일이 수정되면 자동으로 ConfigMap을 적용하고,  
`api-gateway-service` Deployment를 재시작합니다.

---

# 실행 조건

## 자동 실행

다음 조건에서 Workflow가 자동 실행됩니다.

- `dev` 브랜치 Push
- 아래 파일 변경 시

- `.github/k3s/gateway-rules-config.yml`
- `.github/workflows/deploy-gateway-rules-config.yml`

---

## 수동 실행

GitHub Actions 화면에서 직접 실행할 수 있습니다.

```text
GitHub Actions
→ Deploy Gateway Rules Config
→ Run workflow
