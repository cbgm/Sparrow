#!/usr/bin/env python3
"""Unified Sparrow server manager on Linux and macOS.

Normal install updates the current deployment in place. Destructive reinstall
requires a separate explicit command and confirmation. Never adopt other projects.
"""
import argparse
from contextlib import redirect_stderr, redirect_stdout
from datetime import datetime, timezone
import ipaddress
try:
    import fcntl  # POSIX: macOS/Linux only; Windows uses the native GUI manager.
except ImportError:
    fcntl = None
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from urllib.parse import urlparse
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parent
PROJECTS = {"node": ("community-node", "sparrow-community-node"),
            "control-plane": ("control-plane", "sparrow-control-plane"),
            "proxy": ("public-proxy", "sparrow-public-proxy")}
NETWORK = "sparrow-public-edge"
DOMAIN = re.compile(r"(?=.{1,253}$)[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+\Z")
IMAGE_PREFIX = re.compile(r"[a-z0-9.-]+(?:/[a-z0-9._-]+)+\Z")
IMAGE_TAG = re.compile(r"[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}\Z")


class Tee:
    def __init__(self, stream, logfile):
        self.stream, self.logfile = stream, logfile
    def write(self, value):
        self.stream.write(value)
        self.logfile.write(value)
        self.logfile.flush()
    def flush(self):
        self.stream.flush()
        self.logfile.flush()


def public_ipv4(explicit=""):
    # Explicit IPv4 is useful for dynamic-DNS testing; never take an arbitrary
    # HTTP response as a public hostname without IPv4 and scope validation.
    raw = explicit.strip() if explicit else urlopen("https://api.ipify.org", timeout=8).read(64).decode().strip()
    try:
        address = ipaddress.IPv4Address(raw)
    except ipaddress.AddressValueError as exc:
        raise InstallerError("Automatic public DNS requires a valid WAN IPv4 address.") from exc
    require(address.is_global, "WAN IPv4 is not globally routable; public mode requires reachable ingress.")
    return str(address)


def resolve_public_hosts(args):
    if args.mode != "public":
        require(not args.auto_dns and not args.public_ip, "Automatic DNS is only available in Public mode.")
        return
    existing_cp = config("control-plane") if runtime("control-plane").is_file() else {}
    existing_node = config("node") if runtime("node").is_file() else {}
    if not args.control_domain and "control-plane" in selected(args.component):
        args.control_domain = existing_cp.get("PUBLIC_DOMAIN", "")
    if not args.node_domain and "node" in selected(args.component):
        args.node_domain = existing_node.get("PUBLIC_DOMAIN", "")
    if args.auto_dns:
        if not args.node_domain or not args.control_domain:
            slug = public_ipv4(args.public_ip).replace(".", "-")
            if "control-plane" in selected(args.component) and not args.control_domain:
                args.control_domain = f"control-{slug}.sslip.io"
            if "node" in selected(args.component) and not args.node_domain:
                args.node_domain = f"node-{slug}.sslip.io"
    validate_hosts(args.node_domain, args.control_domain)


def public_addresses():
    entries = installed_public()
    if not entries:
        return
    print("Public addresses (configured; external connectivity and TLS must be verified):")
    for component, host in entries:
        if not host:
            continue
        label = "Control Plane" if component == "control-plane" else "Community Node"
        print(f"  {label}: https://{host}")
        if component == "control-plane":
            print(f"  Signed directory: https://{host}/v1/nodes")
        else:
            print(f"  WebSocket: wss://{host}/v1/gateway")


def enable_firebase_credentials(source_file):
    """Add Admin credentials to existing CP without replacing its identity/volumes."""
    require(runtime("control-plane").is_file(), "Control Plane must be installed first.")
    inspect_ownership("control-plane")
    source = Path(source_file).expanduser().resolve(strict=True)
    document = json.loads(source.read_text(encoding="utf-8"))
    require(document.get("type") == "service_account" and document.get("private_key") and
            document.get("client_email"), "Expected Firebase Admin service-account JSON.")
    # The credential file is included via an optional Compose override; no
    # secret content is printed to terminal or logs.
    destination = ROOT / "control-plane" / "secrets" / "firebase-admin.json"
    destination.parent.mkdir(parents=True, exist_ok=True)
    current = destination.read_bytes() if destination.is_file() else None
    incoming = source.read_bytes()
    runtime_file = runtime("control-plane")
    previous_runtime = runtime_file.read_bytes()
    if current is not None and current != incoming:
        raise InstallerError("Different Firebase credentials are already installed; refusing implicit replacement.")
    added = current is None
    if added:
        temporary = destination.with_name(".firebase-admin.pending")
        temporary.write_bytes(incoming)
        temporary.chmod(0o600)
        temporary.replace(destination)
    try:
        update_installed_properties(runtime_file, {"FIREBASE_ADMIN_CREDENTIALS": str(destination)})
        dc("control-plane", "config", "--quiet")
        # Apply optional Compose secret to the push container only. Normal
        # backend update may recreate push again, but volumes remain intact.
        dc("control-plane", "up", "-d", "--no-deps", "--pull", "never", "push")
    except Exception:
        runtime_file.write_bytes(previous_runtime)
        if added:
            destination.unlink(missing_ok=True)
        raise
    print("Firebase Admin credentials configured for existing Control Plane (no reset).")


