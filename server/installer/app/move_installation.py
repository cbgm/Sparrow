#!/usr/bin/env python3
"""Moves a running Sparrow server installation into this folder.

The Docker projects, their named volumes (databases, identities, certificates)
and the shared network stay the same; only the folder that owns the Compose
projects changes. Runtime state (sparrow.conf, .env.runtime, secrets, caches,
proxy routes, node instances) is copied, paths inside it are rewritten, and the
containers are recreated from the new folder. On any failure the containers
are started again from the old folder.
"""
from __future__ import annotations

import argparse
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
LAYOUT = json.loads((APP / "layout.json").read_text(encoding="utf-8"))
COMPONENTS = ("control-plane", "community-node", "public-proxy")
PROJECTS = {"control-plane": "sparrow-control-plane", "community-node": "sparrow-community-node",
            "public-proxy": "sparrow-public-proxy"}
SKIPPED_STATE = {"__pycache__"}


class MoveError(Exception):
    pass


def docker(*arguments: str, check: bool = True) -> subprocess.CompletedProcess:
    result = subprocess.run(["docker", *arguments], capture_output=True, text=True)
    if check and result.returncode:
        raise MoveError(f"docker {' '.join(arguments[:2])} failed: {(result.stderr or result.stdout).strip()}")
    return result


def properties(path: Path) -> dict[str, str]:
    values = {}
    if path.is_file():
        for line in path.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()
    return values


def host_path(label: str) -> Path | None:
    """Compose working_dir label as a host path (Docker Desktop may report Linux-style mounts)."""
    value = label.strip()
    if not value:
        return None
    match = re.match(r"^/(?:run/desktop/mnt/host|host_mnt|mnt)/([a-zA-Z])/(.*)$", value)
    if WINDOWS and match:
        value = f"{match.group(1).upper()}:\\" + match.group(2).replace("/", "\\")
    return Path(value)


def same(first: Path, second: Path) -> bool:
    left, right = str(first.resolve()), str(second.resolve())
    return left.lower() == right.lower() if WINDOWS else left == right


def project_containers(project: str) -> list[tuple[str, Path | None]]:
    result = docker("ps", "--all", "--filter", f"label=com.docker.compose.project={project}", "--format",
                    '{{.ID}}|{{.Label "com.docker.compose.project.working_dir"}}')
    containers = []
    for line in result.stdout.splitlines():
        container, _, workdir = line.partition("|")
        containers.append((container, host_path(workdir)))
    return containers


def find_existing() -> Path | None:
    """Installation folder that owns the running Control Plane, if any."""
    if not shutil.which("docker"):
        return None
    try:
        owners = {workdir for _, workdir in project_containers(PROJECTS["control-plane"]) if workdir}
    except (MoveError, OSError):
        return None
    return next(iter(owners)).parent if len(owners) == 1 else None


def instance_projects(source: Path) -> dict[str, Path]:
    projects = {}
    instances = source / "node-instances"
    for manifest in instances.glob("*/node-instance.json") if instances.is_dir() else ():
        project = json.loads(manifest.read_text(encoding="utf-8")).get("project", "")
        if project:
            projects[project] = manifest.parent.relative_to(source)
    return projects


def compose_arguments(directory: Path) -> list[str]:
    """The same Compose file selection as the installer uses for this component."""
    if directory.name == "public-proxy":
        return ["compose", "-f", str(directory / "docker-compose.yml")]
    conf = properties(directory / "sparrow.conf")
    runtime = directory / ".env.runtime"
    arguments = ["compose", "--env-file", str(runtime), "-f", str(directory / "docker-compose.yml")]
    if conf.get("PUBLIC_TRANSITION") == "lan-in-place":
        if conf.get("PUBLIC_TRANSITION_COMPOSE") == "release":
            arguments += ["-f", str(directory / "docker-compose.release.yml")]
        elif conf.get("PUBLIC_TRANSITION_COMPOSE") != "base":
            raise MoveError(f"Unrecognized LAN transition Compose configuration in {directory}.")
        arguments += ["-f", str(directory / "docker-compose.public-transition.yml")]
    else:
        arguments += ["-f", str(directory / "docker-compose.release.yml")]
        if conf.get("MODE") == "public":
            arguments += ["-f", str(directory / "docker-compose.production.yml")]
            if conf.get("SHARED_PROXY") == "true":
                arguments += ["-f", str(directory / "docker-compose.shared-proxy.yml")]
    if directory.name == "control-plane":
        credentials = properties(runtime).get("FIREBASE_ADMIN_CREDENTIALS", "")
        if (credentials and Path(credentials).is_file()) or (directory / "secrets" / "firebase-admin.json").is_file():
            arguments += ["-f", str(directory / "docker-compose.firebase.yml")]
    return arguments


def instance_arguments(directory: Path) -> list[str]:
    conf = properties(directory / "sparrow.conf")
    arguments = ["compose", "--env-file", str(directory / ".env.runtime"),
                 "-f", str(directory / "docker-compose.yml"), "-f", str(directory / "docker-compose.release.yml")]
    if conf.get("MODE") == "public":
        arguments += ["-f", str(directory / "docker-compose.production.yml"),
                      "-f", str(directory / "docker-compose.shared-proxy.yml")]
    return arguments


