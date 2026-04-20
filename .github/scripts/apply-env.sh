#!/bin/bash
set -euo pipefail

SERVICE="${1:-}"
ENV_DIR=/home/ubuntu/apps/deploy/env

if [ -z "$SERVICE" ]; then
  echo "Usage: apply-env.sh <service-name>"
  exit 1
fi

mkdir -p "$ENV_DIR"
chmod 700 "$ENV_DIR"

CONFIG_CHANGED=false

write_if_changed() {
  local target_file="$1"
  local content="${2:-}"

  local temp_file
  temp_file=$(mktemp)

  printf -- "%s" "$content" > "$temp_file"

  if [ ! -f "$target_file" ]; then
    cp "$temp_file" "$target_file"
    echo "Created: $target_file"
    CONFIG_CHANGED=true
  elif ! cmp -s "$target_file" "$temp_file"; then
    cp "$temp_file" "$target_file"
    echo "Updated: $target_file"
    CONFIG_CHANGED=true
  else
    echo "Unchanged: $target_file"
  fi

  rm -f "$temp_file"
}

write_if_changed "$ENV_DIR/common_config.env" "${COMMON_CONFIG_ENV:-}"
write_if_changed "$ENV_DIR/common_secret.env" "${COMMON_SECRET_ENV:-}"
write_if_changed "$ENV_DIR/${SERVICE}_config.env" "${SERVICE_CONFIG_ENV:-}"
write_if_changed "$ENV_DIR/${SERVICE}_secret.env" "${SERVICE_SECRET_ENV:-}"

# secret 파일 권한 제한
[ -f "$ENV_DIR/common_secret.env" ] && chmod 600 "$ENV_DIR/common_secret.env"
[ -f "$ENV_DIR/${SERVICE}_secret.env" ] && chmod 600 "$ENV_DIR/${SERVICE}_secret.env"

# config 파일도 필요하면 조금 제한
[ -f "$ENV_DIR/common_config.env" ] && chmod 600 "$ENV_DIR/common_config.env"
[ -f "$ENV_DIR/${SERVICE}_config.env" ] && chmod 600 "$ENV_DIR/${SERVICE}_config.env"

echo "CONFIG_CHANGED=$CONFIG_CHANGED"

if [ -n "${GITHUB_ENV:-}" ]; then
  echo "CONFIG_CHANGED=$CONFIG_CHANGED" >> "$GITHUB_ENV"
fi