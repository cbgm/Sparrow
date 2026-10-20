#!/usr/bin/env python3
"""Shared look and behaviour of the Sparrow installer windows (Tk, standard library only).

The Server installer and the Directory installer both build on EngineWindow: a header with a
status pill, white cards and an activity console that runs one engine command at a time.
"""
from __future__ import annotations

import os
from pathlib import Path
import queue
import signal
import subprocess
import threading
import tkinter as tk
from tkinter import font as tkfont, messagebox, ttk

WINDOWS = os.name == "nt"
COLORS = {
    "window": "#F1F4F8",
    "card": "#FFFFFF",
    "border": "#DEE4EC",
    "header": "#0B1B2B",
    "header_text": "#FFFFFF",
    "header_muted": "#94B0C7",
    "accent": "#0891B2",
    "accent_hover": "#0E7490",
    "text": "#192330",
    "muted": "#647282",
    "danger": "#BE2626",
    "danger_soft": "#FEF2F2",
    "console": "#0F1724",
    "console_text": "#D6DEE8",
    "ok": "#16874A",
    "warn": "#C46E06",
    "error": "#BE2626",
    "idle": "#647282",
}
HIDDEN = subprocess.CREATE_NO_WINDOW if WINDOWS else 0


def pick_family(root: tk.Tk, *candidates: str) -> str:
    available = set(tkfont.families(root))
    return next((name for name in candidates if name in available), "TkDefaultFont")


def window_fonts(root: tk.Tk) -> dict:
    family = pick_family(root, "Segoe UI", "SF Pro Text", "Helvetica Neue", "Cantarell",
                         "DejaVu Sans")
    mono = pick_family(root, "Consolas", "Menlo", "DejaVu Sans Mono", "Courier New")
    return {
        "body": (family, 10), "small": (family, 9), "section": (family, 12, "bold"),
        "title": (family, 18, "bold"), "pill": (family, 10, "bold"), "mono": (mono, 9)
    }


def apply_style(root: tk.Tk, fonts: dict) -> None:
    root.configure(bg=COLORS["window"])
    style = ttk.Style(root)
    style.theme_use("clam")
    base = {"font": fonts["body"], "padding": (14, 7), "borderwidth": 1, "focusthickness": 0}
    style.configure("TButton", background=COLORS["card"], foreground=COLORS["text"],
                    bordercolor=COLORS["border"], lightcolor=COLORS["card"],
                    darkcolor=COLORS["card"], **base)
    style.map("TButton", background=[("active", "#E8EDF3"), ("disabled", COLORS["window"])],
              foreground=[("disabled", COLORS["muted"])])
    style.configure("Primary.TButton", background=COLORS["accent"], foreground="white",
                    bordercolor=COLORS["accent"], lightcolor=COLORS["accent"],
                    darkcolor=COLORS["accent"], **base)
    style.map("Primary.TButton", background=[("active", COLORS["accent_hover"]),
                                             ("disabled", "#9CCFDB")],
              foreground=[("disabled", "white")])
    style.configure("Danger.TButton", background=COLORS["danger_soft"], foreground=COLORS["danger"],
                    bordercolor=COLORS["danger"], lightcolor=COLORS["danger_soft"],
                    darkcolor=COLORS["danger_soft"], **base)
    style.configure("TCheckbutton", background=COLORS["card"], foreground=COLORS["text"],
                    font=fonts["body"], indicatorbackground="white",
                    indicatorforeground=COLORS["accent"], indicatormargin=(0, 0, 8, 0))
    style.map("TCheckbutton", background=[("active", COLORS["card"])],
              indicatorbackground=[("disabled", COLORS["window"]), ("selected", "white")])
    style.configure("TEntry", padding=5, fieldbackground="white", bordercolor=COLORS["border"])
    style.map("TEntry", fieldbackground=[("disabled", COLORS["window"]),
                                         ("readonly", COLORS["window"])],
              foreground=[("disabled", COLORS["muted"])])
    style.configure("TCombobox", padding=5, fieldbackground="white", bordercolor=COLORS["border"],
                    arrowcolor=COLORS["text"], background="white")
    style.map("TCombobox", fieldbackground=[("readonly", "white")],
              background=[("readonly", "white")], foreground=[("readonly", COLORS["text"])])
    style.configure("Horizontal.TProgressbar", background=COLORS["accent"],
                    troughcolor=COLORS["border"], bordercolor=COLORS["border"],
                    lightcolor=COLORS["accent"], darkcolor=COLORS["accent"])


