#!/bin/bash
set -euo pipefail

SERVICE="${1:-}"
ENV_DIR=/home/ubuntu/apps/deploy/env
K3S_TEST_DIR=/home/ubuntu/apps/data/k3s-test

if [ -z "$SERVICE" ]; then
  echo "Usage: deploy_test_app.sh <service-name>"
  exit 1
fi

echo "Deploying test service: $SERVICE"

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

create_configmap_if_exists "common-config" "$ENV_DIR/common_config.env"
create_secret_if_exists "common-secret" "$ENV_DIR/common_secret.env"

create_configmap_if_exists "${SERVICE}-config" "$ENV_DIR/${SERVICE}_config.env"
create_secret_if_exists "${SERVICE}-secret" "$ENV_DIR/${SERVICE}_secret.env"

TEST_YAML_FILE="$K3S_TEST_DIR/${SERVICE}-service-test.yml"

if [ -f "$TEST_YAML_FILE" ]; then
  echo "Applying Kubernetes TEST YAML: $TEST_YAML_FILE"
  envsubst < "$TEST_YAML_FILE" | kubectl apply -f -
else
  echo "Test YAML file not found: $TEST_YAML_FILE"
  exit 1
fi

echo "Restarting deployment: ${SERVICE}-service"
kubectl rollout restart deployment/"${SERVICE}-service"

echo "Waiting for rollout status..."
kubectl rollout status deployment/"${SERVICE}-service" --timeout=120s

echo "Test deployment completed: $SERVICE"