def reinstall_public(args):
    """Opt-in destructive operation. Never use from normal Install / Start."""
    require(args.component == "combined" and args.mode == "public",
            "Fresh Public reinstall is supported only for Combined Public.")
    require(args.confirm_delete_data,
            "Reinstall removes databases, server identities, queues and certificates. "
            "Repeat with --confirm-delete-data to authorize deletion.")
    require(not (ROOT / ".sparrow-attached.json").exists(), "Attached deployment cannot be reinstalled.")
    for component in ("node", "control-plane"):
        require(runtime(component).is_file(),
                "Missing original runtime; refusing to delete Docker resources of uncertain ownership.")
        project = PROJECTS[component][1]
        containers = docker("ps", "-aq", "--filter", f"label=com.docker.compose.project={project}",
                            capture=True).stdout.splitlines()
        require(containers, f"No owned containers found for {project}; refusing to delete unknown volumes.")
        inspect_ownership(component)
        volumes = docker("volume", "ls", "--quiet", capture=True).stdout.splitlines()
        for volume in volumes:
            if volume.startswith(project + "_"):
                label = docker("volume", "inspect", "--format",
                               '{{index .Labels "com.docker.compose.project"}}', volume,
                               capture=True).stdout.strip()
                require(label == project, f"Cannot verify volume ownership: {volume}")
    if (ROOT / "public-proxy" / "Caddyfile").is_file():
        containers = docker("ps", "-aq", "--filter", "label=com.docker.compose.project=sparrow-public-proxy",
                            capture=True).stdout.splitlines()
        require(containers, "Cannot verify shared proxy ownership; refusing to delete its certificates.")
        inspect_ownership("proxy")
    # No resources are changed before all ownership and state checks succeed.
    if args.firebase_file:
        selected_file = Path(args.firebase_file).expanduser().resolve(strict=True)
        require(ROOT / "control-plane" / "secrets" not in selected_file.parents,
                "Select a credential JSON stored outside the installation for reinstall.")
    print("EXPLICIT REINSTALL: clearing the owned Combined deployment, including all its data and identities.")
    if (ROOT / "public-proxy" / "Caddyfile").is_file():
        dc("proxy", "down", "--volumes")
    for component in ("node", "control-plane"):
        dc(component, "down", "--volumes")
    (ROOT / "public-proxy" / "Caddyfile").unlink(missing_ok=True)
    for component in ("node", "control-plane"):
        directory = ROOT / PROJECTS[component][0]
        runtime(component).unlink(missing_ok=True)
        for child in (directory / "secrets").iterdir():
            if child.is_file() and child.name != ".gitignore":
                child.unlink()
        (directory / "sparrow.conf").write_text("CONFIGURED=false\nMODE=lan\nPUBLIC_DOMAIN=\nSHARED_PROXY=false\n", encoding="utf-8")
    # Initial bootstrap runs through exactly the same path as a fresh install.
    install(args)


def verify_combined_public():
    """Do not report healthy Combined Public before Node appears in CP directory."""
    if not all(runtime(part).is_file() for part in ("node", "control-plane")):
        return
    cp = config("control-plane")
    node = config("node")
    if cp.get("MODE") != "public" or node.get("MODE") != "public":
        return
    cp_host = cp.get("PUBLIC_DOMAIN", "")
    node_host = node.get("PUBLIC_DOMAIN", "")
    require(cp_host and node_host, "Combined Public installation has missing advertised hostnames.")
    import time
    last = "No signed Node directory response yet"
    for _ in range(12):
        try:
            with urlopen(f"https://{cp_host}/v1/nodes", timeout=8) as response:
                payload = json.load(response)
            # Accept any supported directory envelope, but require the installed
            # Node's advertised public address to be visible in the response.
            serialized = json.dumps(payload)
            if f"{node_host}/v1/gateway" in serialized and "signature" in serialized.lower():
                print("Combined Public: signed directory advertises installed Community Node.")
                return
            last = "Directory did not advertise the installed Node gateway or signing metadata."
        except Exception as exc:
            last = str(exc)
        time.sleep(2)
    raise InstallerError("Combined Public is running but Node registration is not verified: " + last)


class InstallerError(Exception):
    pass


def require(condition, message):
    if not condition:
        raise InstallerError(message)


def run(args, *, cwd=None, capture=False, check=True):
    try:
        return subprocess.run(args, cwd=cwd, check=check, text=True,
                              stdout=subprocess.PIPE if capture else None,
                              stderr=subprocess.PIPE if capture else None)
    except FileNotFoundError as exc:
        raise InstallerError(f"Command unavailable: {args[0]}") from exc
    except subprocess.CalledProcessError as exc:
        detail = (exc.stderr or exc.stdout or "").strip()
        raise InstallerError(f"Command failed: {args[0]} (exit {exc.returncode}) {detail}") from exc


def docker(*args, capture=False, check=True):
    return run(["docker", *args], capture=capture, check=check)


def check_docker():
    require(shutil.which("docker"), "Docker CLI required (Docker Desktop or Engine).")
    docker("info", capture=True)
    version = docker("compose", "version", "--short", capture=True).stdout
    match = re.search(r"(\d+)\.(\d+)\.(\d+)", version)
    require(match and tuple(map(int, match.groups())) >= (2, 24, 4),
            "Docker Compose 2.24.4 or newer required.")


def config(component):
    path = ROOT / PROJECTS[component][0] / "sparrow.conf"
    require(path.is_file(), f"Missing bundle config: {path}")
    values = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values


def runtime(component):
    return ROOT / PROJECTS[component][0] / ".env.runtime"


