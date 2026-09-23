#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if ! command -v python3 >/dev/null 2>&1; then
  echo 'Python 3 is required to run the macOS/Linux Sparrow server manager.' >&2
  exit 1
fi
if [[ "${1:-}" == "nodes" ]]; then
  shift
  exec python3 "$ROOT/Manage-SparrowNodes.py" "$@"
fi
if [[ $# -eq 0 ]]; then
  cat <<'HELP'
Sparrow unified server — Combined by default; independent Community Nodes (macOS/Linux).

Install both components for LAN:
  ./Start-SparrowServer.sh install --component combined --mode lan

Install both for Public using free automatic DNS (shared Caddy):
  ./Start-SparrowServer.sh install --component combined --mode public --auto-dns

Install both for Public with your own two DNS names:
  ./Start-SparrowServer.sh install --component combined --mode public \
    --node-domain node.example.com --control-domain control.example.com

Enable Firebase on an existing deployment (without deleting its data):
  ./Start-SparrowServer.sh install --component combined --mode public \
    --firebase-file /path/to/firebase-admin.json

Explicit destructive reinstall (not used by install):
  ./Start-SparrowServer.sh reinstall-public --component combined --mode public \
    --confirm-delete-data

Manage independent Community Nodes without provisioning a Control Plane:
  ./Start-SparrowServer.sh nodes add --name "Node A" --mode public \
    --auto-dns --control-plane-url https://existing-control.example.com
  ./Start-SparrowServer.sh nodes list
  ./Start-SparrowServer.sh nodes install --id <returned-instance-id>
  ./Start-SparrowServer.sh nodes start|stop|update|status|logs|remove --id <instance-id>

Manage Combined: replace status with start, stop, logs, or preflight;
the public installer always installs both components; --component node/control-plane/proxy is for internal lifecycle diagnostics only.

For available options: ./Start-SparrowServer.sh --help

Never extract this ZIP over an existing server. Do not run it against
existing Compose projects/volumes from another installation directory.
HELP
  exit 0
fi
exec python3 "$ROOT/Invoke-SparrowServer.py" "$@"
