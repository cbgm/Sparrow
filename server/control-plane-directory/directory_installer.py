#!/usr/bin/env python3
"""Installs and manages the independent Sparrow Control Plane Directory (Windows, macOS, Linux).

Operator-only tool: it is never part of the public server bundle. The Directory either runs
behind the HTTPS proxy of an installed Sparrow Server on the same public IP (shared mode), or
with its own Caddy on ports 80/443 (standalone mode). The signing identity in the Docker volume
sparrow-central-directory-state is created once and never replaced.
"""
from __future__ import annotations

import argparse
import base64
import ipaddress
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import time
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parent
COMPOSE = ROOT / "docker-compose.yml"
PRIVATE = ROOT / "private"
HOST_FILE = PRIVATE / "directory-host.txt"
INFO_FILE = PRIVATE / "directory-public.json"
WINDOWS = os.name == "nt"
BEGIN = "# BEGIN SPARROW INDEPENDENT DIRECTORY ROUTE"
END = "# END SPARROW INDEPENDENT DIRECTORY ROUTE"
PUBLIC_PATHS = ("/health /.well-known/sparrow-directory /v1/control-planes /v1/registrations "
                "/v1/registrations/challenge")
HOSTNAME = re.compile(
    r"(?=.{4,253}$)[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+")
AUTOMATIC = re.compile(r"directory-(\d{1,3}-\d{1,3}-\d{1,3}-\d{1,3})\.sslip\.io")
PUBLIC_KEY = re.compile(r"[A-Za-z0-9_-]{58,100}")
CONTAINER = "sparrow-central-directory"
PROXY_CONTAINER = "sparrow-central-directory-caddy"
HIDDEN = subprocess.CREATE_NO_WINDOW if WINDOWS else 0


class DirectoryError(Exception):
    pass


# ---------------------------------------------------------------------- docker
def docker(*arguments: str, check: bool = True) -> subprocess.CompletedProcess:
    result = subprocess.run(["docker", *arguments], capture_output=True, text=True,
                            creationflags=HIDDEN)
    if check and result.returncode:
        detail = "\n".join(
            (result.stderr or result.stdout).strip().splitlines()[-40:]) or "no output"
        raise DirectoryError(
            f"docker {' '.join(arguments[:3])} failed (exit {result.returncode}):\n{detail}")
    return result


def docker_lines(*arguments: str) -> list[str]:
    return [line.strip() for line in docker(*arguments).stdout.splitlines() if line.strip()]


def compose(*arguments: str, check: bool = True) -> subprocess.CompletedProcess:
    return docker("compose", "-f", str(COMPOSE), *arguments, check=check)


def streamed(*arguments: str) -> None:
    """Long-running Docker command whose progress is shown in the activity log."""
    result = subprocess.run(["docker", *arguments], creationflags=HIDDEN)
    if result.returncode:
        raise DirectoryError(f"docker {' '.join(arguments[:4])} failed (exit {result.returncode}).")


def docker_running() -> bool:
    return bool(shutil.which("docker")) and subprocess.run(["docker", "info"], capture_output=True,
                                                           creationflags=HIDDEN).returncode == 0


def check_docker() -> None:
    if not shutil.which("docker"):
        raise DirectoryError(
            "Docker is required. Install Docker Desktop (or Docker Engine) and retry.")
    if not docker_running():
        raise DirectoryError("Docker is not running. Start Docker and retry.")


def container_status(name: str) -> str:
    result = docker("inspect", "--format", "{{.State.Status}}", name, check=False)
    return result.stdout.strip() if result.returncode == 0 else ""


# ---------------------------------------------------------------------- names and addresses
def validate_hostname(name: str) -> str:
    if not HOSTNAME.fullmatch(name) or ".." in name:
        raise DirectoryError(
            "Enter a valid lower-case public DNS hostname, e.g. directory-1-2-3-4.sslip.io.")
    return name


def automatic_ipv4(name: str) -> str:
    match = AUTOMATIC.fullmatch(name.strip().lower())
    if not match:
        return ""
    try:
        return str(ipaddress.IPv4Address(match.group(1).replace("-", ".")))
    except ipaddress.AddressValueError:
        return ""


