#!/usr/bin/env python3
"""Continuously sync the signed Control Plane directory in an isolated runtime.

The worker has only the directory PUBLIC verification pin and two dedicated
volumes. It has no Docker socket, signing identities or directory admin keys.
"""
import json
import os
import tempfile
from pathlib import Path
import signal
import sys
import time

from control_plane_directory_client import DirectoryError, _load_cache, discover

INTERVAL_SECONDS = 15 * 60
CP_CACHE = Path("/cache/control-plane/verified.json")
NODE_CACHE = Path("/cache/node/verified.json")
ADVERTISED = Path("/cache/node/advertised-control-planes.json")
_stopping = False


def _stop(_signal, _frame):
    global _stopping
    _stopping = True


def _atomic_list(entries):
    """Only publish URLs returned by the pinned, signed client.

    All actual trust decisions about a plane identity remain with consumers;
    this is a discovery hint, not a new identity authorization.
    """
    ADVERTISED.parent.mkdir(parents=True, exist_ok=True)
    descriptor, pending = tempfile.mkstemp(prefix=".discovery-", dir=ADVERTISED.parent)
    try:
        # The Gateway runs unprivileged (65532); addresses are public and
        # must be readable across containers, unlike the signed cache file.
        with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
            os.chmod(pending, 0o644)
            json.dump({"controlPlanes": [entry["baseUrl"] for entry in entries],
                       "entries": entries}, stream, separators=(",", ":"))
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(pending, ADVERTISED)
    finally:
        if os.path.exists(pending):
            os.unlink(pending)


def refresh(url, pin):
    """Attempt both independent persistent caches; a failure never deletes one."""
    if not url or not pin:
        print("Directory synchronization disabled: missing URL or public verification pin.", flush=True)
        return
    for label, path in (("control-plane", CP_CACHE), ("node", NODE_CACHE)):
        try:
            urls, state = discover(url, pin, path)
            if label == "node":
                # Includes revocations reflected in the highest verified snapshot.
                # A genuine signed empty list must replace an old nonempty list.
                # Re-authenticate the cached envelope before publishing keys.
                # A URL-only hint must never authorize a different CP identity.
                envelope = _load_cache(path, pin, int(time.time() * 1000))
                if envelope is None:
                    raise DirectoryError("No signed node directory cache")
                _atomic_list(envelope[1]["controlPlanes"])
            print(f"Directory {label}: {state}; verified entries={len(urls)}", flush=True)
        except (DirectoryError, OSError, ValueError, ImportError) as exc:
            print(f"Directory {label} refresh deferred: {str(exc)[:200]}", file=sys.stderr, flush=True)


def main():
    signal.signal(signal.SIGTERM, _stop)
    signal.signal(signal.SIGINT, _stop)
    url = os.environ.get("CONTROL_PLANE_DIRECTORY_URL", "").strip()
    pin = os.environ.get("CONTROL_PLANE_DIRECTORY_PUBLIC_KEY", "").strip()
    while not _stopping:
        refresh(url, pin)
        # Interruptible shutdown; no contacts with the central service while idle.
        for _ in range(INTERVAL_SECONDS):
            if _stopping:
                break
            time.sleep(1)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