def compose(component):
    directory = ROOT / PROJECTS[component][0]
    if component == "proxy":
        require((directory / "Caddyfile").is_file(), "Shared proxy has not been installed.")
        return ["docker", "compose", "-f", str(directory / "docker-compose.yml")]
    require(runtime(component).is_file(), f"{component} is not installed here. Use 'install' first.")
    settings = config(component)
    files = ["docker-compose.yml", "docker-compose.release.yml"]
    if settings.get("MODE") == "public":
        files.append("docker-compose.production.yml")
        if settings.get("SHARED_PROXY") == "true":
            files.append("docker-compose.shared-proxy.yml")
    if component == "control-plane" and (directory / "secrets" / "firebase-admin.json").is_file():
        files.append("docker-compose.firebase.yml")
    args = ["docker", "compose", "--env-file", str(runtime(component))]
    for name in files:
        require((directory / name).is_file(), f"Missing {directory / name}")
        args.extend(("-f", str(directory / name)))
    return args


def dc(component, *args):
    return run([*compose(component), *args], cwd=ROOT / PROJECTS[component][0])


def selected(name):
    if name == "combined":
        return ["control-plane", "node"]
    return [name]


def inspect_ownership(component):
    name = PROJECTS[component][1]
    target = (ROOT / PROJECTS[component][0]).resolve()
    result = docker("ps", "--all", "--filter", f"label=com.docker.compose.project={name}",
                    "--format", '{{.Label "com.docker.compose.project.working_dir"}}', capture=True)
    for raw in result.stdout.splitlines():
        workdir = raw.strip()
        require(workdir, f"Cannot verify working-directory ownership for {name}.")
        # Docker Desktop can return a container-side bind-mount path; never claim
        # that a differently named or unresolvable source directory belongs here.
        try:
            actual = Path(workdir).resolve(strict=True)
        except (OSError, RuntimeError):
            raise InstallerError(f"Cannot verify Compose ownership: {name} / {workdir}. Use original installation.")
        require(actual == target, f"{name} belongs to {actual}; this bundle is {target}. Refusing to take over.")


def check_new(component):
    if runtime(component).exists():
        inspect_ownership(component)
        return
    name = PROJECTS[component][1]
    inspect_ownership(component)
    volumes = docker("volume", "ls", "--quiet", capture=True).stdout.splitlines()
    require(not any(volume.startswith(f"{name}_") for volume in volumes),
            f"Existing {name} volumes found but original runtime config is missing. Do not reinitialize.")


def check_proxy_ownership():
    inspect_ownership("proxy")
    if not (ROOT / "public-proxy" / "Caddyfile").is_file():
        existing = docker("volume", "ls", "--quiet", capture=True).stdout.splitlines()
        require(not any(vol.startswith("sparrow-public-proxy_") for vol in existing),
                "Existing shared proxy TLS volumes without local configuration; refusing to overwrite.")


def validate_hosts(node, cp):
    for host in (node, cp):
        if host:
            require(bool(DOMAIN.fullmatch(host)), f"Invalid public DNS hostname: {host}")
    require(not (node and cp and node == cp), "Node and Control Plane require separate Public hostnames.")


def reject_public_port_conflicts():
    # Only guard new proxy installation; existing proxy lifecycle is independent.
    result = docker("ps", "--format", "{{.Names}} {{.Ports}}", capture=True).stdout
    require(not any(re.search(r"(?:0\.0\.0\.0|\[::\]):(?:80|443)->", line)
                    for line in result.splitlines()),
            "Public 80/443 already published by Docker. Stop only the unrelated test deployment manually.")
    if shutil.which("lsof"):
        for port in (80, 443):
            listener = run(["lsof", "-nP", "-iTCP:" + str(port), "-sTCP:LISTEN"],
                           capture=True, check=False)
            require(listener.returncode != 0 and not listener.stdout.strip(),
                    f"Host TCP {port} already in use.")


def write_config(component, updates):
    location = ROOT / PROJECTS[component][0] / "sparrow.conf"
    require(not runtime(component).exists(), f"{component} is installed; no implicit reconfiguration permitted.")
    existing = config(component)
    existing.update(updates)
    lines = ["# Sparrow fresh-install configuration; preserve this folder and its secrets."]
    for key in sorted(existing):
        value = existing[key]
        # Values are sourced by existing Bash bootstraps. Forbid shell metacharacters
        # rather than allowing untrusted text to become a shell expression.
        require(bool(re.fullmatch(r"[A-Za-z0-9._:/@+,-]*", value)),
                f"Unsupported configuration characters in {key}.")
        lines.append(f"{key}={value}")
    temporary = location.with_suffix(".conf.pending")
    temporary.write_text("\n".join(lines) + "\n", encoding="utf-8")
    temporary.replace(location)


def ensure_public_network():
    result = docker("network", "inspect", NETWORK, capture=True, check=False)
    if result.returncode != 0:
        docker("network", "create", "--driver", "bridge", NETWORK)


def routes(component):
    directory = ROOT / PROJECTS[component][0]
    source = (directory / "Caddyfile").read_text(encoding="utf-8")
    if component == "node":
        marker = "{$COMMUNITY_NODE_SITE_ADDRESS}"
        aliases = {"gateway:8094": "sparrow-node-gateway:8094",
                   "federation:8093": "sparrow-node-federation:8093",
                   "mailbox:8092": "sparrow-node-mailbox:8092"}
        root = "/srv/community-node"
    else:
        marker = "{$CONTROL_PLANE_SITE_ADDRESS}"
        aliases = {"node-registry:8090": "sparrow-control-registry:8090",
                   "presence-directory:8091": "sparrow-control-presence:8091",
                   "push:8095": "sparrow-control-push:8095"}
        root = "/srv/control-plane"
    require(marker in source, f"Unexpected component Caddyfile format: {directory}")
    prefix = source.split(marker, 1)[0]
    for old, new in aliases.items():
        require(old in prefix, f"Unexpected upstream {old} in {directory}")
        prefix = prefix.replace(old, new)
    require("root * /srv" in prefix, f"Unexpected document root in {directory}")
    return prefix.replace("root * /srv", f"root * {root}").rstrip()


