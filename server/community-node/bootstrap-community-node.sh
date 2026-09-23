#!/usr/bin/env bash
set -euo pipefail
# Runtime env contains database passwords; never make new files world-readable.
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/sparrow.conf"
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
if [[ -z "$CONTROL_PLANE_URLS" && -f "$RUNTIME_ENV" ]]; then
  CONTROL_PLANE_URLS="$(grep -E '^ADVERTISED_CONTROL_PLANE_URLS=' "$RUNTIME_ENV" | tail -n 1 | cut -d= -f2- || true)"
  if [[ -z "$CONTROL_PLANE_URLS" ]]; then
    CONTROL_PLANE_URLS="$(grep -E '^CONTROL_PLANE_URLS=' "$RUNTIME_ENV" | tail -n 1 | cut -d= -f2- || true)"
  fi
fi

if [[ "$MODE" != "lan" && "$MODE" != "public" ]]; then
  echo "sparrow.conf MODE must be lan or public." >&2
  exit 1
fi

: "${SPARROW_IMAGE_PREFIX:?sparrow.conf is missing SPARROW_IMAGE_PREFIX}"
: "${SPARROW_IMAGE_TAG:?sparrow.conf is missing SPARROW_IMAGE_TAG}"

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

resolve_configured_control_planes() {
  local local_urls="$CONTROL_PLANE_URLS"
  if [[ -z "${CONTROL_PLANE_DIRECTORY_URL:-}" ]]; then
    [[ -n "$local_urls" ]] || { echo "No local Control Plane is configured." >&2; exit 1; }
    return
  fi
  # The combined installation has its own CP even if the external directory
  # is offline, unsigned, expired or the optional Python crypto dependency is
  # unavailable. Never parse unsigned remote JSON into candidate addresses.
  if [[ -z "${CONTROL_PLANE_DIRECTORY_PUBLIC_KEY:-}" ]]; then
    echo "No pinned directory public key; using local Control Plane only." >&2
    [[ -n "$local_urls" ]] || exit 1
    return
  fi
  local client="${CONTROL_PLANE_DIRECTORY_CLIENT:-$SCRIPT_DIR/../control_plane_directory_client.py}"
  if [[ ! -f "$client" ]]; then
    echo "Signed directory client missing; continuing with local Control Plane." >&2
    [[ -n "$local_urls" ]] || exit 1
    return
  fi
  local verified
  verified="$(python3 "$client" --url "$CONTROL_PLANE_DIRECTORY_URL" \
    --public-key "$CONTROL_PLANE_DIRECTORY_PUBLIC_KEY" \
    --cache "$SCRIPT_DIR/.control-plane-directory-verified.json" 2>/dev/null || true)"
  local directory_urls=""
  if [[ -n "$verified" ]]; then
    directory_urls="$(python3 -c 'import json,sys
try:
 v=json.load(sys.stdin)["controlPlanes"]
 if not isinstance(v,list) or any(not isinstance(x,str) for x in v): raise ValueError()
 print(",".join(v))
except (ValueError, KeyError, TypeError): sys.exit(1)' <<< "$verified" || true)"
  fi
  if [[ -n "$directory_urls" ]]; then
    CONTROL_PLANE_URLS="$(printf '%s\n' "$local_urls,$directory_urls" | tr ',;' '\n' | awk 'NF && !seen[$0]++' | paste -sd, -)"
  else
    echo "Verified directory unavailable; keeping the local Control Plane." >&2
    [[ -n "$local_urls" ]] || exit 1
  fi
}

resolve_configured_control_planes

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
  echo "sparrow.conf CONTROL_PLANE_URLS contains no usable addresses." >&2
  exit 1
fi

if [[ -z "$CONTROL_PLANE_URL" ]]; then
  CONTROL_PLANE_URL="${NORMALIZED_CONTROL_PLANE_URLS[0]}"
  echo "No control plane is currently reachable; starting the node and retrying in the background." >&2
fi

control_plane_host() {
  printf '%s' "$CONTROL_PLANE_URL" | sed -E 's#^[a-zA-Z]+://([^/:]+).*#\1#'
}

