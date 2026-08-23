[CmdletBinding()]
param(
    [string]$PublicTeacherModelA = "qwen3:8b",
    [string]$PublicTeacherModelB = "qwen3:8b",
    [string]$GeneratorModel = "qwen3:8b",
    [string]$ValidatorModelA = "qwen3:8b",
    [string]$ValidatorModelB = "gemma3:12b",
    [double]$MinConfidence = 0.90,
    [int]$PublicBatchSize = 8,
    [int]$PairsPerLabelLanguage = 200,
    [int]$RefillPairsPerLabelLanguage = 200,
    [int]$MaxRefillRounds = 4,
    [int]$BehavioralPairsPerFocus = 150,
    [int]$GenerationBatchSize = 16,
    [int]$ValidationBatchSize = 16,
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
    Write-Host "Checking Ollama model $Model..."
    $installed = & ollama list 2>$null | Select-String -SimpleMatch $Model
    if (-not $installed) {
        Write-Host "Model $Model is not installed. Pulling it now..."
        & ollama pull $Model
        if ($LASTEXITCODE -ne 0) { throw "Could not pull Ollama model $Model" }
    }
}

Write-Host "== Sparrow Safety Training v10: balanced + behavioral automatic run =="
Write-Host "Public teacher A:       $PublicTeacherModelA"
Write-Host "Public teacher B:       $PublicTeacherModelB"
Write-Host "Generator:              $GeneratorModel"
Write-Host "Generated validator A:  $ValidatorModelA"
Write-Host "Generated validator B:  $ValidatorModelB"
Write-Host "Pairs / label / language: $PairsPerLabelLanguage"
Write-Host "Minimum teacher confidence: $MinConfidence"
Write-Host ""
Write-Host "NOTE: gemma3:12b is about 8.1 GB in Ollama. Override -ValidatorModelB gemma3:4b if your machine cannot run it."
Write-Host ""

if (-not (Test-Path $Python)) {
    throw "Missing .venv. Run .\setup_windows.ps1 first."
}

$MediaPipeMajor = & $Python -c "import mediapipe as mp; print(mp.__version__.split('.')[0])" 2>$null
if ($LASTEXITCODE -ne 0 -or $MediaPipeMajor.Trim() -ne "1") {
    Write-Host "Updating training dependencies (MediaPipe 1.x is required for EmbeddingGemma)..."
    & $Python -m pip install --upgrade -e ".[dev]"
    if ($LASTEXITCODE -ne 0) { throw "Could not upgrade training dependencies" }
}

if (-not (Get-Command ollama -ErrorAction SilentlyContinue)) {
    throw "Ollama is not installed or not on PATH. Install Ollama for Windows, reopen PowerShell, then rerun."
}

try {
    Invoke-RestMethod -Method Get -Uri "http://localhost:11434/api/tags" -TimeoutSec 10 | Out-Null
} catch {
    throw "Ollama is installed but its local API is not reachable at http://localhost:11434. Start Ollama and rerun."
}

$models = @(
    $PublicTeacherModelA,
    $PublicTeacherModelB,
    $GeneratorModel,
    $ValidatorModelA,
    $ValidatorModelB
) | Select-Object -Unique
foreach ($model in $models) {
    Ensure-OllamaModel $model
}

