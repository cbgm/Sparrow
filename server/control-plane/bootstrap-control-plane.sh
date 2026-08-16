#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/sparrow.conf"
RUNTIME_ENV="$SCRIPT_DIR/.env.runtime"
SECRETS_DIR="$SCRIPT_DIR/secrets"
BASE_COMPOSE="$SCRIPT_DIR/docker-compose.yml"
RELEASE_COMPOSE="$SCRIPT_DIR/docker-compose.release.yml"
PRODUCTION_COMPOSE="$SCRIPT_DIR/docker-compose.production.yml"
CONTROL_PLANE_HTTP_PORT="${CONTROL_PLANE_HTTP_PORT:-8390}"
PREPARE_ONLY="${1:-}"

for required in "$CONFIG_FILE" "$BASE_COMPOSE" "$RELEASE_COMPOSE" "$PRODUCTION_COMPOSE"; do
  if [[ ! -f "$required" ]]; then
    echo "The deployment bundle is incomplete: $(basename "$required") is missing." >&2
    exit 1
  fi
done

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed or is not available to the current user." >&2
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "Docker is installed but the Docker daemon is not running. Start Docker Desktop/Engine and retry." >&2
  exit 1
fi

COMPOSE_VERSION="$(docker compose version --short 2>/dev/null || true)"
COMPOSE_VERSION_NUMBER="$(printf '%s' "$COMPOSE_VERSION" | grep -Eo '[0-9]+\.[0-9]+\.[0-9]+' | head -n 1)"
if [[ -z "$COMPOSE_VERSION_NUMBER" ]]; then
  echo "Docker Compose 2.24.4 or newer is required." >&2
  exit 1
fi
IFS='.' read -r COMPOSE_MAJOR COMPOSE_MINOR COMPOSE_PATCH <<< "$COMPOSE_VERSION_NUMBER"
if (( COMPOSE_MAJOR < 2 )) || \
   (( COMPOSE_MAJOR == 2 && COMPOSE_MINOR < 24 )) || \
   (( COMPOSE_MAJOR == 2 && COMPOSE_MINOR == 24 && COMPOSE_PATCH < 4 )); then
  echo "Docker Compose 2.24.4 or newer is required." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$CONFIG_FILE"
set +a

update_config_value() {
  local key="$1"
  local value="$2"
  local tmp="${CONFIG_FILE}.tmp"
  if grep -qE "^${key}=" "$CONFIG_FILE"; then
    awk -v key="$key" -v value="$value" '
      BEGIN { updated = 0 }
      index($0, key "=") == 1 && updated == 0 { print key "=" value; updated = 1; next }
      { print }
      END { if (updated == 0) print key "=" value }
    ' "$CONFIG_FILE" > "$tmp"
  else
    cat "$CONFIG_FILE" > "$tmp"
    printf '%s=%s\n' "$key" "$value" >> "$tmp"
  fi
  mv "$tmp" "$CONFIG_FILE"
}

new_control_plane_id() {
  if command -v uuidgen >/dev/null 2>&1; then
    uuidgen | tr -d '-' | tr '[:upper:]' '[:lower:]'
  elif [[ -r /proc/sys/kernel/random/uuid ]]; then
    tr -d '-' < /proc/sys/kernel/random/uuid
  elif command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 16
  else
    date +%s%N | sha256sum | cut -c1-32
  fi
}

MODE="${MODE:-}"
PUBLIC_DOMAIN="${PUBLIC_DOMAIN:-}"
CONTROL_PLANE_ID="${CONTROL_PLANE_ID:-}"
: "${SPARROW_IMAGE_PREFIX:?sparrow.conf is missing SPARROW_IMAGE_PREFIX}"
: "${SPARROW_IMAGE_TAG:?sparrow.conf is missing SPARROW_IMAGE_TAG}"

if [[ -z "$MODE" ]]; then
  if [[ -t 0 ]]; then
    printf 'Control Plane mode [lan/public] (lan): '
    read -r MODE
  fi
  MODE="${MODE:-lan}"
fi
MODE="$(printf '%s' "$MODE" | tr '[:upper:]' '[:lower:]')"
if [[ "$MODE" != "lan" && "$MODE" != "public" ]]; then
  echo "sparrow.conf MODE must be lan or public." >&2
  exit 1
fi

if [[ -z "$CONTROL_PLANE_ID" ]]; then
  CONTROL_PLANE_ID="$(new_control_plane_id)"
fi

