from __future__ import annotations

import json
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

from . import LABELS
from .io import ensure_parent, normalize_text, normalized_text_key, read_jsonl, stable_id, write_jsonl
from .synthetic_generation import GeneratedPair, OllamaContrastiveGenerator
from .teacher_labeling import DEFAULT_OLLAMA_ENDPOINT, collect_expected_teacher_decisions

BEHAVIORAL_PROMPT_VERSION = "sparrow-safety-behavioral-v1"


@dataclass(frozen=True, slots=True)
class BehavioralFocus:
    id: str
    target_label: str
    language: str
    guidance: str


FOCUS_PACKS: tuple[BehavioralFocus, ...] = (
    BehavioralFocus(
        id="credential-intent-en",
        target_label="credential_request",
        language="en",
        guidance=(
            "Concentrate on the difference between ASKING FOR a credential and merely MENTIONING/DELIVERING one. "
            "Positive messages must explicitly ask the recipient to send, tell, share, forward, read out, or disclose a password, PIN, OTP, "
            "verification code, login code, or security code. Negative messages must stay on the same credential topic but be notifications/status "
            "messages such as 'Your verification code is 123456', code-expiry notices, instructions about where to enter a code, completed login/setup "
            "messages, or warnings not to share. Include realistic numeric placeholder codes in many negatives."
        ),
    ),
    BehavioralFocus(
        id="credential-intent-de",
        target_label="credential_request",
        language="de",
        guidance=(
            "Fokussiere auf den Unterschied zwischen dem ANFORDERN eines Zugangscodes und dem bloßen ERWÄHNEN/ÜBERMITTELN eines Codes. "
            "Positive Nachrichten müssen ausdrücklich verlangen, dass der Empfänger Passwort, PIN, OTP, Bestätigungs-, Anmelde- oder Sicherheitscode "
            "sendet, nennt, teilt oder vorliest. Negative Nachrichten bleiben beim gleichen Thema, sind aber Status-/Benachrichtigungstexte wie "
            "'Dein Bestätigungscode lautet 123456', Ablaufhinweise, Eingabehinweise, abgeschlossene Anmeldungen oder Warnungen, Codes niemals zu teilen."
        ),
    ),
    BehavioralFocus(
        id="urgent-pressure-en",
        target_label="urgent_action_request",
        language="en",
        guidance=(
            "Positive messages must pressure the recipient to act immediately/now/without delay AND include a consequence, threat, loss, lock, suspension, "
            "cancellation, penalty, or similar coercive outcome. Negative messages should use urgency/deadline vocabulary but be ordinary reminders, schedules, "
            "status updates, already-completed consequences, or non-coercive time-sensitive information."
        ),
    ),
    BehavioralFocus(
        id="urgent-pressure-de",
        target_label="urgent_action_request",
        language="de",
        guidance=(
            "Positive Nachrichten müssen den Empfänger zu sofortigem/umgehendem/unverzüglichem Handeln drängen UND eine Folge wie Sperrung, Verlust, "
            "Stornierung, Strafe oder Deaktivierung nennen. Verwende natürliche Varianten mit 'sofort', 'jetzt', 'umgehend', 'unverzüglich', 'handle', "
            "'bestätige', 'reagiere', 'erledige'. Negative Nachrichten dürfen ähnliche Frist-/Dringlichkeitswörter enthalten, sind aber normale Erinnerungen, "
            "Termin-/Statusmeldungen oder berichten über bereits eingetretene Folgen ohne Handlungsdruck."
        ),
    ),
    BehavioralFocus(
        id="payment-intent-en",
        target_label="payment_request",
        language="en",
        guidance=(
            "Positive messages must ask/instruct the recipient to send, transfer, pay, wire, reimburse, buy gift cards, or send crypto/value. Vary polite requests, "
            "imperatives, amounts and destinations. Negative messages must remain on the payment topic but describe completed transfers, receipts, balances, invoices, "
            "refund status, or questions such as whether a payment arrived, without asking the recipient to send money/value."
        ),
    ),
    BehavioralFocus(
        id="payment-intent-de",
        target_label="payment_request",
        language="de",
        guidance=(
            "Positive Nachrichten müssen den Empfänger auffordern, Geld/Wert zu senden, zu überweisen, zu bezahlen, Gutscheinkarten zu kaufen oder Krypto zu senden. "
            "Variiere höfliche Bitten und Imperative, Beträge und Ziele; nutze natürliche Formen wie 'Überweise mir 200 Euro auf dieses Konto'. Negative Nachrichten "
            "bleiben beim Zahlungsthema, beschreiben aber bereits erfolgte Überweisungen, Belege, Kontostände, Rechnungen, Erstattungsstatus oder fragen, ob eine Zahlung "
            "angekommen ist, ohne den Empfänger zu einer Zahlung aufzufordern."
        ),
    ),
)


