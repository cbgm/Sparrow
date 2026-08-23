from __future__ import annotations

import argparse
import json
from pathlib import Path

from . import DEFAULT_EMBEDDING_BACKEND, DEFAULT_INPUT_MODE, DEFAULT_MODEL_ID, EMBEDDING_DIMENSIONS, LABELS
from .annotation import merge_reviewed_annotations, prepare_annotation_csv
from .behavioral_augmentation import generate_behavioral_augmentation
from .behavioral_gate import enforce_behavioral_contract
from .teacher_labeling import DEFAULT_OLLAMA_ENDPOINT, auto_label_dataset
from .clustering import assign_near_duplicate_clusters
from .embedding import embed_dataset
from .evaluation import render_markdown_report
from .io import write_jsonl
from .kotlin_export import export_kotlin
from .model_download import download_mediapipe_model
from .normalize import normalize_raw_files
from .parity import export_parity_samples
from .sources import SUPPORTED_SOURCES, download_source
from .splitting import split_by_cluster
from .support import assess_cluster_support
from .synthetic_generation import generate_and_validate_targeted_data
from .quality_gate import enforce_quality_gate
from .training import train_mlp_heads


def _parse_pair_overrides(values: list[str]) -> dict[str, int]:
    result: dict[str, int] = {}
    for value in values:
        if "=" not in value:
            raise ValueError(f"Invalid --pairs-for-label value {value!r}; expected LABEL=COUNT")
        label, raw_count = value.split("=", 1)
        label = label.strip()
        if label not in LABELS:
            raise ValueError(f"Unknown Safety label {label!r}; choose from {list(LABELS)}")
        try:
            count = int(raw_count)
        except ValueError as exc:
            raise ValueError(f"Invalid pair count in {value!r}") from exc
        if count < 1:
            raise ValueError(f"Pair count for {label} must be positive")
        result[label] = count
    return result


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(prog="sparrow-safety-training")
    subparsers = parser.add_subparsers(dest="command", required=True)

    download_model = subparsers.add_parser("download-model", help="Download Sparrow's exact MediaPipe EmbeddingGemma task model")
    download_model.add_argument("--output", type=Path, required=True)
    download_model.add_argument("--metadata", type=Path, required=True)
    download_model.add_argument("--expected-sha256", default=None)

    download = subparsers.add_parser("download", help="Download one approved public source")
    download.add_argument("source", choices=SUPPORTED_SOURCES)
    download.add_argument("--output", type=Path, required=True)

    normalize = subparsers.add_parser("normalize", help="Normalize and exact-deduplicate raw JSONL")
    normalize.add_argument("inputs", nargs="+", type=Path)
    normalize.add_argument("--output", type=Path, required=True)

    prepare = subparsers.add_parser("prepare-annotations", help="OPTIONAL: create a manual review CSV")
    prepare.add_argument("--input", type=Path, required=True)
    prepare.add_argument("--output", type=Path, required=True)

    merge = subparsers.add_parser("merge-annotations", help="OPTIONAL: merge manually reviewed labels and curated seed data")
    merge.add_argument("--input", type=Path, required=True)
    merge.add_argument("--annotations", type=Path, required=True)
    merge.add_argument("--seed", nargs="*", type=Path, default=[])
    merge.add_argument("--output", type=Path, required=True)

    auto_label = subparsers.add_parser("auto-label", help="Automatically label public messages with dual Ollama teacher passes")
    auto_label.add_argument("--input", type=Path, required=True)
    auto_label.add_argument("--output", type=Path, required=True)
    auto_label.add_argument("--review-queue", type=Path, required=True)
    auto_label.add_argument("--report", type=Path, required=True)
    auto_label.add_argument("--cache", type=Path, required=True)
    auto_label.add_argument("--model-a", default="qwen3:8b")
    auto_label.add_argument("--model-b", default="qwen3:8b")
    auto_label.add_argument("--endpoint", default=DEFAULT_OLLAMA_ENDPOINT)
    auto_label.add_argument("--min-confidence", type=float, default=0.90)
    auto_label.add_argument("--batch-size", type=int, default=8)
    auto_label.add_argument("--seed", nargs="*", type=Path, default=[])
    auto_label.add_argument("--timeout-seconds", type=float, default=180.0)
    auto_label.add_argument("--max-retries", type=int, default=3)

    generate = subparsers.add_parser(
        "generate-targeted",
        help="Generate balanced EN/DE contrastive Safety data and validate it with two Ollama teachers",
    )
    generate.add_argument("--base-input", type=Path, required=True)
    generate.add_argument("--output", type=Path, required=True)
    generate.add_argument("--generated-output", type=Path, required=True)
    generate.add_argument("--rejected-output", type=Path, required=True)
    generate.add_argument("--report", type=Path, required=True)
    generate.add_argument("--generation-cache", type=Path, required=True)
    generate.add_argument("--validation-cache", type=Path, required=True)
    generate.add_argument("--generator-model", default="qwen3:8b")
    generate.add_argument("--validator-model-a", default="qwen3:8b")
    generate.add_argument("--validator-model-b", default="gemma3:12b")
    generate.add_argument("--endpoint", default=DEFAULT_OLLAMA_ENDPOINT)
    generate.add_argument("--pairs-per-label-language", type=int, default=200)
    generate.add_argument(
        "--pairs-for-label",
        action="append",
        default=[],
        metavar="LABEL=COUNT",
        help="Override pair count per language for one Safety label; repeat as needed",
    )
    generate.add_argument("--languages", nargs="+", default=["en", "de"])
    generate.add_argument("--generation-batch-size", type=int, default=8)
    generate.add_argument("--validation-batch-size", type=int, default=16)
    generate.add_argument("--min-confidence", type=float, default=0.90)
    generate.add_argument("--timeout-seconds", type=float, default=240.0)
    generate.add_argument("--max-retries", type=int, default=3)


    behavioral = subparsers.add_parser(
        "generate-behavioral",
        help="Generate focused behavioral contrastive augmentation while holding product-contract messages out of training",
    )
    behavioral.add_argument("--base-input", type=Path, required=True)
    behavioral.add_argument("--contract", type=Path, required=True)
    behavioral.add_argument("--output", type=Path, required=True)
    behavioral.add_argument("--generated-output", type=Path, required=True)
    behavioral.add_argument("--rejected-output", type=Path, required=True)
    behavioral.add_argument("--report", type=Path, required=True)
    behavioral.add_argument("--generation-cache", type=Path, required=True)
    behavioral.add_argument("--validation-cache", type=Path, required=True)
    behavioral.add_argument("--generator-model", default="qwen3:8b")
    behavioral.add_argument("--validator-model-a", default="qwen3:8b")
    behavioral.add_argument("--validator-model-b", default="gemma3:12b")
    behavioral.add_argument("--endpoint", default=DEFAULT_OLLAMA_ENDPOINT)
    behavioral.add_argument("--pairs-per-focus", type=int, default=150)
    behavioral.add_argument("--generation-batch-size", type=int, default=16)
    behavioral.add_argument("--validation-batch-size", type=int, default=16)
    behavioral.add_argument("--min-confidence", type=float, default=0.90)
    behavioral.add_argument("--timeout-seconds", type=float, default=240.0)
    behavioral.add_argument("--max-retries", type=int, default=3)

    embed = subparsers.add_parser("embed", help="Embed reviewed messages with EmbeddingGemma")
    embed.add_argument("--input", type=Path, required=True)
    embed.add_argument("--output", type=Path, required=True)
    embed.add_argument("--metadata", type=Path, required=True)
    embed.add_argument("--backend", choices=("mediapipe", "sentence_transformers"), default=DEFAULT_EMBEDDING_BACKEND)
    embed.add_argument("--model-path", type=Path, default=None, help="Required for MediaPipe; use the downloaded embedding_gemma.task")
    embed.add_argument("--model-id", default=DEFAULT_MODEL_ID)
    embed.add_argument("--model-revision", default=None, help="Optional exact Hugging Face commit/tag; latest resolved commit is recorded when omitted")
    embed.add_argument("--input-mode", choices=("sentence_similarity", "classification"), default=DEFAULT_INPUT_MODE)
    embed.add_argument("--dimensions", type=int, default=EMBEDDING_DIMENSIONS)
    embed.add_argument("--batch-size", type=int, default=64)
    embed.add_argument("--no-reuse-existing", action="store_true", help="Recompute all vectors instead of reusing a compatible existing NPZ")

    cluster = subparsers.add_parser("cluster", help="Assign near-duplicate groups from normalized embeddings")
    cluster.add_argument("--input", type=Path, required=True)
    cluster.add_argument("--embeddings", type=Path, required=True)
    cluster.add_argument("--output", type=Path, required=True)
    cluster.add_argument("--similarity", type=float, default=0.985)
    cluster.add_argument("--neighbors", type=int, default=8)

    support = subparsers.add_parser("support-check", help="Check post-clustering per-label support before splitting")
    support.add_argument("--input", type=Path, required=True)
    support.add_argument("--output", type=Path, required=True)

    split = subparsers.add_parser("split", help="Create cluster-safe train/validation/test splits")
    split.add_argument("--input", type=Path, required=True)
    split.add_argument("--output", type=Path, required=True)
    split.add_argument("--seed", type=int, default=20260820)

    train = subparsers.add_parser("train", help="Train four one-vs-rest compact MLP heads")
    train.add_argument("--input", type=Path, required=True)
    train.add_argument("--embeddings", type=Path, required=True)
    train.add_argument("--embedding-metadata", type=Path, required=True)
    train.add_argument("--output-model", type=Path, required=True)
    train.add_argument("--output-report", type=Path, required=True)
    train.add_argument("--min-validation-precision", type=float, default=0.80)
    train.add_argument("--min-validation-recall", type=float, default=0.50)
    train.add_argument("--max-validation-fpr", type=float, default=0.02)
    train.add_argument("--validation-precision-margin", type=float, default=0.05)
    train.add_argument("--behavioral-contract", type=Path, default=None)
    train.add_argument("--behavioral-contract-embeddings", type=Path, default=None)

    evaluate = subparsers.add_parser("evaluate", help="Render the JSON training report as Markdown")
    evaluate.add_argument("--report", type=Path, required=True)
    evaluate.add_argument("--output", type=Path, required=True)

    behavioral_gate = subparsers.add_parser("behavioral-gate", help="Require every frozen Sparrow behavioral contract example to classify exactly as intended")
    behavioral_gate.add_argument("--model", type=Path, required=True)
    behavioral_gate.add_argument("--contract", type=Path, required=True)
    behavioral_gate.add_argument("--embeddings", type=Path, required=True)
    behavioral_gate.add_argument("--output", type=Path, required=True)

    gate = subparsers.add_parser("quality-gate", help="Refuse export when test support/metrics are still too weak")
    gate.add_argument("--report", type=Path, required=True)
    gate.add_argument("--output", type=Path, required=True)
    gate.add_argument("--min-test-positives", type=int, default=20)
    gate.add_argument("--min-test-precision", type=float, default=0.80)
    gate.add_argument("--min-test-recall", type=float, default=0.50)
    gate.add_argument("--max-test-fpr", type=float, default=0.02)

    export = subparsers.add_parser("export-kotlin", help="Export trained weights as Kotlin source")
    export.add_argument("--model", type=Path, required=True)
    export.add_argument("--output", type=Path, required=True)

    parity = subparsers.add_parser("export-parity", help="Export sample Python embeddings for Android parity checks")
    parity.add_argument("--input", type=Path, required=True)
    parity.add_argument("--embeddings", type=Path, required=True)
    parity.add_argument("--output", type=Path, required=True)
    parity.add_argument("--count", type=int, default=8)

    args = parser.parse_args(argv)
    result = None
    if args.command == "download-model":
        result = download_mediapipe_model(
            args.output,
            args.metadata,
            expected_sha256=args.expected_sha256,
        )
    elif args.command == "download":
        records = download_source(args.source)
        write_jsonl(args.output, (record.to_dict() for record in records))
        result = {"source": args.source, "rows": len(records), "output": str(args.output)}
    elif args.command == "normalize":
        result = normalize_raw_files(args.inputs, args.output)
    elif args.command == "prepare-annotations":
        result = {"rows": prepare_annotation_csv(args.input, args.output), "output": str(args.output)}
    elif args.command == "merge-annotations":
        result = merge_reviewed_annotations(args.input, args.annotations, args.output, args.seed)
    elif args.command == "auto-label":
        result = auto_label_dataset(
            args.input,
            args.output,
            args.review_queue,
            args.report,
            args.cache,
            model_a=args.model_a,
            model_b=args.model_b,
            endpoint=args.endpoint,
            min_confidence=args.min_confidence,
            batch_size=args.batch_size,
            seed_paths=args.seed,
            timeout_seconds=args.timeout_seconds,
            max_retries=args.max_retries,
        )
    elif args.command == "generate-targeted":
        result = generate_and_validate_targeted_data(
            args.base_input,
            args.output,
            args.generated_output,
            args.rejected_output,
            args.report,
            args.generation_cache,
            args.validation_cache,
            generator_model=args.generator_model,
            validator_model_a=args.validator_model_a,
            validator_model_b=args.validator_model_b,
            endpoint=args.endpoint,
            pairs_per_label_language=args.pairs_per_label_language,
            pairs_per_label_language_by_label=_parse_pair_overrides(args.pairs_for_label),
            languages=tuple(args.languages),
            generation_batch_size=args.generation_batch_size,
            validation_batch_size=args.validation_batch_size,
            min_confidence=args.min_confidence,
            timeout_seconds=args.timeout_seconds,
            max_retries=args.max_retries,
        )
    elif args.command == "generate-behavioral":
        result = generate_behavioral_augmentation(
            args.base_input,
            args.contract,
            args.output,
            args.generated_output,
            args.rejected_output,
            args.report,
            args.generation_cache,
            args.validation_cache,
            generator_model=args.generator_model,
            validator_model_a=args.validator_model_a,
            validator_model_b=args.validator_model_b,
            endpoint=args.endpoint,
            pairs_per_focus=args.pairs_per_focus,
            generation_batch_size=args.generation_batch_size,
            validation_batch_size=args.validation_batch_size,
            min_confidence=args.min_confidence,
            timeout_seconds=args.timeout_seconds,
            max_retries=args.max_retries,
        )
    elif args.command == "embed":
        result = embed_dataset(
            args.input,
            args.output,
            args.metadata,
            backend=args.backend,
            media_pipe_model_path=args.model_path,
            model_id=args.model_id,
            input_mode=args.input_mode,
            dimensions=args.dimensions,
            batch_size=args.batch_size,
            model_revision=args.model_revision,
            reuse_existing=not args.no_reuse_existing,
        )
    elif args.command == "cluster":
        result = assign_near_duplicate_clusters(
            args.input,
            args.embeddings,
            args.output,
            similarity_threshold=args.similarity,
            neighbors=args.neighbors,
        )
    elif args.command == "support-check":
        result = assess_cluster_support(args.input, args.output)
    elif args.command == "split":
        result = split_by_cluster(args.input, args.output, seed=args.seed)
    elif args.command == "train":
        result = train_mlp_heads(
            args.input,
            args.embeddings,
            args.embedding_metadata,
            args.output_model,
            args.output_report,
            behavioral_contract_path=args.behavioral_contract,
            behavioral_contract_embeddings_path=args.behavioral_contract_embeddings,
            min_validation_precision=args.min_validation_precision,
            min_validation_recall=args.min_validation_recall,
            max_validation_false_positive_rate=args.max_validation_fpr,
            validation_precision_margin=args.validation_precision_margin,
        )
    elif args.command == "evaluate":
        text = render_markdown_report(args.report, args.output)
        result = {"output": str(args.output), "characters": len(text)}
    elif args.command == "behavioral-gate":
        result = enforce_behavioral_contract(
            args.model,
            args.contract,
            args.embeddings,
            args.output,
        )
    elif args.command == "quality-gate":
        result = enforce_quality_gate(
            args.report,
            args.output,
            min_test_positives=args.min_test_positives,
            min_test_precision=args.min_test_precision,
            min_test_recall=args.min_test_recall,
            max_test_false_positive_rate=args.max_test_fpr,
        )
    elif args.command == "export-kotlin":
        export_kotlin(args.model, args.output)
        result = {"output": str(args.output)}
    elif args.command == "export-parity":
        export_parity_samples(args.input, args.embeddings, args.output, count=args.count)
        result = {"output": str(args.output), "count": args.count}

    print(json.dumps(result, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