public_ipv4() {
  local value=""
  local url
  for url in https://api.ipify.org https://checkip.amazonaws.com; do
    if command -v curl >/dev/null 2>&1; then
      value="$(curl --fail --silent --show-error --max-time 5 "$url" 2>/dev/null || true)"
    elif command -v wget >/dev/null 2>&1; then
      value="$(wget --quiet --timeout=5 --output-document=- "$url" 2>/dev/null || true)"
    fi
    value="$(printf '%s' "$value" | tr -d '[:space:]')"
    if [[ "$value" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; then
      printf '%s' "$value"
      return 0
    fi
  done
  return 1
}

primary_ipv4() {
  if command -v ip >/dev/null 2>&1; then
    ip route get 1.1.1.1 2>/dev/null | awk '{for (i=1; i<=NF; i++) if ($i == "src") {print $(i+1); exit}}'
    return
  fi
  if command -v route >/dev/null 2>&1 && command -v ipconfig >/dev/null 2>&1; then
    local interface_name
    interface_name="$(route -n get default 2>/dev/null | awk '/interface:/ {print $2; exit}')"
    if [[ -n "$interface_name" ]]; then
      ipconfig getifaddr "$interface_name" 2>/dev/null || true
      return
    fi
  fi
  hostname -I 2>/dev/null | awk '{print $1}'
}

if [[ "$MODE" == "public" && -z "$PUBLIC_DOMAIN" ]]; then
  PUBLIC_IP="$(public_ipv4 || true)"
  if [[ -z "$PUBLIC_IP" ]]; then
    echo "Could not detect the public IPv4 address. Set PUBLIC_DOMAIN in sparrow.conf." >&2
    exit 1
  fi
  PUBLIC_DOMAIN="${PUBLIC_IP//./-}.sslip.io"
fi

update_config_value CONFIGURED true
update_config_value MODE "$MODE"
update_config_value PUBLIC_DOMAIN "$PUBLIC_DOMAIN"
update_config_value CONTROL_PLANE_ID "$CONTROL_PLANE_ID"

mkdir -p "$SECRETS_DIR"

ensure_secret() {
  local path="$1"
  if [[ -d "$path" ]]; then
    if [[ -z "$(find "$path" -mindepth 1 -maxdepth 1 -print -quit 2>/dev/null)" ]]; then
      rmdir "$path"
    else
      echo "Expected a secret file but found a non-empty directory: $path" >&2
      exit 1
    fi
  fi
  if [[ ! -f "$path" ]]; then
    head -c 48 /dev/urandom | base64 | tr -d '\n' > "$path"
    chmod 600 "$path" 2>/dev/null || true
  fi
}

ensure_file_path() {
  local path="$1"
  if [[ -d "$path" ]]; then
    if [[ -z "$(find "$path" -mindepth 1 -maxdepth 1 -print -quit 2>/dev/null)" ]]; then
      rmdir "$path"
    else
      echo "Expected a file but found a non-empty directory: $path" >&2
      exit 1
    fi
  fi
}

ensure_secret "$SECRETS_DIR/node-registry-database-password.txt"
ensure_secret "$SECRETS_DIR/presence-redis-password.txt"
ensure_secret "$SECRETS_DIR/push-database-password.txt"
ensure_secret "$SECRETS_DIR/push-internal-api-token.txt"

FIREBASE_CREDENTIALS="$SECRETS_DIR/firebase-admin.json"
if [[ ! -f "$FIREBASE_CREDENTIALS" ]]; then
  echo "firebase-admin.json is missing from the secrets folder." >&2
  exit 1
fi

REGISTRY_PASSWORD="$(tr -d '\r\n' < "$SECRETS_DIR/node-registry-database-password.txt")"
PRESENCE_PASSWORD="$(tr -d '\r\n' < "$SECRETS_DIR/presence-redis-password.txt")"
PUSH_PASSWORD="$(tr -d '\r\n' < "$SECRETS_DIR/push-database-password.txt")"
PUSH_TOKEN="$(tr -d '\r\n' < "$SECRETS_DIR/push-internal-api-token.txt")"

SITE_ADDRESS=":80"
if [[ "$MODE" == "public" ]]; then
  SITE_ADDRESS="$PUBLIC_DOMAIN"
fi

cat > "$RUNTIME_ENV" <<EOF_RUNTIME
CONTROL_PLANE_PROJECT_NAME=sparrow-control-plane
CONTROL_PLANE_ID=$CONTROL_PLANE_ID
CONTROL_PLANE_BIND_ADDRESS=0.0.0.0
CONTROL_PLANE_HTTP_PORT=$CONTROL_PLANE_HTTP_PORT
CONTROL_PLANE_SITE_ADDRESS=$SITE_ADDRESS
CONTROL_PLANE_DOMAIN=$PUBLIC_DOMAIN
FIREBASE_ADMIN_CREDENTIALS=./secrets/firebase-admin.json
NODE_REGISTRY_DATABASE_PASSWORD=$REGISTRY_PASSWORD
PRESENCE_REDIS_PASSWORD=$PRESENCE_PASSWORD
PUSH_DATABASE_PASSWORD=$PUSH_PASSWORD
PUSH_INTERNAL_API_TOKEN=$PUSH_TOKEN
NODE_REGISTRY_DATABASE_PASSWORD_FILE=./secrets/node-registry-database-password.txt
PRESENCE_REDIS_PASSWORD_FILE=./secrets/presence-redis-password.txt
PUSH_DATABASE_PASSWORD_FILE=./secrets/push-database-password.txt
PUSH_INTERNAL_API_TOKEN_FILE=./secrets/push-internal-api-token.txt
REGISTRY_AUTHORITY_IDENTITY_FILE=./secrets/registry-authority.identity
REGISTRY_AUTHORITY_CERTIFICATE_FILE=./secrets/registry-authority-certificate.json
SPARROW_IMAGE_PREFIX=$SPARROW_IMAGE_PREFIX
SPARROW_IMAGE_TAG=$SPARROW_IMAGE_TAG
EOF_RUNTIME

COMPOSE=(
  docker compose
  --env-file "$RUNTIME_ENV"
  -f "$BASE_COMPOSE"
  -f "$RELEASE_COMPOSE"
)
if [[ "$MODE" == "public" ]]; then
  COMPOSE+=( -f "$PRODUCTION_COMPOSE" )
fi

cd "$SCRIPT_DIR"
"${COMPOSE[@]}" config --quiet

if [[ "$PREPARE_ONLY" == "--prepare-only" ]]; then
  echo "Control Plane configuration prepared in $RUNTIME_ENV"
  exit 0
fi

"${COMPOSE[@]}" pull

ensure_registry_authority() {
  local authority_identity="$SECRETS_DIR/registry-authority.identity"
  local authority_certificate="$SECRETS_DIR/registry-authority-certificate.json"
  local root_identity="$SECRETS_DIR/registry-root.identity"
  local registry_image="${SPARROW_IMAGE_PREFIX}-node-registry:${SPARROW_IMAGE_TAG}"
  local root_volume=""
  local candidate

  ensure_file_path "$authority_identity"
  ensure_file_path "$authority_certificate"
  ensure_file_path "$root_identity"

  if [[ -f "$authority_identity" && -f "$authority_certificate" ]]; then
    return
  fi

  rm -f "$authority_identity" "$authority_certificate"

  if [[ ! -f "$root_identity" ]]; then
    for candidate in \
      sparrow-control-plane_registry-identity \
      securechat-control-plane_registry-identity \
      control-plane_registry-identity; do
      if docker volume inspect "$candidate" >/dev/null 2>&1 && \
         docker run --rm \
           -v "$candidate:/identity:ro" \
           --entrypoint /bin/sh \
           "$registry_image" \
           -c 'test -f /identity/registry.identity' >/dev/null 2>&1; then
        root_volume="$candidate"
        break
      fi
    done

    if [[ -z "$root_volume" ]]; then
      root_volume="sparrow-control-plane_registry-identity"
      docker volume create "$root_volume" >/dev/null
      docker run --rm \
        --user 0:0 \
        -v "$root_volume:/identity" \
        --entrypoint /bin/sh \
        "$registry_image" \
        -c 'chown 65532:65532 /identity && chmod 700 /identity'
      docker run --rm \
        -v "$root_volume:/identity" \
        --entrypoint java \
        "$registry_image" \
        -cp '/app/lib/*' \
        com.cbgm.sparrow.server.security.NodeRequestSignatureCli \
        /identity/registry.identity GET /bootstrap >/dev/null
    fi

    docker run --rm \
      -v "$root_volume:/identity:ro" \
      --entrypoint /bin/sh \
      "$registry_image" \
      -c 'cat /identity/registry.identity' > "$root_identity"
    chmod 600 "$root_identity" 2>/dev/null || true
  fi

  docker run --rm \
    --user "$(id -u):$(id -g)" \
    -v "$SECRETS_DIR:/secrets" \
    --entrypoint java \
    "$registry_image" \
    -cp '/app/lib/*' \
    com.cbgm.sparrow.server.registry.RegistryAuthorityProvisioningCli \
    /secrets/registry-root.identity \
    /secrets/registry-authority.identity \
    /secrets/registry-authority-certificate.json

  if [[ ! -f "$authority_identity" || ! -f "$authority_certificate" ]]; then
    echo "Registry authority provisioning did not create the expected files." >&2
    exit 1
  fi
}

ensure_registry_authority

"${COMPOSE[@]}" run --rm --no-deps registry-signing-init

"${COMPOSE[@]}" up -d node-registry-database push-database
"${COMPOSE[@]}" up -d --force-recreate presence-redis

wait_for_running() {
  local service="$1"
  local deadline=$((SECONDS + 60))
  while (( SECONDS < deadline )); do
    local container_id state
    container_id="$("${COMPOSE[@]}" ps -q "$service" 2>/dev/null | head -n 1)"
    if [[ -n "$container_id" ]]; then
      state="$(docker inspect --format '{{.State.Status}}' "$container_id" 2>/dev/null || true)"
      if [[ "$state" == "running" ]]; then
        return 0
      fi
    fi
    sleep 1
  done
  echo "$service did not start." >&2
  exit 1
}

wait_for_running node-registry-database
wait_for_running presence-redis
wait_for_running push-database
sleep 3

sql_escape() {
  printf '%s' "$1" | sed "s/'/''/g"
}

sync_postgres_password() {
  local service="$1"
  local user="$2"
  local database="$3"
  local password="$4"
  local escaped
  escaped="$(sql_escape "$password")"
  "${COMPOSE[@]}" exec -T "$service" \
    psql -v ON_ERROR_STOP=1 -U "$user" -d "$database" \
    -c "ALTER ROLE \"$user\" WITH PASSWORD '$escaped';"
}

sync_postgres_password node-registry-database sparrow_registry sparrow_registry "$REGISTRY_PASSWORD"
sync_postgres_password push-database sparrow_push sparrow_push "$PUSH_PASSWORD"

SCHEMA_STATE="$("${COMPOSE[@]}" exec -T push-database \
  psql -v ON_ERROR_STOP=1 -At -U sparrow_push -d sparrow_push -c \
  "SELECT CASE WHEN to_regclass('public.push_devices') IS NULL THEN 'EMPTY' WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'push_devices' AND column_name = 'routing_id') THEN 'CURRENT' ELSE 'OBSOLETE' END;" | tr -d '\r\n')"
if [[ "$SCHEMA_STATE" == "OBSOLETE" ]]; then
  "${COMPOSE[@]}" exec -T push-database \
    psql -v ON_ERROR_STOP=1 -U sparrow_push -d sparrow_push -c \
    "DROP TABLE IF EXISTS push_wake_ups CASCADE; DROP TABLE IF EXISTS pending_envelopes CASCADE; DROP TABLE IF EXISTS push_devices CASCADE;"
fi

"${COMPOSE[@]}" up -d --remove-orphans
"${COMPOSE[@]}" up -d --force-recreate caddy

http_ready() {
  local url="$1"
  if command -v curl >/dev/null 2>&1; then
    curl --fail --silent --show-error --max-time 4 "$url" >/dev/null 2>&1
  elif command -v wget >/dev/null 2>&1; then
    wget --quiet --timeout=4 --output-document=/dev/null "$url" >/dev/null 2>&1
  else
    return 1
  fi
}

wait_for_endpoint() {
  local name="$1"
  local url="$2"
  local deadline=$((SECONDS + 90))
  while (( SECONDS < deadline )); do
    if http_ready "$url"; then
      echo "$name ready: $url"
      return 0
    fi
    sleep 2
  done
  echo "$name did not become ready: $url" >&2
  "${COMPOSE[@]}" ps || true
  "${COMPOSE[@]}" logs --tail=200 || true
  exit 1
}

wait_for_endpoint "control-plane registry route" "http://127.0.0.1:${CONTROL_PLANE_HTTP_PORT}/health/registry"
wait_for_endpoint "control-plane presence route" "http://127.0.0.1:${CONTROL_PLANE_HTTP_PORT}/health/presence"
wait_for_endpoint "control-plane push route" "http://127.0.0.1:${CONTROL_PLANE_HTTP_PORT}/health/push"

if [[ "$MODE" == "public" ]]; then
  CONTROL_PLANE_URL="https://$PUBLIC_DOMAIN"
else
  HOST_ADDRESS="$(primary_ipv4)"
  HOST_ADDRESS="${HOST_ADDRESS:-localhost}"
  CONTROL_PLANE_URL="http://${HOST_ADDRESS}:${CONTROL_PLANE_HTTP_PORT}"
fi

echo "Sparrow Control Plane is running: $CONTROL_PLANE_URL"
