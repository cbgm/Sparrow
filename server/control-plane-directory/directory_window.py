#!/usr/bin/env python3
"""Sparrow Directory installer window for Windows, macOS and Linux (Tk, standard library only)."""
from __future__ import annotations

from pathlib import Path
import subprocess
import sys
import threading
import tkinter as tk
from tkinter import filedialog, messagebox, ttk

HERE = Path(__file__).resolve().parent
if not (HERE / "ui_window.py").is_file():  # source tree: same look as the server installer
    sys.path.insert(0, str(HERE.parent / "installer" / "app"))

import directory_installer as engine  # noqa: E402
from ui_window import COLORS, HIDDEN, Card, EngineWindow, apply_style, window_fonts  # noqa: E402

# Started from the source code: create a Directory folder first, like the server installer.
SOURCE_TREE = (HERE / "Build-SparrowDirectoryInstaller.py").is_file()

SHARED = "Sparrow Server's proxy (same IP)"
STANDALONE = "Own proxy (free ports 80/443)"


class DirectoryInstaller(EngineWindow):

    def __init__(self, root: tk.Tk):
        super().__init__(root, "Sparrow Directory", HERE, (1000, 720))
        self.public_ip = ""
        # The running server wins over the folder of the last installation, which may be gone.
        info = engine.installed_info()
        server = engine.find_server_folder() or engine.server_folder(info.get("serverRoot", ""))
        hostname = engine.installed_hostname()
        self.hostname = tk.StringVar(value=hostname)
        self.automatic = tk.BooleanVar(value=not hostname or bool(engine.automatic_ipv4(hostname)))
        self.proxy = tk.StringVar(
            value=STANDALONE if info and not info.get("serverRoot") else SHARED)
        self.server = tk.StringVar(value=str(server or ""))
        self.build()
        self.refresh()
        threading.Thread(target=self.lookup_ip, daemon=True).start()
        self.root.after(100, self.pump)

    # ------------------------------------------------------------------ layout
    def build(self) -> None:
        body = self.build_frame("Sparrow Directory")
        body.columnconfigure(0, weight=1, uniform="column")
        body.columnconfigure(1, weight=1, uniform="column")
        left = tk.Frame(body, bg=COLORS["window"])
        right = tk.Frame(body, bg=COLORS["window"])
        left.grid(row=0, column=0, sticky="nsew", padx=(0, 12))
        right.grid(row=0, column=1, sticky="nsew")

        card = Card(left, "Settings", self.fonts)
        card.pack(fill="x")
        card.add(
            ttk.Checkbutton(card, text="Automatic hostname from the public IP (free sslip.io name)",
                            variable=self.automatic, command=self.update_fields), sticky="w")
        self.hostname_entry = card.add(
            ttk.Entry(card, textvariable=self.hostname, font=self.fonts["body"]),
            "Directory hostname")
        combo = ttk.Combobox(card, textvariable=self.proxy, state="readonly",
                             font=self.fonts["body"], values=(SHARED, STANDALONE))
        combo.bind("<<ComboboxSelected>>", lambda _: self.update_fields())
        card.add(combo, "HTTPS proxy")
        chooser = tk.Frame(card, bg=COLORS["card"])
        chooser.columnconfigure(0, weight=1)
        self.server_entry = ttk.Entry(chooser, textvariable=self.server, font=self.fonts["body"])
        self.server_entry.grid(row=0, column=0, sticky="ew")
        self.server_button = self.button(chooser, "Choose...", self.choose_server)
        self.server_button.grid(row=0, column=1, padx=(8, 0))
        card.add(chooser, "Server folder")
        card.hint("The running Sparrow Server is found automatically. Its Control Plane is "
                  "registered in the Directory. Identities and data are always kept.")

        card = Card(right, "Directory", self.fonts)
        card.pack(fill="x")
        self.address_frame = tk.Frame(card, bg=COLORS["card"])
        self.address_frame.columnconfigure(0, weight=1)
        card.add(self.address_frame)
        actions = tk.Frame(card, bg=COLORS["card"])
        self.start_button = self.button(actions, "Install", self.install, "Primary")
        self.start_button.grid(row=0, column=0, columnspan=2, sticky="ew", padx=(0, 8), pady=4)
        self.button(actions, "Stop",
                    lambda: self.engine("Stopping", "stop")).grid(row=0, column=2, sticky="ew",
                                                                  pady=4)
        for column in range(3):
            actions.columnconfigure(column, weight=1, uniform="action")
        for index, (text, action) in enumerate(
            (("Status", "status"), ("Logs", "logs"), ("Servers", "planes"))):
            self.button(actions, text,
                        lambda chosen=action, title=text: self.engine(title, chosen)).grid(
                            row=1, column=index, sticky="ew", padx=(0, 8), pady=4)
        card.add(actions, sticky="ew")
        card.hint("Apps fetch and pin the Directory key automatically. When the URL changes, "
                  "enter the new one in the app's settings.")

    # ------------------------------------------------------------------ state
    def lookup_ip(self) -> None:
        address = engine.public_ipv4()
        self.root.after(0, lambda: self.ip_found(address))

    def ip_found(self, address: str) -> None:
        self.public_ip = address
        self.update_fields()
        self.refresh()

    def update_fields(self) -> None:
        if self.automatic.get():
            if self.public_ip:
                self.hostname.set(engine.automatic_hostname(self.public_ip))
            self.hostname_entry.configure(state="disabled")
        else:
            self.hostname_entry.configure(state="normal")
        shared = "normal" if self.proxy.get() == SHARED else "disabled"
        self.server_entry.configure(state=shared)
        self.server_button.configure(state=shared)

    def refresh(self) -> None:
        state, level = engine.state()
        self.set_pill(state, level)
        ip = f"public IP {self.public_ip}" if self.public_ip else "public IP unknown"
        self.subtitle.configure(text=f"Control Plane Directory   |   {ip}")
        installed = engine.installed_hostname()
        self.start_button.configure(text="Start / apply settings" if installed else "Install")
        for child in self.address_frame.winfo_children():
            child.destroy()
        info = engine.installed_info()
        rows = [(label, info[key])
                for label, key in (("Directory URL", "url"), ("Public key", "publicKey"))
                if info.get(key)]
        if installed and not info.get("url"):
            rows.append(("Directory URL", f"https://{installed}"))
        if not rows:
            tk.Label(self.address_frame,
                     text="The Directory URL appears here once it is installed.",
                     font=self.fonts["small"], bg=COLORS["card"],
                     fg=COLORS["muted"]).grid(sticky="w")
        for index, (label, value) in enumerate(rows):
            tk.Label(self.address_frame, text=label, font=self.fonts["small"], bg=COLORS["card"],
                     fg=COLORS["muted"]).grid(row=2 * index, column=0, sticky="w", pady=(4, 0))
            shown = value if len(value) <= 44 else value[:17] + "..." + value[-17:]
            tk.Label(self.address_frame, text=shown, font=self.fonts["body"], bg=COLORS["card"],
                     fg=COLORS["text"]).grid(row=2 * index + 1, column=0, sticky="w")
            ttk.Button(self.address_frame, text="Copy",
                       command=lambda text=value: self.copy(text)).grid(
                           row=2 * index + 1, column=1, sticky="e", padx=(8, 0))
        if installed and self.public_ip and engine.automatic_ipv4(installed) not in (
                "", self.public_ip):
            tk.Label(
                self.address_frame,
                text="Your public IP changed. Click Start to move the Directory "
                "to the new address.", font=self.fonts["small"], bg=COLORS["card"],
                fg=COLORS["warn"], justify="left",
                wraplength=380).grid(row=2 * len(rows), column=0, columnspan=2, sticky="w",
                                     pady=(6, 0))

    # ------------------------------------------------------------------ actions
    def engine(self, title: str, *arguments: str) -> None:
        self.run(title, [sys.executable, "-u", str(HERE / "directory_installer.py"), *arguments])

    def choose_server(self) -> None:
        folder = filedialog.askdirectory(title="Folder of your Sparrow Server")
        if folder:
            self.server.set(folder)

    def install(self) -> None:
        hostname = self.hostname.get().strip().lower()
        if not hostname:
            messagebox.showwarning("Sparrow Directory",
                                   "Enter the Directory hostname (the public IP is unknown).")
            return
        arguments = ["install", "--hostname", hostname]
        if self.proxy.get() == SHARED:
            if not self.server.get().strip():
                messagebox.showwarning("Sparrow Directory",
                                       "Choose the Sparrow Server folder, or use an own proxy.")
                return
            arguments += ["--server-folder", self.server.get().strip()]
            where = f"behind the HTTPS proxy of\n{self.server.get().strip()}"
        else:
            arguments.append("--standalone")
            where = "with its own HTTPS proxy on ports 80/443"
        if messagebox.askyesno(
                "Sparrow Directory",
                f"Install / start the Directory as\nhttps://{hostname}\n{where}?"
                "\n\nIts signing identity and data are kept."):
            self.engine("Installing the Directory", *arguments)


