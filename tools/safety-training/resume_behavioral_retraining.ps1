[CmdletBinding()]
param(
    [string]$GeneratorModel = "qwen3:8b",
    [string]$ValidatorModelA = "qwen3:8b",
    [string]$ValidatorModelB = "gemma3:12b",
    [int]$PairsPerFocus = 150,
    [int]$GenerationBatchSize = 8,
    [int]$ValidationBatchSize = 16,
    [double]$MinConfidence = 0.90,
    [int]$MinTestPositives = 20,
    [double]$MinTestPrecision = 0.80,
    [double]$MinTestRecall = 0.50,
    [double]$MaxTestFpr = 0.02
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root
$Python = Join-Path $Root ".venv\Scripts\python.exe"

function Run-Python {
    param([string[]]$Arguments)
    & $Python @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Python command failed: $($Arguments -join ' ')"
    }
}

function Ensure-OllamaModel {
    param([string]$Model)
    $installed = & ollama list 2>$null | Select-String -SimpleMatch $Model
    if (-not $installed) {
        Write-Host "Model $Model is not installed. Pulling it now..."
        & ollama pull $Model
        if ($LASTEXITCODE -ne 0) { throw "Could not pull Ollama model $Model" }
    }
}

if (-not (Test-Path $Python)) { throw "Missing .venv. Run .\setup_windows.ps1 first." }
if (-not (Test-Path .\models\embedding_gemma.task)) { throw "Missing models\embedding_gemma.task. Run the full training pipeline first." }
if (-not (Test-Path .\data\processed\labeled.jsonl)) { throw "Missing v9 data\processed\labeled.jsonl. Run the full training pipeline first." }
if (-not (Get-Command ollama -ErrorAction SilentlyContinue)) { throw "Ollama is not installed or not on PATH." }
try {
    Invoke-RestMethod -Method Get -Uri "http://localhost:11434/api/tags" -TimeoutSec 10 | Out-Null
} catch {
    throw "Ollama API is not reachable at http://localhost:11434. Start Ollama and rerun."
}
@($GeneratorModel, $ValidatorModelA, $ValidatorModelB) | Select-Object -Unique | ForEach-Object { Ensure-OllamaModel $_ }

Write-Host "== Sparrow Safety v10 behavioral retraining =="
Write-Host "Existing public/main generation caches and embeddings are reused."
Write-Host "Behavioral focus pairs: $PairsPerFocus per focus pack"
Write-Host ""

Write-Host "[1/10] Generating focused EN/DE behavioral contrastive augmentation..."
Run-Python @(
    ".\generate_behavioral.py",
    "--base-input", ".\data\processed\labeled.jsonl",
    "--contract", ".\data\challenge\runtime_contract.jsonl",
    "--output", ".\data\processed\labeled-behavioral.jsonl",
    "--generated-output", ".\data\generated\behavioral-validated.jsonl",
    "--rejected-output", ".\data\generated\behavioral-rejected.jsonl",
    "--report", ".\data\generated\behavioral-report.json",
    "--generation-cache", ".\data\generated\behavioral-generation-cache.jsonl",
    "--validation-cache", ".\data\generated\behavioral-validation-cache.jsonl",
    "--generator-model", $GeneratorModel,
    "--validator-model-a", $ValidatorModelA,
    "--validator-model-b", $ValidatorModelB,
    "--pairs-per-focus", $PairsPerFocus.ToString(),
    "--generation-batch-size", $GenerationBatchSize.ToString(),
    "--validation-batch-size", $ValidationBatchSize.ToString(),
    "--min-confidence", $MinConfidence.ToString([System.Globalization.CultureInfo]::InvariantCulture)
)

Write-Host "[2/10] Incrementally embedding the augmented dataset..."
Run-Python @(
    ".\embed.py",
    "--input", ".\data\processed\labeled-behavioral.jsonl",
    "--backend", "mediapipe",
    "--model-path", ".\models\embedding_gemma.task",
    "--output", ".\data\embeddings\labeled-128.npz",
    "--metadata", ".\data\embeddings\metadata.json"
)