def render_proxy(entries):
    require(entries, "No Public components configured.")
    validate_hosts(next((host for component, host in entries if component == "node"), ""),
                   next((host for component, host in entries if component == "control-plane"), ""))
    output = []
    for component, _ in entries:
        output.append(routes(component))
    for component, hostname in entries:
        snippet = "sparrow_community_routes" if component == "node" else "sparrow_control_routes"
        output.append(f"{hostname} {{\n    import {snippet}\n}}")
    return "\n\n".join(output) + "\n"


def installed_public():
    result = []
    for component in ("control-plane", "node"):
        if runtime(component).is_file():
            settings = config(component)
            if settings.get("MODE") == "public" and settings.get("SHARED_PROXY") == "true":
                result.append((component, settings.get("PUBLIC_DOMAIN", "")))
    return result


BACKEND_SERVICES = {"node": ("mailbox", "federation", "gateway"),
                    "control-plane": ("node-registry", "presence-directory", "push")}

def update_installed_properties(path, updates):
    """Atomically change only selected key/value settings, retaining all secrets."""
    require(path.is_file(), f"Installed settings not found: {path}")
    lines = path.read_text(encoding="utf-8").splitlines()
    existing = dict(item.split("=", 1) for item in lines if "=" in item and not item.startswith("#"))
    if all(existing.get(key) == value for key, value in updates.items()):
        return False
    for key, value in updates.items():
        require(re.fullmatch(r"[A-Z_]+", key) and "\n" not in value and "\r" not in value,
                f"Invalid installed configuration update: {key}")
    output = []
    remaining = dict(updates)
    for line in lines:
        key = line.split("=", 1)[0]
        if key in remaining and not line.startswith("#"):
            output.append(f"{key}={remaining.pop(key)}")
        else:
            output.append(line)
    output.extend(f"{key}={value}" for key, value in remaining.items())
    backup = path.with_name(path.name + ".previous")
    shutil.copy2(path, backup)
    tmp = path.with_name(path.name + ".pending")
    tmp.write_text("\n".join(output) + "\n", encoding="utf-8")
    tmp.chmod(path.stat().st_mode & 0o777)
    tmp.replace(path)
    return True


def sync_installed_node_discovery(directory_url):
    """Refresh gateway-advertised Control Planes, not just the initial bootstrap."""
    if not runtime("node").is_file():
        return
    settings = config("node")
    values = {}
    for line in runtime("node").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            values[key] = value
    client = urlparse(values.get("CLIENT_ENDPOINT", ""))
    require(client.scheme in ("ws", "wss") and client.hostname,
            "Installed node has no valid client endpoint; cannot publish Control Plane addresses.")
    mode = settings.get("MODE")
    require(mode in ("lan", "public"), "Installed node has invalid network mode.")
    selected_directory = directory_url or settings.get("CONTROL_PLANE_DIRECTORY_URL", "")
    raw = [u.strip() for u in settings.get("CONTROL_PLANE_URLS", "").replace(";", ",").split(",") if u.strip()]
    # Runtime routing uses ONLY explicit local/manual endpoints as its static
    # fallback. Signed directory entries belong to the dynamic, revocable
    # worker publication, not to CONTROL_PLANE_URLS forever.
    explicit_raw = list(raw)
    if selected_directory:
        key = (settings.get("CONTROL_PLANE_DIRECTORY_PUBLIC_KEY") or
               config("control-plane").get("CONTROL_PLANE_DIRECTORY_PUBLIC_KEY", ""))
        if not key:
            print("Signed Control Plane discovery disabled: no pinned directory verification key; keeping local/manual addresses.")
        else:
            try:
                # Cryptography is optional for a self-contained local combined
                # installation; never parse unverified remote data if absent.
                from control_plane_directory_client import DirectoryError, discover
                candidates, cache_status = discover(
                    selected_directory, key,
                    ROOT / "community-node" / ".control-plane-directory-verified.json")
                raw.extend(candidates)
                print("Control Plane directory:", cache_status)
            except (ImportError, ValueError, OSError) as exc:
                # A network failure or invalid response must not be treated as an
                # unsigned JSON directory. Local/manual CP remains available.
                print(f"Verified directory unavailable; keeping local/manual addresses: {exc}")
    advertised, inside = [], []
    for candidate in raw:
        value = candidate.strip().rstrip("/")
        parsed = urlparse(value)
        require(parsed.scheme in ({"https"} if mode == "public" else {"http", "https"}) and
                parsed.hostname and not parsed.username and not parsed.password and not parsed.query and
                not parsed.fragment, f"Invalid Control Plane URL: {value}")
        pub, internal = value, value
        if parsed.hostname in ("localhost", "127.0.0.1", "::1"):
            require(mode == "lan", "Public Control Plane must not advertise localhost.")
            pub = f"http://{client.hostname}:{parsed.port or 80}"
            internal = f"http://host.docker.internal:{parsed.port or 80}"
        elif mode == "public" and settings.get("SHARED_PROXY") == "true" and \
                parsed.hostname == settings.get("LOCAL_CONTROL_PLANE_DOMAIN"):
            internal = "http://sparrow-control-edge:8080"
        if pub not in advertised:
            advertised.append(pub)
        if internal not in inside:
            inside.append(internal)
    require(inside, "No available Control Planes; installed node unchanged.")
    static_inside = []
    for candidate in explicit_raw:
        value = candidate.strip().rstrip("/")
        parsed = urlparse(value)
        internal = ("http://host.docker.internal:" + str(parsed.port or 80)
                    if parsed.hostname in ("localhost", "127.0.0.1", "::1")
                    else "http://sparrow-control-edge:8080"
                    if mode == "public" and settings.get("SHARED_PROXY") == "true" and
                    parsed.hostname == settings.get("LOCAL_CONTROL_PLANE_DOMAIN")
                    else value)
        if internal not in static_inside:
            static_inside.append(internal)
    require(static_inside, "Combined installation has no configured local Control Plane.")
    updates = {"CONTROL_PLANE_URL": static_inside[0], "CONTROL_PLANE_URLS": ",".join(static_inside),
               "ADVERTISED_CONTROL_PLANE_URLS": ",".join(advertised),
               "LOCAL_CONTROL_PLANE_DOMAIN": settings.get("LOCAL_CONTROL_PLANE_DOMAIN", ""),
               "MANUAL_CONTROL_PLANE_URLS": settings.get("CONTROL_PLANE_URLS", "")}
    # Only configured local/manual URLs plus authenticated snapshot entries
    # are advertised. Never restore opaque, previously unverified runtime URLs
    # on an outage; the signed persistent cache above is the offline fallback.
    existing_config = settings.get("CONTROL_PLANE_DIRECTORY_URL", "")
    runtime_changed = any(values.get(key) != value for key, value in updates.items())
    if not runtime_changed and existing_config == selected_directory:
        return
    inspect_ownership("node")
    dc("node", "config", "--quiet")
    update_installed_properties(ROOT / "community-node" / "sparrow.conf",
                                {"CONTROL_PLANE_DIRECTORY_URL": selected_directory})
    update_installed_properties(runtime("node"), updates)
    dc("node", "config", "--quiet")
    # Reload gateway /v1/control-planes from the existing identity and volumes,
    # without depending on GHCR for a configuration-only correction.
    dc("node", "up", "-d", "--no-deps", "--pull", "never", *BACKEND_SERVICES["node"])
    print("Refreshed installed node discovery:", updates["ADVERTISED_CONTROL_PLANE_URLS"])



