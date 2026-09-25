#!/usr/bin/env bash
# Operator-only Linux installer. No Control Plane or Community Node dependency.
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
COMPOSE=(docker compose -f "$PWD/docker-compose.yml")
usage() {
  printf '%s\n' 'Usage: ./Start-SparrowDirectory.sh [--hostname directory.example.com]' \
    'Installs an independent Directory + its own HTTPS Caddy on this host.' \
    'Requires DNS pointing to this host, and available public TCP ports 80/443.' \
    'Do not run on the same IP/port as the combined server Caddy.'
}
HOSTNAME=''
while (($#)); do
  case "$1" in
    --hostname) (($# >= 2)) || { usage >&2; exit 2; }; HOSTNAME="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) usage >&2; exit 2 ;;
  esac
done
command -v docker >/dev/null || { echo 'Docker is required.' >&2; exit 1; }
docker info >/dev/null || { echo 'Docker daemon is not available.' >&2; exit 1; }
docker compose version >/dev/null || { echo 'Docker Compose v2 is required.' >&2; exit 1; }
if [[ -z "$HOSTNAME" && -s private/directory-host.txt ]]; then
  HOSTNAME="$(<private/directory-host.txt)"
fi
if [[ -z "$HOSTNAME" ]]; then
  read -r -p 'Public Directory hostname (e.g. directory.example.com): ' HOSTNAME
fi
if [[ ! "$HOSTNAME" =~ ^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$ || ${#HOSTNAME} -gt 253 ]]; then
  echo 'A lower-case public DNS hostname is required.' >&2; exit 2
fi
# Fail before touching any persistent identity when this host already serves HTTPS.
proxy_id="$(docker ps --filter name='^/sparrow-central-directory-caddy$' --format '{{.ID}}' | head -n 1)"
if [[ -z "$proxy_id" ]]; then
  if docker ps --format '{{.Names}} {{.Ports}}' | grep -E '(^|[^0-9])(0\.0\.0\.0|\[::\]):(80|443)->' >/dev/null; then
    echo 'Ports 80/443 are already owned by another container. Use a separate host/IP or always-on front proxy; no containers were changed.' >&2
    exit 1
  fi
  # Check host-level ingress when possible, not just Docker containers.
  if command -v ss >/dev/null && ss -ltnH '( sport = :80 or sport = :443 )' | grep -q .; then
    echo 'Ports 80/443 already have a host listener. Directory standalone HTTPS cannot start.' >&2
    exit 1
  fi
fi
mkdir -p private
chmod 700 private
if [[ ! -f private/admin.env ]]; then
  umask 077
  printf 'SPARROW_DIRECTORY_ADMIN_TOKEN=%s\n' "$(head -c 48 /dev/urandom | base64 | tr -d '\n')" > private/admin.env
fi
chmod 600 private/admin.env
# Never replace an existing signing identity, data volume, or an unrecognized Caddy config.
new_caddy="$(mktemp)"
trap 'rm -f "$new_caddy"' EXIT
cat > "$new_caddy" <<CADDY
$HOSTNAME {
    @directory_public path /health /.well-known/sparrow-directory /v1/control-planes /v1/registrations /v1/registrations/challenge
    handle @directory_public {
        reverse_proxy directory:9080
    }
    handle { respond "Not found" 404 }
}
CADDY
if [[ -f private/Caddyfile && ! -s private/directory-host.txt ]]; then
  echo 'Existing Caddyfile ownership is unknown. Refusing to overwrite it.' >&2; exit 1
fi
if [[ -f private/Caddyfile && -s private/directory-host.txt ]]; then
  old_host="$(<private/directory-host.txt)"
  if [[ "$old_host" == "$HOSTNAME" ]] && ! cmp -s private/Caddyfile "$new_caddy"; then
    echo 'Existing Caddyfile differs from installer-managed content. Refusing to overwrite it.' >&2; exit 1
  fi
fi
if ! docker network inspect sparrow-public-edge >/dev/null 2>&1; then
  docker network create sparrow-public-edge >/dev/null
fi
echo 'Building standalone Directory image...'
"${COMPOSE[@]}" build directory
# Named volume is created by Compose, then initialized in a short-lived privileged helper.
docker volume create sparrow-central-directory-state >/dev/null
docker run --rm --user 0:0 --cap-drop ALL --cap-add CHOWN --cap-add FOWNER --cap-add DAC_OVERRIDE \
  --mount type=volume,source=sparrow-central-directory-state,target=/state \
  --entrypoint /bin/sh sparrow-central-directory:local \
  -c 'mkdir -p /state && chown -R 10001:10001 /state && chmod 700 /state'
if "${COMPOSE[@]}" run --rm --no-deps --entrypoint /bin/sh directory -c 'test -e /state/signing.pem'; then
  echo 'Reusing existing Directory identity.'
else
  echo 'Creating Directory signing identity (once)...'
  "${COMPOSE[@]}" run --rm --no-deps directory init-key --key-file /state/signing.pem
fi
"${COMPOSE[@]}" run --rm --no-deps directory show-public-key --key-file /state/signing.pem
install -m 600 "$new_caddy" private/Caddyfile
"${COMPOSE[@]}" up -d --no-deps directory
"${COMPOSE[@]}" --profile standalone up -d --no-deps directory-caddy
printf '%s\n' "$HOSTNAME" > private/directory-host.txt
chmod 600 private/directory-host.txt
echo "Directory started: https://$HOSTNAME/v1/control-planes"
echo 'HTTPS certificate issuance and external reachability must be checked before use.'
