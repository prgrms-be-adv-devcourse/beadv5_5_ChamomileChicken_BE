#!/bin/bash
set -euo pipefail

SERVICE="${1:-${SERVICE:-}}"
ENV_DIR=/home/ubuntu/apps/deploy/env
K3S_DIR=/home/ubuntu/apps/data/k3s-service
DOCKERHUB_USERNAME="${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required}"
IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG is required}"
KUBECONFIG_PATH="/home/ubuntu/.kube/config"

if [ -z "$SERVICE" ]; then
  echo "Usage: deploy-apps.sh <service-name>"
  exit 1
fi

if [ ! -f "$KUBECONFIG_PATH" ]; then
  echo "Kubeconfig not found: $KUBECONFIG_PATH"
  exit 1
fi

echo "Deploying service: $SERVICE"
echo "Using kubeconfig: $KUBECONFIG_PATH"

validate_env_file() {
  local file=$1

  if [ ! -f "$file" ]; then
    return 1
  fi

  if [ ! -s "$file" ]; then
    echo "Env file is empty: $file"
    return 1
  fi

  return 0
}

create_configmap_if_exists() {
  local name=$1
  local file=$2

  if validate_env_file "$file"; then
    echo "Applying ConfigMap: $name from $file"
    kubectl --kubeconfig "$KUBECONFIG_PATH" create configmap "$name" \
      --from-env-file="$file" \
      --dry-run=client -o yaml | kubectl --kubeconfig "$KUBECONFIG_PATH" apply -f -
  else
    echo "ConfigMap file missing or empty, skipping: $file"
  fi
}

create_secret_if_exists() {
  local name=$1
  local file=$2

  if validate_env_file "$file"; then
    echo "Applying Secret: $name from $file"
    kubectl --kubeconfig "$KUBECONFIG_PATH" create secret generic "$name" \
      --from-env-file="$file" \
      --dry-run=client -o yaml | kubectl --kubeconfig "$KUBECONFIG_PATH" apply -f -
  else
    echo "Secret file missing or empty, skipping: $file"
  fi
}

create_configmap_if_exists "common-config" "$ENV_DIR/common_config.env"
create_secret_if_exists "common-secret" "$ENV_DIR/common_secret.env"

create_configmap_if_exists "${SERVICE}-config" "$ENV_DIR/${SERVICE}_config.env"
create_secret_if_exists "${SERVICE}-secret" "$ENV_DIR/${SERVICE}_secret.env"

if [ -f "$K3S_DIR/${SERVICE}-service.yml" ]; then
  echo "Applying Kubernetes YAML: $K3S_DIR/${SERVICE}-service.yml"
  export DOCKERHUB_USERNAME IMAGE_TAG
  envsubst '$DOCKERHUB_USERNAME $IMAGE_TAG' < "$K3S_DIR/${SERVICE}-service.yml" | \
    kubectl --kubeconfig "$KUBECONFIG_PATH" apply -f -
else
  echo "YAML file not found: $K3S_DIR/${SERVICE}-service.yml"
  exit 1
fi

echo "Checking deployment: ${SERVICE}-service"
kubectl --kubeconfig "$KUBECONFIG_PATH" get deployment "${SERVICE}-service"

echo "Checking service: ${SERVICE}-service"
kubectl --kubeconfig "$KUBECONFIG_PATH" get svc "${SERVICE}-service"

echo "Waiting for rollout status..."
kubectl --kubeconfig "$KUBECONFIG_PATH" rollout status deployment/"$SERVICE-service" --timeout=300s

echo "Deployment completed: $SERVICE"