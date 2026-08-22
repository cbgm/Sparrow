from __future__ import annotations

import json
from pathlib import Path

from . import LABELS


def render_markdown_report(report_path: Path, output_path: Path) -> str:
    report = json.loads(report_path.read_text(encoding="utf-8"))
    constraints = report.get("selection_constraints", {})
    lines = [
        "# Sparrow Safety classifier evaluation",
        "",
        "Threshold/model selection uses validation only; the test split remains held out until final evaluation.",
        "",
        (
            "Validation constraints: "
            f"precision >= `{float(constraints.get('min_validation_precision', 0.0)):.3f}`, "
            f"recall >= `{float(constraints.get('min_validation_recall', 0.0)):.3f}`, "
            f"FPR <= `{float(constraints.get('max_validation_false_positive_rate', 1.0)):.4f}`"
        ),
        "",
        "| Label | Split | Precision | Recall | F1 | FPR | Positives |",
        "|---|---:|---:|---:|---:|---:|---:|",
    ]
    for label in LABELS:
        entry = report["labels"][label]
        for split in ("train", "validation", "test"):
            metrics = entry[split]
            lines.append(
                f"| `{label}` | {split} | {metrics['precision']:.3f} | {metrics['recall']:.3f} | "
                f"{metrics['f1']:.3f} | {metrics['false_positive_rate']:.4f} | {metrics['positives']} |"
            )
        lines.append(
            f"| `{label}` | threshold | `{entry['threshold']:.6f}` |  |  |  | "
            f"{entry['threshold_selection']['strategy']}; C={entry.get('regularization_c', 'n/a')} |"
        )
    text = "\n".join(lines) + "\n"
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(text, encoding="utf-8")
    return text
