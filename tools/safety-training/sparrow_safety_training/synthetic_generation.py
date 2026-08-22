from __future__ import annotations

import json
import time
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from . import LABELS
from .io import ensure_parent, normalize_text, normalized_text_key, read_jsonl, stable_id, write_jsonl
from .teacher_labeling import (
    DEFAULT_OLLAMA_ENDPOINT,
    TeacherDecision,
    collect_dual_teacher_decisions,
)

GENERATION_PROMPT_VERSION = "sparrow-safety-contrastive-generator-v1"

_LABEL_GUIDANCE = {
    "urgent_action_request": (
        "The positive asks/pressures the recipient to act immediately or very soon, usually with a consequence. "
        "The negative should discuss urgency/deadlines/reminders without coercive immediate pressure."
    ),
    "credential_request": (
        "The positive asks the recipient to disclose/send/share a password, PIN, OTP, verification/login/security code. "
        "The negative should use similar credential vocabulary but be advice, a received code, setup instructions, or a warning not to share."
    ),
    "payment_request": (
        "The positive asks/instructs the recipient to send/pay/transfer money or value, buy gift cards, or send cryptocurrency. "
        "The negative should use similar payment vocabulary but describe completed payments, receipts, balances, or whether money arrived."
    ),
    "private_key_request": (
        "The positive asks the recipient to disclose/send/share a seed phrase, recovery phrase, mnemonic, private key, wallet seed, or backup words. "
        "The negative should use the same crypto-secret vocabulary but be security advice, backup/setup guidance, or a warning not to share."
    ),
}

_LANGUAGE_NAME = {
    "en": "English",
    "de": "German",
}


def _generation_schema(pair_count: int) -> dict[str, Any]:
    return {
        "type": "object",
        "properties": {
            "pairs": {
                "type": "array",
                "minItems": pair_count,
                "maxItems": pair_count,
                "items": {
                    "type": "object",
                    "properties": {
                        "index": {"type": "integer", "minimum": 0, "maximum": pair_count - 1},
                        "positive_text": {"type": "string", "minLength": 4, "maxLength": 500},
                        "negative_text": {"type": "string", "minLength": 4, "maxLength": 500},
                    },
                    "required": ["index", "positive_text", "negative_text"],
                    "additionalProperties": False,
                },
            }
        },
        "required": ["pairs"],
        "additionalProperties": False,
    }


def _generation_system_prompt(target_label: str, language: str, extra_guidance: str | None = None) -> str:
    language_name = _LANGUAGE_NAME.get(language, language)
    focus = f"\nAdditional behavioral focus:\n{extra_guidance.strip()}\n" if extra_guidance else ""
    return f"""You generate diverse contrastive training pairs for Sparrow, a messaging safety classifier.
Generate natural {language_name} chat/SMS-style messages only.

Target label: {target_label}
Definition: {_LABEL_GUIDANCE[target_label]}

For EVERY pair:
- positive_text MUST have {target_label}=true.
- positive_text MUST have all other Sparrow Safety labels=false.
- negative_text MUST have ALL four Sparrow Safety labels=false.
- The negative must be a hard contrast: reuse similar topic/vocabulary while changing the intent so the target request/pressure is absent.
- Do not use URLs, email addresses, phone numbers, or sender metadata as the deciding cue.
- Avoid copying well-known phishing templates verbatim.
- Vary scenario, syntax, tone, register, amounts/codes/crypto terminology, and message length.
- Keep messages realistic for person-to-person or service-style messaging.
- Do not quote or explain the labels in the generated message text.
- Do not make the negative a trivial unrelated sentence.
- Never include an actual usable secret, real account number, or real private key.
{focus}
Return only data matching the supplied JSON schema.
"""


def _generation_user_prompt(global_indices: list[int]) -> str:
    local = [{"index": index} for index, _ in enumerate(global_indices)]
    return (
        "Generate one unique positive/negative contrastive pair for every requested local index. "
        "Return exactly one pair for each index.\n\n"
        + json.dumps(local)
    )


@dataclass(frozen=True, slots=True)
class GeneratedPair:
    global_index: int
    positive_text: str
    negative_text: str


