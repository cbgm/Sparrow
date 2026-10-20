#!/usr/bin/env python3
"""Updates a Sparrow server installation in place on Linux and macOS.

Sources: the latest GitHub release, a sparrow-server.zip, an extracted bundle
folder, or a Sparrow source checkout (server images are built locally).
Runtime state (sparrow.conf, .env.runtime, secrets, identities, logs, Docker
projects and volumes) is never copied over or removed.
"""
from __future__ import annotations

import argparse
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import uuid
import zipfile

APP = Path(__file__).resolve().parent
ROOT = APP.parent
REPOSITORY = re.compile(r"[A-Za-z0-9][A-Za-z0-9_.-]{0,99}/[A-Za-z0-9][A-Za-z0-9_.-]{0,99}")
RELEASE_TAG = re.compile(r"(?:server-)?v[A-Za-z0-9_.-]+")
SSLIP = r"-(\d{1,3}-){3}\d{1,3}\.sslip\.io"


class UpdateError(Exception):
    pass


def properties(path: Path) -> dict[str, str]:
    values = {}
    if path.is_file():
        for line in path.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()
    return values


def set_properties(path: Path, updates: dict[str, str]) -> None:
    lines = path.read_text(encoding="utf-8").splitlines()
    seen = set()
    for index, line in enumerate(lines):
        key = line.split("=", 1)[0].strip()
        if "=" in line and key in updates:
            lines[index] = f"{key}={updates[key]}"
            seen.add(key)
    lines += [f"{key}={value}" for key, value in updates.items() if key not in seen]
    partial = path.with_name(path.name + ".pending")
    partial.write_text("\n".join(lines) + "\n", encoding="utf-8")
    os.chmod(partial, path.stat().st_mode & 0o777)
    partial.replace(path)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def release_repository(install: Path) -> str:
    for candidate in (install / "app" / "bundle.json", install / "bundle-source.json"):
        if candidate.is_file():
            repository = json.loads(candidate.read_text(encoding="utf-8")).get("repository", "")
            if repository:
                return repository
    return os.environ.get("SPARROW_GITHUB_REPOSITORY", "cbgm/sparrow")


def download_release(install: Path, work: Path) -> Path:
    repository = release_repository(install)
    if not REPOSITORY.fullmatch(repository) or ".." in repository:
        raise UpdateError(f"Invalid GitHub repository '{repository}'.")
    headers = {"User-Agent": "Sparrow-Server-Updater", "Accept": "application/vnd.github+json"}
    print(f"Looking up the latest Sparrow server release of {repository} ...", flush=True)
    request = urllib.request.Request(f"https://api.github.com/repos/{repository}/releases?per_page=100", headers=headers)
    with urllib.request.urlopen(request, timeout=60) as response:
        releases = json.load(response)
    suffix = "-linux.zip"

    def bundle_assets(release):
        return [asset for asset in release.get("assets", [])
                if asset["name"].lower().startswith("sparrow-server-") and asset["name"].lower().endswith(suffix)]

    candidates = [release for release in releases
                  if not release.get("draft") and not release.get("prerelease") and
                  RELEASE_TAG.fullmatch(release.get("tag_name", "")) and len(bundle_assets(release)) == 1 and
                  any(asset["name"] == "SHA256SUMS.txt" for asset in release.get("assets", []))]
    if not candidates:
        raise UpdateError(f"No published release of {repository} contains a *{suffix} server bundle and SHA256SUMS.txt.")
    release = max(candidates, key=lambda item: item.get("published_at") or "")
    asset = bundle_assets(release)[0]
    sums = next(item for item in release["assets"] if item["name"] == "SHA256SUMS.txt")
    for item in (asset, sums):
        url = item["browser_download_url"]
        if not url.lower().startswith(f"https://github.com/{repository.lower()}/releases/download/"):
            raise UpdateError("Release asset URL does not belong to the configured GitHub repository.")
    zip_path = work / asset["name"]
    print(f"Downloading {asset['name']} from release {release['tag_name']} ...", flush=True)
    for item, target in ((asset, zip_path), (sums, work / "SHA256SUMS.txt")):
        with urllib.request.urlopen(urllib.request.Request(item["browser_download_url"], headers=headers),
                                    timeout=300) as response, target.open("wb") as stream:
            shutil.copyfileobj(response, stream, 1 << 20)
    expected = None
    for line in (work / "SHA256SUMS.txt").read_text(encoding="utf-8").splitlines():
        match = re.fullmatch(r"([A-Fa-f0-9]{64})\s+\*?(.+)", line.strip())
        if match and match.group(2).strip() == asset["name"]:
            expected = match.group(1).lower()
    if expected is None:
        raise UpdateError("SHA256SUMS.txt does not list the server bundle.")
    if sha256(zip_path) != expected:
        raise UpdateError("Downloaded server bundle failed checksum verification.")
    print(f"Verified release {release['tag_name']}.", flush=True)
    return zip_path