def _batch(values: list[int], size: int) -> Iterable[list[int]]:
    for start in range(0, len(values), size):
        yield values[start : start + size]


def _cache_key(model: str, focus_id: str, pair_index: int) -> str:
    return f"{BEHAVIORAL_PROMPT_VERSION}\x1f{model}\x1f{focus_id}\x1f{pair_index}"


def _load_cache(path: Path) -> dict[str, GeneratedPair]:
    if not path.exists():
        return {}
    result: dict[str, GeneratedPair] = {}
    for row in read_jsonl(path):
        if row.get("prompt_version") != BEHAVIORAL_PROMPT_VERSION:
            continue
        key = str(row.get("generation_key", ""))
        if not key:
            continue
        result[key] = GeneratedPair(
            global_index=int(row["pair_index"]),
            positive_text=normalize_text(str(row["positive_text"])),
            negative_text=normalize_text(str(row["negative_text"])),
        )
    return result


def _append_cache(path: Path, entries: Iterable[dict[str, Any]]) -> None:
    ensure_parent(path)
    with path.open("a", encoding="utf-8", newline="\n") as handle:
        for entry in entries:
            handle.write(json.dumps(entry, ensure_ascii=False, sort_keys=True))
            handle.write("\n")
            handle.flush()


def _expected(target_label: str, positive: bool) -> dict[str, bool]:
    return {label: bool(positive and label == target_label) for label in LABELS}


def _candidate(*, model: str, focus: BehavioralFocus, pair_index: int, polarity: str, text: str) -> dict[str, Any]:
    positive = polarity == "positive"
    expected = _expected(focus.target_label, positive)
    return {
        "id": stable_id("sparrow-safety-behavioral-v1", focus.id, polarity, normalized_text_key(text)),
        "text": text,
        "language": focus.language,
        "source": "generated_behavioral_contrastive",
        "source_id": f"{focus.id}:{pair_index}:{polarity}",
        "source_split": "",
        "source_label": f"{focus.target_label}:{polarity}",
        "synthetic": True,
        "reviewed": False,
        "cluster_id": "",
        "split": "",
        "duplicate_sources": [],
        "expected_labels": expected,
        "generation_model": model,
        "generation_prompt_version": BEHAVIORAL_PROMPT_VERSION,
        "generation_focus_id": focus.id,
        "generation_target_label": focus.target_label,
        "generation_pair_index": pair_index,
        "generation_polarity": polarity,
    }


def _deduplicate(rows: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], int, int]:
    by_text: dict[str, list[dict[str, Any]]] = {}
    for row in rows:
        by_text.setdefault(normalized_text_key(str(row["text"])), []).append(row)
    unique: list[dict[str, Any]] = []
    duplicate_rows_removed = 0
    conflicting_groups = 0
    for values in by_text.values():
        vectors = {tuple(bool(value["expected_labels"][label]) for label in LABELS) for value in values}
        if len(vectors) > 1:
            conflicting_groups += 1
            duplicate_rows_removed += len(values)
            continue
        unique.append(values[0])
        duplicate_rows_removed += len(values) - 1
    unique.sort(key=lambda row: str(row["id"]))
    return unique, duplicate_rows_removed, conflicting_groups


