#!/usr/bin/env python3
"""Moves automatic sslip.io hostnames to the current public IPv4 address.

A generated name such as control-1-2-3-4.sslip.io always points to 1.2.3.4. When the internet
provider assigns a new address, the Control Plane, Community Node, managed nodes and the
Directory route move to the new generated names. Identities, secrets, data and custom DNS
names are never changed. The following Start recreates the containers and re-registers the
Control Plane in the Directory.

Usage: public_ip.py [--then COMMAND ...]   (runs COMMAND afterwards, e.g. the server Start)
"""
from __future__ import annotations

from datetime import datetime
import ipaddress
from pathlib import Path
import re
import shutil
import socket
import subprocess
import sys
from urllib.request import urlopen

APP = Path(__file__).resolve().parent
ROOT = APP.parent
GENERATED = re.compile(r"control-(\d{1,3}-\d{1,3}-\d{1,3}-\d{1,3})\.sslip\.io")
# Settings that may contain this installation's generated names (managed nodes move themselves).
FILES = ("control-plane/sparrow.conf", "control-plane/.env.runtime", "community-node/sparrow.conf",
         "community-node/.env.runtime", "public-proxy/Caddyfile")


class AddressError(Exception):
    pass


def properties(path: Path) -> dict[str, str]:
    values = {}
    if path.is_file():
        for line in path.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()
    return values


def public_ipv4() -> str:
    for url in ("https://api.ipify.org", "https://ipv4.icanhazip.com"):
        try:
            address = ipaddress.IPv4Address(urlopen(url, timeout=8).read(64).decode().strip())
        except (OSError, ValueError):
            continue
        if address.is_global:
            return str(address)
    raise AddressError(
        "The public IPv4 address could not be detected; the hostnames were not changed.")


def installed_ipv4() -> str:
    """The address encoded in the installed automatic Control Plane name, or '' for custom names."""
    cp = properties(ROOT / "control-plane" / "sparrow.conf")
    if cp.get("MODE") != "public" or not (ROOT / "control-plane" / ".env.runtime").is_file():
        return ""
    match = GENERATED.fullmatch(cp.get("PUBLIC_DOMAIN", "").lower())
    return match.group(1).replace("-", ".") if match else ""


def follow() -> bool:
    """Rewrites the generated names when the public IPv4 changed; returns True when it did."""
    old = installed_ipv4()
    if not old:
        return False
    new = public_ipv4()
    if new == old:
        print(f"Public IP {new} unchanged; the server addresses are current.")
        return False
    old_dashed, new_dashed = old.replace(".", "-"), new.replace(".", "-")
    try:
        resolved = socket.gethostbyname(f"control-{new_dashed}.sslip.io")
    except OSError as error:
        raise AddressError(
            f"control-{new_dashed}.sslip.io does not resolve yet ({error}); nothing changed."
        ) from error
    if resolved != new:
        raise AddressError(
            f"control-{new_dashed}.sslip.io resolves to {resolved}, not {new}; nothing changed.")
    print(f"Your public IP changed from {old} to {new}. Moving the automatic addresses ...",
          flush=True)
    pattern = re.compile(r"(?<![A-Za-z0-9-])((?:node|control|directory)(?:-[a-z0-9]+)?)-" +
                         re.escape(old_dashed) + r"\.sslip\.io")
    backups = ROOT / "backups" / f"public-ip-{datetime.now():%Y%m%d%H%M%S}"
    originals: dict[Path, str] = {}
    try:
        for relative in FILES:
            path = ROOT / relative
            if not path.is_file():
                continue
            text = path.read_bytes().decode("utf-8")  # keeps the file's line endings
            updated = pattern.sub(lambda match: f"{match.group(1)}-{new_dashed}.sslip.io", text)
            if updated == text:
                continue
            (backups / relative).parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, backups / relative)
            originals[path] = text
            path.write_bytes(updated.encode("utf-8"))
        nodes = list((ROOT / "node-instances").glob("*/node-instance.json"))
        if nodes:
            result = subprocess.run([
                sys.executable,
                str(APP / "Manage-SparrowNodes.py"), "reconcile-public-ip", "--public-ip", new
            ], cwd=ROOT)
            if result.returncode:
                raise AddressError("The Community Nodes could not be moved to the new address.")
    except (OSError, AddressError):
        for path, text in originals.items():
            path.write_bytes(text.encode("utf-8"))
        print("The previous addresses were restored.", flush=True)
        raise
    for label, prefix in (("Control Plane", "control"), ("Community Node", "node")):
        print(f"{label}: https://{prefix}-{new_dashed}.sslip.io")
    if any(f"directory-{old_dashed}.sslip.io" in text for text in originals.values()):
        print(
            f"Directory: https://directory-{new_dashed}.sslip.io  (enter it in the app's settings)")
    print(f"Backups of the previous settings: {backups}", flush=True)
    return True


def main() -> int:
    arguments = sys.argv[1:]
    then = arguments[arguments.index("--then") + 1:] if "--then" in arguments else []
    try:
        follow()
    except (AddressError, OSError) as error:
        # The server still starts with its current addresses.
        print(f"Sparrow addresses: {error}", flush=True)
        if not then:
            return 1
    return subprocess.call(then) if then else 0


if __name__ == "__main__":
    sys.exit(main())