def update_backends(component, image_prefix, image_tag):
    require(component in BACKEND_SERVICES, "Only backend components support image updates.")
    inspect_ownership(component)
    settings = config(component)
    require(settings.get("SPARROW_IMAGE_PREFIX") == image_prefix and settings.get("SPARROW_IMAGE_TAG") == image_tag,
            "Existing installation image selection differs from requested values. Select the installed prefix/tag for this update.")
    project = PROJECTS[component][1]
    for service in BACKEND_SERVICES[component]:
        existing = docker("ps", "-aq", "--filter", f"label=com.docker.compose.project={project}",
                          "--filter", f"label=com.docker.compose.service={service}", capture=True).stdout.strip()
        require(existing, f"{project} is missing service {service}; refusing to create a partial replacement.")
    dc(component, "config", "--quiet")
    print(f"Pulling existing {component} backend images from GHCR ...")
    dc(component, "pull", *BACKEND_SERVICES[component])
    dc(component, "up", "-d", "--no-deps", "--pull", "never", *BACKEND_SERVICES[component])


def refresh_installed_control_plane_directory():
    """Cache a cryptographically verified directory for this Control Plane.

    This read-only operation is best effort, never writes Control Plane identity,
    and retains an earlier verified snapshot on central directory failure.
    """
    if not runtime("control-plane").is_file():
        return
    settings = config("control-plane")
    url = settings.get("CONTROL_PLANE_DIRECTORY_URL", "")
    pin = settings.get("CONTROL_PLANE_DIRECTORY_PUBLIC_KEY", "")
    if not url or not pin:
        return
    try:
        from control_plane_directory_client import discover
        _, state = discover(url, pin, ROOT / "control-plane" / ".control-plane-directory-verified.json")
        print("Control Plane signed directory:", state)
    except (ImportError, OSError, ValueError) as exc:
        print("Control Plane directory refresh deferred; local Control Plane remains available: " + str(exc)[:200])


def start_directory_sync_best_effort():
    """Start the persistent Docker worker separately from message transport.

    The image is built from public verifier files only. Build/polling failures
    are never fatal to running Caddy, node, or Control Plane containers.
    """
    cp = config("control-plane")
    if (cp.get("MODE") != "public" or not cp.get("CONTROL_PLANE_DIRECTORY_URL") or not cp.get("CONTROL_PLANE_DIRECTORY_PUBLIC_KEY")):
        return
    if not runtime("node").is_file() or not (ROOT / "public-proxy" / "Caddyfile").is_file():
        return
    try:
        result = run([*compose("proxy")[:], "--profile", "directory", "up", "-d", "--no-deps", "directory-sync"],
                     cwd=ROOT / "public-proxy", capture=True, check=False)
        if result.returncode:
            print("Directory background refresh unavailable; paired server remains online: " +
                  ((result.stderr or result.stdout) or "worker image not available")[:250])
        else:
            print("Directory background synchronization enabled (15-minute interval).")
    except (OSError, ValueError, subprocess.TimeoutExpired, InstallerError) as exc:
        print("Directory background refresh deferred; paired server remains online: " + str(exc)[:200])


