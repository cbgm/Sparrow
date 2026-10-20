#!/usr/bin/env python3
"""Builds the Sparrow server installer (sparrow-server.zip) on Windows, Linux and macOS.

Release and internal workflows use registry images; --local-images builds the six
server images from this source tree with Docker and embeds them in the bundle.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import uuid
import zipfile
from urllib.parse import urlsplit

INSTALLER = Path(__file__).resolve().parent
SERVER = INSTALLER.parent
REPOSITORY = SERVER.parent
APP = INSTALLER / "app"
LAYOUT = json.loads((APP / "layout.json").read_text(encoding="utf-8"))
IMAGE_PREFIX = re.compile(r"[a-z0-9.-]+(?:/[a-z0-9._-]+)+")
IMAGE_TAG = re.compile(r"[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}")
GITHUB_REPOSITORY = re.compile(r"[A-Za-z0-9][A-Za-z0-9_.-]{0,99}/[A-Za-z0-9][A-Za-z0-9_.-]{0,99}")
DIRECTORY_KEY = re.compile(r"[A-Za-z0-9_-]{58,100}")


class BuildError(Exception):
    pass


def parse_args():
    parser = argparse.ArgumentParser(description="Build the Sparrow server installer bundle.")
    parser.add_argument("--output", default=str(REPOSITORY / "dist"), help="output folder for sparrow-server.zip")
    parser.add_argument("--image-prefix", default=None)
    parser.add_argument("--image-tag", default=None)
    parser.add_argument("--local-images", action="store_true", help="build and embed local server images")
    parser.add_argument("--install", default=None, help="create a ready server folder here instead of a ZIP")
    return parser.parse_args()


def directory_defaults() -> tuple[str, str]:
    url = (os.environ.get("CONTROL_PLANE_RELEASE_DIRECTORY_URL") or os.environ.get("CONTROL_PLANE_DIRECTORY_URL") or "").strip()
    if url:
        parts = urlsplit(url)
        if (parts.scheme != "https" or not parts.hostname or parts.username or parts.password or
                parts.query or parts.fragment or parts.path not in ("", "/")):
            raise BuildError("CONTROL_PLANE_DIRECTORY_URL must be an HTTPS base URL without path, query or credentials.")
        url = f"https://{parts.netloc}"
    key = os.environ.get("CONTROL_PLANE_DIRECTORY_PUBLIC_KEY", "").strip()
    if key and not DIRECTORY_KEY.fullmatch(key):
        raise BuildError("CONTROL_PLANE_DIRECTORY_PUBLIC_KEY must be a base64url-encoded DER Ed25519 public key.")
    return url, key


def component_source(component: str, name: str) -> Path:
    if component == "public-proxy":
        return INSTALLER / "public-proxy" / name
    if component == "control-plane" and name == "Bootstrap-ControlPlane.ps1":
        return SERVER / "scripts" / "Bootstrap-ControlPlane.Bundle.ps1"
    return SERVER / component / name


def docker(*arguments: str) -> None:
    result = subprocess.run(["docker", *arguments])
    if result.returncode:
        raise BuildError(f"Docker command failed (exit {result.returncode}). No installer was produced.")


def docker_output(*arguments: str) -> str:
    return subprocess.run(["docker", *arguments], capture_output=True, text=True, check=True).stdout.strip()


def build_local_images(app: Path, prefix: str, tag: str) -> None:
    if not shutil.which("docker"):
        raise BuildError("Docker is required to build local images.")
    if subprocess.run(["docker", "info"], capture_output=True).returncode:
        raise BuildError("Start Docker before building local server images.")
    images = []
    for service in LAYOUT["services"]:
        name = f"{prefix}-{service}:{tag}"
        print(f"Building server image {name} ...", flush=True)
        docker("build", "--file", str(SERVER / "Dockerfile"), "--build-arg", f"SERVICE={service}",
               "--tag", name, str(REPOSITORY))
        image_id = docker_output("image", "inspect", "--format", "{{.Id}}", name)
        if not re.fullmatch(r"sha256:[a-f0-9]{64}", image_id):
            raise BuildError(f"Could not inspect the locally built image {name}.")
        images.append({"service": service, "name": name, "id": image_id})
    archive = app / "local-images.tar"
    print("Saving the server images ...", flush=True)
    docker("save", "--output", str(archive), *[image["name"] for image in images])
    digest = hashlib.sha256()
    with archive.open("rb") as stream:
        for block in iter(lambda: stream.read(1 << 20), b""):
            digest.update(block)
    manifest = {"version": 1, "archive": archive.name, "sha256": digest.hexdigest(),
                "prefix": prefix, "tag": tag, "images": images}
    (app / "local-images.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")


def stage_bundle(root: Path, prefix: str, tag: str, local: bool) -> None:
    app = root / "app"
    shutil.copytree(APP, app, ignore=shutil.ignore_patterns("__pycache__", *LAYOUT["generated"]))
    for name in ("Start-SparrowServer.cmd", "Start-SparrowServer.sh", "Start-SparrowServer.command"):
        shutil.copy2(INSTALLER / name, root / name)
    shutil.copy2(INSTALLER / "README_RUNTIME.md", root / "README.md")
    (root / "node-instances" / "routes").mkdir(parents=True)
    url, key = directory_defaults()
    for component, names in LAYOUT["components"].items():
        target = root / component
        target.mkdir()
        for name in names:
            shutil.copy2(component_source(component, name), target / name)
        if component == "public-proxy":
            continue
        (target / "secrets").mkdir()
        (target / "secrets" / ".gitignore").write_text("*\n!.gitignore\n", encoding="utf-8")
        plane_key = "CONTROL_PLANE_ID=" if component == "control-plane" else "CONTROL_PLANE_URLS="
        settings = ["CONFIGURED=false", "MODE=lan", "PUBLIC_DOMAIN=", "PUBLIC_HOSTNAME_MODE=Manual",
                    "SHARED_PROXY=false", f"SPARROW_IMAGE_PREFIX={prefix}", f"SPARROW_IMAGE_TAG={tag}",
                    plane_key, f"CONTROL_PLANE_DIRECTORY_URL={url}", f"CONTROL_PLANE_DIRECTORY_PUBLIC_KEY={key}"]
        (target / "sparrow.conf").write_text("\n".join(settings) + "\n", encoding="utf-8")
    repository = os.environ.get("GITHUB_REPOSITORY", "cbgm/sparrow")
    if not GITHUB_REPOSITORY.fullmatch(repository):
        raise BuildError("Invalid GitHub release repository.")
    info = {"format": 2, "repository": repository, "imagePrefix": prefix, "imageTag": tag,
            "source": "local" if local else "registry",
            "createdAt": datetime.now(timezone.utc).isoformat(timespec="seconds")}
    (app / "bundle.json").write_text(json.dumps(info, indent=2), encoding="utf-8")
    if local:
        build_local_images(app, prefix, tag)


def write_zip(root: Path, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    partial = destination.with_suffix(".zip.partial")
    with zipfile.ZipFile(partial, "w", zipfile.ZIP_DEFLATED, allowZip64=True) as archive:
        for path in sorted(root.rglob("*")):
            name = path.relative_to(root).as_posix()
            if path.is_dir():
                if not any(path.iterdir()):
                    archive.writestr(name + "/", "")
                continue
            entry = zipfile.ZipInfo.from_file(path, name)
            executable = path.suffix in (".sh", ".command")
            entry.external_attr = (0o100755 if executable else 0o100644) << 16
            entry.compress_type = zipfile.ZIP_STORED if path.suffix == ".tar" else zipfile.ZIP_DEFLATED
            with path.open("rb") as source, archive.open(entry, "w", force_zip64=True) as target:
                shutil.copyfileobj(source, target, 1 << 20)
    partial.replace(destination)


def main() -> int:
    args = parse_args()
    if args.local_images:
        prefix = args.image_prefix or "local/sparrow"
        tag = args.image_tag or ("local-" + datetime.now().strftime("%Y%m%d%H%M%S") + "-" + uuid.uuid4().hex[:8])
    else:
        prefix = args.image_prefix or "ghcr.io/cbgm/sparrow"
        tag = args.image_tag or "latest"
    try:
        if not IMAGE_PREFIX.fullmatch(prefix):
            raise BuildError("Invalid image prefix.")
        if not IMAGE_TAG.fullmatch(tag):
            raise BuildError("Invalid image tag.")
        if args.install:
            target = Path(args.install).expanduser().resolve()
            if target.exists() and any(target.iterdir()):
                raise BuildError(f"{target} is not empty. Choose a new, empty folder.")
            target.mkdir(parents=True, exist_ok=True)
            stage_bundle(target, prefix, tag, args.local_images)
            for script in list(target.glob("*.sh")) + list(target.glob("*.command")) + list(target.glob("*/*.sh")):
                script.chmod(0o755)
            print(f"Created the server folder {target}.")
            return 0
        destination = Path(args.output).expanduser().resolve() / "sparrow-server.zip"
        with tempfile.TemporaryDirectory(prefix="sparrow-bundle-") as work:
            root = Path(work) / "bundle"
            root.mkdir()
            stage_bundle(root, prefix, tag, args.local_images)
            write_zip(root, destination)
    except (BuildError, OSError, subprocess.CalledProcessError) as error:
        print(f"Sparrow build failed: {error}", file=sys.stderr)
        return 1
    origin = "local server images" if args.local_images else "registry images"
    print(f"Created {destination} ({origin} {prefix}-*:{tag}).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
