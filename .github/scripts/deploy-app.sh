#!/bin/bash
set -euo pipefail

SERVICE="${1:-${SERVICE:-}}"
ENV_DIR=/home/ubuntu/apps/deploy/env
K3S_DIR=/home/ubuntu/apps/data/k3s-service
DOCKERHUB_USERNAME="${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required}"
IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG is required}"
KUBECONFIG_PATH="/home/ubuntu/.kube/config"
RESTART_ON_CONFIG_CHANGE="${CONFIG_CHANGED:-false}"

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

create_configmap_if_exists() {
  local name=$1
  local file=$2

  if [ -f "$file" ]; then
    echo "Applying ConfigMap: $name from $file"
    kubectl --kubeconfig "$KUBECONFIG_PATH" create configmap "$name" \
      --from-env-file="$file" \
      --dry-run=client -o yaml | kubectl --kubeconfig "$KUBECONFIG_PATH" apply -f -
  else
    echo "ConfigMap file not found, skipping: $file"
  fi
}

create_secret_if_exists() {
  local name=$1
  local file=$2

  if [ -f "$file" ]; then
    echo "Applying Secret: $name from $file"
    kubectl --kubeconfig "$KUBECONFIG_PATH" create secret generic "$name" \
      --from-env-file="$file" \
      --dry-run=client -o yaml | kubectl --kubeconfig "$KUBECONFIG_PATH" apply -f -
  else
    echo "Secret file not found, skipping: $file"
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

echo "Checking service: ${SERVICE}-service"
kubectl --kubeconfig "$KUBECONFIG_PATH" get svc "${SERVICE}-service"

if [ "$RESTART_ON_CONFIG_CHANGE" = "true" ]; then
  echo "Config changed. Restarting deployment: ${SERVICE}-service"
  kubectl --kubeconfig "$KUBECONFIG_PATH" rollout restart deployment/"${SERVICE}-service"
else
  echo "No config change detected. Skipping restart."
fi

echo "Waiting for rollout status..."
kubectl --kubeconfig "$KUBECONFIG_PATH" rollout status deployment/"$SERVICE-service" --timeout=300s

echo "Deployment completed: $SERVICE"