class OllamaContrastiveGenerator:
    def __init__(
        self,
        *,
        model: str,
        endpoint: str = DEFAULT_OLLAMA_ENDPOINT,
        timeout_seconds: float = 240.0,
        max_retries: int = 3,
    ) -> None:
        self.model = model
        self.endpoint = endpoint
        self.timeout_seconds = timeout_seconds
        self.max_retries = max_retries

    def generate(
        self,
        *,
        target_label: str,
        language: str,
        global_indices: list[int],
        extra_guidance: str | None = None,
    ) -> list[GeneratedPair]:
        if not global_indices:
            return []

        try:
            return self._generate_batch_with_retries(
                target_label=target_label,
                language=language,
                global_indices=global_indices,
                extra_guidance=extra_guidance,
            )
        except RuntimeError as exc:
            # Local LLMs occasionally violate even a strict JSON schema for larger batches.
            # Never weaken validation or guess which row belongs where. Instead reduce the
            # request size until the model can satisfy the schema. Successful sub-batches
            # are returned to the caller and cached immediately by the outer pipeline.
            if len(global_indices) == 1:
                raise RuntimeError(
                    f"Ollama could not generate a valid structured pair for target={target_label!r}, "
                    f"language={language!r}, pair_index={global_indices[0]} after retries."
                ) from exc

            midpoint = len(global_indices) // 2
            left = global_indices[:midpoint]
            right = global_indices[midpoint:]
            print(
                f"generation batch of {len(global_indices)} remained invalid after retries; "
                f"splitting into {len(left)} + {len(right)}",
                flush=True,
            )
            return [
                *self.generate(
                    target_label=target_label,
                    language=language,
                    global_indices=left,
                    extra_guidance=extra_guidance,
                ),
                *self.generate(
                    target_label=target_label,
                    language=language,
                    global_indices=right,
                    extra_guidance=extra_guidance,
                ),
            ]

    def _generate_batch_with_retries(
        self,
        *,
        target_label: str,
        language: str,
        global_indices: list[int],
        extra_guidance: str | None = None,
    ) -> list[GeneratedPair]:
        last_error: Exception | None = None
        temperatures = (0.85, 0.55, 0.20)

        for attempt in range(1, self.max_retries + 1):
            temperature = temperatures[min(attempt - 1, len(temperatures) - 1)]
            body = {
                "model": self.model,
                "messages": [
                    {"role": "system", "content": _generation_system_prompt(target_label, language, extra_guidance)},
                    {"role": "user", "content": _generation_user_prompt(global_indices)},
                ],
                "stream": False,
                "think": False,
                "format": _generation_schema(len(global_indices)),
                "options": {"temperature": temperature, "top_p": 0.9},
            }

            try:
                response = self._post(body)
                return self._parse_response(response, global_indices)
            except RuntimeError as exc:
                last_error = exc
                if attempt == self.max_retries:
                    break
                print(
                    f"invalid structured generation response for batch size {len(global_indices)} "
                    f"(attempt {attempt}/{self.max_retries}); retrying",
                    flush=True,
                )
                time.sleep(min(4.0, float(attempt)))

        raise RuntimeError(
            f"Ollama returned invalid structured generation output after {self.max_retries} attempts "
            f"for a batch of {len(global_indices)} pair(s)."
        ) from last_error

    @staticmethod
    def _parse_response(response: dict[str, Any], global_indices: list[int]) -> list[GeneratedPair]:
        try:
            content = response["message"]["content"]
            if isinstance(content, str):
                parsed = json.loads(content)
            elif isinstance(content, dict):
                parsed = content
            else:
                raise TypeError("message.content must be JSON text or an object")
            raw_pairs = parsed["pairs"]
        except (KeyError, TypeError, json.JSONDecodeError) as exc:
            raise RuntimeError("Ollama returned an invalid structured generation response") from exc

        if not isinstance(raw_pairs, list):
            raise RuntimeError("Ollama generation response 'pairs' must be an array")

        expected = set(range(len(global_indices)))
        by_local_index: dict[int, GeneratedPair] = {}
        for item in raw_pairs:
            if not isinstance(item, dict):
                continue
            raw_index = item.get("index")
            if not isinstance(raw_index, int) or isinstance(raw_index, bool):
                continue
            # A hallucinated extra item with an impossible local index is harmless.
            # Ignore it, but never ignore duplicates or omissions for valid indices.
            if raw_index not in expected:
                continue
            if raw_index in by_local_index:
                raise RuntimeError(f"Ollama generator returned duplicate batch index {raw_index}")
            positive = normalize_text(str(item.get("positive_text", "")))
            negative = normalize_text(str(item.get("negative_text", "")))
            if not positive or not negative:
                raise RuntimeError("Ollama generator returned an empty contrastive message")
            by_local_index[raw_index] = GeneratedPair(
                global_index=global_indices[raw_index],
                positive_text=positive,
                negative_text=negative,
            )

        missing = expected - set(by_local_index)
        if missing:
            raise RuntimeError(f"Ollama generator omitted batch indices: {sorted(missing)}")
        return [by_local_index[index] for index in range(len(global_indices))]

    def _post(self, body: dict[str, Any]) -> dict[str, Any]:
        request = Request(
            self.endpoint,
            data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        last_error: Exception | None = None
        for attempt in range(1, self.max_retries + 1):
            try:
                with urlopen(request, timeout=self.timeout_seconds) as response:  # noqa: S310 - local/user-supplied endpoint
                    payload = json.loads(response.read().decode("utf-8"))
                    if not isinstance(payload, dict):
                        raise TypeError("Ollama response envelope must be a JSON object")
                    return payload
            except (HTTPError, URLError, TimeoutError, json.JSONDecodeError, TypeError) as exc:
                last_error = exc
                if attempt == self.max_retries:
                    break
                time.sleep(min(8.0, 2.0 ** (attempt - 1)))
        raise RuntimeError(
            f"Could not get a valid generation response from Ollama at {self.endpoint} "
            f"after {self.max_retries} attempts."
        ) from last_error


def _generation_key(model: str, target_label: str, language: str, pair_index: int) -> str:
    return f"{GENERATION_PROMPT_VERSION}\x1f{model}\x1f{target_label}\x1f{language}\x1f{pair_index}"


def _load_generation_cache(path: Path) -> dict[str, GeneratedPair]:
    if not path.exists():
        return {}
    result: dict[str, GeneratedPair] = {}
    for row in read_jsonl(path):
        if row.get("prompt_version") != GENERATION_PROMPT_VERSION:
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


def _append_generation_cache(path: Path, entries: Iterable[dict[str, Any]]) -> None:
    ensure_parent(path)
    with path.open("a", encoding="utf-8", newline="\n") as handle:
        for entry in entries:
            handle.write(json.dumps(entry, ensure_ascii=False, sort_keys=True))
            handle.write("\n")
            handle.flush()


def _expected_labels(target_label: str, positive: bool) -> dict[str, bool]:
    return {label: bool(positive and label == target_label) for label in LABELS}


def _candidate_record(
    *,
    model: str,
    target_label: str,
    language: str,
    pair_index: int,
    polarity: str,
    text: str,
) -> dict[str, Any]:
    positive = polarity == "positive"
    expected = _expected_labels(target_label, positive)
    record_id = stable_id(
        "sparrow-safety-generated-v1",
        target_label,
        language,
        polarity,
        normalized_text_key(text),
    )
    return {
        "id": record_id,
        "text": text,
        "language": language,
        "source": "generated_contrastive",
        "source_id": f"{target_label}:{language}:{pair_index}:{polarity}",
        "source_split": "",
        "source_label": f"{target_label}:{polarity}",
        "synthetic": True,
        "reviewed": False,
        "cluster_id": "",
        "split": "",
        "duplicate_sources": [],
        "expected_labels": expected,
        "generation_model": model,
        "generation_prompt_version": GENERATION_PROMPT_VERSION,
        "generation_target_label": target_label,
        "generation_pair_index": pair_index,
        "generation_polarity": polarity,
    }


def _deduplicate_candidates(rows: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], int, int]:
    by_text: dict[str, list[dict[str, Any]]] = {}
    for row in rows:
        by_text.setdefault(normalized_text_key(str(row["text"])), []).append(row)

    unique: list[dict[str, Any]] = []
    duplicate_rows_removed = 0
    conflicting_groups = 0
    for values in by_text.values():
        expected_vectors = {
            tuple(bool(value["expected_labels"][label]) for label in LABELS)
            for value in values
        }
        if len(expected_vectors) > 1:
            conflicting_groups += 1
            duplicate_rows_removed += len(values)
            continue
        unique.append(values[0])
        duplicate_rows_removed += len(values) - 1
    unique.sort(key=lambda row: str(row["id"]))
    return unique, duplicate_rows_removed, conflicting_groups


