#!/bin/bash
set -euo pipefail

SERVICE="${1:-${SERVICE:-}}"
K3S_DIR="${K3S_DIR:-/home/ubuntu/apps/data/k3s-service}"
DOCKERHUB_USERNAME="${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required}"
IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG is required}"
KUBECONFIG_PATH="${KUBECONFIG_PATH:-/home/ubuntu/.kube/config}"
ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-300s}"

if [ -z "$SERVICE" ]; then
  echo "Usage: deploy-app.sh <service-name>"
  exit 1
fi

if [ ! -f "$KUBECONFIG_PATH" ]; then
  echo "Kubeconfig not found: $KUBECONFIG_PATH"
  exit 1
fi

YAML_FILE="$K3S_DIR/${SERVICE}-service.yml"
DEPLOYMENT_NAME="${SERVICE}-service"
APP_LABEL="${SERVICE}-service"

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

echo "Deploying service: $SERVICE"
echo "Using kubeconfig: $KUBECONFIG_PATH"
echo "Using image tag: $IMAGE_TAG"

if [ ! -f "$YAML_FILE" ]; then
  echo "YAML file not found: $YAML_FILE"
  exit 1
fi

echo "Applying Kubernetes YAML: $YAML_FILE"
export DOCKERHUB_USERNAME IMAGE_TAG
envsubst '$DOCKERHUB_USERNAME $IMAGE_TAG' < "$YAML_FILE" | \
  kubectl --kubeconfig "$KUBECONFIG_PATH" apply -f -

echo "Waiting for rollout status..."
if ! kubectl --kubeconfig "$KUBECONFIG_PATH" rollout status deployment/"$DEPLOYMENT_NAME" --timeout="$ROLLOUT_TIMEOUT"; then
  print_rollout_diagnostics "$DEPLOYMENT_NAME" "$APP_LABEL"
  exit 1
fi

echo "Deployment completed: $SERVICE"
