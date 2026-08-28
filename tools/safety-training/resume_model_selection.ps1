[CmdletBinding()]
param(
    [double]$MinTestPrecision = 0.80,
    [double]$MinTestRecall = 0.50,
    [double]$MaxTestFpr = 0.02,
    [double]$ValidationPrecisionMargin = 0.05,
    [int]$MinTestPositives = 20
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

if (-not (Test-Path $Python)) { throw "Missing .venv. Run .\setup_windows.ps1 first." }
$Required = @(
    ".\data\processed\split.jsonl",
    ".\data\embeddings\labeled-128.npz",
    ".\data\embeddings\metadata.json",
    ".\data\challenge\runtime_contract.jsonl",
    ".\data\embeddings\runtime-contract-128.npz"
)
foreach ($Path in $Required) {
    if (-not (Test-Path $Path)) { throw "Missing $Path. Complete the behavioral retraining through stage 4 first." }
}

$Culture = [System.Globalization.CultureInfo]::InvariantCulture
Write-Host "== Sparrow Safety v13 precision-first model reselection =="
Write-Host "No Ollama, generation, labeling, embedding, clustering, or splitting is run."
Write-Host "Deployment precision floor: $MinTestPrecision"
Write-Host "Validation precision margin: $ValidationPrecisionMargin"
Write-Host ""

Write-Host "[1/5] Training MLP candidates across realistic positive sampling ratios..."
Run-Python @(
    ".\train.py",
    "--input", ".\data\processed\split.jsonl",
    "--embeddings", ".\data\embeddings\labeled-128.npz",
    "--embedding-metadata", ".\data\embeddings\metadata.json",
    "--output-model", ".\artifacts\message-safety-mlp-model.json",
    "--output-report", ".\artifacts\evaluation.json",
    "--behavioral-contract", ".\data\challenge\runtime_contract.jsonl",
    "--behavioral-contract-embeddings", ".\data\embeddings\runtime-contract-128.npz",
    "--min-validation-precision", $MinTestPrecision.ToString($Culture),
    "--min-validation-recall", $MinTestRecall.ToString($Culture),
    "--max-validation-fpr", $MaxTestFpr.ToString($Culture),
    "--validation-precision-margin", $ValidationPrecisionMargin.ToString($Culture)
)

Write-Host "[2/5] Rendering evaluation..."
Run-Python @(
    ".\evaluate.py",
    "--report", ".\artifacts\evaluation.json",
    "--output", ".\artifacts\evaluation.md"
)

Write-Host "[3/5] Enforcing held-out statistical quality gate..."
Run-Python @(
    ".\quality_gate.py",
    "--report", ".\artifacts\evaluation.json",
    "--output", ".\artifacts\quality-gate.json",
    "--min-test-positives", $MinTestPositives.ToString(),
    "--min-test-precision", $MinTestPrecision.ToString($Culture),
    "--min-test-recall", $MinTestRecall.ToString($Culture),
    "--max-test-fpr", $MaxTestFpr.ToString($Culture)
)

Write-Host "[4/5] Enforcing strict behavioral contract..."
Run-Python @(
    ".\behavioral_gate.py",
    "--model", ".\artifacts\message-safety-mlp-model.json",
    "--contract", ".\data\challenge\runtime_contract.jsonl",
    "--embeddings", ".\data\embeddings\runtime-contract-128.npz",
    "--output", ".\artifacts\behavioral-gate.json"
)

Write-Host "[5/5] Exporting Kotlin MLP model..."
Run-Python @(
    ".\export_kotlin.py",
    "--model", ".\artifacts\message-safety-mlp-model.json",
    "--output", ".\artifacts\GeneratedMessageSafetyMlpModel.kt"
)

Write-Host ""
Write-Host "SPARROW SAFETY v13 MODEL RESELECTION COMPLETE"
Write-Host "Send back:"
Write-Host "  artifacts\evaluation.md"
Write-Host "  artifacts\quality-gate.json"
Write-Host "  artifacts\behavioral-gate.json"
Write-Host "  artifacts\message-safety-mlp-model.json"
Write-Host "  artifacts\GeneratedMessageSafetyMlpModel.kt"
