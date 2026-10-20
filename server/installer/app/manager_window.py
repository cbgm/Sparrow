#!/usr/bin/env python3
"""Sparrow Server installer window for Windows, macOS and Linux desktops (Tk, standard library only)."""
from __future__ import annotations

import os
from pathlib import Path
import subprocess
import sys
import tkinter as tk
from tkinter import filedialog, messagebox, ttk

from manager_core import (ROOT, SOURCE_TREE, WINDOWS, addresses, create_command, default_server_folder,
                          find_existing_installation, move_command, describe, engine_command, installed, load_settings,
                          local_build_tag, node_command, save_preferences, server_state, update_command,
                          version_text)
from ui_window import COLORS, HIDDEN, Card, EngineWindow, apply_style, window_fonts


class Installer(EngineWindow):
    failure_hint = "See the activity log and the logs folder."

    def __init__(self, root: tk.Tk):
        super().__init__(root, "Sparrow Server", ROOT, (1120, 820))
        self.settings, self.preferences = load_settings()
        self.build()
        self.refresh()
        self.root.after(100, self.pump)

    # ------------------------------------------------------------------ layout
    def build(self) -> None:
        body = self.build_frame("Sparrow Server", (("Open logs folder", self.open_logs),))
        body.columnconfigure(0, weight=11, uniform="column")
        body.columnconfigure(1, weight=9, uniform="column")
        left = tk.Frame(body, bg=COLORS["window"])
        right = tk.Frame(body, bg=COLORS["window"])
        left.grid(row=0, column=0, sticky="nsew", padx=(0, 12))
        right.grid(row=0, column=1, sticky="nsew")
        self.build_settings(left)
        self.build_server(right)
        self.build_updates(right)

    def build_settings(self, parent) -> None:
        card = Card(parent, "Settings", self.fonts)
        card.pack(fill="x")
        advanced = tk.Label(card, text="Advanced...", font=self.fonts["body"], bg=COLORS["card"],
                            fg=COLORS["accent"], cursor="hand2")
        advanced.grid(row=0, column=1, sticky="e")
        advanced.bind("<Button-1>", lambda _: self.open_advanced())
        self.mode = tk.StringVar(value="Public (reachable from the internet)" if self.settings["mode"] == "public"
                                 else "LAN (this network only)")
        combo = ttk.Combobox(card, textvariable=self.mode, state="readonly", font=self.fonts["body"],
                             values=("LAN (this network only)", "Public (reachable from the internet)"))
        card.add(combo, "Reachability")
        combo.bind("<<ComboboxSelected>>", lambda _: self.update_fields())
        self.auto_dns = tk.BooleanVar(value=self.settings["autoDns"])
        card.add(ttk.Checkbutton(card, text="Automatic public hostnames (free sslip.io names, no domain needed)",
                                 variable=self.auto_dns, command=self.update_fields), sticky="w")
        self.node_domain = tk.StringVar(value=self.settings["nodeDomain"])
        self.control_domain = tk.StringVar(value=self.settings["controlDomain"])
        self.directory_url = tk.StringVar(value=self.settings["directoryUrl"])
        self.node_entry = card.add(ttk.Entry(card, textvariable=self.node_domain, font=self.fonts["body"]),
                                   "Community Node hostname")
        self.control_entry = card.add(ttk.Entry(card, textvariable=self.control_domain, font=self.fonts["body"]),
                                      "Control Plane hostname")
        card.add(ttk.Entry(card, textvariable=self.directory_url, font=self.fonts["body"]), "Directory Server URL")

        push = Card(parent, "Push notifications and phone verification", self.fonts)
        push.pack(fill="x", pady=(12, 0))
        self.push = tk.BooleanVar(value=self.settings["push"])
        push.add(ttk.Checkbutton(push, text="Enable push notifications (Firebase)", variable=self.push,
                                 command=self.update_fields), sticky="w")
        self.firebase = tk.StringVar(value=self.settings["firebaseFile"])
        chooser = tk.Frame(push, bg=COLORS["card"])
        chooser.columnconfigure(0, weight=1)
        self.firebase_entry = ttk.Entry(chooser, textvariable=self.firebase, font=self.fonts["body"])
        self.firebase_entry.grid(row=0, column=0, sticky="ew")
        self.firebase_button = self.button(chooser, "Choose...", self.choose_firebase)
        self.firebase_button.grid(row=0, column=1, padx=(8, 0))
        push.add(chooser, "Service account JSON")
        push.hint("Leave the file empty to keep the installed credentials. "
                  "They need FCM and Authentication access to Sparrow's Firebase project.")

        self.image_prefix = tk.StringVar(value=self.settings["imagePrefix"])
        self.image_tag = tk.StringVar(value=self.settings["imageTag"])
        self.update_fields()

    def open_advanced(self) -> None:
        dialog = tk.Toplevel(self.root, bg=COLORS["window"], padx=16, pady=16)
        dialog.title("Advanced settings")
        dialog.transient(self.root)
        card = Card(dialog, "Advanced", self.fonts)
        card.pack(fill="both", expand=True)
        state = "readonly" if local_build_tag() else "normal"
        card.add(ttk.Entry(card, textvariable=self.image_prefix, font=self.fonts["body"], width=36, state=state),
                 "Image prefix")
        card.add(ttk.Entry(card, textvariable=self.image_tag, font=self.fonts["body"], width=36, state=state),
                 "Image tag")
        if state == "readonly":
            card.hint("A local build is installed; its images are used.")
        card.add(self.row_of(card, ("Reinstall from scratch...", self.reinstall, "Danger")), sticky="w")
        card.hint("Public mode only. Erases every server database, identity, queued message and certificate.")
        ttk.Button(dialog, text="Close", command=dialog.destroy).pack(anchor="e", pady=(12, 0))

    def build_server(self, parent) -> None:
        self.found = tk.Frame(parent, bg="#E6F6FA", highlightthickness=1, highlightbackground=COLORS["accent"],
                              padx=16, pady=12)
        self.found_text = tk.Label(self.found, font=self.fonts["body"], bg="#E6F6FA", fg=COLORS["text"],
                                   justify="left", anchor="w", wraplength=420)
        self.found_text.pack(fill="x")
        self.button(self.found, "Move server here", self.move_here, "Primary").pack(anchor="w", pady=(8, 0))
        card = Card(parent, "Server", self.fonts)
        card.pack(fill="x")
        self.server_card = card
        self.address_frame = tk.Frame(card, bg=COLORS["card"])
        self.address_frame.columnconfigure(1, weight=1)
        card.add(self.address_frame)
        actions = tk.Frame(card, bg=COLORS["card"])
        self.start_button = self.button(actions, "Install", lambda: self.server_action("start"), "Primary")
        self.start_button.grid(row=0, column=0, columnspan=2, sticky="ew", padx=(0, 8), pady=4)
        self.button(actions, "Stop", lambda: self.server_action("stop")).grid(row=0, column=2, sticky="ew", pady=4)
        for column in range(3):
            actions.columnconfigure(column, weight=1, uniform="action")
        for index, (text, command) in enumerate((("Status", lambda: self.server_action("status")),
                                                 ("Logs", lambda: self.server_action("logs")),
                                                 ("Nodes...", self.open_nodes))):
            self.button(actions, text, command).grid(row=1, column=index, sticky="ew", padx=(0, 8), pady=4)
        card.add(actions, sticky="ew")

    def build_updates(self, parent) -> None:
        card = Card(parent, "Updates", self.fonts)
        card.pack(fill="x", pady=(12, 0))
        self.updates_card = card
        self.version = tk.Label(card, font=self.fonts["body"], bg=COLORS["card"], fg=COLORS["text"], anchor="w")
        card.add(self.version, "Installed")
        card.add(self.row_of(card, ("Update to latest release", self.update_release, "Primary")), sticky="w")
        card.add(self.row_of(card, ("Build from source...", self.build_source, ""),
                             ("Apply ZIP...", self.apply_zip, "")), sticky="w")
        self.migrate_row = card.add(self.row_of(card, ("Move existing server here...", self.move_here, "")),
                                    sticky="w")
        card.hint("Updates keep all data, identities and settings. The server restarts briefly.")

    # ------------------------------------------------------------------ state
    def update_fields(self) -> None:
        public = self.mode.get().startswith("Public")
        manual = public and not self.auto_dns.get()
        for entry in (self.node_entry, self.control_entry):
            entry.configure(state="normal" if manual else "disabled")
        push = "normal" if self.push.get() else "disabled"
        self.firebase_entry.configure(state=push)
        self.firebase_button.configure(state=push)

    def collect(self) -> dict:
        self.settings.update({
            "mode": "public" if self.mode.get().startswith("Public") else "lan",
            "autoDns": self.auto_dns.get(), "nodeDomain": self.node_domain.get().strip(),
            "controlDomain": self.control_domain.get().strip(), "directoryUrl": self.directory_url.get().strip(),
            "push": self.push.get(), "firebaseFile": self.firebase.get().strip(),
            "imagePrefix": self.image_prefix.get().strip(), "imageTag": self.image_tag.get().strip(),
        })
        self.preferences["settings"] = self.settings
        save_preferences(self.preferences)
        return self.settings

    def refresh(self) -> None:
        state, level = server_state()
        self.set_pill(state, level)
        cp_mode = "Public" if self.settings["mode"] == "public" else "LAN"
        self.subtitle.configure(text=f"Control Plane + Community Node   |   {cp_mode if installed() else 'not installed'}"
                                     f"   |   {ROOT}")
        self.start_button.configure(text="Start / apply settings" if installed() else "Install")
        self.version.configure(text=version_text() if installed() else "-")
        for child in self.address_frame.winfo_children():
            child.destroy()
        found = addresses()
        if not found:
            tk.Label(self.address_frame, text="Public addresses appear here once the server runs in Public mode.",
                     font=self.fonts["small"], bg=COLORS["card"], fg=COLORS["muted"]).grid(sticky="w")
        for row, (label, value) in enumerate(found.items()):
            tk.Label(self.address_frame, text=label, font=self.fonts["body"], bg=COLORS["card"],
                     fg=COLORS["muted"]).grid(row=row, column=0, sticky="w", padx=(0, 12), pady=2)
            tk.Label(self.address_frame, text=value, font=self.fonts["body"], bg=COLORS["card"],
                     fg=COLORS["text"]).grid(row=row, column=1, sticky="w")
            ttk.Button(self.address_frame, text="Copy", command=lambda text=value: self.copy(text)).grid(
                row=row, column=2, padx=(8, 0), pady=2)
        if installed():
            self.migrate_row.grid_remove()
            self.found.pack_forget()
            self.updates_card.pack(fill="x", pady=(12, 0), after=self.server_card)
        else:
            self.migrate_row.grid()
            existing = find_existing_installation()
            self.existing = existing
            if existing is not None:
                self.found_text.configure(text=f"Your running server was found in\n{existing}\n\n"
                                               "Move it into this folder; afterwards you can delete the old folder.")
                self.found.pack(fill="x", pady=(0, 12), before=self.server_card)
                self.updates_card.pack_forget()
                self.start_button.configure(state="disabled")
            else:
                self.found.pack_forget()
                self.updates_card.pack(fill="x", pady=(12, 0), after=self.server_card)

    def open_logs(self) -> None:
        folder = ROOT / "logs"
        folder.mkdir(exist_ok=True)
        if WINDOWS:
            os.startfile(folder)  # noqa: S606 - opens the folder in Explorer
        else:
            subprocess.Popen(["open" if sys.platform == "darwin" else "xdg-open", str(folder)])

    # ------------------------------------------------------------------ actions
    def server_action(self, action: str) -> None:
        settings = self.collect()
        if action == "start":
            if settings["mode"] == "public" and not settings["autoDns"] and not (
                    settings["nodeDomain"] and settings["controlDomain"]):
                messagebox.showwarning("Sparrow Server", "Enter both hostnames or use automatic public hostnames.")
                return
            summary = "\n".join(f"{label}: {value}" for label, value in describe(settings))
            verb = "Start the server with these settings?" if installed() else "Install the server?"
            if not messagebox.askyesno("Sparrow Server", f"{verb}\n\n{summary}\n\nExisting data is kept."):
                return
        self.run({"start": "Starting", "stop": "Stopping", "status": "Status", "logs": "Logs"}[action],
                 engine_command(action, settings))

    def reinstall(self) -> None:
        settings = self.collect()
        if settings["mode"] != "public":
            messagebox.showinfo("Reinstall", "Reinstalling from scratch is only available in Public mode.")
            return
        if messagebox.askyesno("Delete all server data", "Reinstall from scratch? All databases, queued messages, "
                               "server identities and certificates are permanently erased. Apps must trust the "
                               "NEW server identities.", icon="warning"):
            self.run("Reinstalling", engine_command("reinstall", settings))

    def choose_firebase(self) -> None:
        path = filedialog.askopenfilename(title="Firebase service account",
                                          filetypes=(("Service account JSON", "*.json"), ("All files", "*.*")))
        if path:
            self.firebase.set(path)

    def updated(self, installation: Path):
        def reopen():
            subprocess.Popen([sys.executable, str(installation / "app" / "manager_window.py")], cwd=installation)
            self.root.destroy()
        return reopen

    def update_release(self) -> None:
        text = ("Download the latest Sparrow server release and update this installation? "
                "Data and settings are kept; the server restarts briefly." if installed()
                else "Download the latest Sparrow server release into this folder before installing?")
        if messagebox.askyesno("Update to latest release", text):
            self.run("Updating to the latest release", update_command("release"), self.updated(ROOT))

    def build_source(self) -> None:
        if not installed():
            messagebox.showinfo("Build from source", "A local build updates an existing installation. Install first.")
            return
        folder = filedialog.askdirectory(title="Your Sparrow source folder (repository root)",
                                         initialdir=self.preferences.get("sourceDirectory") or str(Path.home()))
        if not folder:
            return
        if not (Path(folder) / "server" / "installer" / "build.py").is_file():
            messagebox.showerror("Build from source", f"{folder} is not a Sparrow source folder.")
            return
        self.preferences["sourceDirectory"] = folder
        save_preferences(self.preferences)
        if messagebox.askyesno("Build from source", f"Build all server images from\n{folder}\nwith Docker and apply "
                               "them? This takes several minutes; data and settings are kept."):
            self.run("Building from source", update_command("source", folder), self.updated(ROOT))

    def apply_zip(self) -> None:
        path = filedialog.askopenfilename(title="Sparrow server bundle", filetypes=(("Server bundle", "*.zip"),))
        if path and messagebox.askyesno("Apply ZIP", f"Apply\n{path}\nto this installation? Data and settings are kept."):
            self.run("Applying the bundle", update_command("zip", path), self.updated(ROOT))

    def move_here(self) -> None:
        source = getattr(self, "existing", None)
        if source is None:
            folder = filedialog.askdirectory(title="Folder of your current Sparrow server")
            if not folder:
                return
            source = Path(folder)
            if not (source / "control-plane" / ".env.runtime").is_file():
                messagebox.showerror("Move server", f"{folder} does not contain an installed Sparrow server.")
                return
        if messagebox.askyesno("Move server here", f"Move the server from\n{source}\ninto\n{ROOT}?\n\n"
                               "Its data, identities, certificates and settings are kept. The server is offline "
                               "for about a minute. If anything fails it is restored in the old folder."):
            self.run("Moving the server", move_command(source), self.moved)

    def moved(self) -> None:
        self.settings, self.preferences = load_settings()
        self.refresh()
        messagebox.showinfo("Server moved", "The server now runs from this folder. Click Start / apply settings "
                            "once, check that messaging works, then you can delete the old folder.")

    def open_nodes(self) -> None:
        NodesDialog(self)


