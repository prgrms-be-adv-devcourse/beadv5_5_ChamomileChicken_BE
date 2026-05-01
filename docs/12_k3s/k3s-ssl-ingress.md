# K3s SSL / Ingress 가이드

## 1. 문서 목적

이 문서는 현재 K3s 환경에서 도메인, HTTPS, Ingress, cert-manager 인증서 발급이 어떻게 연결되는지 정리한 문서입니다.

대상 파일은 아래와 같습니다.

- `.github/k3s/cluster-issuer.yml`
- `.github/k3s/main-ingress.yml`

서비스별 Kubernetes manifest나 CD 스크립트와는 역할이 다르므로 별도 문서로 분리합니다.

## 2. 전체 구조

현재 외부 HTTPS 진입 구조는 아래 흐름입니다.

```text
사용자 브라우저
-> https://jabaclass.store
-> Traefik Ingress
-> main-ingress
-> api-gateway-service 또는 frontend-service
```

인증서 발급은 아래 흐름입니다.

```text
main-ingress.yml
-> cert-manager annotation
-> ClusterIssuer letsencrypt-prod
-> Let's Encrypt HTTP-01 challenge
-> TLS Secret main-tls 생성
```

## 3. cluster-issuer.yml

파일: `.github/k3s/cluster-issuer.yml`

이 파일은 cert-manager가 Let's Encrypt에서 인증서를 발급받을 때 사용할 발급자 설정입니다.

핵심 설정은 아래와 같습니다.

- Kind: `ClusterIssuer`
- 이름: `letsencrypt-prod`
- ACME 서버: `https://acme-v02.api.letsencrypt.org/directory`
- challenge 방식: `http01`
- Ingress class: `traefik`
- private key secret: `letsencrypt-prod`

즉, 클러스터 전체에서 사용할 수 있는 Let's Encrypt 발급자를 하나 정의한 상태입니다.

주의할 점:

- `spec.acme.email`은 실제 운영자가 받을 수 있는 이메일이어야 합니다.
- cert-manager가 클러스터에 설치되어 있어야 합니다.
- HTTP-01 challenge를 위해 외부에서 `jabaclass.store`의 HTTP 경로로 접근 가능해야 합니다.

## 4. main-ingress.yml

파일: `.github/k3s/main-ingress.yml`

이 파일은 외부 도메인 요청을 어떤 내부 서비스로 보낼지 정의합니다.

핵심 설정은 아래와 같습니다.

- Ingress 이름: `main-ingress`
- Ingress class: `traefik`
- 도메인: `jabaclass.store`
- TLS secret: `main-tls`
- ClusterIssuer annotation: `letsencrypt-prod`

관련 annotation:

```yaml
cert-manager.io/cluster-issuer: letsencrypt-prod
traefik.ingress.kubernetes.io/redirect-entry-point: https
```

`cert-manager.io/cluster-issuer`는 이 Ingress의 TLS 인증서를 어떤 ClusterIssuer로 발급받을지 지정합니다.

## 5. TLS Secret

`main-ingress.yml`에는 아래 설정이 있습니다.

```yaml
tls:
  - hosts:
      - jabaclass.store
    secretName: main-tls
```

이 설정 때문에 cert-manager는 `jabaclass.store` 인증서를 발급하고, 결과 인증서를 `main-tls` Secret으로 저장합니다.

Ingress는 이후 HTTPS 요청 처리 시 이 Secret을 사용합니다.

## 6. 현재 라우팅

현재 `main-ingress.yml` 기준 라우팅은 아래와 같습니다.

API Gateway로 연결되는 경로:

- `/api`
- `/swagger-ui`
- `/v3/api-docs`
- `/docs`
- `/oauth2`
- `/login/oauth2`

연결 대상:

- `api-gateway-service:8080`

Frontend로 연결되는 경로:

- `/oauth2/callback`
- `/`

연결 대상:

- `frontend-service:3000`

즉, API, Swagger, OAuth 인증 시작/처리 요청은 게이트웨이로 보내고, OAuth 완료 후 프론트엔드가 처리해야 하는 callback 경로와 기본 화면 요청은 프론트엔드로 보냅니다.

`/oauth2/callback`은 `/oauth2`보다 더 구체적인 경로이므로 `main-ingress.yml`에서 `/oauth2`보다 먼저 배치합니다.

## 7. 적용 순서

처음 SSL을 구성할 때는 보통 아래 순서가 안전합니다.

1. cert-manager 설치 여부 확인
2. DNS가 K3s 노드 또는 로드밸런서로 향하는지 확인
3. `cluster-issuer.yml` 적용
4. `main-ingress.yml` 적용
5. Certificate, Challenge, Secret 상태 확인

예시 명령:

```bash
kubectl apply -f .github/k3s/cluster-issuer.yml
kubectl apply -f .github/k3s/main-ingress.yml
```

확인 명령:

```bash
kubectl get clusterissuer
kubectl get ingress main-ingress
kubectl get certificate
kubectl get secret main-tls
kubectl describe ingress main-ingress
```

cert-manager 리소스까지 확인해야 할 때:

```bash
kubectl get certificate,certificaterequest,order,challenge
```

## 8. 자주 보는 문제

### 인증서가 발급되지 않는 경우

먼저 아래 항목을 확인합니다.

- `jabaclass.store` DNS가 실제 K3s 진입점으로 연결되어 있는지
- Traefik Ingress가 외부 HTTP 요청을 받을 수 있는지
- `cluster-issuer.yml`의 email 값이 유효한지
- cert-manager Pod가 정상인지
- `kubectl describe challenge`에 실패 사유가 있는지

### HTTPS 접속은 되지만 라우팅이 이상한 경우

먼저 아래 항목을 확인합니다.

- `main-ingress.yml`의 path 순서
- `/oauth2/callback`이 `/oauth2`보다 먼저 선언되어 있는지
- `/` 경로가 마지막에 있는지
- 각 backend service 이름과 port가 실제 Service와 맞는지
- `api-gateway-service`, `frontend-service` Pod 상태

### Secret이 없는 경우

`main-tls` Secret은 직접 만드는 값이 아니라 cert-manager가 인증서 발급 후 생성하는 값입니다.

없다면 아래 순서로 확인합니다.

```bash
kubectl describe clusterissuer letsencrypt-prod
kubectl describe ingress main-ingress
kubectl get certificate,order,challenge
```

## 9. 관련 문서

- `k3s-yml.md`
- `k3s-service-deployment-structure.md`
- `k3s-cicd-final-guide.md`

## 10. 요약

현재 SSL 연결은 `cluster-issuer.yml`과 `main-ingress.yml`이 함께 동작하는 구조입니다.

- `cluster-issuer.yml`: Let's Encrypt 인증서를 발급받는 방법 정의
- `main-ingress.yml`: `jabaclass.store` 도메인, TLS Secret, 외부 라우팅 정의
- `main-tls`: cert-manager가 발급 결과로 생성하는 TLS Secret

따라서 HTTPS 문제가 생기면 Service manifest보다 Ingress, ClusterIssuer, Certificate, Challenge 상태를 먼저 확인하는 편이 좋습니다.