class Card(tk.Frame):
    """White panel with a title; rows are added in a two-column grid."""

    def __init__(self, parent, title: str, fonts: dict):
        super().__init__(parent, bg=COLORS["card"], highlightthickness=1,
                         highlightbackground=COLORS["border"], padx=18, pady=14)
        tk.Label(self, text=title, font=fonts["section"], bg=COLORS["card"],
                 fg=COLORS["text"]).grid(row=0, column=0, columnspan=2, sticky="w", pady=(0, 8))
        self.columnconfigure(1, weight=1)
        self.fonts = fonts
        self.row = 1

    def label(self, text: str) -> tk.Label:
        widget = tk.Label(self, text=text, font=self.fonts["body"], bg=COLORS["card"],
                          fg=COLORS["text"], anchor="w")
        widget.grid(row=self.row, column=0, sticky="w", padx=(0, 14), pady=4)
        return widget

    def add(self, widget: tk.Widget, label: str = "", sticky: str = "ew") -> tk.Widget:
        if label:
            self.label(label)
            widget.grid(row=self.row, column=1, sticky=sticky, pady=4)
        else:
            widget.grid(row=self.row, column=0, columnspan=2, sticky=sticky, pady=4)
        self.row += 1
        return widget

    def hint(self, text: str) -> tk.Label:
        widget = tk.Label(self, text=text, font=self.fonts["small"], bg=COLORS["card"],
                          fg=COLORS["muted"], justify="left", anchor="w", wraplength=430)
        return self.add(widget, sticky="w")


