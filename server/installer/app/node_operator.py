#!/usr/bin/env python3
"""Applies the operator name to the installed Community Node and every added node, then runs the given command.

Usage: node_operator.py --operator="Example Org" [--then COMMAND ...]

Each node puts the name into its signed descriptor (NODE_OPERATOR). Apps never route a circuit through two nodes
of one operator and let their circuits end at different operators, so all nodes run by the same person or
organization should carry the same name. An empty name removes it: the node then counts as an operator of its own.
The setting takes effect when the following Start recreates the federation service; added nodes take it on their
next start.
"""
from __future__ import annotations

from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent
KEY = "NODE_OPERATOR"
# Must match SparrowNodeDescriptor.MAXIMUM_OPERATOR_LENGTH on the server.
MAXIMUM_LENGTH = 64


def runtimes() -> list[Path]:
    instances = ROOT / "node-instances"
    added = sorted(directory / ".env.runtime" for directory in instances.iterdir()
                   if (directory / "node-instance.json").is_file()) if instances.is_dir() else []
    return [path for path in [ROOT / "community-node" / ".env.runtime", *added] if path.is_file()]


def parse(operator: str) -> str:
    name = " ".join(operator.split())
    if len(name) > MAXIMUM_LENGTH:
        raise ValueError(f"The operator name is longer than {MAXIMUM_LENGTH} characters: {name}")
    if "\"" in name or "$" in name:
        raise ValueError(f"The operator name must not contain \" or $: {name}")
    return name


def apply_to(runtime: Path, name: str) -> bool:
    lines = runtime.read_text(encoding="utf-8").splitlines()
    current = next((line.split("=", 1)[1] for line in lines if line.startswith(KEY + "=")), "")
    if current == name:
        return False
    output = [line for line in lines if not line.startswith(KEY + "=")]
    if name:
        output.append(f"{KEY}={name}")
    shutil.copy2(runtime, runtime.with_name(runtime.name + ".previous"))
    pending = runtime.with_name(runtime.name + ".pending")
    pending.write_text("\n".join(output) + "\n", encoding="utf-8")
    pending.chmod(runtime.stat().st_mode & 0o777)
    pending.replace(runtime)
    return True


def apply(name: str) -> None:
    targets = runtimes()
    if not targets:
        if name:
            print("The operator name is applied with the next Start, once the server is installed.", flush=True)
        return
    changed = [runtime for runtime in targets if apply_to(runtime, name)]
    if changed and name:
        print(f"Operator: {name}", flush=True)
    elif changed:
        print("Operator name removed.", flush=True)


def main() -> int:
    arguments = sys.argv[1:]
    then = arguments[arguments.index("--then") + 1:] if "--then" in arguments else []
    own = arguments[:arguments.index("--then")] if "--then" in arguments else arguments
    operator = next((argument.split("=", 1)[1] for argument in own if argument.startswith("--operator=")), "")
    try:
        apply(parse(operator))
    except (ValueError, OSError) as error:
        print(f"Sparrow: {error}", file=sys.stderr, flush=True)
        return 1
    return subprocess.call(then) if then else 0


if __name__ == "__main__":
    sys.exit(main())
