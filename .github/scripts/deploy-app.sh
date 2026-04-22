#!/bin/bash
set -euo pipefail

SERVICE="${1:-${SERVICE:-}}"
ENV_DIR="${ENV_DIR:-/home/ubuntu/apps/deploy/env}"
K3S_DIR="${K3S_DIR:-/home/ubuntu/apps/data/k3s-service}"
DOCKERHUB_USERNAME="${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required}"
IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG is required}"
KUBECONFIG_PATH="${KUBECONFIG_PATH:-/home/ubuntu/.kube/config}"

IMAGE_CHANGED="${IMAGE_CHANGED:-false}"
MANIFEST_CHANGED="${MANIFEST_CHANGED:-false}"
CONFIG_CHANGED="${CONFIG_CHANGED:-false}"

COMMON_CONFIG_CHANGED="${COMMON_CONFIG_CHANGED:-false}"
COMMON_SECRET_CHANGED="${COMMON_SECRET_CHANGED:-false}"
SERVICE_CONFIG_CHANGED="${SERVICE_CONFIG_CHANGED:-false}"
SERVICE_SECRET_CHANGED="${SERVICE_SECRET_CHANGED:-false}"

ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-180s}"

if [ -z "$SERVICE" ]; then
  echo "Usage: deploy-apps.sh <service-name>"
  exit 1
fi

if [ ! -f "$KUBECONFIG_PATH" ]; then
  echo "Kubeconfig not found: $KUBECONFIG_PATH"
  exit 1
fi

SERVICE_SECRET_FILE="$ENV_DIR/${SERVICE}_secret.env"
COMMON_SECRET_FILE="$ENV_DIR/common_secret.env"
SERVICE_CONFIG_FILE="$ENV_DIR/${SERVICE}_config.env"
COMMON_CONFIG_FILE="$ENV_DIR/common_config.env"
YAML_FILE="$K3S_DIR/${SERVICE}-service.yml"
DEPLOYMENT_NAME="${SERVICE}-service"
APP_LABEL="${SERVICE}-service"

cleanup() {
  rm -f "$SERVICE_SECRET_FILE"
}
trap cleanup EXIT

print_rollout_diagnostics() {
  local deployment_name="$1"
  local app_label="$2"

  echo "Rollout failed. Collecting Kubernetes diagnostics..."
  echo "===== deployment ====="
  kubectl --kubeconfig "$KUBECONFIG_PATH" get deployment "$deployment_name" -o wide || true

  echo "===== describe deployment ====="
  kubectl --kubeconfig "$KUBECONFIG_PATH" describe deployment "$deployment_name" || true

  echo "===== replicasets ====="
  kubectl --kubeconfig "$KUBECONFIG_PATH" get rs -l app="$app_label" -o wide || true

  echo "===== pods ====="
  kubectl --kubeconfig "$KUBECONFIG_PATH" get pods -l app="$app_label" -o wide || true

  local pod_names
  pod_names=$(kubectl --kubeconfig "$KUBECONFIG_PATH" get pods -l app="$app_label" \
    -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' 2>/dev/null || true)

  for pod in $pod_names; do
    echo "===== describe pod: $pod ====="
    kubectl --kubeconfig "$KUBECONFIG_PATH" describe pod "$pod" || true

    echo "===== logs: $pod ====="
    kubectl --kubeconfig "$KUBECONFIG_PATH" logs "$pod" --tail=200 || true

    echo "===== previous logs: $pod ====="
    kubectl --kubeconfig "$KUBECONFIG_PATH" logs "$pod" --previous --tail=200 || true
  done
}

create_configmap_if_exists() {
  local name="$1"
  local file="$2"

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
  local name="$1"
  local file="$2"

  if [ -f "$file" ]; then
    echo "Applying Secret: $name from $file"
    kubectl --kubeconfig "$KUBECONFIG_PATH" create secret generic "$name" \
      --from-env-file="$file" \
      --dry-run=client -o yaml | kubectl --kubeconfig "$KUBECONFIG_PATH" apply -f -
  else
    echo "Secret file not found, skipping: $file"
  fi
}

echo "Deploying service: $SERVICE"
echo "Using kubeconfig: $KUBECONFIG_PATH"
echo "Using image tag: $IMAGE_TAG"
echo "IMAGE_CHANGED=$IMAGE_CHANGED, MANIFEST_CHANGED=$MANIFEST_CHANGED, CONFIG_CHANGED=$CONFIG_CHANGED"
echo "COMMON_CONFIG_CHANGED=$COMMON_CONFIG_CHANGED, COMMON_SECRET_CHANGED=$COMMON_SECRET_CHANGED, SERVICE_CONFIG_CHANGED=$SERVICE_CONFIG_CHANGED, SERVICE_SECRET_CHANGED=$SERVICE_SECRET_CHANGED"

if [ ! -f "$YAML_FILE" ]; then
  echo "YAML file not found: $YAML_FILE"
  exit 1
fi

# 1) ConfigMap/Secret은 변경됐을 때만 apply
if [ "$COMMON_CONFIG_CHANGED" = "true" ]; then
  create_configmap_if_exists "common-config" "$COMMON_CONFIG_FILE"
fi

if [ "$COMMON_SECRET_CHANGED" = "true" ]; then
  create_secret_if_exists "common-secret" "$COMMON_SECRET_FILE"
fi

if [ "$SERVICE_CONFIG_CHANGED" = "true" ]; then
  create_configmap_if_exists "${SERVICE}-config" "$SERVICE_CONFIG_FILE"
fi

if [ "$SERVICE_SECRET_CHANGED" = "true" ]; then
  create_secret_if_exists "${SERVICE}-secret" "$SERVICE_SECRET_FILE"
fi

# 2) manifest는 항상 apply
echo "Applying Kubernetes YAML: $YAML_FILE"
export DOCKERHUB_USERNAME IMAGE_TAG
envsubst '$DOCKERHUB_USERNAME $IMAGE_TAG' < "$YAML_FILE" | \
  kubectl --kubeconfig "$KUBECONFIG_PATH" apply -f -

# 3) config-only일 때만 restart
if [ "$CONFIG_CHANGED" = "true" ] && [ "$IMAGE_CHANGED" != "true" ] && [ "$MANIFEST_CHANGED" != "true" ]; then
  echo "Config only changed. Restarting deployment: $DEPLOYMENT_NAME"
  kubectl --kubeconfig "$KUBECONFIG_PATH" rollout restart deployment/"$DEPLOYMENT_NAME"
else
  echo "No config-only restart required."
fi

# 4) rollout status는 항상 확인
echo "Waiting for rollout status..."
if ! kubectl --kubeconfig "$KUBECONFIG_PATH" rollout status deployment/"$DEPLOYMENT_NAME" --timeout="$ROLLOUT_TIMEOUT"; then
  print_rollout_diagnostics "$DEPLOYMENT_NAME" "$APP_LABEL"
  exit 1
fi

echo "Deployment completed: $SERVICE"