def register_installed_public_control_plane():
    """Best effort only: a central directory outage must never block messaging.

    Provisioned URL + independently pinned signer opt the public plane in.
    Directory approval remains a separate action performed by its operator.
    """
    cp = config("control-plane")
    if cp.get("MODE") != "public" or not runtime("control-plane").is_file():
        return
    url = cp.get("CONTROL_PLANE_DIRECTORY_URL", "")
    pin = cp.get("CONTROL_PLANE_DIRECTORY_PUBLIC_KEY", "")
    hostname = cp.get("PUBLIC_DOMAIN", "")
    if not url or not pin or not hostname:
        return
    identity = ROOT / "control-plane" / "secrets" / "registry-root.identity"
    if not identity.is_file():
        print("Directory registration skipped: existing Control Plane root identity not ready.")
        return
    if not (ROOT / "public-proxy" / "Caddyfile").is_file():
        print("Directory registration skipped: public HTTPS proxy is not ready.")
        return
    marker = ROOT / "control-plane" / ".directory-registration-last-attempt"
    import time
    if (marker.is_file() and marker.stat().st_mtime >= max(
            identity.stat().st_mtime, (ROOT / "control-plane" / "sparrow.conf").stat().st_mtime)
            and time.time() - marker.stat().st_mtime < 9 * 60):
        return
    helper = ROOT / "control_plane_directory_registration.py"
    if not helper.is_file():
        print("Directory registration client missing; local Control Plane remains available.")
        return
    # The manager needs only its standard-library Python. Ed25519 lives in a
    # tightly scoped one-shot Docker client; no cryptography on the host.
    # DO NOT use --env-file control-plane/sparrow.conf: it may hold other data.
    # Only three explicit, non-admin configuration values cross this boundary.
    cmd_base = [*compose("proxy"), "--profile", "registration"]
    built = run([*cmd_base, "build", "directory-register"],
                cwd=ROOT / "public-proxy", capture=True, check=False)
    if built.returncode:
        print("Directory registration deferred: one-shot client image unavailable; local server remains online: " +
              ((built.stderr or built.stdout) or "Docker build failed")[:230])
        return
    result = run([*cmd_base, "run", "--rm", "--no-deps",
                  "-e", "CONTROL_PLANE_DIRECTORY_URL=" + url,
                  "-e", "CONTROL_PLANE_DIRECTORY_PUBLIC_KEY=" + pin,
                  "-e", "SPARROW_REGISTRATION_PLANE_URL=https://" + hostname,
                  "directory-register"], cwd=ROOT / "public-proxy",
                 capture=True, check=False)
    if result.returncode:
        print("Directory registration deferred (local server remains online): " +
              (result.stderr.strip() or "registration client failed")[:250])
        return
    print("Directory registration: " + result.stdout.strip()[:220])
    marker.touch()