def public_ipv4() -> str:
    for url in ("https://api.ipify.org", "https://ipv4.icanhazip.com"):
        try:
            address = ipaddress.IPv4Address(urlopen(url, timeout=8).read(64).decode().strip())
        except (OSError, ValueError):
            continue
        if address.is_global:
            return str(address)
    return ""


def automatic_hostname(ip: str) -> str:
    return "directory-" + ip.replace(".", "-") + ".sslip.io"


def installed_hostname() -> str:
    return HOST_FILE.read_text(encoding="utf-8").strip() if HOST_FILE.is_file() else ""


def suggested_hostname(ip: str) -> str:
    """The installed name; an automatic sslip.io name follows the current public IPv4."""
    current = installed_hostname()
    if ip and (not current or automatic_ipv4(current)):
        return automatic_hostname(ip)
    return current


def installed_info() -> dict:
    try:
        return json.loads(INFO_FILE.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def host_path(label: str) -> Path | None:
    """Compose working_dir label as a host path (Docker Desktop may report Linux-style mounts)."""
    value = label.strip()
    match = re.match(r"^/(?:run/desktop/mnt/host|host_mnt|mnt)/([a-zA-Z])/(.*)$", value)
    if WINDOWS and match:
        value = f"{match.group(1).upper()}:\\" + match.group(2).replace("/", "\\")
    return Path(value) if value else None


def running_server_proxy() -> tuple[str, Path | None]:
    """ID and server folder of the running Sparrow Server HTTPS proxy, if exactly one runs."""
    lines = docker_lines("ps", "--filter", "label=com.docker.compose.project=sparrow-public-proxy",
                         "--filter", "label=com.docker.compose.service=caddy", "--format",
                         '{{.ID}}|{{.Label "com.docker.compose.project.working_dir"}}')
    if len(lines) != 1:
        return "", None
    container, _, workdir = lines[0].partition("|")
    folder = host_path(workdir)
    return container, folder.parent if folder else None


def running_directory_folder() -> Path | None:
    """Folder whose Compose project runs the Directory container, if it still exists."""
    if not docker_running():
        return None
    label = '{{index .Config.Labels "com.docker.compose.project.working_dir"}}'
    result = docker("inspect", "--format", label, CONTAINER, check=False)
    folder = host_path(result.stdout) if result.returncode == 0 else None
    return folder if folder and folder.is_dir() else None


def server_folder(path: str) -> Path | None:
    """The folder when it holds an installed Sparrow Server, otherwise None."""
    folder = Path(path) if path else None
    if folder and (folder / "control-plane" / "sparrow.conf").is_file():
        return folder
    return None


def find_server_folder() -> Path | None:
    if not docker_running():
        return None
    try:
        return server_folder(str(running_server_proxy()[1] or ""))
    except DirectoryError:
        return None


# ---------------------------------------------------------------------- shared proxy route
def directory_route(hostname: str) -> str:
    return (f"{BEGIN}\n{hostname} {{\n    @directory_public path {PUBLIC_PATHS}\n"
            "    handle @directory_public {\n"
            "        reverse_proxy sparrow-central-directory:9080\n    }\n"
            '    handle {\n        respond "Not found" 404\n    }\n}\n' + END)


def same_route(route: str, hostname: str) -> bool:
    route = route.replace("\r\n", "\n")
    expected = directory_route(hostname)
    return route in (expected, expected.replace(" /.well-known/sparrow-directory", ""))


def managed_route(contents: str) -> str:
    start, end = contents.find(BEGIN), contents.find(END)
    if (start >= 0) != (end >= 0):
        raise DirectoryError(
            "The server Caddyfile has an incomplete Directory route; nothing was changed.")
    if start < 0:
        return ""
    if end <= start or contents.find(BEGIN, start + len(BEGIN)) >= 0 or contents.find(
            END, end + len(END)) >= 0:
        raise DirectoryError(
            "The server Caddyfile has several Directory routes; nothing was changed.")
    return contents[start:end + len(END)]


def route_hostname(route: str) -> str:
    for line in route.replace("\r\n", "\n").split("\n"):
        line = line.strip()
        if not line or line in (BEGIN, END):
            continue
        match = re.fullmatch(r"([a-z0-9](?:[a-z0-9.-]*[a-z0-9])?)\s*\{", line)
        return match.group(1) if match and HOSTNAME.fullmatch(match.group(1)) else ""
    return ""


def is_sparrow_route(route: str) -> bool:
    upstreams = re.findall(r"(?m)^\s*reverse_proxy\s+([^\s#]+)\s*$", route)
    return (bool(route_hostname(route)) and upstreams == ["sparrow-central-directory:9080"] and
            re.search(r'respond\s+"Not found"\s+404', route) is not None and
            re.search(r"(?m)^\s*@directory_public\s+path\s+", route) is not None)


def check_route_change(current_route: str, previous: str, hostname: str) -> None:
    """Allows the installer's own route and automatic sslip.io moves; custom routes stay."""
    if not current_route:
        return
    if previous:
        if same_route(current_route, previous):
            return
        if automatic_ipv4(previous) and automatic_ipv4(hostname) and same_route(
                current_route, hostname):
            print(f"The server already moved the Directory route: {previous} -> {hostname}.")
            return
        raise DirectoryError(
            "The Directory route in the server Caddyfile was changed by hand; nothing was changed.")
    if not is_sparrow_route(current_route):
        raise DirectoryError(
            "The marked Directory route in the server Caddyfile is not Sparrow's; "
            "nothing was changed."
        )
    owner = route_hostname(current_route)
    if owner != hostname and not (automatic_ipv4(owner) and automatic_ipv4(hostname)):
        raise DirectoryError(f"The server's Directory route belongs to {owner}. Use that hostname.")
    print(f"Adopting the existing Directory route of {owner}.")


# ---------------------------------------------------------------------- settings files
def read(path: Path) -> str:
    return path.read_bytes().decode("utf-8")  # keeps the file's line endings


def write(path: Path, text: str) -> None:
    path.write_bytes(text.encode("utf-8"))


def properties(path: Path) -> dict[str, str]:
    values = {}
    if path.is_file():
        for line in path.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()
    return values


def set_properties(path: Path, updates: dict[str, str]) -> None:
    original = read(path)
    revised = original
    for key, value in updates.items():
        pattern = re.compile(rf"(?m)^{re.escape(key)}=[^\r\n]*")
        line = f"{key}={value}"
        if pattern.search(revised):
            revised = pattern.sub(lambda _: line, revised)
        else:
            revised = revised.rstrip() + f"\n{line}\n"
    if revised != original:
        write(path.with_name(path.name + ".before-directory"), original)
        write(path, revised)


def server_files(server: Path) -> dict[str, Path]:
    files = {
        "caddyfile": server / "public-proxy" / "Caddyfile",
        "cp": server / "control-plane" / "sparrow.conf",
        "node": server / "community-node" / "sparrow.conf"
    }
    missing = [str(path) for path in files.values() if not path.is_file()]
    if missing:
        raise DirectoryError(
            f"{server} is not an installed Sparrow Server. Missing: {', '.join(missing)}")
    return files


def ensure_admin_token() -> None:
    PRIVATE.mkdir(exist_ok=True)
    admin = PRIVATE / "admin.env"
    if not admin.is_file():
        token = base64.b64encode(secrets.token_bytes(48)).decode()
        admin.write_text(f"SPARROW_DIRECTORY_ADMIN_TOKEN={token}\n", encoding="utf-8")
    if not WINDOWS:
        PRIVATE.chmod(0o700)
        admin.chmod(0o600)


# ---------------------------------------------------------------------- install steps
def check_standalone_ports() -> None:
    for line in docker_lines("ps", "--format", "{{.Names}} {{.Ports}}"):
        published = re.search(r"(?:0\.0\.0\.0|\[::\]):(?:80|443)->", line)
        if published and not line.startswith(PROXY_CONTAINER + " "):
            raise DirectoryError(
                f"Ports 80/443 already belong to another container ({line.split()[0]}). "
                "Share the Sparrow Server's proxy instead, or use another machine/IP.")


def prepare_identity() -> str:
    """Builds the image, prepares the volume and the signing key; returns the public key."""
    print("Building the Directory image ...", flush=True)
    streamed("compose", "-f", str(COMPOSE), "build", "--progress=plain", "directory")
    # The service drops ALL capabilities, so a short-lived helper prepares the named volume.
    # Never replace or delete the volume: it holds the signing key and the database.
    print("Preparing the persistent Directory storage ...", flush=True)
    docker("run", "--rm", "--user", "0:0", "--cap-drop", "ALL", "--cap-add", "CHOWN", "--cap-add",
           "FOWNER", "--cap-add", "DAC_OVERRIDE", "--mount",
           "type=volume,source=sparrow-central-directory-state,target=/state", "--entrypoint",
           "/bin/sh", "sparrow-central-directory:local", "-c",
           "mkdir -p /state && chown -R 10001:10001 /state && chmod 700 /state")
    presence = compose("run", "--rm", "--no-deps", "--entrypoint", "/bin/sh", "directory", "-c",
                       "test -e /state/signing.pem", check=False)
    if presence.returncode == 1:
        print("Creating the Directory signing identity (once) ...", flush=True)
        compose("run", "--rm", "--no-deps", "directory", "init-key", "--key-file",
                "/state/signing.pem")
    elif presence.returncode:
        raise DirectoryError(
            f"Cannot check the existing Directory key (exit {presence.returncode}).")
    output = compose("run", "--rm", "--no-deps", "directory", "show-public-key", "--key-file",
                     "/state/signing.pem").stdout.splitlines()
    keys = [line.strip() for line in output if PUBLIC_KEY.fullmatch(line.strip())]
    if not keys:
        raise DirectoryError("Could not read the Directory public key; nothing else was changed.")
    return keys[-1]


def start_standalone_proxy(hostname: str, previous: str) -> None:
    caddyfile = PRIVATE / "Caddyfile"
    content = (f"{hostname} {{\n    @directory_public path {PUBLIC_PATHS}\n"
               "    handle @directory_public {\n        reverse_proxy directory:9080\n    }\n"
               '    handle { respond "Not found" 404 }\n}\n')
    if caddyfile.is_file():
        prior = caddyfile.read_text(encoding="utf-8").replace("\r\n", "\n")
        moved = previous and previous != hostname
        if prior != content and not moved and hostname not in prior:
            raise DirectoryError(f"{caddyfile} was changed by hand; it was not overwritten.")
    caddyfile.write_text(content, encoding="utf-8")
    compose("--profile", "standalone", "up", "-d", "--no-deps", "directory-caddy")
    print("Directory HTTPS proxy started. Ports 80/443 must be forwarded to this machine.")


def update_server_route(proxy: str, caddyfile: Path, current: str, current_route: str,
                        hostname: str) -> None:
    route = directory_route(hostname)
    if "\r\n" in current:
        route = route.replace("\n", "\r\n")
    updated = current.replace(current_route,
                              route) if current_route else current.rstrip() + "\n\n" + route + "\n"
    if updated == current:
        return
    print("Adding the Directory hostname to the server's HTTPS proxy ...", flush=True)
    write(caddyfile.with_name("Caddyfile.before-directory"), current)
    write(caddyfile, updated)
    try:
        docker("exec", proxy, "caddy", "validate", "--config", "/etc/caddy/Caddyfile")
        docker("exec", proxy, "caddy", "reload", "--config", "/etc/caddy/Caddyfile")
    except DirectoryError:
        write(caddyfile, current)
        docker("exec", proxy, "caddy", "reload", "--config", "/etc/caddy/Caddyfile", check=False)
        raise


def register_control_plane(server: Path, url: str, key: str) -> None:
    """Publishes the server's Control Plane with its own identity and checks the signed list."""
    cp = properties(server / "control-plane" / "sparrow.conf")
    identity = server / "control-plane" / "secrets" / "registry-root.identity"
    proxy_compose = server / "public-proxy" / "docker-compose.yml"
    domain = cp.get("PUBLIC_DOMAIN", "").lower()
    if cp.get("MODE") != "public" or not domain:
        print("The server's Control Plane is not public; it is not registered in the Directory.")
        return
    if not identity.is_file() or not proxy_compose.is_file():
        raise DirectoryError(
            "The server is missing its Control Plane identity or proxy; registration was skipped.")
    (server / "control-plane" / "directory-registration-proofs").mkdir(exist_ok=True)
    print(f"Registering the Control Plane https://{domain} ...", flush=True)
    # A Docker-readable copy of the identity under public-proxy; the real file is never changed.
    stage = server / "public-proxy" / ".sparrow-registration"
    staged_compose = server / "public-proxy" / ".sparrow-directory-registration.compose.yml"
    stage.mkdir(exist_ok=True)
    (stage / "registry-root.identity").write_bytes(identity.read_bytes())
    try:
        source = proxy_compose.read_text(encoding="utf-8")
        pattern = re.compile(
            r"(?m)^(?P<prefix>\s*-\s*)[^\r\n]+:/run/control-plane/registry-root\.identity:ro\s*$")
        if len(pattern.findall(source)) != 1:
            raise DirectoryError(
                "The server proxy Compose file has no single identity mount; registration skipped.")
        staged_compose.write_text(
            pattern.sub(
                r"\g<prefix>.sparrow-registration/registry-root.identity:"
                r"/run/control-plane/registry-root.identity:ro",
                source, count=1), encoding="utf-8")
        base = ("compose", "-f", str(staged_compose), "--profile", "registration")
        docker(*base, "build", "directory-register")
        result = docker(*base, "run", "--rm", "--no-deps", "-e",
                        f"CONTROL_PLANE_DIRECTORY_URL={url}", "-e",
                        f"CONTROL_PLANE_DIRECTORY_PUBLIC_KEY={key}", "-e",
                        f"SPARROW_REGISTRATION_PLANE_URL=https://{domain}", "directory-register")
        if result.stdout.strip():
            print("Directory registration: " + " ".join(result.stdout.split())[:300])
    finally:
        staged_compose.unlink(missing_ok=True)
        (stage / "registry-root.identity").unlink(missing_ok=True)
        try:
            stage.rmdir()
        except OSError:
            pass
    expected, last = f"https://{domain}", ""
    for attempt in range(5):
        try:
            planes = registered_planes(url)
            if any(plane.get("baseUrl") == expected for plane in planes):
                print(f"Verified: the Directory publishes {expected}.")
                return
            last = f"the signed list does not contain {expected} yet"
        except (OSError, ValueError) as error:
            last = str(error)
        time.sleep(2)
    print(
        f"Could not verify the registration from here ({last}). Check it with 'Registered servers'."
    )


def install(hostname: str, server: Path | None) -> None:
    check_docker()
    hostname = validate_hostname(hostname.strip().lower())
    if "sparrow-public-edge" not in docker_lines("network", "ls", "--format", "{{.Name}}"):
        docker("network", "create", "sparrow-public-edge")
    previous = installed_hostname()
    files, proxy, current, current_route = {}, "", "", ""
    if server is None:
        check_standalone_ports()
    else:
        server = server.expanduser().resolve()
        files = server_files(server)
        proxy, running = running_server_proxy()
        if not proxy:
            raise DirectoryError(
                "The Sparrow Server's HTTPS proxy is not running. Start the server first, "
                "or choose 'Own HTTPS proxy'.")
        if running and server_folder(str(running)) and running.resolve() != server:
            raise DirectoryError(f"The running server proxy belongs to {running}, not {server}.")
        current = read(files["caddyfile"])
        if not re.search(r"(?m)^\s*import sparrow_control_routes\s*$", current) or \
                not re.search(r"(?m)^\s*import sparrow_community_routes\s*$", current):
            raise DirectoryError("This is not a Sparrow Server Caddyfile; nothing was changed.")
        for name in ("cp", "node"):
            if properties(files[name]).get("PUBLIC_DOMAIN", "") == hostname:
                raise DirectoryError(
                    "The Directory needs its own hostname, different from the server's.")
        current_route = managed_route(current)
        check_route_change(current_route, previous, hostname)
    ensure_admin_token()
    key = prepare_identity()
    for name in ("cp", "node"):
        pinned = properties(files[name]).get("CONTROL_PLANE_DIRECTORY_PUBLIC_KEY",
                                             "") if files else ""
        if pinned and pinned != key:
            raise DirectoryError(
                f"{files[name]} trusts a different Directory key; it is never replaced silently.")
    print("Starting the Directory ...", flush=True)
    compose("up", "-d", "--no-deps", "directory")
    if server is None:
        start_standalone_proxy(hostname, previous)
    else:
        update_server_route(proxy, files["caddyfile"], current, current_route, hostname)
    if previous and previous != hostname:
        print(f"Directory hostname changed: {previous} -> {hostname} (signing identity unchanged).")
    HOST_FILE.write_text(hostname + "\n", encoding="utf-8")
    url = f"https://{hostname}"
    INFO_FILE.write_text(
        json.dumps({"url": url, "publicKey": key, "serverRoot": str(server or "")}),
        encoding="utf-8")
    if server is not None:
        for name in ("cp", "node"):
            set_properties(
                files[name],
                {"CONTROL_PLANE_DIRECTORY_URL": url, "CONTROL_PLANE_DIRECTORY_PUBLIC_KEY": key})
        register_control_plane(server, url, key)
    print(f"Directory URL: {url}")
    print(f"Directory public key: {key}")
    print(
        "Apps fetch and pin this key automatically. Enter the URL in the app's settings "
        "if it changed."
    )


# ---------------------------------------------------------------------- other actions
def registered_planes(url: str) -> list[dict]:
    with urlopen(url.rstrip("/") + "/v1/control-planes", timeout=10) as response:
        return list(json.loads(response.read(1 << 20)).get("payload", {}).get("controlPlanes", []))


def state() -> tuple[str, str]:
    if not docker_running():
        return "Docker not running", "error"
    status = container_status(CONTAINER)
    if not status:
        return "Not installed", "idle"
    return ("Running", "ok") if status == "running" else ("Stopped", "warn")


def print_planes(hostname: str) -> None:
    url = installed_info().get("url") or (f"https://{hostname}" if hostname else "")
    if not url:
        raise DirectoryError("Install the Directory first.")
    try:
        planes = registered_planes(url)
    except (OSError, ValueError) as error:
        raise DirectoryError(f"{url} is not reachable from here: {error}") from error
    if not planes:
        print(f"{url} is online; no Control Plane is registered yet.")
    for plane in planes:
        print(f"Registered: {plane.get('baseUrl')} ({plane.get('controlPlaneId')})")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Install and manage the Sparrow Control Plane Directory.")
    commands = parser.add_subparsers(dest="command", required=True)
    install_command = commands.add_parser("install", help="install or start the Directory")
    install_command.add_argument("--hostname", default="",
                                 help="public hostname (default: automatic sslip.io)")
    mode = install_command.add_mutually_exclusive_group()
    mode.add_argument("--server-folder", type=Path,
                      help="share the HTTPS proxy of this Sparrow Server")
    mode.add_argument("--standalone", action="store_true",
                      help="run an own HTTPS proxy on ports 80/443")
    for name, text in (("stop", "stop the Directory"), ("status", "show the containers"),
                       ("logs", "show recent Directory logs"), ("planes",
                                                                "list registered Control Planes"),
                       ("detect", "print the detected server folder and hostname")):
        commands.add_parser(name, help=text)
    args = parser.parse_args()
    try:
        if args.command == "install":
            hostname = args.hostname or suggested_hostname(public_ipv4())
            if not hostname:
                raise DirectoryError("The public IPv4 address is unknown; enter a hostname.")
            server = None if args.standalone else (args.server_folder or find_server_folder())
            print(
                f"Installing the Directory as {hostname} "
                f"({'sharing ' + str(server) if server else 'own HTTPS proxy'}) ...", flush=True)
            install(hostname, server)
        elif args.command == "stop":
            check_docker()
            streamed("compose", "-f", str(COMPOSE), "--profile", "standalone", "stop")
        elif args.command == "status":
            check_docker()
            for line in docker_lines(
                    "ps", "--all", "--filter", f"name={CONTAINER}", "--format",
                    "{{.Names}}: {{.Status}}") or ["The Directory is not installed."]:
                print(line)
        elif args.command == "logs":
            check_docker()
            streamed("compose", "-f", str(COMPOSE), "logs", "--tail", "200", "directory")
        elif args.command == "planes":
            print_planes(installed_hostname())
        else:
            print(
                json.dumps({
                    "server": str(find_server_folder() or ""),
                    "hostname": suggested_hostname(public_ipv4())
                }))
    except (DirectoryError, OSError) as error:
        print(f"Directory: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
