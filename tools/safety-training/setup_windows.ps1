[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

Write-Host "== Sparrow Safety Training: Windows setup =="
Write-Host "Project: $Root"

if (-not (Get-Command py -ErrorAction SilentlyContinue)) {
    throw "Python launcher 'py' was not found. Install Python first, then rerun this script."
}

& py -3.11 --version
if ($LASTEXITCODE -ne 0) {
    throw "Python 3.11 is not installed. Run: py install 3.11"
}

if (-not (Test-Path .\.venv\Scripts\python.exe)) {
    Write-Host "Creating .venv with Python 3.11..."
    & py -3.11 -m venv .venv
    if ($LASTEXITCODE -ne 0) { throw "Could not create .venv" }
}

$Python = (Resolve-Path .\.venv\Scripts\python.exe).Path
Write-Host "Using: $Python"
& $Python --version

Write-Host "Upgrading pip..."
& $Python -m pip install --upgrade pip
if ($LASTEXITCODE -ne 0) { throw "pip upgrade failed" }

Write-Host "Installing/upgrading Sparrow Safety training dependencies..."
& $Python -m pip install --upgrade -e ".[dev]"
if ($LASTEXITCODE -ne 0) { throw "Dependency installation failed" }

Write-Host "Checking MediaPipe version..."
& $Python -c "import mediapipe as mp; print('MediaPipe', mp.__version__); assert int(mp.__version__.split('.')[0]) >= 1, 'MediaPipe 1.x required'"
if ($LASTEXITCODE -ne 0) { throw "MediaPipe 1.x validation failed" }

Write-Host "Running unit tests..."
& $Python -m pytest
if ($LASTEXITCODE -ne 0) { throw "Unit tests failed" }

Write-Host ""
Write-Host "Setup complete."
Write-Host "Next: install/start Ollama, then run .\run_uci_training.ps1"