def build_from_source(source: Path, work: Path) -> Path:
    builder = source / "server" / "installer" / "build.py"
    if not builder.is_file():
        raise UpdateError(f"{source} is not a Sparrow source folder with the current server installer.")
    print(f"Building server images from {source} (this can take several minutes) ...", flush=True)
    result = subprocess.run([sys.executable, str(builder), "--local-images", "--output", str(work / "build")])
    if result.returncode:
        raise UpdateError(f"Local server build failed (exit {result.returncode}). The installation was not changed.")
    return work / "build" / "sparrow-server.zip"


def expand(zip_path: Path, work: Path) -> Path:
    if not zip_path.is_file():
        raise UpdateError(f"Bundle not found: {zip_path}")
    print("Unpacking the server bundle ...", flush=True)
    target = work / "bundle"
    with zipfile.ZipFile(zip_path) as archive:
        for entry in archive.infolist():
            destination = (target / entry.filename).resolve()
            if target.resolve() not in destination.parents and destination != target.resolve():
                raise UpdateError("The bundle contains an unsafe path.")
        archive.extractall(target)
    for script in list(target.rglob("*.sh")) + list(target.rglob("*.command")):
        script.chmod(0o755)
    return target


def install_code(bundle: Path, install: Path) -> None:
    """Replaces manager code and component templates; migrates the old flat layout."""
    for required in ("app/Invoke-SparrowServer.py", "app/bundle.json", "app/layout.json",
                     "community-node/bootstrap-community-node.sh", "control-plane/bootstrap-control-plane.sh"):
        if not (bundle / required).is_file():
            raise UpdateError(f"Not a complete Sparrow server bundle: missing {required}. No files changed.")
    if bundle.resolve() == install.resolve():
        raise UpdateError("The bundle is already this installation. No files changed.")
    layout = json.loads((bundle / "app" / "layout.json").read_text(encoding="utf-8"))
    staged_app = bundle / "app"
    copies: list[str] = []
    removals: list[str] = []

    def app_files(folder: Path) -> list[str]:
        if not folder.is_dir():
            return []
        return sorted("app/" + path.relative_to(folder).as_posix() for path in folder.rglob("*")
                      if path.is_file() and "__pycache__" not in path.parts)

    new_app = app_files(staged_app)
    copies += new_app
    removals += [name for name in app_files(install / "app") if name not in new_app]
    copies += layout["topLevel"]
    copies += [f"{component}/{name}" for component, names in layout["components"].items() for name in names]
    keep_legacy_images = (install / "local-images.json").is_file() and not (staged_app / "local-images.json").is_file()
    removals += [name for name in layout["legacy"] if (install / name).is_file() and
                 not (keep_legacy_images and name in ("local-images.json", "local-images.tar"))]
    for component in ("control-plane", "community-node"):
        if properties(install / component / "sparrow.conf").get("CONFIGURED") != "true" and \
                not (install / component / ".env.runtime").is_file():
            copies.append(f"{component}/sparrow.conf")
    helpers = [helper for helper in layout["instanceHelpers"] if (install / "node-instances" / helper).is_file()]
    for name in copies:
        if not (bundle / name).is_file():
            raise UpdateError(f"Bundle is missing {name}. No files changed.")

    backup = install / ".sparrow-code-backups" / (datetime.now().strftime("%Y%m%d-%H%M%S") + "-" + uuid.uuid4().hex[:8])
    touched = [name for name in copies + removals if (install / name).is_file()] + \
        [f"node-instances/{helper}" for helper in helpers]
    for name in touched:
        if name.endswith("local-images.tar"):
            continue
        saved = backup / name
        saved.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(install / name, saved)
    applied: list[str] = []
    try:
        for name in copies:
            destination = install / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            applied.append(name)
            shutil.copy2(bundle / name, destination)
        for helper in helpers:
            name = f"node-instances/{helper}"
            applied.append(name)
            shutil.copy2(staged_app / helper, install / name)
        for name in removals:
            applied.append(name)
            (install / name).unlink()
    except OSError as error:
        for name in reversed(applied):
            saved = backup / name
            if saved.is_file():
                shutil.copy2(saved, install / name)
            elif name in copies:
                (install / name).unlink(missing_ok=True)
        raise UpdateError(f"Code update failed; previous code restored: {error}") from error
    if keep_legacy_images:
        for name in ("local-images.json", "local-images.tar"):
            if (install / name).is_file():
                shutil.move(str(install / name), str(install / "app" / name))
        print("Kept the installed local build images.")
    for folder in ("directory-sync", "directory-registration"):
        path = install / folder
        if path.is_dir() and not any(path.iterdir()):
            path.rmdir()
    for script in list(install.glob("*.sh")) + list(install.glob("*.command")) + \
            list(install.glob("*/bootstrap-*.sh")):
        script.chmod(0o755)
    print(f"Updated {len(copies) + len(helpers)} manager files in place ({len(removals)} outdated files removed): {install}")
    print(f"Previous code backup: {backup}")
    print("Preserved: sparrow.conf, .env.runtime, secrets, server identities, Docker projects, volumes and TLS data.")


