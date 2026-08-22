from __future__ import annotations

import json
from pathlib import Path

from . import LABELS


def render_markdown_report(report_path: Path, output_path: Path) -> str:
    report = json.loads(report_path.read_text(encoding="utf-8"))
    constraints = report.get("selection_constraints", {})
    classifier_type = str(report.get("classifier_type", "unknown"))
    lines = [
        "# Sparrow Safety classifier evaluation",
        "",
        f"Classifier: `{classifier_type}`",
        "",
        (
            "Model/threshold selection uses validation plus the explicit product behavioral contract; "
            "the statistical test split remains held out until final evaluation."
        ),
        "",
        (
            "Validation constraints: "
            f"precision >= `{float(constraints.get('min_validation_precision', 0.0)):.3f}`, "
            f"recall >= `{float(constraints.get('min_validation_recall', 0.0)):.3f}`, "
            f"FPR <= `{float(constraints.get('max_validation_false_positive_rate', 1.0)):.4f}`"
        ),
        "",
        f"Behavioral contract required: `{bool(constraints.get('behavioral_contract_required', False))}`",
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
        selection = entry["threshold_selection"]
        model_note = (
            f"hidden={entry.get('hidden_size', 'n/a')}, alpha={entry.get('regularization_alpha', 'n/a')}, "
            f"contract={'pass' if selection.get('behavioral_contract_satisfied', True) else 'FAIL'}"
        )
        lines.append(
            f"| `{label}` | threshold | `{entry['threshold']:.6f}` |  |  |  | "
            f"{selection['strategy']}; {model_note} |"
        )
    text = "\n".join(lines) + "\n"
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(text, encoding="utf-8")
    return text
