#!/usr/bin/env python3
"""Text menu of the Sparrow Server manager for machines without a screen (for example over SSH)."""
from __future__ import annotations

import os
from pathlib import Path
import subprocess
import sys

from manager_core import (ROOT, SOURCE_TREE, WINDOWS, create_command, default_server_folder,
                          find_existing_installation, move_command, addresses, describe, engine_command, installed, load_settings,
                          local_build_tag, node_command, save_preferences, server_state, update_command,
                          version_text)

# ------------------------------------------------------------------ terminal
def supports_color() -> bool:
    if os.environ.get("NO_COLOR") or not sys.stdout.isatty():
        return False
    if WINDOWS:
        os.system("")  # enables ANSI escape sequences in the Windows console
    return True


COLOR = supports_color()


def paint(text: str, code: str) -> str:
    return f"\033[{code}m{text}\033[0m" if COLOR else text


def bold(text): return paint(text, "1")
def dim(text): return paint(text, "2")
def accent(text): return paint(text, "36")
def good(text): return paint(text, "32")
def warn(text): return paint(text, "33")
def bad(text): return paint(text, "31")


def clear() -> None:
    if sys.stdout.isatty():
        os.system("cls" if WINDOWS else "clear")


def ask(prompt: str, default: str = "") -> str:
    suffix = f" [{default}]" if default else ""
    try:
        value = input(f"{prompt}{suffix}: ").strip()
    except EOFError:
        raise SystemExit(0)
    return value or default


def confirm(prompt: str) -> bool:
    return ask(f"{prompt} (y/n)", "n").lower() in ("y", "yes", "j", "ja")


def pause() -> None:
    ask(dim("Press Enter to return to the menu"))


LEVEL_COLOURS = {"ok": good, "warn": warn, "error": bad, "idle": dim}


def run(command: list[str]) -> int:
    print(dim("$ " + " ".join(command)))
    try:
        return subprocess.run(command, cwd=ROOT).returncode
    except KeyboardInterrupt:
        print(warn("\nStopped waiting. The operation may still be finishing; no data was deleted."))
        return 130
    except OSError as error:
        print(bad(f"Could not start: {error}"))
        return 1


def restart_manager(installation: Path) -> None:
    """Runs the freshly updated manager code."""
    manager = installation / "app" / "manager_terminal.py"
    print(good("Opening the updated manager ..."))
    os.chdir(installation)
    if WINDOWS:
        raise SystemExit(subprocess.run([sys.executable, str(manager)]).returncode)
    os.execv(sys.executable, [sys.executable, str(manager)])


# ------------------------------------------------------------------ screens
def header(settings: dict) -> None:
    clear()
    state, level = server_state()
    colour = LEVEL_COLOURS[level]
    width = 72
    print(accent("=" * width))
    title = bold(" Sparrow Server")
    print(title + " " * (width - 15 - len(state) - 2) + colour("● " + state))
    print(accent("=" * width))
    if installed():
        for label, value in addresses().items():
            print(f" {label:<15} {value}")
        print(f" {'Server version':<15} {version_text()}")
    else:
        print(" Not installed yet. Check Settings, then choose Install.")
    print(dim(f" Folder          {ROOT}"))
    print()


def menu(settings: dict) -> str:
    header(settings)
    start = "Start / apply settings" if installed() else "Install"
    entries = [("1", start), ("2", "Stop"), ("3", "Status (details)"), ("4", "Logs"), ("5", "Settings"),
               ("6", "Update to latest release"), ("7", "Build from source"),
               ("8", "Apply ZIP / move an existing server here"), ("9", "Community Nodes"),
               ("0", "Reinstall from scratch (deletes all data)"), ("q", "Quit")]
    for key, label in entries:
        text = bad(label) if key == "0" else label
        print(f"  {accent(key)}  {text}")
    print()
    return ask("Choose").lower()


def edit_settings(settings: dict, preferences: dict) -> None:
    while True:
        header(settings)
        print(bold(" Settings") + dim("  (applied with Start / Install)"))
        rows = describe(settings)
        for index, (label, value) in enumerate(rows, 1):
            print(f"  {accent(str(index))}  {label:<32} {value}")
        print(f"  {accent('b')}  Back")
        choice = ask("Change").lower()
        if choice in ("b", ""):
            preferences["settings"] = settings
            save_preferences(preferences)
            return
        if not choice.isdigit() or not 1 <= int(choice) <= len(rows):
            continue
        label = rows[int(choice) - 1][0]
        if label == "Reachability":
            settings["mode"] = "lan" if settings["mode"] == "public" else "public"
        elif label == "Hostnames":
            settings["autoDns"] = not settings["autoDns"]
        elif label == "Community Node hostname":
            settings["nodeDomain"] = ask("Community Node hostname", settings["nodeDomain"])
        elif label == "Control Plane hostname":
            settings["controlDomain"] = ask("Control Plane hostname", settings["controlDomain"])
        elif label == "Directory Server URL":
            settings["directoryUrl"] = ask("Directory Server URL (empty = none)", settings["directoryUrl"])
        elif label == "Push notifications":
            settings["push"] = not settings["push"]
        elif label == "Firebase credentials file":
            path = ask("Path of the Firebase service account JSON (empty = keep installed)", settings["firebaseFile"])
            if path and not Path(path).expanduser().is_file():
                print(bad("File not found.")); pause()
            else:
                settings["firebaseFile"] = str(Path(path).expanduser().resolve()) if path else ""
        elif label == "Images":
            if local_build_tag():
                print(warn("A local build is installed; its images are used.")); pause()
            else:
                settings["imagePrefix"] = ask("Image prefix", settings["imagePrefix"])
                settings["imageTag"] = ask("Image tag", settings["imageTag"])


