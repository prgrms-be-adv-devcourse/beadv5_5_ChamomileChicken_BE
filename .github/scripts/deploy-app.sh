#!/bin/bash
set -euo pipefail

SERVICE="${1:-}"
ENV_DIR=/home/ubuntu/apps/deploy/env
K3S_DIR=/home/ubuntu/apps/data/k3s-service
DOCKERHUB_USERNAME="${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required}"
IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG is required}"

if [ -z "$SERVICE" ]; then
  echo "Usage: deploy-apps.sh <service-name>"
  exit 1
fi

echo "Deploying service: $SERVICE"

create_configmap_if_exists() {
  local name=$1
  local file=$2

  if [ -f "$file" ]; then
    echo "Applying ConfigMap: $name from $file"
    kubectl create configmap "$name" \
      --from-env-file="$file" \
      --dry-run=client -o yaml | kubectl apply -f -
  else
    echo "ConfigMap file not found, skipping: $file"
  fi
}

create_secret_if_exists() {
  local name=$1
  local file=$2

  if [ -f "$file" ]; then
    echo "Applying Secret: $name from $file"
    kubectl create secret generic "$name" \
      --from-env-file="$file" \
      --dry-run=client -o yaml | kubectl apply -f -
  else
    echo "Secret file not found, skipping: $file"
  fi
}

# 1. 공통 env 적용
create_configmap_if_exists "common-config" "$ENV_DIR/common_config.env"
create_secret_if_exists "common-secret" "$ENV_DIR/common_secret.env"

# 2. 서비스별 env 적용
create_configmap_if_exists "${SERVICE}-config" "$ENV_DIR/${SERVICE}_config.env"
create_secret_if_exists "${SERVICE}-secret" "$ENV_DIR/${SERVICE}_secret.env"

# 3. deployment yaml 적용
if [ -f "$K3S_DIR/${SERVICE}-service.yml" ]; then
  echo "Applying Kubernetes YAML: $K3S_DIR/${SERVICE}-service.yml"
  envsubst "$DOCKERHUB_USERNAME $IMAGE_TAG" < "$K3S_DIR/${SERVICE}-service.yml" | kubectl apply -f -
else
  echo "YAML file not found: $K3S_DIR/${SERVICE}-service.yml"
  exit 1
fi

# 4. deployment 재시작
echo "Restarting deployment: $SERVICE-service"
kubectl rollout restart deployment/"$SERVICE-service"

# 5. rollout 상태 확인
echo "Waiting for rollout status..."
kubectl rollout status deployment/"$SERVICE-service" --timeout=120s

echo "Deployment completed: $SERVICE"
