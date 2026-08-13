#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/securechat.conf"
RUNTIME_ENV="$SCRIPT_DIR/.env.runtime"
SECRETS_DIR="$SCRIPT_DIR/secrets"
BASE_COMPOSE="$SCRIPT_DIR/docker-compose.yml"
RELEASE_COMPOSE="$SCRIPT_DIR/docker-compose.release.yml"
PRODUCTION_COMPOSE="$SCRIPT_DIR/docker-compose.production.yml"
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

MODE="${MODE:-lan}"
PUBLIC_DOMAIN="${PUBLIC_DOMAIN:-}"
CONTROL_PLANE_URLS="${CONTROL_PLANE_URLS:-}"

if [[ "$MODE" != "lan" && "$MODE" != "public" ]]; then
  echo "securechat.conf MODE must be lan or public." >&2
  exit 1
fi

: "${CONTROL_PLANE_URLS:?securechat.conf is missing CONTROL_PLANE_URLS}"
: "${SECURECHAT_IMAGE_PREFIX:?securechat.conf is missing SECURECHAT_IMAGE_PREFIX}"
: "${SECURECHAT_IMAGE_TAG:?securechat.conf is missing SECURECHAT_IMAGE_TAG}"

http_ready() {
  local url="$1"
  if command -v curl >/dev/null 2>&1; then
    curl --fail --silent --show-error --max-time 4 "$url" >/dev/null 2>&1
    return
  fi
  if command -v wget >/dev/null 2>&1; then
    wget --quiet --timeout=4 --output-document=/dev/null "$url" >/dev/null 2>&1
    return
  fi
  return 1
}