def apply_images(bundle: Path, install: Path) -> None:
    """Selects the bundle's server images and lets the updated manager apply them."""
    info = json.loads((bundle / "app" / "bundle.json").read_text(encoding="utf-8"))
    local = install / "app" / "local-images.json"
    if local.is_file():
        manifest = json.loads(local.read_text(encoding="utf-8"))
        prefix, tag = manifest["prefix"], manifest["tag"]
    else:
        prefix, tag = info["imagePrefix"], info["imageTag"]
    if not shutil.which("docker") or subprocess.run(["docker", "info"], capture_output=True).returncode:
        raise UpdateError("The manager was updated, but Docker is not running. Start Docker and run this update again.")
    cp = properties(install / "control-plane" / "sparrow.conf")
    node = properties(install / "community-node" / "sparrow.conf")
    for component in ("control-plane", "community-node"):
        selection = {"SPARROW_IMAGE_PREFIX": prefix, "SPARROW_IMAGE_TAG": tag}
        set_properties(install / component / "sparrow.conf", selection)
        set_properties(install / component / ".env.runtime", selection)
    arguments = [sys.executable, str(install / "app" / "Invoke-SparrowServer.py"), "install",
                 "--component", "combined", "--mode", cp.get("MODE", "lan"),
                 "--image-prefix", prefix, "--image-tag", tag]
    automatic = cp.get("PUBLIC_HOSTNAME_MODE") == "CaddyAutomatic" or (
        re.fullmatch("control" + SSLIP, cp.get("PUBLIC_DOMAIN", "")) and
        re.fullmatch("node" + SSLIP, node.get("PUBLIC_DOMAIN", "")))
    if cp.get("MODE") == "public":
        if automatic:
            arguments.append("--auto-dns")
        else:
            arguments += ["--node-domain", node.get("PUBLIC_DOMAIN", ""), "--control-domain", cp.get("PUBLIC_DOMAIN", "")]
    print(f"Applying server images {prefix}-*:{tag} to the existing installation ...", flush=True)
    result = subprocess.run(arguments, cwd=install)
    if result.returncode:
        raise UpdateError(f"The manager was updated, but applying the server images failed (exit {result.returncode}). "
                          "Existing data was not reset.")


def main() -> int:
    parser = argparse.ArgumentParser(prog="Start-SparrowServer.sh update",
                                     description="Update this Sparrow server in place. Data and settings are kept.")
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--release", action="store_true", help="the latest published GitHub release")
    source.add_argument("--zip", type=Path, help="a sparrow-server.zip bundle")
    source.add_argument("--source", type=Path, help="a Sparrow source folder; builds the server images locally")
    source.add_argument("--folder", type=Path, help="an extracted bundle folder (moves old installations to the new layout)")
    parser.add_argument("--installation", type=Path, default=ROOT, help="installation folder (default: this one)")
    args = parser.parse_args()
    install = args.installation.expanduser().resolve()
    try:
        if (install / ".sparrow-attached.json").exists():
            raise UpdateError("Attached deployments need a separate reviewed update procedure.")
        if not (install / "control-plane" / "sparrow.conf").is_file() or not (install / "community-node" / "sparrow.conf").is_file():
            raise UpdateError(f"No Sparrow server installation found at {install}.")
        installed = all((install / component / ".env.runtime").is_file() for component in ("control-plane", "community-node"))
        with tempfile.TemporaryDirectory(prefix="sparrow-update-") as directory:
            work = Path(directory)
            if args.release:
                bundle = expand(download_release(install, work), work)
            elif args.zip:
                bundle = expand(args.zip.expanduser().resolve(), work)
            elif args.source:
                bundle = expand(build_from_source(args.source.expanduser().resolve(), work), work)
            else:
                bundle = args.folder.expanduser().resolve()
            if not (bundle / "app" / "layout.json").is_file():
                raise UpdateError("This bundle uses an older layout and cannot be applied by this manager.")
            install_code(bundle, install)
            if args.folder:
                print("Manager updated. The running server was not changed; use 'update' for new server versions.")
            elif not installed:
                print("Manager code updated. The server is not installed yet: run ./Start-SparrowServer.sh install.")
            else:
                apply_images(bundle, install)
                print("Update complete.")
    except (UpdateError, OSError, ValueError, KeyError, zipfile.BadZipFile) as error:
        print(f"Sparrow update failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