def install(args):
    components = selected(args.component)
    require(args.component == "combined", "Sparrow installation requires Combined (Control Plane + Community Node).")
    # Reuse the installed directory bootstrap if the operator leaves the field
    # unspecified on a normal update; a provided URL overrides it explicitly.
    args.directory_url = (args.directory_url or config("node").get("CONTROL_PLANE_DIRECTORY_URL", "")
                          or config("control-plane").get("CONTROL_PLANE_DIRECTORY_URL", ""))
    validate_hosts(args.node_domain, args.control_domain)
    if args.mode == "public" and (ROOT / "public-proxy" / "Caddyfile").is_file():
        # Do not start a new backend before discovering that its hostname would
        # need an unreviewed live proxy reconfiguration.
        require(not any(not runtime(component).is_file() for component in components),
                "Adding a Public component to an existing proxy requires a reviewed route/TLS cutover; no files changed.")
    require(bool(IMAGE_PREFIX.fullmatch(args.image_prefix)), "Invalid image prefix.")
    require(bool(IMAGE_TAG.fullmatch(args.image_tag)), "Invalid image tag.")
    if args.mode == "public":
        require(not ("node" in components and not args.node_domain), "--node-domain required in Public mode.")
        require(not ("control-plane" in components and not args.control_domain),
                "--control-domain required in Public mode.")
        for component, hostname in installed_public():
            expected = args.node_domain if component == "node" else args.control_domain
            if component in components:
                require(hostname == expected, f"Cannot change installed {component} Public hostname implicitly.")
            elif component == "node":
                require(hostname != args.control_domain, "Cannot reuse the installed Node hostname.")
            else:
                require(hostname != args.node_domain, "Cannot reuse the installed Control Plane hostname.")
        check_proxy_ownership()
    for component in components:
        check_new(component)
        # An installed node can refresh its directory with the same Install
        # operation; node identity and storage must remain unchanged.
        if runtime(component).exists():
            current = config(component)
            require(current.get("MODE") == args.mode and
                    current.get("SHARED_PROXY", "false") == ("true" if args.mode == "public" else "false"),
                    f"{component} already installed in another mode; reconfiguration needs a guarded migration.")
    if args.directory_url:
        from urllib.parse import urlparse
        parsed = urlparse(args.directory_url)
        require(parsed.scheme in ({"https"} if args.mode == "public" else {"http", "https"})
                and parsed.netloc and not parsed.username and not parsed.password and not parsed.fragment,
                "Invalid Control Plane directory URL (HTTPS required in Public mode).")
    if args.component == "node" and not args.directory_url and not runtime("node").is_file():
        raise InstallerError("Node-only install requires --directory-url pointing to a trusted Control Plane.")
    if args.firebase_file:
        require("control-plane" in components, "--firebase-file requires Control Plane.")
        source = Path(args.firebase_file).expanduser().resolve(strict=True)
        document = json.loads(source.read_text(encoding="utf-8"))
        require(document.get("type") == "service_account" and document.get("private_key") and document.get("client_email"),
                "Expected service account JSON. Authorization to Android Firebase project must be configured separately.")
    if args.mode == "public" and not (ROOT / "public-proxy" / "Caddyfile").is_file():
        reject_public_port_conflicts()
    # Refresh discovery even if an unrelated Control Plane image pull later
    # fails. Previously a changed JSON directory never reached the running
    # gateway, which kept advertising old LAN IPs to Android clients.
    if "node" in components and runtime("node").is_file():
        sync_installed_node_discovery(args.directory_url)
    # Read-only preflight above is complete; no existing runtime or old volume is overwritten.
    if runtime("control-plane").is_file():
        update_installed_properties(ROOT / "control-plane" / "sparrow.conf",
                                    {"CONTROL_PLANE_DIRECTORY_URL": args.directory_url})
    if args.mode == "public":
        # Docker must mount a user-writable, non-secret proof directory; do not
        # let Compose create an unwritable root-owned host directory.
        (ROOT / "control-plane" / "directory-registration-proofs").mkdir(
            parents=True, exist_ok=True)
        ensure_public_network()
    if "control-plane" in components and not runtime("control-plane").exists():
        write_config("control-plane", {"CONFIGURED": "true", "MODE": args.mode,
                      "PUBLIC_DOMAIN": args.control_domain, "SHARED_PROXY": "true" if args.mode == "public" else "false",
                      "CONTROL_PLANE_DIRECTORY_URL": args.directory_url,
                      "CONTROL_PLANE_DIRECTORY_PUBLIC_KEY": config("control-plane").get("CONTROL_PLANE_DIRECTORY_PUBLIC_KEY", ""),
                      "SPARROW_IMAGE_PREFIX": args.image_prefix, "SPARROW_IMAGE_TAG": args.image_tag})
        if args.firebase_file:
            dest = ROOT / "control-plane" / "secrets" / "firebase-admin.json"
            dest.parent.mkdir(exist_ok=True, parents=True)
            require(not dest.exists(), "Existing Firebase file must not be replaced automatically.")
            shutil.copyfile(Path(args.firebase_file).expanduser(), dest)
            dest.chmod(0o600)
        run(["bash", str(ROOT / "control-plane" / "bootstrap-control-plane.sh")])
    if "node" in components and not runtime("node").exists():
        cp_url = (f"https://{args.control_domain}" if args.mode == "public" else "http://localhost:8390") if args.component == "combined" else ""
        write_config("node", {"CONFIGURED": "true", "MODE": args.mode, "PUBLIC_DOMAIN": args.node_domain,
                      "SHARED_PROXY": "true" if args.mode == "public" else "false",
                      "LOCAL_CONTROL_PLANE_DOMAIN": args.control_domain if args.component == "combined" and args.mode == "public" else "",
                      "CONTROL_PLANE_DIRECTORY_URL": args.directory_url,
                      "CONTROL_PLANE_DIRECTORY_PUBLIC_KEY": (config("node").get("CONTROL_PLANE_DIRECTORY_PUBLIC_KEY", "") or
                                                             config("control-plane").get("CONTROL_PLANE_DIRECTORY_PUBLIC_KEY", "")),
                      "CONTROL_PLANE_URLS": cp_url,
                      "SPARROW_IMAGE_PREFIX": args.image_prefix, "SPARROW_IMAGE_TAG": args.image_tag})
        run(["bash", str(ROOT / "community-node" / "bootstrap-community-node.sh")])
    if args.mode == "public":
        entries = installed_public()
        candidate = render_proxy(entries)
        proxy = ROOT / "public-proxy" / "Caddyfile"
        if proxy.exists() and proxy.read_text(encoding="utf-8") != candidate:
            # Allow ONLY this revision's additive proof route on an already
            # configured Combined installation. Keep all existing hostnames,
            # HTTPS certificates, identities and any unrelated routing intact.
            previous = candidate.replace(
                "    # Serve only short-lived proof files from a dedicated non-secret directory.\n"
                "    # A separate proxy mounts this directory at the same absolute location.\n"
                "    @directory_registration path_regexp directory_registration ^/\\.well-known/sparrow-directory-registration/[A-Za-z0-9_-]{32}$\n"
                "    handle @directory_registration {\n"
                "        root * /srv/control-plane/directory-proofs\n"
                "        header Cache-Control \"no-store\"\n"
                "        file_server\n"
                "    }\n\n", "")
            require(proxy.read_text(encoding="utf-8") == previous,
                    "Existing Public proxy routes differ. Refusing automatic mutation; use a reviewed cutover.")
            proxy.write_text(candidate, encoding="utf-8")
        elif not proxy.exists():
            proxy.write_text(candidate, encoding="utf-8")
        # The shared Caddy container binds this dedicated public-only folder read-only.
        # Never mount the Control Plane secrets directory into the public proxy.
        (ROOT / "control-plane" / "directory-registration-proofs").mkdir(parents=True, exist_ok=True)
        existing_proxy = docker("ps", "--all", "--filter", "label=com.docker.compose.project=sparrow-public-proxy",
                                "--format", "{{.ID}}", capture=True).stdout.strip()
        # Compose up recreates Caddy only when needed (new read-only proof mount)
        # and retains its persistent certificate volumes.
        dc("proxy", "up", "-d")
    # Configure new credentials on an already installed Control Plane before the
    # normal backend update. A freshly bootstrapped CP already imported them.
    if args.firebase_file and runtime("control-plane").is_file():
        enable_firebase_credentials(args.firebase_file)
    # Install / Start always refreshes existing Sparrow backend images from GHCR.
    # No database/Redis/Caddy image updates, no re-bootstrap or new identities.
    for component in components:
        if runtime(component).is_file():
            update_backends(component, args.image_prefix, args.image_tag)
    public_addresses()
    start_directory_sync_best_effort()
    refresh_installed_control_plane_directory()
    if args.mode == "public":
        try:
            register_installed_public_control_plane()
        except (OSError, ValueError, subprocess.TimeoutExpired) as exc:
            print("Directory registration deferred; local server remains available: " + str(exc)[:200])
    if args.verify_public and args.component == "combined" and args.mode == "public":
        verify_combined_public()
    print("Sparrow installed/started; backend images checked via GHCR. Verify health and messaging on real devices.")