normalize_control_plane_url() {
  local value="$1"
  value="$(printf '%s' "$value" | xargs)"
  [[ -n "$value" ]] || return 1

  if [[ ! "$value" =~ ^[A-Za-z][A-Za-z0-9+.-]*:// ]]; then
    if [[ "$MODE" == "public" ]]; then
      value="https://$value"
    else
      value="http://$value"
    fi
  fi

  if [[ "$value" != http://* && "$value" != https://* ]]; then
    echo "Control-plane addresses must use HTTP or HTTPS: $value" >&2
    return 1
  fi
  if [[ "$MODE" == "public" && "$value" != https://* ]]; then
    echo "Public mode requires HTTPS control-plane addresses: $value" >&2
    return 1
  fi

  printf '%s' "${value%/}"
}

CONTROL_PLANE_URL=""
NORMALIZED_CONTROL_PLANE_URLS=()
IFS=',;' read -r -a CONTROL_PLANE_CANDIDATES <<< "$CONTROL_PLANE_URLS"
for raw_candidate in "${CONTROL_PLANE_CANDIDATES[@]}"; do
  candidate="$(normalize_control_plane_url "$raw_candidate" || true)"
  [[ -n "$candidate" ]] || continue
  NORMALIZED_CONTROL_PLANE_URLS+=("$candidate")
  if [[ -z "$CONTROL_PLANE_URL" ]] && http_ready "$candidate/v1/nodes"; then
    CONTROL_PLANE_URL="$candidate"
  fi
done

if [[ ${#NORMALIZED_CONTROL_PLANE_URLS[@]} -eq 0 ]]; then
  echo "securechat.conf CONTROL_PLANE_URLS contains no usable addresses." >&2
  exit 1
fi

if [[ -z "$CONTROL_PLANE_URL" ]]; then
  echo "None of the control planes configured in securechat.conf are reachable." >&2
  exit 1
fi

control_plane_host() {
  printf '%s' "$CONTROL_PLANE_URL" | sed -E 's#^[a-zA-Z]+://([^/:]+).*#\1#'
}

container_control_plane_url() {
  local value="$1"
  local host
  host="$(printf '%s' "$value" | sed -E 's#^[a-zA-Z]+://([^/:]+).*#\1#')"
  if [[ "$host" == "localhost" || "$host" == "127.0.0.1" ]]; then
    printf '%s' "$value" | sed -E 's#(https?://)(localhost|127\.0\.0\.1)#\1host.docker.internal#'
    return
  fi
  printf '%s' "$value"
}

container_control_plane_urls() {
  local converted=()
  local candidate
  for candidate in "${NORMALIZED_CONTROL_PLANE_URLS[@]}"; do
    converted+=("$(container_control_plane_url "$candidate")")
  done
  local joined
  joined="$(IFS=,; printf '%s' "${converted[*]}")"
  printf '%s' "$joined"
}

primary_ipv4() {
  local destination resolved interface_name
  destination="$(control_plane_host)"

  if command -v getent >/dev/null 2>&1; then
    resolved="$(getent ahostsv4 "$destination" 2>/dev/null | awk 'NR == 1 {print $1}')"
    if [[ -n "$resolved" ]]; then
      destination="$resolved"
    fi
  fi

  if command -v ip >/dev/null 2>&1; then
    ip route get "$destination" 2>/dev/null | awk '{for (i=1; i<=NF; i++) if ($i == "src") {print $(i+1); exit}}'
    return
  fi

  if command -v route >/dev/null 2>&1 && command -v ipconfig >/dev/null 2>&1; then
    interface_name="$(route -n get "$destination" 2>/dev/null | awk '/interface:/ {print $2; exit}')"
    if [[ -n "$interface_name" ]]; then
      ipconfig getifaddr "$interface_name"
      return
    fi
  fi

  hostname -I 2>/dev/null | awk '{print $1}'
}

public_ipv4() {
  local value
  for url in https://api.ipify.org https://checkip.amazonaws.com; do
    if command -v curl >/dev/null 2>&1; then
      value="$(curl --fail --silent --show-error --max-time 5 "$url" 2>/dev/null || true)"
    elif command -v wget >/dev/null 2>&1; then
      value="$(wget --quiet --timeout=5 --output-document=- "$url" 2>/dev/null || true)"
    else
      value=""
    fi
    value="$(printf '%s' "$value" | tr -d '[:space:]')"
    if [[ "$value" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; then
      printf '%s' "$value"
      return 0
    fi
  done
  return 1
}

HOST_ADDRESS="$(primary_ipv4)"
if [[ -z "$HOST_ADDRESS" ]]; then
  echo "Could not determine a usable IPv4 address for this node." >&2
  exit 1
fi

if [[ "$MODE" == "public" ]]; then
  if [[ -z "$PUBLIC_DOMAIN" ]]; then
    PUBLIC_IP="$(public_ipv4 || true)"
    if [[ -z "$PUBLIC_IP" ]]; then
      echo "Could not detect the public IPv4 address. Set PUBLIC_DOMAIN in securechat.conf." >&2
      exit 1
    fi
    PUBLIC_DOMAIN="${PUBLIC_IP//./-}.sslip.io"
  fi
  SITE_ADDRESS="$PUBLIC_DOMAIN"
  CLIENT_ENDPOINT="wss://$PUBLIC_DOMAIN/v1/gateway"
  HTTP_ENDPOINT="https://$PUBLIC_DOMAIN"
else
  SITE_ADDRESS=":80"
  CLIENT_ENDPOINT="ws://$HOST_ADDRESS:8490/v1/gateway"
  HTTP_ENDPOINT="http://$HOST_ADDRESS:8490"
fi

mkdir -p "$SECRETS_DIR"
ensure_secret() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    head -c 48 /dev/urandom | base64 | tr -d '\n' > "$path"
    chmod 600 "$path" 2>/dev/null || true
  fi
}

ensure_secret "$SECRETS_DIR/mailbox-database-password.txt"
ensure_secret "$SECRETS_DIR/federation-database-password.txt"
ensure_secret "$SECRETS_DIR/federation-internal-api-token.txt"
ensure_secret "$SECRETS_DIR/gateway-internal-api-token.txt"

cat > "$RUNTIME_ENV" <<EOF_RUNTIME
COMMUNITY_NODE_PROJECT_NAME=securechat-community-node
COMMUNITY_NODE_BIND_ADDRESS=0.0.0.0
COMMUNITY_NODE_HTTP_PORT=8490
COMMUNITY_NODE_SITE_ADDRESS=$SITE_ADDRESS
COMMUNITY_NODE_DOMAIN=$PUBLIC_DOMAIN
CONTROL_PLANE_URL=$(container_control_plane_url "$CONTROL_PLANE_URL")
CONTROL_PLANE_URLS=$(container_control_plane_urls)
ADVERTISED_CONTROL_PLANE_URLS=$(IFS=,; printf '%s' "${NORMALIZED_CONTROL_PLANE_URLS[*]}")
CLIENT_ENDPOINT=$CLIENT_ENDPOINT
FEDERATION_ENDPOINT=$HTTP_ENDPOINT
MAILBOX_ENDPOINT=$HTTP_ENDPOINT
SECURECHAT_IMAGE_PREFIX=$SECURECHAT_IMAGE_PREFIX
SECURECHAT_IMAGE_TAG=$SECURECHAT_IMAGE_TAG
SECURECHAT_UPDATE_INTERVAL_SECONDS=300
MAILBOX_DATABASE_PASSWORD_FILE=./secrets/mailbox-database-password.txt
FEDERATION_DATABASE_PASSWORD_FILE=./secrets/federation-database-password.txt
FEDERATION_INTERNAL_API_TOKEN_FILE=./secrets/federation-internal-api-token.txt
GATEWAY_INTERNAL_API_TOKEN_FILE=./secrets/gateway-internal-api-token.txt
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

if [[ "$PREPARE_ONLY" != "--prepare-only" ]]; then
  "${COMPOSE[@]}" pull
  "${COMPOSE[@]}" up -d --remove-orphans --wait --wait-timeout 300
fi
