param(
    [Parameter(Mandatory = $true)]
    [string]$Domain,

    [Parameter(Mandatory = $true)]
    [string]$FirebaseAdminCredentials
)

$ErrorActionPreference = "Stop"

$serverDirectory = Split-Path -Parent $PSScriptRoot
$secretDirectory = Join-Path $serverDirectory "secrets"
$environmentFile = Join-Path $serverDirectory ".env.production"

if (-not (Test-Path -LiteralPath $FirebaseAdminCredentials -PathType Leaf)) {
    throw "Firebase Admin credential file does not exist: $FirebaseAdminCredentials"
}

New-Item -ItemType Directory -Path $secretDirectory -Force | Out-Null

function New-RandomSecretFile {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (Test-Path -LiteralPath $Path) {
        Write-Host "Keeping existing secret: $Path"
        return
    }

    $bytes = New-Object byte[] 48
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }

    $secret = [Convert]::ToBase64String($bytes)
    Set-Content -LiteralPath $Path -Value $secret -Encoding Ascii -NoNewline
    Write-Host "Created secret: $Path"
}

$secretNames = @(
    "node-registry-database-password",
    "presence-redis-password",
    "mailbox-database-password",
    "push-database-password",
    "federation-database-password",
    "federation-internal-api-token",
    "gateway-internal-api-token",
    "push-internal-api-token"
)

foreach ($secretName in $secretNames) {
    New-RandomSecretFile -Path (Join-Path $secretDirectory "$secretName.txt")
}

if (Test-Path -LiteralPath $environmentFile) {
    Write-Host "Keeping existing environment file: $environmentFile"
} else {
    $content = @"
SECURECHAT_DOMAIN=$Domain
FIREBASE_ADMIN_CREDENTIALS=$FirebaseAdminCredentials
COMPOSE_PARALLEL_LIMIT=1
NODE_REGISTRY_DATABASE_PASSWORD_FILE=./secrets/node-registry-database-password.txt
PRESENCE_REDIS_PASSWORD_FILE=./secrets/presence-redis-password.txt
MAILBOX_DATABASE_PASSWORD_FILE=./secrets/mailbox-database-password.txt
PUSH_DATABASE_PASSWORD_FILE=./secrets/push-database-password.txt
FEDERATION_DATABASE_PASSWORD_FILE=./secrets/federation-database-password.txt
FEDERATION_INTERNAL_API_TOKEN_FILE=./secrets/federation-internal-api-token.txt
GATEWAY_INTERNAL_API_TOKEN_FILE=./secrets/gateway-internal-api-token.txt
PUSH_INTERNAL_API_TOKEN_FILE=./secrets/push-internal-api-token.txt
"@
    Set-Content -LiteralPath $environmentFile -Value $content.Trim() -Encoding Ascii
    Write-Host "Created environment file: $environmentFile"
}

Write-Host "Production secrets are ready. Protect and back up the server/secrets directory."
