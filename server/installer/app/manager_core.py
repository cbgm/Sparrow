#!/usr/bin/env python3
"""Shared state and commands of the Sparrow Server manager (window and terminal).

Every action runs the platform engine: PowerShell on Windows, Python on Linux/macOS.
"""
from __future__ import annotations

import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

APP = Path(__file__).resolve().parent
ROOT = APP.parent
WINDOWS = os.name == "nt"
# Started from the source code (server/installer) instead of an installed server folder.
SOURCE_TREE = (ROOT / "build.py").is_file()
PREFERENCES = ROOT / ".sparrow-manager.json"
PROJECTS = {"sparrow-control-plane", "sparrow-community-node"}
ONE_SHOT_SERVICES = {"blob-storage-init", "registry-signing-init"}
SSLIP = r"-(\d{1,3}-){3}\d{1,3}\.sslip\.io"


# ------------------------------------------------------------------ installed state
def properties(path: Path) -> dict[str, str]:
    values = {}
    if path.is_file():
        for line in path.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()
    return values


def installed() -> bool:
    return all((ROOT / part / ".env.runtime").is_file() for part in ("control-plane", "community-node"))


def local_build_tag() -> str:
    manifest = APP / "local-images.json"
    if manifest.is_file():
        return json.loads(manifest.read_text(encoding="utf-8")).get("tag", "")
    return ""