New-Item -ItemType Directory -Force -Path `
    .\models, `
    .\data\raw, `
    .\data\processed, `
    .\data\labels, `
    .\data\generated, `
    .\data\embeddings, `
    .\artifacts | Out-Null

if (-not (Test-Path .\models\embedding_gemma.task)) {
    Write-Host "[1/12] Downloading Sparrow's MediaPipe EmbeddingGemma model..."
    Run-Python @(
        ".\download_embedding_model.py",
        "--output", ".\models\embedding_gemma.task",
        "--metadata", ".\models\embedding_gemma.metadata.json"
    )
} else {
    Write-Host "[1/12] EmbeddingGemma model already exists; keeping the exact existing bytes."
}

Write-Host "[1/12] Validating MediaPipe + EmbeddingGemma compatibility..."
Run-Python @(
    ".\validate_embedding_model.py",
    "--model-path", ".\models\embedding_gemma.task"
)

if (-not (Test-Path .\data\raw\uci_sms.jsonl)) {
    Write-Host "[2/12] Downloading UCI SMS Spam Collection..."
    Run-Python @(
        ".\download_datasets.py", "uci_sms",
        "--output", ".\data\raw\uci_sms.jsonl"
    )
} else {
    Write-Host "[2/12] UCI dataset already exists; skipping download."
}

Write-Host "[3/12] Normalizing and exact-deduplicating public data..."
Run-Python @(
    ".\normalize.py",
    ".\data\raw\uci_sms.jsonl",
    "--output", ".\data\processed\unlabeled.jsonl"
)

Write-Host "[4/12] Auto-labeling public data (existing teacher cache is reused)..."
Run-Python @(
    ".\auto_label.py",
    "--input", ".\data\processed\unlabeled.jsonl",
    "--output", ".\data\processed\public-labeled.jsonl",
    "--review-queue", ".\data\labels\review_queue.jsonl",
    "--report", ".\data\labels\auto_label_report.json",
    "--cache", ".\data\labels\teacher_cache.jsonl",
    "--model-a", $PublicTeacherModelA,
    "--model-b", $PublicTeacherModelB,
    "--min-confidence", $MinConfidence.ToString([System.Globalization.CultureInfo]::InvariantCulture),
    "--batch-size", $PublicBatchSize.ToString(),
    "--seed", ".\data\seed\contrastive_seed.jsonl"
)

$SafetyLabels = @(
    "urgent_action_request",
    "credential_request",
    "payment_request",
    "private_key_request"
)
$PairTargets = @{}
foreach ($label in $SafetyLabels) {
    $PairTargets[$label] = $PairsPerLabelLanguage
}

$supportReady = $false
for ($refillRound = 0; $refillRound -le $MaxRefillRounds; $refillRound++) {
    Write-Host "[5/12] Generating/validating targeted EN/DE contrast pairs (support round $($refillRound + 1))..."
    Write-Host "This stage is resumable; only pair indices not already present in the caches are generated."
    foreach ($label in $SafetyLabels) {
        Write-Host "  $label`: $($PairTargets[$label]) pairs/language"
    }

    $generationArgs = @(
        ".\generate_targeted.py",
        "--base-input", ".\data\processed\public-labeled.jsonl",
        "--output", ".\data\processed\labeled.jsonl",
        "--generated-output", ".\data\generated\validated.jsonl",
        "--rejected-output", ".\data\generated\rejected.jsonl",
        "--report", ".\data\generated\report.json",
        "--generation-cache", ".\data\generated\generation_cache.jsonl",
        "--validation-cache", ".\data\generated\validation_cache.jsonl",
        "--generator-model", $GeneratorModel,
        "--validator-model-a", $ValidatorModelA,
        "--validator-model-b", $ValidatorModelB,
        "--pairs-per-label-language", $PairsPerLabelLanguage.ToString(),
        "--generation-batch-size", $GenerationBatchSize.ToString(),
        "--validation-batch-size", $ValidationBatchSize.ToString(),
        "--min-confidence", $MinConfidence.ToString([System.Globalization.CultureInfo]::InvariantCulture)
    )
    foreach ($label in $SafetyLabels) {
        $generationArgs += @("--pairs-for-label", "$label=$($PairTargets[$label])")
    }
    Run-Python $generationArgs

    Write-Host "[6/12] Generating 128-dimensional Sparrow-compatible EmbeddingGemma vectors..."
    Write-Host "Compatible vectors are reused; only newly accepted generated rows are embedded."
    Run-Python @(
        ".\embed.py",
        "--input", ".\data\processed\labeled.jsonl",
        "--backend", "mediapipe",
        "--model-path", ".\models\embedding_gemma.task",
        "--output", ".\data\embeddings\labeled-128.npz",
        "--metadata", ".\data\embeddings\metadata.json"
    )

    Write-Host "[7/12] Clustering near duplicates..."
    Run-Python @(
        ".\cluster_duplicates.py",
        "--input", ".\data\processed\labeled.jsonl",
        "--embeddings", ".\data\embeddings\labeled-128.npz",
        "--output", ".\data\processed\clustered.jsonl"
    )

    Write-Host "[8/12] Checking post-clustering class support before splitting..."
    Run-Python @(
        ".\support_check.py",
        "--input", ".\data\processed\clustered.jsonl",
        "--output", ".\data\generated\support.json"
    )
    $support = Get-Content .\data\generated\support.json -Raw | ConvertFrom-Json

    if ($support.satisfied) {
        Write-Host "All Safety labels have enough independent support."
        $supportReady = $true
        break
    }

    Write-Host "Support is still insufficient after clustering:"
    foreach ($label in @($support.missing_labels)) {
        $labelSupport = $support.labels.PSObject.Properties[$label].Value
        Write-Host "  $label`: $($labelSupport.positives)/$($labelSupport.required_positives) positives; shortage=$($labelSupport.positive_shortage)"
    }

    if ($refillRound -ge $MaxRefillRounds) {
        break
    }

    Write-Host "Automatically generating more data only for the insufficient label(s)..."
    foreach ($label in @($support.missing_labels)) {
        $PairTargets[$label] = [int]$PairTargets[$label] + $RefillPairsPerLabelLanguage
        Write-Host "  $label -> $($PairTargets[$label]) pairs/language"
    }
}

if (-not $supportReady) {
    throw "Automatic targeted refilling exhausted $MaxRefillRounds refill round(s) without enough post-clustering label support. See data\generated\support.json and data\generated\report.json. Increase -MaxRefillRounds or -RefillPairsPerLabelLanguage; do not lower the split requirements."
}

Write-Host ""
Write-Host "Base v9-style dataset has sufficient post-clustering support."
Write-Host "Continuing with v10 focused behavioral augmentation + nonlinear retraining..."
& .\resume_behavioral_retraining.ps1 `
    -GeneratorModel $GeneratorModel `
    -ValidatorModelA $ValidatorModelA `
    -ValidatorModelB $ValidatorModelB `
    -PairsPerFocus $BehavioralPairsPerFocus `
    -GenerationBatchSize $GenerationBatchSize `
    -ValidationBatchSize $ValidationBatchSize `
    -MinConfidence $MinConfidence `
    -MinTestPositives $MinTestPositives `
    -MinTestPrecision $MinTestPrecision `
    -MinTestRecall $MinTestRecall `
    -MaxTestFpr $MaxTestFpr
if ($LASTEXITCODE -ne 0) {
    throw "v10 behavioral retraining failed"
}