def finish(code: int, success: str) -> None:
    print()
    print(good("✔ " + success) if code == 0 else bad(f"✖ Failed (exit {code}). See the output above and the logs folder."))
    pause()


def start(settings: dict) -> None:
    if settings["mode"] == "public" and not settings["autoDns"] and not (settings["nodeDomain"] and settings["controlDomain"]):
        print(bad("Enter both hostnames in Settings or turn on automatic hostnames.")); pause(); return
    print(bold("Applying:"))
    for label, value in describe(settings):
        print(f"  {label:<32} {value}")
    if confirm("Continue"):
        finish(run(engine_command("start", settings)), "Server started. Existing data was kept.")


def reinstall(settings: dict) -> None:
    print(bad(bold("This deletes ALL server data: databases, queued messages, identities and certificates.")))
    print("Apps must trust the new server identities afterwards. Public mode only.")
    if settings["mode"] != "public":
        print(warn("Switch Reachability to Public in Settings first.")); pause(); return
    if ask("Type DELETE to continue") == "DELETE":
        finish(run(engine_command("reinstall", settings)), "Server reinstalled.")


def update(source: str, preferences: dict) -> None:
    path, installation = "", ROOT
    if source == "source":
        path = ask("Sparrow source folder (repository root)", preferences.get("sourceDirectory", ""))
        if not (Path(path).expanduser() / "server" / "installer" / "build.py").is_file():
            print(bad("That is not a Sparrow source folder (server/installer/build.py is missing).")); pause(); return
        path = str(Path(path).expanduser().resolve())
        preferences["sourceDirectory"] = path
        save_preferences(preferences)
        if not installed():
            print(warn("A local build updates an existing installation. Install first.")); pause(); return
        print("Builds all six server images with Docker (several minutes). Data and settings are kept.")
    elif source == "zip":
        print("  1  Apply a sparrow-server.zip to this installation")
        print("  2  Move an existing server into this folder")
        if ask("Choose", "1") == "2":
            found = find_existing_installation()
            source = Path(ask("Folder of the current server", str(found or ""))).expanduser().resolve()
            print("Data, identities, certificates and settings are kept; the server is offline for about a minute.")
            if confirm(f"Move the server from {source} into {ROOT}"):
                finish(run(move_command(source)), "Server moved. Choose Start once, then you can delete the old folder.")
            return
        path = str(Path(ask("Path of sparrow-server.zip")).expanduser().resolve())
    else:
        print("Downloads the newest release from GitHub, verifies it and updates in place. Data and settings are kept.")
    if not confirm("Continue"):
        return
    code = run(update_command(source, path, installation))
    if code == 0:
        print(good("✔ Update finished."))
        restart_manager(installation)
    finish(code, "")


def nodes() -> None:
    while True:
        header({})
        print(bold(" Community Nodes") + dim("  (additional independent nodes)"))
        run(node_command("list"))
        print()
        entries = [("1", "Add a node"), ("2", "Install / start / stop / update / status / logs / remove a node"), ("b", "Back")]
        for key, label in entries:
            print(f"  {accent(key)}  {label}")
        choice = ask("Choose").lower()
        if choice in ("b", ""):
            return
        if choice == "1":
            name = ask("Name")
            mode = "public" if confirm("Public (reachable from the internet)?") else "lan"
            arguments = ["add", "--name", name, "--mode", mode]
            if mode == "public":
                if confirm("Automatic hostname (sslip.io)?"):
                    arguments.append("--auto-dns")
                else:
                    arguments += ["--node-domain", ask("Hostname")]
            plane = ask("Control Plane URL (empty = this installation)")
            if plane:
                arguments += ["--control-plane-url", plane]
            finish(run(node_command(*arguments)), "Node added. Install it next.")
        elif choice == "2":
            node_id = ask("Node ID (first column)")
            action = ask("Action (install/start/stop/update/status/logs/remove)", "status").lower()
            if action not in ("install", "start", "stop", "update", "status", "logs", "remove"):
                continue
            if action == "remove" and not confirm(f"Remove node {node_id}? Its data is kept"):
                continue
            finish(run(node_command(action, "--id", node_id)), f"Node {action} completed.")


def create_from_source() -> int:
    print(bold("Create your Sparrow server"))
    print("This is the source code. The server folder is created in the location you choose.")
    target = Path(ask("Server folder", str(default_server_folder()))).expanduser()
    if run(create_command(target)):
        return 1
    os.chdir(target)
    if WINDOWS:
        return subprocess.run([sys.executable, str(target / "app" / "manager_terminal.py")]).returncode
    os.execv(sys.executable, [sys.executable, str(target / "app" / "manager_terminal.py")])
    return 0


def main() -> int:
    if SOURCE_TREE:
        return create_from_source()
    settings, preferences = load_settings()
    while True:
        choice = menu(settings)
        if choice in ("q", "quit", "exit"):
            return 0
        if choice == "1":
            start(settings)
        elif choice in ("2", "3", "4"):
            action = {"2": "stop", "3": "status", "4": "logs"}[choice]
            finish(run(engine_command(action, settings)), f"{action.capitalize()} completed.")
        elif choice == "5":
            edit_settings(settings, preferences)
        elif choice == "6":
            update("release", preferences)
        elif choice == "7":
            update("source", preferences)
        elif choice == "8":
            update("zip", preferences)
        elif choice == "9":
            nodes()
        elif choice == "0":
            reinstall(settings)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print()
        sys.exit(0)