def lifecycle(args):
    components = selected(args.component)
    for component in components:
        if component == "proxy":
            if args.action in {"start", "restart", "stop"}:
                check_proxy_ownership()
        else:
            require(runtime(component).is_file(), f"{component} not installed in this bundle.")
            inspect_ownership(component)
    for component in components:
        if args.action == "status":
            dc(component, "ps", "--all")
        elif args.action == "logs":
            dc(component, "logs", "--no-color", "--tail", "120")
        elif args.action == "stop":
            dc(component, "stop")
        elif args.action in {"start", "restart"}:
            dc(component, "start")
    # Combined stop/start only touches proxy when both backends are selected.
    if args.component == "combined" and (ROOT / "public-proxy" / "Caddyfile").is_file():
        if args.action == "stop":
            # Profiled services are NOT included in an ordinary compose stop.
            # Stop the autonomous worker as well when the combined server stops.
            run([*compose("proxy"), "--profile", "directory", "stop", "directory-sync"],
                cwd=ROOT / "public-proxy", check=False, capture=True)
            dc("proxy", "stop")
        elif args.action in {"start", "restart"}:
            dc("proxy", "start")
        elif args.action in {"status", "logs"}:
            dc("proxy", "ps", "--all") if args.action == "status" else dc("proxy", "logs", "--no-color", "--tail", "120")
    if args.action in {"start", "restart"} and "control-plane" in components:
        start_directory_sync_best_effort()
        refresh_installed_control_plane_directory()
        if config("control-plane").get("MODE") == "public":
            try:
                register_installed_public_control_plane()
            except (OSError, ValueError, subprocess.TimeoutExpired) as exc:
                print("Directory registration deferred; server is still running: " + str(exc)[:200])


def parse_args():
    parser = argparse.ArgumentParser(description="Sparrow unified server — macOS/Linux terminal manager")
    parser.add_argument("action", choices=("install", "reinstall-public", "start", "stop", "restart", "status", "logs", "preflight"))
    parser.add_argument("--component", choices=("combined", "node", "control-plane", "proxy"), default="combined",
                        help="install: combined only; individual components are for internal lifecycle diagnostics")
    parser.add_argument("--mode", choices=("lan", "public"), default=None)
    parser.add_argument("--node-domain", default="")
    parser.add_argument("--control-domain", default="")
    parser.add_argument("--directory-url", default="")
    parser.add_argument("--image-prefix", default="ghcr.io/cbgm/sparrow")
    parser.add_argument("--image-tag", default="latest")
    parser.add_argument("--firebase-file", default="", help="authorized Firebase Admin service-account JSON")
    parser.add_argument("--auto-dns", action="store_true", help="generate free sslip.io hostnames from WAN IPv4")
    parser.add_argument("--public-ip", default="", help="optional explicit public IPv4 for automatic DNS")
    parser.add_argument("--verify-public", action="store_true", help="require advertised public Node in the signed CP directory")
    parser.add_argument("--confirm-delete-data", action="store_true", help="explicit authorization for destructive Public reinstall")
    return parser.parse_args()


def main():
    args = parse_args()
    if args.mode is None:
        installed = [config(part).get("MODE") for part in selected(args.component) if part != "proxy" and runtime(part).is_file()]
        args.mode = installed[0] if installed and all(mode == installed[0] for mode in installed) else "lan"
    try:
        require(sys.platform.startswith("linux") or sys.platform == "darwin",
                "For Windows use Start-SparrowServer.cmd (native Windows manager).")
        require(not (ROOT / ".sparrow-attached.json").exists(),
                "This bundle is attached to a legacy cutover. Use its original Windows manager, not the fresh-install CLI.")
        # Prevent overlapping install, proxy edits, and lifecycle actions.
        with (ROOT / ".sparrow-manager.lock").open("a+") as manager_lock:
            try:
                fcntl.flock(manager_lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError as exc:
                raise InstallerError("Another Sparrow manager command is active in this directory.") from exc
            check_docker()
            if args.action == "preflight":
                for component in selected(args.component):
                    check_proxy_ownership() if component == "proxy" else check_new(component)
                    print(f"{component}: {'installed' if component != 'proxy' and runtime(component).exists() else 'not installed here'}")
            elif args.action in ("install", "reinstall-public"):
                resolve_public_hosts(args)
                if args.action == "reinstall-public":
                    reinstall_public(args)
                else:
                    install(args)
            else:
                lifecycle(args)
                if args.action == "status":
                    public_addresses()
    except (InstallerError, ValueError, OSError, json.JSONDecodeError) as exc:
        print(f"Sparrow: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    log_root = ROOT / "logs"
    log_root.mkdir(parents=True, exist_ok=True)
    os.chmod(log_root, 0o700)
    log_file = log_root / ("sparrow-server-" + datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ") + f"-{os.getpid()}.log")
    with log_file.open("x", encoding="utf-8") as logfile:
        os.chmod(log_file, 0o600)
        with redirect_stdout(Tee(sys.stdout, logfile)), redirect_stderr(Tee(sys.stderr, logfile)):
            print("Sparrow log:", log_file)
            sys.exit(main())
