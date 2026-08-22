[CmdletBinding()]
param(
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

if (-not (Test-Path $Python)) {
    throw "Missing .venv. Run .\setup_windows.ps1 first."
}

$required = @(
    ".\data\processed\split.jsonl",
    ".\data\embeddings\labeled-128.npz",
    ".\data\embeddings\metadata.json"
)
foreach ($path in $required) {
    if (-not (Test-Path $path)) {
        throw "Missing required completed-stage artifact: $path. Run .\run_uci_training.ps1 instead."
    }
}

Write-Host "== Sparrow Safety Training: resume from stage 9 =="
Write-Host "Threshold/model selection is validation-only. The test split is not used for tuning."
Write-Host ""

Write-Host "[9/12] Training/choosing thresholds from validation-only deployment constraints..."
Run-Python @(
    ".\train.py",
    "--input", ".\data\processed\split.jsonl",
    "--embeddings", ".\data\embeddings\labeled-128.npz",
    "--embedding-metadata", ".\data\embeddings\metadata.json",
    "--output-model", ".\artifacts\message-safety-linear-model.json",
    "--output-report", ".\artifacts\evaluation.json",
    "--min-validation-precision", $MinTestPrecision.ToString([System.Globalization.CultureInfo]::InvariantCulture),
    "--min-validation-recall", $MinTestRecall.ToString([System.Globalization.CultureInfo]::InvariantCulture),
    "--max-validation-fpr", $MaxTestFpr.ToString([System.Globalization.CultureInfo]::InvariantCulture)
)

Write-Host "[10/12] Rendering evaluation report..."
Run-Python @(
    ".\evaluate.py",
    "--report", ".\artifacts\evaluation.json",
    "--output", ".\artifacts\evaluation.md"
)

Write-Host "[11/12] Enforcing held-out test quality gate..."
Run-Python @(
    ".\quality_gate.py",
    "--report", ".\artifacts\evaluation.json",
    "--output", ".\artifacts\quality-gate.json",
    "--min-test-positives", $MinTestPositives.ToString(),
    "--min-test-precision", $MinTestPrecision.ToString([System.Globalization.CultureInfo]::InvariantCulture),
    "--min-test-recall", $MinTestRecall.ToString([System.Globalization.CultureInfo]::InvariantCulture),
    "--max-test-fpr", $MaxTestFpr.ToString([System.Globalization.CultureInfo]::InvariantCulture)
)

Write-Host "[12/12] Quality gate passed. Exporting Kotlin model and parity samples..."
Run-Python @(
    ".\export_kotlin.py",
    "--model", ".\artifacts\message-safety-linear-model.json",
    "--output", ".\artifacts\GeneratedMessageSafetyLinearModel.kt"
)
Run-Python @(
    ".\export_parity.py",
    "--input", ".\data\processed\split.jsonl",
    "--embeddings", ".\data\embeddings\labeled-128.npz",
    "--output", ".\artifacts\embedding-parity.json"
)

Write-Host ""
Write-Host "RESUME COMPLETE"
Write-Host "Send artifacts\evaluation.md and artifacts\quality-gate.json for review."
