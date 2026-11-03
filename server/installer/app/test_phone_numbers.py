#!/usr/bin/env python3
"""Applies the operator's test phone numbers to the installed Control Plane, then runs the given command.

Usage: test_phone_numbers.py --numbers=+4917012345678,+4917098765432 [--then COMMAND ...]

A real phone number stays with the device that registered it (see the presence directory). Test numbers instead
move to the device that registers them last, without SMS verification, so reinstalled test devices keep working.
The server receives only the numbers' phone aliases, never the numbers. An empty list removes all test numbers.
The setting takes effect when the following Start recreates the presence directory.
"""
from __future__ import annotations

import base64
import hashlib
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent
RUNTIME = ROOT / "control-plane" / ".env.runtime"
KEY = "PRESENCE_TEST_PHONE_ALIASES"
E164 = re.compile(r"\+[1-9][0-9]{1,14}")
# Must match ClientRoutingIds.fromPhoneNumber on the server.
PHONE_ALIAS_PREFIX = "scphone1_"


def phone_number(value: str) -> str | None:
    number = re.sub(r"[\s\-().\/]", "", value)
    if number.startswith("00"):
        number = "+" + number[2:]
    elif re.fullmatch(r"[1-9][0-9]{7,14}", number):
        # Digits with country code but without "+", e.g. 15550000001.
        number = "+" + number
    return number if E164.fullmatch(number) else None


def parse(numbers: str) -> list[str]:
    parsed = []
    for value in re.split(r"[,;\n]+", numbers):
        if not value.strip():
            continue
        # "+49 170 1234567" is one number; "15550000001 15550000002" are two.
        candidates = [phone_number(value)]
        if candidates[0] is None:
            candidates = [phone_number(part) for part in value.split()]
        if None in candidates:
            raise ValueError(f"Not a phone number with country code (e.g. +15550000001): {value.strip()}")
        parsed.extend(candidates)
    return list(dict.fromkeys(parsed))


def alias(number: str) -> str:
    digest = hashlib.sha256(number.encode("utf-8")).digest()
    return PHONE_ALIAS_PREFIX + base64.urlsafe_b64encode(digest).decode("ascii").rstrip("=")


def apply(numbers: list[str]) -> None:
    if not RUNTIME.is_file():
        if numbers:
            print("Test phone numbers are applied with the next Start, once the server is installed.", flush=True)
        return
    lines = RUNTIME.read_text(encoding="utf-8").splitlines()
    value = ",".join(alias(number) for number in numbers)
    current = next((line.split("=", 1)[1] for line in lines if line.startswith(KEY + "=")), "")
    if current != value:
        output = [line for line in lines if not line.startswith(KEY + "=")]
        if value:
            output.append(f"{KEY}={value}")
        shutil.copy2(RUNTIME, RUNTIME.with_name(RUNTIME.name + ".previous"))
        pending = RUNTIME.with_name(RUNTIME.name + ".pending")
        pending.write_text("\n".join(output) + "\n", encoding="utf-8")
        pending.chmod(RUNTIME.stat().st_mode & 0o777)
        pending.replace(RUNTIME)
    if numbers:
        print("Test phone numbers: " + ", ".join(numbers), flush=True)
    elif current:
        print("Test phone numbers removed.", flush=True)


def main() -> int:
    arguments = sys.argv[1:]
    then = arguments[arguments.index("--then") + 1:] if "--then" in arguments else []
    own = arguments[:arguments.index("--then")] if "--then" in arguments else arguments
    numbers = next((argument.split("=", 1)[1] for argument in own if argument.startswith("--numbers=")), "")
    try:
        apply(parse(numbers))
    except (ValueError, OSError) as error:
        print(f"Sparrow: {error}", file=sys.stderr, flush=True)
        return 1
    return subprocess.call(then) if then else 0


if __name__ == "__main__":
    sys.exit(main())