class EngineWindow:
    """Header, status bar and activity console; runs one engine at a time and streams its output."""

    failure_hint = "See the activity log."

    def __init__(self, root: tk.Tk, title: str, cwd: Path, size: tuple[int, int]):
        self.root = root
        self.cwd = cwd
        self.process: subprocess.Popen | None = None
        self.output: queue.Queue = queue.Queue()
        self.after_success = None
        self.busy_buttons: list[ttk.Button] = []
        self.fonts = window_fonts(root)
        root.title(title)
        root.minsize(size[0] - 140, size[1] - 140)
        root.geometry(f"{size[0]}x{size[1]}")
        apply_style(root, self.fonts)

    # ------------------------------------------------------------------ layout
    def button(self, parent, text: str, command, kind: str = "", busy: bool = True) -> ttk.Button:
        widget = ttk.Button(parent, text=text, command=command,
                            style=f"{kind}.TButton" if kind else "TButton", cursor="hand2")
        if busy:
            self.busy_buttons.append(widget)
        return widget

    def row_of(self, parent, *buttons: tuple) -> tk.Frame:
        frame = tk.Frame(parent, bg=COLORS["card"])
        for index, (text, command, kind) in enumerate(buttons):
            self.button(frame, text, command, kind).grid(row=0, column=index, padx=(0, 8), pady=2,
                                                         sticky="w")
        return frame

    def build_frame(self, title: str, links: tuple = ()) -> tk.Frame:
        """Header, status bar and activity console; returns the body frame for the cards."""
        header = tk.Frame(self.root, bg=COLORS["header"], padx=22, pady=14)
        header.pack(fill="x")
        tk.Label(header, text=title, font=self.fonts["title"], bg=COLORS["header"],
                 fg=COLORS["header_text"]).grid(row=0, column=0, sticky="w")
        self.subtitle = tk.Label(header, font=self.fonts["body"], bg=COLORS["header"],
                                 fg=COLORS["header_muted"])
        self.subtitle.grid(row=1, column=0, sticky="w")
        header.columnconfigure(0, weight=1)
        self.pill = tk.Label(header, font=self.fonts["pill"], fg="white", padx=16, pady=6)
        self.pill.grid(row=0, column=1, rowspan=2, padx=(0, 12))
        tk.Button(header, text="Refresh", command=self.refresh, bg=COLORS["header"],
                  fg=COLORS["header_muted"], activebackground=COLORS["header"],
                  activeforeground="white", relief="flat", bd=0, font=self.fonts["body"],
                  cursor="hand2").grid(row=0, column=2, rowspan=2)

        self.status = tk.Label(self.root, text="Ready.", anchor="w", padx=22, pady=5,
                               bg=COLORS["card"], fg=COLORS["muted"], font=self.fonts["small"])
        self.status.pack(side="bottom", fill="x")
        self.build_activity(links)
        body = tk.Frame(self.root, bg=COLORS["window"], padx=16, pady=16)
        body.pack(fill="both", expand=True)
        return body

    def build_activity(self, links: tuple) -> None:
        panel = tk.Frame(self.root, bg=COLORS["window"], padx=16)
        panel.pack(side="bottom", fill="x")
        bar = tk.Frame(panel, bg=COLORS["window"])
        bar.pack(fill="x", pady=(0, 6))
        tk.Label(bar, text="Activity", font=self.fonts["section"], bg=COLORS["window"],
                 fg=COLORS["text"]).pack(side="left")
        self.cancel_button = ttk.Button(bar, text="Cancel", command=self.cancel, state="disabled")
        self.cancel_button.pack(side="right")
        for text, command in links:
            tk.Button(bar, text=text, command=command, bg=COLORS["window"], fg=COLORS["accent"],
                      relief="flat", bd=0, activebackground=COLORS["window"],
                      font=self.fonts["body"], cursor="hand2").pack(side="right", padx=12)
        self.progress = ttk.Progressbar(bar, mode="indeterminate", length=220)
        frame = tk.Frame(panel, bg=COLORS["console"])
        frame.pack(fill="x", pady=(0, 10))
        self.log = tk.Text(frame, height=8, bg=COLORS["console"], fg=COLORS["console_text"],
                           relief="flat", font=self.fonts["mono"], insertbackground="white",
                           wrap="word", padx=10, pady=8, state="disabled")
        scroll = ttk.Scrollbar(frame, command=self.log.yview)
        self.log.configure(yscrollcommand=scroll.set)
        scroll.pack(side="right", fill="y")
        self.log.pack(side="left", fill="both", expand=True)

    def set_pill(self, text: str, level: str) -> None:
        self.pill.configure(text="  " + text + "  ", bg=COLORS[level])

    # ------------------------------------------------------------------ hooks
    def refresh(self) -> None:
        """Re-reads the installation state; called after every engine run."""

    def update_fields(self) -> None:
        """Enables or disables inputs; called whenever the window becomes idle."""

    # ------------------------------------------------------------------ helpers
    def copy(self, text: str) -> None:
        self.root.clipboard_clear()
        self.root.clipboard_append(text)
        self.status.configure(text=f"Copied {text}")

    def write(self, text: str) -> None:
        self.log.configure(state="normal")
        self.log.insert("end", text)
        if int(self.log.index("end-1c").split(".")[0]) > 4000:
            self.log.delete("1.0", "1000.0")
        self.log.see("end")
        self.log.configure(state="disabled")

    # ------------------------------------------------------------------ running engines
    def run(self, title: str, command: list[str], on_success=None) -> None:
        if self.process is not None:
            return
        self.log.configure(state="normal")
        self.log.delete("1.0", "end")
        self.log.configure(state="disabled")
        self.write(f"{title}\n")
        self.after_success = on_success
        options = {
            "cwd": self.cwd, "stdout": subprocess.PIPE, "stderr": subprocess.STDOUT,
            "stdin": subprocess.DEVNULL
        }
        if WINDOWS:
            options[
                "creationflags"] = subprocess.CREATE_NO_WINDOW | subprocess.CREATE_NEW_PROCESS_GROUP
        else:
            options["start_new_session"] = True
        try:
            self.process = subprocess.Popen(command, **options)
        except OSError as error:
            self.write(f"Could not start: {error}\n")
            return
        self.set_busy(True, title)
        threading.Thread(target=self.read_output, args=(self.process, ), daemon=True).start()

    def read_output(self, process: subprocess.Popen) -> None:
        for raw in iter(process.stdout.readline, b""):
            try:
                text = raw.decode("utf-8")
            except UnicodeDecodeError:
                text = raw.decode("cp850" if WINDOWS else "latin-1", errors="replace")
            self.output.put(text.replace("\r\n", "\n"))
        self.output.put(("done", process.wait()))

    def pump(self) -> None:
        try:
            while True:
                item = self.output.get_nowait()
                if isinstance(item, tuple):
                    self.finished(item[1])
                else:
                    self.write(item)
        except queue.Empty:
            pass
        self.root.after(100, self.pump)

    def finished(self, code: int) -> None:
        self.process = None
        self.set_busy(False, "")
        if code == 0:
            self.write("\nCompleted.\n")
            self.status.configure(text="Completed.")
            if self.after_success:
                self.after_success()
                return
        else:
            self.write(f"\nFailed (exit {code}). Existing data was not deleted.\n")
            self.status.configure(text=f"Failed (exit {code}). {self.failure_hint}")
        self.refresh()

    def set_busy(self, busy: bool, title: str) -> None:
        for widget in self.busy_buttons:
            widget.configure(state="disabled" if busy else "normal")
        self.cancel_button.configure(state="normal" if busy else "disabled")
        if busy:
            self.progress.pack(side="right", padx=12)
            self.progress.start(12)
            self.status.configure(text=title + " ...")
        else:
            self.progress.stop()
            self.progress.pack_forget()
            self.update_fields()

    def cancel(self) -> None:
        if self.process is None or not messagebox.askyesno(
                "Cancel", "Stop the running operation? Some services may already have started. "
                "No data is removed."):
            return
        if WINDOWS:
            subprocess.run(
                ["taskkill", "/PID", str(self.process.pid), "/T", "/F"], capture_output=True)
        else:
            os.killpg(self.process.pid, signal.SIGTERM)