Write-Host "[3/10] Re-clustering near duplicates and creating leakage-safe splits..."
Run-Python @(
    ".\cluster_duplicates.py",
    "--input", ".\data\processed\labeled-behavioral.jsonl",
    "--embeddings", ".\data\embeddings\labeled-128.npz",
    "--output", ".\data\processed\clustered.jsonl"
)
Run-Python @(
    ".\split.py",
    "--input", ".\data\processed\clustered.jsonl",
    "--output", ".\data\processed\split.jsonl"
)

Write-Host "[4/10] Embedding the frozen behavioral contract with the same EmbeddingGemma runtime..."
Run-Python @(
    ".\embed.py",
    "--input", ".\data\challenge\runtime_contract.jsonl",
    "--backend", "mediapipe",
    "--model-path", ".\models\embedding_gemma.task",
    "--output", ".\data\embeddings\runtime-contract-128.npz",
    "--metadata", ".\data\embeddings\runtime-contract-metadata.json",
    "--no-reuse-existing"
)

Write-Host "[5/10] Training tiny nonlinear MLP heads; model/threshold selection must satisfy validation + behavior contract..."
Run-Python @(
    ".\train.py",
    "--input", ".\data\processed\split.jsonl",
    "--embeddings", ".\data\embeddings\labeled-128.npz",
    "--embedding-metadata", ".\data\embeddings\metadata.json",
    "--output-model", ".\artifacts\message-safety-mlp-model.json",
    "--output-report", ".\artifacts\evaluation.json",
    "--behavioral-contract", ".\data\challenge\runtime_contract.jsonl",
    "--behavioral-contract-embeddings", ".\data\embeddings\runtime-contract-128.npz",
    "--min-validation-precision", $MinTestPrecision.ToString([System.Globalization.CultureInfo]::InvariantCulture),
    "--min-validation-recall", $MinTestRecall.ToString([System.Globalization.CultureInfo]::InvariantCulture),
    "--max-validation-fpr", $MaxTestFpr.ToString([System.Globalization.CultureInfo]::InvariantCulture)
)

Write-Host "[6/10] Rendering held-out statistical evaluation..."
Run-Python @(
    ".\evaluate.py",
    "--report", ".\artifacts\evaluation.json",
    "--output", ".\artifacts\evaluation.md"
)

Write-Host "[7/10] Enforcing held-out statistical quality gate..."
Run-Python @(
    ".\quality_gate.py",
    "--report", ".\artifacts\evaluation.json",
    "--output", ".\artifacts\quality-gate.json",
    "--min-test-positives", $MinTestPositives.ToString(),
    "--min-test-precision", $MinTestPrecision.ToString([System.Globalization.CultureInfo]::InvariantCulture),
    "--min-test-recall", $MinTestRecall.ToString([System.Globalization.CultureInfo]::InvariantCulture),
    "--max-test-fpr", $MaxTestFpr.ToString([System.Globalization.CultureInfo]::InvariantCulture)
)

Write-Host "[8/10] Enforcing strict product behavioral contract..."
Run-Python @(
    ".\behavioral_gate.py",
    "--model", ".\artifacts\message-safety-mlp-model.json",
    "--contract", ".\data\challenge\runtime_contract.jsonl",
    "--embeddings", ".\data\embeddings\runtime-contract-128.npz",
    "--output", ".\artifacts\behavioral-gate.json"
)

Write-Host "[9/10] Exporting Kotlin MLP model..."
Run-Python @(
    ".\export_kotlin.py",
    "--model", ".\artifacts\message-safety-mlp-model.json",
    "--output", ".\artifacts\GeneratedMessageSafetyMlpModel.kt"
)

Write-Host "[10/10] Exporting Android embedding parity references..."
Run-Python @(
    ".\export_parity.py",
    "--input", ".\data\processed\split.jsonl",
    "--embeddings", ".\data\embeddings\labeled-128.npz",
    "--output", ".\artifacts\embedding-parity.json"
)

Write-Host ""
Write-Host "SPARROW SAFETY v10 BEHAVIORAL RETRAINING COMPLETE"
Write-Host "Send back:"
Write-Host "  artifacts\evaluation.md"
Write-Host "  artifacts\quality-gate.json"
Write-Host "  artifacts\behavioral-gate.json"
Write-Host "  data\generated\behavioral-report.json"
Write-Host "  artifacts\message-safety-mlp-model.json"
Write-Host "  artifacts\GeneratedMessageSafetyMlpModel.kt"
Write-Host "  artifacts\embedding-parity.json"
