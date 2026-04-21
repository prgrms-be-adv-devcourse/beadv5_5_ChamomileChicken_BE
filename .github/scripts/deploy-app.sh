#!/bin/bash
set -euo pipefail

SERVICE="${1:-${SERVICE:-}}"
ENV_DIR="${ENV_DIR:-/home/ubuntu/apps/deploy/env}"
K3S_DIR="${K3S_DIR:-/home/ubuntu/apps/data/k3s-service}"
DOCKERHUB_USERNAME="${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required}"
IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG is required}"
KUBECONFIG_PATH="${KUBECONFIG_PATH:-/home/ubuntu/.kube/config}"

CONFIG_CHANGED="${CONFIG_CHANGED:-false}"
IMAGE_CHANGED="${IMAGE_CHANGED:-true}"
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

cleanup() {
  rm -f "$SERVICE_SECRET_FILE"
}
trap cleanup EXIT

print_rollout_diagnostics() {
  local deployment_name="$1"
  local service_name="$2"

  echo "Rollout failed. Collecting Kubernetes diagnostics..."
  echo "===== deployment ====="
  kubectl --kubeconfig "$KUBECONFIG_PATH" get deployment "$deployment_name" -o wide || true

  echo "===== describe deployment ====="
  kubectl --kubeconfig "$KUBECONFIG_PATH" describe deployment "$deployment_name" || true

  echo "===== pods ====="
  kubectl --kubeconfig "$KUBECONFIG_PATH" get pods -l app="$service_name" -o wide || true

  local pod_names
  pod_names=$(kubectl --kubeconfig "$KUBECONFIG_PATH" get pods -l app="$service_name" \
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
echo "IMAGE_CHANGED=$IMAGE_CHANGED, CONFIG_CHANGED=$CONFIG_CHANGED"

create_configmap_if_exists "common-config" "$COMMON_CONFIG_FILE"
create_secret_if_exists "common-secret" "$COMMON_SECRET_FILE"
create_configmap_if_exists "${SERVICE}-config" "$SERVICE_CONFIG_FILE"
create_secret_if_exists "${SERVICE}-secret" "$SERVICE_SECRET_FILE"

YAML_FILE="$K3S_DIR/${SERVICE}-service.yml"
if [ ! -f "$YAML_FILE" ]; then
  echo "YAML file not found: $YAML_FILE"
  exit 1
fi

DEPLOYMENT_NAME="${SERVICE}-service"
SERVICE_NAME="${SERVICE}-service"

if [ "$IMAGE_CHANGED" = "true" ]; then
  echo "Image changed. Applying Kubernetes YAML: $YAML_FILE"
  export DOCKERHUB_USERNAME IMAGE_TAG
  envsubst '$DOCKERHUB_USERNAME $IMAGE_TAG' < "$YAML_FILE" | \
    kubectl --kubeconfig "$KUBECONFIG_PATH" apply -f -

elif [ "$CONFIG_CHANGED" = "true" ]; then
  echo "Only config changed. Restarting deployment: $DEPLOYMENT_NAME"
  kubectl --kubeconfig "$KUBECONFIG_PATH" rollout restart deployment/"$DEPLOYMENT_NAME"

else
  echo "No image/config change requiring rollout."
fi

echo "Checking deployment: $DEPLOYMENT_NAME"
kubectl --kubeconfig "$KUBECONFIG_PATH" get deployment "$DEPLOYMENT_NAME"

echo "Checking service: $SERVICE_NAME"
kubectl --kubeconfig "$KUBECONFIG_PATH" get svc "$SERVICE_NAME"

if [ "$IMAGE_CHANGED" = "true" ] || [ "$CONFIG_CHANGED" = "true" ]; then
  echo "Waiting for rollout status..."
  if ! kubectl --kubeconfig "$KUBECONFIG_PATH" rollout status deployment/"$DEPLOYMENT_NAME" --timeout="$ROLLOUT_TIMEOUT"; then
    print_rollout_diagnostics "$DEPLOYMENT_NAME" "$SERVICE_NAME"
    exit 1
  fi
else
  echo "Skipping rollout status check because no rollout was triggered."
fi

echo "Deployment completed: $SERVICE"