def copy_state(source: Path, target: Path) -> None:
    """Copies everything that is not a manager template; the new templates stay."""
    for component in COMPONENTS:
        templates = set(LAYOUT["components"][component])
        for path in sorted((source / component).rglob("*")):
            relative = path.relative_to(source / component)
            if relative.parts[0] in templates or SKIPPED_STATE & set(relative.parts):
                continue
            destination = target / component / relative
            if path.is_dir():
                destination.mkdir(parents=True, exist_ok=True)
            elif path.is_file():
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(path, destination)
    if (source / "node-instances").is_dir():
        shutil.copytree(source / "node-instances", target / "node-instances", dirs_exist_ok=True)
    for name in ("local-images.json", "local-images.tar"):
        for candidate in (source / "app" / name, source / name):
            if candidate.is_file():
                shutil.copy2(candidate, target / "app" / name)
                break
    if (source / ".sparrow-manager.json").is_file() and not (target / ".sparrow-manager.json").exists():
        shutil.copy2(source / ".sparrow-manager.json", target / ".sparrow-manager.json")


def rewrite_paths(source: Path, target: Path) -> None:
    """Points absolute paths in the copied settings (for example Firebase credentials) at the new folder."""
    old_forms = {str(source), str(source).replace("\\", "/"), str(source).replace("/", "\\")}
    new = str(target)
    for path in [*target.glob("*/.env.runtime"), *target.glob("*/sparrow.conf"),
                 *target.glob("node-instances/*/.env.runtime"), *target.glob("node-instances/*/sparrow.conf")]:
        text = path.read_text(encoding="utf-8")
        updated = text
        for old in old_forms:
            pattern = re.compile(re.escape(old), re.IGNORECASE if WINDOWS else 0)
            replacement = new if "\\" in old else new.replace("\\", "/")
            updated = pattern.sub(lambda _: replacement, updated)
        if updated != text:
            path.write_text(updated, encoding="utf-8")


def start_from(root: Path, projects: dict[str, Path]) -> None:
    for component in ("control-plane", "community-node"):
        print(f"Starting {component} from {root} ...", flush=True)
        run_compose(compose_arguments(root / component) + ["up", "-d", "--no-build", "--pull", "never"], root / component)
    if (root / "public-proxy" / "Caddyfile").is_file():
        print("Starting the shared public proxy ...", flush=True)
        run_compose(compose_arguments(root / "public-proxy") + ["up", "-d", "--no-build", "--pull", "never", "caddy"],
                    root / "public-proxy")
    for project, relative in projects.items():
        print(f"Starting Community Node {project} ...", flush=True)
        run_compose(instance_arguments(root / relative) + ["up", "-d", "--no-build", "--pull", "never"], root / relative)


def run_compose(arguments: list[str], cwd: Path) -> None:
    result = subprocess.run(["docker", *arguments], cwd=cwd)
    if result.returncode:
        raise MoveError(f"docker {' '.join(arguments[-6:])} failed (exit {result.returncode}).")


def remove_containers(containers: list[str]) -> None:
    if containers:
        docker("rm", "--force", *containers)


def move(source: Path, target: Path) -> None:
    for component in ("control-plane", "community-node"):
        if not (source / component / ".env.runtime").is_file():
            raise MoveError(f"{source} has no installed {component}.")
        if (target / component / ".env.runtime").is_file():
            raise MoveError(f"{target} already contains an installed server; nothing was moved.")
    if (source / ".sparrow-attached.json").exists():
        raise MoveError("Attached legacy deployments cannot be moved automatically.")
    if same(source, target):
        raise MoveError("The server already runs from this folder.")
    if subprocess.run(["docker", "info"], capture_output=True).returncode:
        raise MoveError("Docker is not running. Start Docker and try again.")
    projects = {PROJECTS[component]: Path(component) for component in COMPONENTS}
    instances = instance_projects(source)
    projects.update(instances)
    containers: list[str] = []
    for project, relative in projects.items():
        for container, workdir in project_containers(project):
            if workdir is None or not same(workdir, source / relative):
                raise MoveError(f"Container {container} of {project} belongs to {workdir}, not {source}. "
                                "Nothing was moved.")
            containers.append(container)
    print(f"Moving the server from {source} to {target} ...", flush=True)
    copy_state(source, target)
    rewrite_paths(source, target)
    print("Settings, secrets and caches copied. Recreating the containers from the new folder ...", flush=True)
    remove_containers(containers)
    try:
        start_from(target, instances)
    except MoveError as error:
        print(f"Starting from the new folder failed: {error}", flush=True)
        print("Restoring the server in the old folder ...", flush=True)
        created = [container for project in projects for container, _ in project_containers(project)]
        remove_containers(created)
        start_from(source, instances)
        raise MoveError("The server was restored in the old folder; nothing was lost.") from error
    print(f"Done. The server now runs from {target}.")
    print(f"You can delete the old folder {source} once you checked that messaging works.")


def main() -> int:
    parser = argparse.ArgumentParser(description="Move a running Sparrow server installation into this folder.")
    parser.add_argument("--from", dest="source", type=Path, help="old installation folder (default: detect)")
    parser.add_argument("--find", action="store_true", help="only print the detected installation folder")
    args = parser.parse_args()
    if args.find:
        found = find_existing()
        print(found if found and not same(found, ROOT) else "")
        return 0
    try:
        source = args.source.expanduser().resolve() if args.source else find_existing()
        if source is None:
            raise MoveError("No running Sparrow server was found in Docker. Choose its folder instead.")
        move(source, ROOT)
    except (MoveError, OSError, ValueError) as error:
        print(f"Sparrow move failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