def load_preferences() -> dict:
    try:
        return json.loads(PREFERENCES.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def save_preferences(preferences: dict) -> None:
    PREFERENCES.write_text(json.dumps(preferences, indent=2), encoding="utf-8")


def default_settings() -> dict:
    cp = properties(ROOT / "control-plane" / "sparrow.conf")
    node = properties(ROOT / "community-node" / "sparrow.conf")
    automatic = cp.get("PUBLIC_HOSTNAME_MODE") == "CaddyAutomatic" or bool(
        re.fullmatch("control" + SSLIP, cp.get("PUBLIC_DOMAIN", ""))) or not cp.get("PUBLIC_DOMAIN")
    return {
        "mode": cp.get("MODE", "lan") if installed() or cp.get("CONFIGURED") == "true" else "public",
        "autoDns": automatic,
        "nodeDomain": node.get("PUBLIC_DOMAIN", ""),
        "controlDomain": cp.get("PUBLIC_DOMAIN", ""),
        "directoryUrl": node.get("CONTROL_PLANE_DIRECTORY_URL") or cp.get("CONTROL_PLANE_DIRECTORY_URL", ""),
        "push": True,
        "firebaseFile": "",
        "imagePrefix": cp.get("SPARROW_IMAGE_PREFIX", "ghcr.io/cbgm/sparrow"),
        "imageTag": cp.get("SPARROW_IMAGE_TAG", "latest"),
        # "combined" (Control Plane + Community Node) or "node" (Community Node only).
        "component": "combined",
    }


def server_state() -> tuple[str, str]:
    """Short status text and its level (ok, warn, error, idle), from a quick read-only docker ps."""
    if not installed():
        return "Not installed", "idle"
    if not shutil.which("docker"):
        return "Docker missing", "error"
    try:
        result = subprocess.run(
            ["docker", "ps", "--all", "--filter", "label=com.docker.compose.project", "--format",
             '{{.Label "com.docker.compose.project"}}|{{.Label "com.docker.compose.service"}}|{{.State}}'],
            capture_output=True, text=True, timeout=8)
    except (OSError, subprocess.TimeoutExpired):
        return "Docker not responding", "error"
    if result.returncode:
        return "Docker not running", "error"
    states = []
    for line in result.stdout.splitlines():
        project, _, rest = line.partition("|")
        service, _, state = rest.partition("|")
        if project in PROJECTS and service not in ONE_SHOT_SERVICES:
            states.append(state)
    running = states.count("running")
    if not states:
        return "No containers", "warn"
    if running == len(states):
        return f"Running {running}/{len(states)}", "ok"
    if running == 0:
        return "Stopped", "warn"
    return f"Partly running {running}/{len(states)}", "warn"


# ------------------------------------------------------------------ engines
def powershell(script: str, parameters: dict[str, str]) -> list[str]:
    command = ["powershell.exe", "-NoLogo", "-NoProfile", "-ExecutionPolicy", "Bypass",
               "-File", str(APP / script)]
    for key, value in parameters.items():
        if value:  # Windows PowerShell drops empty native arguments
            command += [f"-{key}", value]
    return command


def python(script: str, *arguments: str) -> list[str]:
    return [sys.executable, "-u", str(APP / script), *arguments]


def engine_command(action: str, settings: dict) -> list[str]:
    command = platform_engine(action, settings)
    if action == "start" and installed() and settings["mode"] == "public" and settings["autoDns"]:
        # Automatic sslip.io names contain the public IP: follow a new address before starting.
        return python("public_ip.py", "--then", *command)
    return command


def platform_engine(action: str, settings: dict) -> list[str]:
    public = settings["mode"] == "public"
    reinstall = action == "reinstall"
    if WINDOWS:
        parameters = {"Action": "Start" if action in ("start", "reinstall") else action.capitalize(),
                      "Component": "Combined", "Mode": settings["mode"], "ResultPath": "", "FailurePath": ""}
        if action in ("start", "reinstall"):
            parameters.update({
                "PublicHostnameMode": "CaddyAutomatic" if public and settings["autoDns"] else "Manual",
                "NodeDomain": "" if settings["autoDns"] else settings["nodeDomain"],
                "ControlPlaneDomain": "" if settings["autoDns"] else settings["controlDomain"],
                "DirectoryUrl": settings["directoryUrl"],
                "ImagePrefix": settings["imagePrefix"], "ImageTag": settings["imageTag"],
                "FirebaseCredentialsPath": settings["firebaseFile"],
                "FirebaseMode": "Enabled" if settings["push"] else "Disabled",
            })
        if reinstall:
            parameters.update({"ExistingDeployment": "ReplaceTestData",
                               "ReplacementConfirmation": "DELETE SPARROW SERVER DATA"})
        return powershell("Invoke-SparrowServer.ps1", parameters)
    if action not in ("start", "reinstall"):
        return python("Invoke-SparrowServer.py", action, "--component", "combined")
    arguments = ["reinstall-public" if reinstall else "install", "--component", "combined", "--mode", settings["mode"],
                 "--image-prefix", settings["imagePrefix"], "--image-tag", settings["imageTag"]]
    if public and settings["autoDns"]:
        arguments.append("--auto-dns")
    elif public:
        arguments += ["--node-domain", settings["nodeDomain"], "--control-domain", settings["controlDomain"]]
    if settings["directoryUrl"]:
        arguments += ["--directory-url", settings["directoryUrl"]]
    if settings["push"] and settings["firebaseFile"]:
        arguments += ["--firebase-file", settings["firebaseFile"]]
    if reinstall:
        arguments.append("--confirm-delete-data")
    return python("Invoke-SparrowServer.py", *arguments)


def update_command(source: str, path: str = "", installation: Path = ROOT) -> list[str]:
    if WINDOWS:
        return powershell("Update-SparrowServer.ps1", {"From": source.capitalize(), "Path": path,
                                                       "InstallationDirectory": str(installation)})
    option = {"release": "--release", "zip": "--zip", "source": "--source", "folder": "--folder"}[source]
    arguments = [option] + ([path] if path else []) + ["--installation", str(installation)]
    return python("update_server.py", *arguments)


# An installed server's own configuration always wins; remembered choices only
# fill what it does not store (or everything before the first installation).
REMEMBERED_WHEN_INSTALLED = ("push", "firebaseFile")


def load_settings() -> tuple[dict, dict]:
    preferences = load_preferences()
    settings = default_settings()
    remembered = preferences.get("settings", {})
    keys = REMEMBERED_WHEN_INSTALLED if installed() else settings.keys()
    settings.update({key: remembered[key] for key in keys if key in remembered and key in settings})
    local = APP / "local-images.json"
    if local.is_file():
        manifest = json.loads(local.read_text(encoding="utf-8"))
        settings["imagePrefix"], settings["imageTag"] = manifest["prefix"], manifest["tag"]
    return settings, preferences


def addresses() -> dict[str, str]:
    cp = properties(ROOT / "control-plane" / "sparrow.conf")
    node = properties(ROOT / "community-node" / "sparrow.conf")
    if not installed() or cp.get("MODE") != "public":
        return {}
    return {"Control Plane": f"https://{cp.get('PUBLIC_DOMAIN', '')}",
            "Community Node": f"https://{node.get('PUBLIC_DOMAIN', '')}",
            "Gateway": f"wss://{node.get('PUBLIC_DOMAIN', '')}/v1/gateway"}


# Not a valid node ID, so it can never collide with an added node.
MAIN_NODE = "(main)"


def main_node() -> tuple[str, str, str, str, str] | None:
    """The Community Node of the Control Plane + Node installation, listed before the added nodes."""
    if not installed():
        return None
    node = properties(ROOT / "community-node" / "sparrow.conf")
    mode = node.get("MODE", "lan")
    if mode == "public":
        endpoint = f"https://{node.get('PUBLIC_DOMAIN', '')}"
    else:
        port = properties(ROOT / "community-node" / ".env.runtime").get("COMMUNITY_NODE_HTTP_PORT", "8490")
        endpoint = f"LAN port {port}"
    return MAIN_NODE, "Main node (main installation)", mode, "installed", endpoint


def version_text() -> str:
    local = local_build_tag()
    if local:
        return f"{local} (local build)"
    cp = properties(ROOT / "control-plane" / "sparrow.conf")
    return f"{cp.get('SPARROW_IMAGE_TAG', '?')} (release images)"


def node_command(*arguments: str) -> list[str]:
    return python("Manage-SparrowNodes.py", *arguments)


def describe(settings: dict) -> list[tuple[str, str]]:
    public = settings["mode"] == "public"
    rows = [("Reachability", "Public (internet)" if public else "LAN (this network only)")]
    if public:
        rows.append(("Hostnames", "automatic (sslip.io)" if settings["autoDns"] else "own domains"))
        if not settings["autoDns"]:
            rows += [("Community Node hostname", settings["nodeDomain"] or "-"),
                     ("Control Plane hostname", settings["controlDomain"] or "-")]
    rows += [("Directory Server URL", settings["directoryUrl"] or "-"),
             ("Push notifications", "on" if settings["push"] else "off"),
             ("Firebase credentials file", settings["firebaseFile"] or "keep installed"),
             ("Images", f"{settings['imagePrefix']}-*:{settings['imageTag']}")]
    return rows


def default_server_folder() -> Path:
    return Path.home() / "Sparrow Server"


def create_command(target: Path) -> list[str]:
    """Creates a server folder from the source code (uses the published server images)."""
    return [sys.executable, str(ROOT / "build.py"), "--install", str(target)]


def find_existing_installation() -> Path | None:
    """The folder of the Sparrow server already running in Docker, if it is not this one."""
    from move_installation import find_existing, same
    found = find_existing()
    return found if found is not None and not same(found, ROOT) else None


def move_command(source: Path | None = None) -> list[str]:
    return python("move_installation.py", *(["--from", str(source)] if source else []))
