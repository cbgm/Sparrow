#!/usr/bin/env python3
"""Build operator-only Windows/Linux directory installer archives.

Run from any directory: python server/control-plane-directory/Build-SparrowDirectoryInstaller.py
Never call this script from the public unified-server-bundle packaging step.
"""
from pathlib import Path
import argparse
import zipfile

ROOT = Path(__file__).resolve().parent
WIN = (
    'Start-SparrowDirectory.cmd', 'Start-SparrowDirectory.ps1',
    'Dockerfile', '.dockerignore', 'docker-compose.yml',
    'directory_service.py', 'directory_admin_cli.py', 'requirements.txt',
    'README-INSTALL.md',
)
LINUX = (
    'Start-SparrowDirectory.sh', 'Dockerfile', '.dockerignore',
    'docker-compose.yml', 'directory_service.py',
    'directory_admin_cli.py', 'requirements.txt', 'README-INSTALL.md',
)


def build(out: Path, platform: str, filenames: tuple[str, ...]) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(out, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in sorted(filenames):
            source = ROOT / name
            if not source.is_file():
                raise FileNotFoundError(source)
            metadata = zipfile.ZipInfo(name, date_time=(2026, 1, 1, 0, 0, 0))
            metadata.create_system = 3
            metadata.external_attr = ((0o755 if name.endswith('.sh') else 0o644) << 16)
            metadata.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(metadata, source.read_bytes())
    print(f'{platform}: {out}')


def main() -> None:
    parser = argparse.ArgumentParser(description='Build private Sparrow Directory installers')
    parser.add_argument('--output-dir', type=Path, default=ROOT / 'dist')
    args = parser.parse_args()
    build(args.output_dir / 'sparrow-directory-windows.zip', 'Windows', WIN)
    build(args.output_dir / 'sparrow-directory-linux.zip', 'Linux', LINUX)


if __name__ == '__main__':
    main()
