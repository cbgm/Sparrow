#!/usr/bin/env bash
# Operator-only Directory installer: opens the window on a desktop, otherwise runs the engine.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if ! command -v python3 >/dev/null 2>&1; then
  echo 'Python 3 is required to run the Sparrow Directory installer.' >&2
  exit 1
fi
if [[ $# -eq 0 ]]; then
  if [[ "$(uname)" == "Darwin" || -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ]] && python3 -c 'import tkinter' 2>/dev/null; then
    exec python3 "$ROOT/directory_window.py"
  fi
  # No desktop: install or start with the automatic hostname and the detected server folder.
  set -- install
fi
exec python3 -u "$ROOT/directory_installer.py" "$@"