def generate_behavioral_augmentation(
    base_input_path: Path,
    contract_path: Path,
    output_path: Path,
    generated_output_path: Path,
    rejected_output_path: Path,
    report_path: Path,
    generation_cache_path: Path,
    validation_cache_path: Path,
    *,
    generator_model: str,
    validator_model_a: str,
    validator_model_b: str,
    endpoint: str = DEFAULT_OLLAMA_ENDPOINT,
    pairs_per_focus: int = 150,
    generation_batch_size: int = 8,
    validation_batch_size: int = 16,
    min_confidence: float = 0.90,
    timeout_seconds: float = 240.0,
    max_retries: int = 3,
) -> dict[str, Any]:
    if pairs_per_focus < 1:
        raise ValueError("pairs_per_focus must be positive")
    if not 1 <= generation_batch_size <= 16:
        raise ValueError("generation_batch_size must be between 1 and 16")
    if not 1 <= validation_batch_size <= 32:
        raise ValueError("validation_batch_size must be between 1 and 32")

    base_rows = list(read_jsonl(base_input_path))
    contract_rows = list(read_jsonl(contract_path))
    if not base_rows:
        raise ValueError("Base labeled dataset is empty")
    if not contract_rows:
        raise ValueError("Behavioral contract is empty")
    contract_keys = {normalized_text_key(str(row["text"])) for row in contract_rows}
    filtered_base = [row for row in base_rows if normalized_text_key(str(row["text"])) not in contract_keys]
    base_contract_collisions_removed = len(base_rows) - len(filtered_base)

    generator = OllamaContrastiveGenerator(
        model=generator_model,
        endpoint=endpoint,
        timeout_seconds=timeout_seconds,
        max_retries=max_retries,
    )
    cache = _load_cache(generation_cache_path)
    generated: list[tuple[BehavioralFocus, GeneratedPair]] = []

    for number, focus in enumerate(FOCUS_PACKS, start=1):
        print(
            f"behavioral generation {number}/{len(FOCUS_PACKS)}: {focus.id} ({pairs_per_focus} pairs)",
            flush=True,
        )
        indices = list(range(pairs_per_focus))
        for batch_indices in _batch(indices, generation_batch_size):
            missing = [idx for idx in batch_indices if _cache_key(generator_model, focus.id, idx) not in cache]
            if missing:
                fresh = generator.generate(
                    target_label=focus.target_label,
                    language=focus.language,
                    global_indices=missing,
                    extra_guidance=focus.guidance,
                )
                entries: list[dict[str, Any]] = []
                for pair in fresh:
                    key = _cache_key(generator_model, focus.id, pair.global_index)
                    cache[key] = pair
                    entries.append(
                        {
                            "prompt_version": BEHAVIORAL_PROMPT_VERSION,
                            "generation_key": key,
                            "model": generator_model,
                            "focus_id": focus.id,
                            "target_label": focus.target_label,
                            "language": focus.language,
                            "pair_index": pair.global_index,
                            "positive_text": pair.positive_text,
                            "negative_text": pair.negative_text,
                        }
                    )
                _append_cache(generation_cache_path, entries)
            for idx in batch_indices:
                generated.append((focus, cache[_cache_key(generator_model, focus.id, idx)]))

    candidates: list[dict[str, Any]] = []
    for focus, pair in generated:
        candidates.append(_candidate(model=generator_model, focus=focus, pair_index=pair.global_index, polarity="positive", text=pair.positive_text))
        candidates.append(_candidate(model=generator_model, focus=focus, pair_index=pair.global_index, polarity="negative", text=pair.negative_text))
    candidates, duplicates_removed, conflicting_duplicate_groups = _deduplicate(candidates)

    # The product contract is an acceptance suite, not training data. Never allow exact
    # contract strings into the augmentation, even if the local generator happens to emit one.
    contract_generated_collisions_removed = sum(
        normalized_text_key(str(row["text"])) in contract_keys for row in candidates
    )
    candidates = [row for row in candidates if normalized_text_key(str(row["text"])) not in contract_keys]

    decisions, pass_a_rejections = collect_expected_teacher_decisions(
        candidates,
        validation_cache_path,
        model_a=validator_model_a,
        model_b=validator_model_b,
        min_confidence=min_confidence,
        endpoint=endpoint,
        batch_size=validation_batch_size,
        timeout_seconds=timeout_seconds,
        max_retries=max_retries,
        progress_prefix="behavioral validation",
    )

    accepted: list[dict[str, Any]] = []
    rejected: list[dict[str, Any]] = []
    rejection_counter: Counter[str] = Counter()
    accepted_by_focus: Counter[str] = Counter()
    for row in candidates:
        record_id = str(row["id"])
        left = decisions[(record_id, "A")]
        expected = {label: bool(row["expected_labels"][label]) for label in LABELS}
        reasons = list(pass_a_rejections.get(record_id, ()))
        right = decisions.get((record_id, "B"))
        minimum_confidence = min(left.confidences[label] for label in LABELS)

        if right is not None:
            agreement = all(left.labels[label] == right.labels[label] for label in LABELS)
            matches = all(left.labels[label] == expected[label] for label in LABELS) and all(
                right.labels[label] == expected[label] for label in LABELS
            )
            minimum_confidence = min(
                minimum_confidence,
                *(right.confidences[label] for label in LABELS),
            )
            if not agreement:
                reasons.append("validator_disagreement")
            if not matches:
                reasons.append("does_not_match_intended_labels")
            if minimum_confidence < min_confidence:
                reasons.append("below_confidence_threshold")

        # Keep rejection reasons unique when both pass-A prefiltering and pass-B
        # evaluation identify the same failure.
        reasons = list(dict.fromkeys(reasons))

        if reasons:
            for reason in reasons:
                rejection_counter[reason] += 1
            rejected.append(
                {
                    **row,
                    "validator_pass_a": left.to_dict(),
                    "validator_pass_b": right.to_dict() if right is not None else None,
                    "validator_min_confidence": minimum_confidence,
                    "rejection_reasons": reasons,
                }
            )
            continue

        accepted_by_focus[str(row["generation_focus_id"])] += 1
        accepted.append(
            {
                "id": record_id,
                "text": row["text"],
                "language": row["language"],
                "source": row["source"],
                "source_id": row["source_id"],
                "source_split": "",
                "source_label": row["source_label"],
                "synthetic": True,
                "reviewed": False,
                **expected,
                "cluster_id": "",
                "split": "",
                "duplicate_sources": [],
                "labeling_method": "behavioral_generated_then_dual_teacher_validated",
                "generation_model": generator_model,
                "generation_prompt_version": BEHAVIORAL_PROMPT_VERSION,
                "generation_focus_id": row["generation_focus_id"],
                "generation_target_label": row["generation_target_label"],
                "generation_pair_index": row["generation_pair_index"],
                "generation_polarity": row["generation_polarity"],
                "validator_model_a": validator_model_a,
                "validator_model_b": validator_model_b,
                "validator_min_confidence": minimum_confidence,
            }
        )

    accepted.sort(key=lambda row: str(row["id"]))
    rejected.sort(key=lambda row: str(row["id"]))
    write_jsonl(generated_output_path, accepted)
    write_jsonl(rejected_output_path, rejected)

    combined_by_text: dict[str, dict[str, Any]] = {}
    for row in filtered_base:
        combined_by_text.setdefault(normalized_text_key(str(row["text"])), dict(row))
    generated_collisions_with_base = 0
    for row in accepted:
        key = normalized_text_key(str(row["text"]))
        if key in combined_by_text:
            generated_collisions_with_base += 1
            continue
        combined_by_text[key] = row
    combined = sorted(combined_by_text.values(), key=lambda row: str(row["id"]))
    write_jsonl(output_path, combined)

    report = {
        "behavioral_prompt_version": BEHAVIORAL_PROMPT_VERSION,
        "generator_model": generator_model,
        "validator_model_a": validator_model_a,
        "validator_model_b": validator_model_b,
        "min_confidence": min_confidence,
        "pairs_per_focus": pairs_per_focus,
        "focuses": [focus.id for focus in FOCUS_PACKS],
        "base_rows": len(base_rows),
        "base_contract_collisions_removed": base_contract_collisions_removed,
        "candidate_rows_after_generation": len(candidates),
        "generation_exact_duplicates_removed": duplicates_removed,
        "generation_conflicting_duplicate_groups_removed": conflicting_duplicate_groups,
        "generated_contract_collisions_removed": contract_generated_collisions_removed,
        "accepted_generated_rows": len(accepted),
        "accepted_by_focus": dict(accepted_by_focus),
        "rejected_generated_rows": len(rejected),
        "generated_acceptance_rate": len(accepted) / max(1, len(candidates)),
        "rejection_reasons": dict(rejection_counter),
        "generated_collisions_with_base": generated_collisions_with_base,
        "combined_rows": len(combined),
        "contract": str(contract_path),
        "generation_cache": str(generation_cache_path),
        "validation_pass_a_rejected_rows": len(pass_a_rejections),
        "validation_pass_b_rows": len(candidates) - len(pass_a_rejections),
        "validation_cache": str(validation_cache_path),
        "generated_output": str(generated_output_path),
        "rejected_output": str(rejected_output_path),
        "combined_output": str(output_path),
    }
    ensure_parent(report_path)
    report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return report
