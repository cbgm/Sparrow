#!/usr/bin/env python3
"""Build operator-only Windows and Linux/macOS Directory installer archives or folders.

Run from any directory: python server/control-plane-directory/Build-SparrowDirectoryInstaller.py
With --install DIR it creates a ready Directory folder instead; the settings of the running
Directory (or of this source folder) are copied, so the new folder takes it over.
Never call this script from the public unified-server-bundle packaging step.
"""
from pathlib import Path
import argparse
import os
import shutil
import sys
import zipfile

ROOT = Path(__file__).resolve().parent
# The window shares its look with the server installer; the archive carries a copy.
SHARED = {'ui_window.py': ROOT.parent / 'installer' / 'app' / 'ui_window.py'}
COMMON = (
    'directory_window.py', 'directory_installer.py', 'ui_window.py',
    'Dockerfile', '.dockerignore', 'docker-compose.yml',
    'directory_service.py', 'directory_admin_cli.py', 'requirements.txt',
    'README-INSTALL.md',
)
WIN = ('Start-SparrowDirectory.cmd', *COMMON)
LINUX = ('Start-SparrowDirectory.sh', 'Start-SparrowDirectory.command', *COMMON)


def build(out: Path, platform: str, filenames: tuple[str, ...]) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(out, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in sorted(filenames):
            source = SHARED.get(name, ROOT / name)
            if not source.is_file():
                raise FileNotFoundError(source)
            metadata = zipfile.ZipInfo(name, date_time=(2026, 1, 1, 0, 0, 0))
            metadata.create_system = 3
            executable = name.endswith(('.sh', '.command'))
            metadata.external_attr = ((0o755 if executable else 0o644) << 16)
            metadata.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(metadata, source.read_bytes())
    print(f'{platform}: {out}')


def create_folder(target: Path) -> None:
    """A ready Directory folder for this platform that takes over the current settings."""
    sys.path.insert(0, str(ROOT))
    import directory_installer as engine
    if target.exists() and any(target.iterdir()):
        raise SystemExit(f'{target} is not empty. Choose a new, empty folder.')
    target.mkdir(parents=True, exist_ok=True)
    for name in WIN if os.name == 'nt' else LINUX:
        shutil.copy2(SHARED.get(name, ROOT / name), target / name)
        if name.endswith(('.sh', '.command')):
            (target / name).chmod(0o755)
    running = engine.running_directory_folder()
    for folder in (running, ROOT):
        if folder and (folder / 'private' / 'admin.env').is_file():
            shutil.copytree(folder / 'private', target / 'private')
            print(f'Took over the Directory settings of {folder}.')
            break
    print(f'Created the Directory folder {target}.')


def main() -> None:
    parser = argparse.ArgumentParser(description='Build private Sparrow Directory installers')
    parser.add_argument('--output-dir', type=Path, default=ROOT / 'dist')
    parser.add_argument('--install', type=Path, help='create a ready Directory folder here')
    args = parser.parse_args()
    if args.install:
        create_folder(args.install.expanduser().resolve())
        return
    build(args.output_dir / 'sparrow-directory-windows.zip', 'Windows', WIN)
    build(args.output_dir / 'sparrow-directory-linux.zip', 'Linux', LINUX)


if __name__ == '__main__':
    main()