class CreateFolder:
    """First start from the source code: creates the Directory folder, then opens it there."""

    def __init__(self, root: tk.Tk):
        self.root = root
        root.title("Sparrow Directory")
        fonts = window_fonts(root)
        apply_style(root, fonts)
        card = Card(root, "Create your Sparrow Directory", fonts)
        card.pack(fill="both", expand=True, padx=24, pady=24)
        card.hint("This is the source code. Choose a folder for your Directory; the installer is "
                  "set up there and opens right away. A running Directory is taken over with its "
                  "identity and data; afterwards you can delete its old folder.")
        desktop = Path.home() / "Desktop"
        self.folder = tk.StringVar(value=str((desktop if desktop.is_dir() else Path.home())
                                             / "Sparrow Directory"))
        chooser = tk.Frame(card, bg=COLORS["card"])
        chooser.columnconfigure(0, weight=1)
        ttk.Entry(chooser, textvariable=self.folder, width=52).grid(row=0, column=0, sticky="ew")
        ttk.Button(chooser, text="Choose...", command=self.choose).grid(row=0, column=1,
                                                                        padx=(8, 0))
        card.add(chooser, "Directory folder")
        self.message = card.hint("")
        ttk.Button(card, text="Create and open", command=self.create,
                   style="Primary.TButton").grid(row=card.row, column=1, sticky="e", pady=(12, 0))

    def choose(self) -> None:
        folder = filedialog.askdirectory(title="Where should the Directory folder be created?")
        if folder:
            self.folder.set(str(Path(folder) / "Sparrow Directory"))

    def create(self) -> None:
        target = Path(self.folder.get()).expanduser()
        result = subprocess.run([sys.executable, str(HERE / "Build-SparrowDirectoryInstaller.py"),
                                 "--install", str(target)], capture_output=True, text=True,
                                creationflags=HIDDEN)
        if result.returncode:
            self.message.configure(text=(result.stderr or result.stdout).strip(),
                                   fg=COLORS["danger"])
            return
        subprocess.Popen([sys.executable, str(target / "directory_window.py")], cwd=target)
        self.root.destroy()


def main() -> int:
    try:
        root = tk.Tk()
    except tk.TclError as error:
        print(f"No desktop available ({error}). Use: python3 directory_installer.py install",
              file=sys.stderr)
        return 1
    try:
        CreateFolder(root) if SOURCE_TREE else DirectoryInstaller(root)
    except Exception as error:  # show startup problems instead of failing silently
        messagebox.showerror("Sparrow Directory could not start", str(error))
        raise
    root.mainloop()
    return 0


if __name__ == "__main__":
    sys.exit(main())