class NodesDialog:
    """Lists independent Community Nodes and runs their actions."""

    def __init__(self, installer: Installer):
        self.installer = installer
        self.window = tk.Toplevel(installer.root, bg=COLORS["window"], padx=16, pady=16)
        self.window.title("Community Nodes")
        self.window.geometry("760x420")
        columns = ("id", "name", "mode", "state", "endpoint")
        self.table = ttk.Treeview(self.window, columns=columns, show="headings", height=10)
        for column, width in zip(columns, (120, 140, 70, 120, 260)):
            self.table.heading(column, text=column.capitalize())
            self.table.column(column, width=width)
        self.table.pack(fill="both", expand=True)
        buttons = tk.Frame(self.window, bg=COLORS["window"])
        buttons.pack(fill="x", pady=(12, 0))
        ttk.Button(buttons, text="Add...", command=self.add, style="Primary.TButton").pack(side="left", padx=(0, 8))
        for action in ("install", "start", "stop", "update", "status", "logs", "remove"):
            ttk.Button(buttons, text=action.capitalize(), command=lambda chosen=action: self.act(chosen)).pack(
                side="left", padx=(0, 8))
        self.reload()

    def reload(self) -> None:
        self.table.delete(*self.table.get_children())
        result = subprocess.run(node_command("list"), cwd=ROOT, capture_output=True, text=True,
                                creationflags=HIDDEN)
        for line in result.stdout.splitlines():
            cells = line.split("\t")
            if len(cells) == 5:
                self.table.insert("", "end", values=cells)

    def selected(self) -> str:
        selection = self.table.selection()
        if not selection:
            messagebox.showinfo("Community Nodes", "Select a node first.", parent=self.window)
            return ""
        return str(self.table.item(selection[0], "values")[0])

    def act(self, action: str) -> None:
        node = self.selected()
        if not node:
            return
        if action == "remove" and not messagebox.askyesno("Remove node", f"Remove node {node}?", parent=self.window):
            return
        self.installer.run(f"Community Node {node}: {action}", node_command(action, "--id", node), self.reload)

    def add(self) -> None:
        dialog = tk.Toplevel(self.window, bg=COLORS["card"], padx=18, pady=16)
        dialog.title("Add Community Node")
        name, plane = tk.StringVar(), tk.StringVar()
        public, automatic, hostname = tk.BooleanVar(value=True), tk.BooleanVar(value=True), tk.StringVar()
        rows = (("Name", ttk.Entry(dialog, textvariable=name, width=40)),
                ("", ttk.Checkbutton(dialog, text="Public (reachable from the internet)", variable=public)),
                ("", ttk.Checkbutton(dialog, text="Automatic hostname (sslip.io)", variable=automatic)),
                ("Own hostname", ttk.Entry(dialog, textvariable=hostname, width=40)),
                ("Control Plane URL", ttk.Entry(dialog, textvariable=plane, width=40)))
        for row, (label, widget) in enumerate(rows):
            tk.Label(dialog, text=label, bg=COLORS["card"]).grid(row=row, column=0, sticky="w", padx=(0, 10), pady=4)
            widget.grid(row=row, column=1, sticky="w", pady=4)
        tk.Label(dialog, text="Leave the Control Plane URL empty to use this installation.", bg=COLORS["card"],
                 fg=COLORS["muted"]).grid(row=len(rows), column=0, columnspan=2, sticky="w")

        def submit():
            if not name.get().strip():
                return
            arguments = ["add", "--name", name.get().strip(), "--mode", "public" if public.get() else "lan"]
            if public.get():
                arguments += ["--auto-dns"] if automatic.get() else ["--node-domain", hostname.get().strip()]
            if plane.get().strip():
                arguments += ["--control-plane-url", plane.get().strip()]
            dialog.destroy()
            self.installer.run(f"Adding node {name.get().strip()}", node_command(*arguments), self.reload)

        ttk.Button(dialog, text="Add", style="Primary.TButton", command=submit).grid(
            row=len(rows) + 1, column=1, sticky="e", pady=(12, 0))


