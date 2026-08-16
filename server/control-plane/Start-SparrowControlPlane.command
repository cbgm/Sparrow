#!/bin/bash
SCRIPT_DIR="$(cd -- "$(dirname -- "$0")" && pwd)"
cd "$SCRIPT_DIR"
exec bash "$SCRIPT_DIR/start-sparrow-control-plane.sh" "$@"