container_control_plane_url() {
  local value="$1"
  local host
  host="$(printf '%s' "$value" | sed -E 's#^[a-zA-Z]+://([^/:]+).*#\1#')"
  if [[ "$MODE" == "public" && -n "${LOCAL_CONTROL_PLANE_DOMAIN:-}" && "$host" == "$LOCAL_CONTROL_PLANE_DOMAIN" ]]; then
    printf '%s' 'http://sparrow-control-edge:8080'
    return
  fi
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

# Host-side localhost is appropriate for probing a Combined LAN Control Plane,
# but clients must see the reachable LAN address advertised by the node.
advertised_control_plane_urls() {
  local converted=() candidate
  for candidate in "${NORMALIZED_CONTROL_PLANE_URLS[@]}"; do
    if [[ "$MODE" == "lan" && "$candidate" =~ ^http://(localhost|127\.0\.0\.1):([0-9]+)$ ]]; then
      candidate="http://$HOST_ADDRESS:${BASH_REMATCH[2]}"
    fi
    converted+=("$candidate")
  done
  printf '%s\n' "${converted[@]}" | awk 'NF && !seen[$0]++' | paste -sd, -
}

if [[ "$MODE" == "public" ]]; then
  if [[ -z "$PUBLIC_DOMAIN" ]]; then
    PUBLIC_IP="$(public_ipv4 || true)"
    if [[ -z "$PUBLIC_IP" ]]; then
      echo "Could not detect the public IPv4 address. Set PUBLIC_DOMAIN in sparrow.conf." >&2
      exit 1
    fi
    PUBLIC_DOMAIN="${PUBLIC_IP//./-}.sslip.io"
  fi
  SITE_ADDRESS="$PUBLIC_DOMAIN"
  if [[ "${SHARED_PROXY:-false}" == "true" ]]; then SITE_ADDRESS=":80"; fi
  CLIENT_ENDPOINT="wss://$PUBLIC_DOMAIN/v1/gateway"
  HTTP_ENDPOINT="https://$PUBLIC_DOMAIN"
else
  SITE_ADDRESS=":80"
  CLIENT_ENDPOINT="ws://$HOST_ADDRESS:${COMMUNITY_NODE_HTTP_PORT:-8490}/v1/gateway"
  HTTP_ENDPOINT="http://$HOST_ADDRESS:${COMMUNITY_NODE_HTTP_PORT:-8490}"
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
COMMUNITY_NODE_PROJECT_NAME=${COMMUNITY_NODE_PROJECT_NAME:-sparrow-community-node}
COMMUNITY_NODE_BIND_ADDRESS=${COMMUNITY_NODE_BIND_ADDRESS:-0.0.0.0}
COMMUNITY_NODE_HTTP_PORT=${COMMUNITY_NODE_HTTP_PORT:-8490}
MAILBOX_DIAGNOSTIC_PORT=${MAILBOX_DIAGNOSTIC_PORT:-8492}
MAILBOX_DATABASE_PORT=${MAILBOX_DATABASE_PORT:-5636}
FEDERATION_DATABASE_PORT=${FEDERATION_DATABASE_PORT:-5638}
FEDERATION_DIAGNOSTIC_PORT=${FEDERATION_DIAGNOSTIC_PORT:-8493}
GATEWAY_DIAGNOSTIC_PORT=${GATEWAY_DIAGNOSTIC_PORT:-8494}
COMMUNITY_NODE_DIRECTORY_CACHE_VOLUME=${COMMUNITY_NODE_DIRECTORY_CACHE_VOLUME:-sparrow-node-directory-cache}
COMMUNITY_NODE_SITE_ADDRESS=$SITE_ADDRESS
COMMUNITY_NODE_DOMAIN=$PUBLIC_DOMAIN
CONTROL_PLANE_URL=$(container_control_plane_url "$CONTROL_PLANE_URL")
CONTROL_PLANE_URLS=$(container_control_plane_urls)
ADVERTISED_CONTROL_PLANE_URLS=$(advertised_control_plane_urls)
LOCAL_CONTROL_PLANE_DOMAIN=$(grep -E '^LOCAL_CONTROL_PLANE_DOMAIN=' "$CONFIG_FILE" | tail -n 1 | cut -d= -f2- || true)
MANUAL_CONTROL_PLANE_URLS=$(grep -E '^CONTROL_PLANE_URLS=' "$CONFIG_FILE" | tail -n 1 | cut -d= -f2- || true)
CLIENT_ENDPOINT=$CLIENT_ENDPOINT
FEDERATION_ENDPOINT=$HTTP_ENDPOINT
MAILBOX_ENDPOINT=$HTTP_ENDPOINT
SPARROW_IMAGE_PREFIX=$SPARROW_IMAGE_PREFIX
SPARROW_IMAGE_TAG=$SPARROW_IMAGE_TAG
SPARROW_UPDATE_INTERVAL_SECONDS=300
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
  if [[ "${SHARED_PROXY:-false}" == "true" ]]; then
    COMPOSE+=( -f "$SCRIPT_DIR/docker-compose.shared-proxy.yml" )
  fi
fi

cd "$SCRIPT_DIR"
"${COMPOSE[@]}" config --quiet

if [[ "$PREPARE_ONLY" != "--prepare-only" ]]; then
  "${COMPOSE[@]}" pull
  # Compose file-based secrets are bind mounts. Force recreation refreshes
  # stale secret mounts without removing the persistent database volumes.
  "${COMPOSE[@]}" up -d --remove-orphans --force-recreate --wait --wait-timeout 300
fi