class CreateFromSource:
    """First start from the source code: creates the server folder, then opens it."""

    def __init__(self, root: tk.Tk):
        self.root = root
        root.title("Sparrow Server")
        fonts = window_fonts(root)
        apply_style(root, fonts)
        card = Card(root, "Create your Sparrow server", fonts)
        card.pack(fill="both", expand=True, padx=24, pady=24)
        card.hint("This is the source code. Choose a folder for your server; the installer is set up there and "
                  "opens right away. Your server's data will live in that folder.")
        self.folder = tk.StringVar(value=str(default_server_folder()))
        chooser = tk.Frame(card, bg=COLORS["card"])
        chooser.columnconfigure(0, weight=1)
        ttk.Entry(chooser, textvariable=self.folder, width=52).grid(row=0, column=0, sticky="ew")
        ttk.Button(chooser, text="Choose...", command=self.choose).grid(row=0, column=1, padx=(8, 0))
        card.add(chooser, "Server folder")
        self.message = card.hint("")
        ttk.Button(card, text="Create and open", command=self.create, style="Primary.TButton").grid(row=card.row, column=1, sticky="e",
                                                                          pady=(12, 0))

    def choose(self) -> None:
        folder = filedialog.askdirectory(title="Where should the server folder be created?")
        if folder:
            self.folder.set(str(Path(folder) / "Sparrow Server"))

    def create(self) -> None:
        target = Path(self.folder.get()).expanduser()
        result = subprocess.run(create_command(target), capture_output=True, text=True,
                                creationflags=HIDDEN)
        if result.returncode:
            self.message.configure(text=(result.stderr or result.stdout).strip(), fg=COLORS["danger"])
            return
        subprocess.Popen([sys.executable, str(target / "app" / "manager_window.py")], cwd=target)
        self.root.destroy()


def main() -> int:
    try:
        root = tk.Tk()
    except tk.TclError as error:
        print(f"No desktop available ({error}); starting the text menu instead.", file=sys.stderr)
        import manager_terminal
        return manager_terminal.main()
    try:
        if SOURCE_TREE:
            CreateFromSource(root)
            root.mainloop()
            return 0
        Installer(root)
    except Exception as error:  # show startup problems instead of failing silently
        messagebox.showerror("Sparrow Server could not start", str(error))
        raise
    root.mainloop()
    return 0


if __name__ == "__main__":
    sys.exit(main())