def _batch(values: list[Any], size: int) -> Iterable[list[Any]]:
    for start in range(0, len(values), size):
        yield values[start : start + size]


def generate_and_validate_targeted_data(
    base_input_path: Path,
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
    pairs_per_label_language: int = 200,
    pairs_per_label_language_by_label: dict[str, int] | None = None,
    languages: tuple[str, ...] = ("en", "de"),
    generation_batch_size: int = 8,
    validation_batch_size: int = 16,
    min_confidence: float = 0.90,
    timeout_seconds: float = 240.0,
    max_retries: int = 3,
) -> dict[str, Any]:
    if pairs_per_label_language < 1:
        raise ValueError("pairs_per_label_language must be positive")
    pair_targets = {label: pairs_per_label_language for label in LABELS}
    if pairs_per_label_language_by_label:
        unknown = sorted(set(pairs_per_label_language_by_label) - set(LABELS))
        if unknown:
            raise ValueError(f"Unknown Safety label override(s): {unknown}")
        for label, count in pairs_per_label_language_by_label.items():
            if count < 1:
                raise ValueError(f"Pair target for {label} must be positive")
            pair_targets[label] = int(count)
    if not 1 <= generation_batch_size <= 16:
        raise ValueError("generation_batch_size must be between 1 and 16")
    if not 1 <= validation_batch_size <= 32:
        raise ValueError("validation_batch_size must be between 1 and 32")
    for language in languages:
        if language not in _LANGUAGE_NAME:
            raise ValueError(f"Unsupported generation language {language!r}; choose from {sorted(_LANGUAGE_NAME)}")

    base_rows = list(read_jsonl(base_input_path))
    if not base_rows:
        raise ValueError("Base labeled dataset is empty")

    generator = OllamaContrastiveGenerator(
        model=generator_model,
        endpoint=endpoint,
        timeout_seconds=timeout_seconds,
        max_retries=max_retries,
    )
    generation_cache = _load_generation_cache(generation_cache_path)
    # Never shrink a previously expanded targeted dataset on resume. The runner may
    # start from its default pair target again after a restart, while the cache already
    # contains successful refill indices. Preserve the highest cached target so those
    # generated/validated rows remain part of the dataset and their embeddings can be reused.
    if generation_cache_path.exists():
        for cached_row in read_jsonl(generation_cache_path):
            if cached_row.get("prompt_version") != GENERATION_PROMPT_VERSION:
                continue
            if cached_row.get("model") != generator_model:
                continue
            cached_label = str(cached_row.get("target_label", ""))
            cached_language = str(cached_row.get("language", ""))
            if cached_label not in pair_targets or cached_language not in languages:
                continue
            try:
                cached_target = int(cached_row["pair_index"]) + 1
            except (KeyError, TypeError, ValueError):
                continue
            pair_targets[cached_label] = max(pair_targets[cached_label], cached_target)

    generated_pairs: list[tuple[str, str, GeneratedPair]] = []

    total_groups = len(LABELS) * len(languages)
    group_number = 0
    for target_label in LABELS:
        for language in languages:
            group_number += 1
            print(
                f"targeted generation {group_number}/{total_groups}: {target_label} / {language} "
                f"({pair_targets[target_label]} pairs)",
                flush=True,
            )
            pair_indices = list(range(pair_targets[target_label]))
            for batch_indices in _batch(pair_indices, generation_batch_size):
                missing_indices = [
                    pair_index
                    for pair_index in batch_indices
                    if _generation_key(generator_model, target_label, language, pair_index) not in generation_cache
                ]
                if missing_indices:
                    fresh = generator.generate(
                        target_label=target_label,
                        language=language,
                        global_indices=missing_indices,
                    )
                    cache_entries: list[dict[str, Any]] = []
                    for pair in fresh:
                        key = _generation_key(generator_model, target_label, language, pair.global_index)
                        generation_cache[key] = pair
                        cache_entries.append(
                            {
                                "prompt_version": GENERATION_PROMPT_VERSION,
                                "generation_key": key,
                                "model": generator_model,
                                "target_label": target_label,
                                "language": language,
                                "pair_index": pair.global_index,
                                "positive_text": pair.positive_text,
                                "negative_text": pair.negative_text,
                            }
                        )
                    _append_generation_cache(generation_cache_path, cache_entries)
                for pair_index in batch_indices:
                    pair = generation_cache[_generation_key(generator_model, target_label, language, pair_index)]
                    generated_pairs.append((target_label, language, pair))

    candidate_rows: list[dict[str, Any]] = []
    for target_label, language, pair in generated_pairs:
        candidate_rows.append(
            _candidate_record(
                model=generator_model,
                target_label=target_label,
                language=language,
                pair_index=pair.global_index,
                polarity="positive",
                text=pair.positive_text,
            )
        )
        candidate_rows.append(
            _candidate_record(
                model=generator_model,
                target_label=target_label,
                language=language,
                pair_index=pair.global_index,
                polarity="negative",
                text=pair.negative_text,
            )
        )

    candidate_rows, duplicates_removed, conflicting_duplicate_groups = _deduplicate_candidates(candidate_rows)
    decisions = collect_dual_teacher_decisions(
        candidate_rows,
        validation_cache_path,
        model_a=validator_model_a,
        model_b=validator_model_b,
        endpoint=endpoint,
        batch_size=validation_batch_size,
        timeout_seconds=timeout_seconds,
        max_retries=max_retries,
        progress_prefix="generated validation",
    )

    accepted_generated: list[dict[str, Any]] = []
    rejected_generated: list[dict[str, Any]] = []
    rejection_counter: Counter[str] = Counter()

    for row in candidate_rows:
        record_id = str(row["id"])
        left = decisions[(record_id, "A")]
        right = decisions[(record_id, "B")]
        expected = {label: bool(row["expected_labels"][label]) for label in LABELS}
        agreement = all(left.labels[label] == right.labels[label] for label in LABELS)
        matches_intended = all(left.labels[label] == expected[label] for label in LABELS) and all(
            right.labels[label] == expected[label] for label in LABELS
        )
        minimum_confidence = min(
            *(left.confidences[label] for label in LABELS),
            *(right.confidences[label] for label in LABELS),
        )
        high_confidence = minimum_confidence >= min_confidence

        reasons: list[str] = []
        if not agreement:
            reasons.append("validator_disagreement")
        if not matches_intended:
            reasons.append("does_not_match_intended_labels")
        if not high_confidence:
            reasons.append("below_confidence_threshold")

        if not reasons:
            accepted_generated.append(
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
                    "labeling_method": "generated_then_dual_teacher_validated",
                    "generation_model": generator_model,
                    "generation_prompt_version": GENERATION_PROMPT_VERSION,
                    "generation_target_label": row["generation_target_label"],
                    "generation_pair_index": row["generation_pair_index"],
                    "generation_polarity": row["generation_polarity"],
                    "validator_model_a": validator_model_a,
                    "validator_model_b": validator_model_b,
                    "validator_min_confidence": minimum_confidence,
                    "validator_pass_a_reason": left.reason,
                    "validator_pass_b_reason": right.reason,
                }
            )
        else:
            for reason in reasons:
                rejection_counter[reason] += 1
            rejected_generated.append(
                {
                    **row,
                    "validator_pass_a": left.to_dict(),
                    "validator_pass_b": right.to_dict(),
                    "validator_min_confidence": minimum_confidence,
                    "rejection_reasons": reasons,
                }
            )

    accepted_generated.sort(key=lambda row: str(row["id"]))
    rejected_generated.sort(key=lambda row: str(row["id"]))
    write_jsonl(generated_output_path, accepted_generated)
    write_jsonl(rejected_output_path, rejected_generated)

    combined_by_text: dict[str, dict[str, Any]] = {}
    for row in base_rows:
        combined_by_text.setdefault(normalized_text_key(str(row["text"])), dict(row))
    generated_collisions_with_base = 0
    for row in accepted_generated:
        key = normalized_text_key(str(row["text"]))
        if key in combined_by_text:
            generated_collisions_with_base += 1
            continue
        combined_by_text[key] = row
    combined = sorted(combined_by_text.values(), key=lambda row: str(row["id"]))
    write_jsonl(output_path, combined)

    generated_positives = {
        label: sum(bool(row[label]) for row in accepted_generated)
        for label in LABELS
    }
    generated_by_language = Counter(str(row.get("language", "und")) for row in accepted_generated)
    report = {
        "generation_prompt_version": GENERATION_PROMPT_VERSION,
        "generator_model": generator_model,
        "validator_model_a": validator_model_a,
        "validator_model_b": validator_model_b,
        "min_confidence": min_confidence,
        "pairs_per_label_language": pairs_per_label_language,
        "pairs_per_label_language_by_label": dict(pair_targets),
        "languages": list(languages),
        "candidate_rows_after_generation": len(candidate_rows),
        "generation_exact_duplicates_removed": duplicates_removed,
        "generation_conflicting_duplicate_groups_removed": conflicting_duplicate_groups,
        "accepted_generated_rows": len(accepted_generated),
        "rejected_generated_rows": len(rejected_generated),
        "generated_acceptance_rate": len(accepted_generated) / max(1, len(candidate_rows)),
        "generated_positives": generated_positives,
        "generated_languages": dict(generated_by_language),
        "rejection_reasons": dict(rejection_counter),
        "base_rows": len(base_rows),
        "generated_collisions_with_base": generated_collisions_with_base,
        "combined_rows": len(combined),
        "generation_cache": str(generation_cache_path),
        "validation_cache": str(validation_cache_path),
        "generated_output": str(generated_output_path),
        "rejected_output": str(rejected_output_path),
        "combined_output": str(output_path),
    }
    ensure_parent(report_path)
    report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return report
