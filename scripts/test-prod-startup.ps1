$root = (Resolve-Path "$PSScriptRoot\..").Path
$envFile = Join-Path $root ".env"
$jarPath = Join-Path $root "backend\target\privychat-backend-1.0.0.jar"

if (-not (Test-Path $jarPath)) {
    throw "JAR not found at $jarPath"
}

# 1. Parse .env
Write-Host "Loading environment from .env..."
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $idx = $line.IndexOf("=")
        $key = $line.Substring(0, $idx).Trim()
        $val = $line.Substring($idx + 1).Trim()
        [System.Environment]::SetEnvironmentVariable($key, $val, "Process")
    }
}

$env:SPRING_PROFILES_ACTIVE = "prod"
$env:PORT = "5000"
$env:SERVER_PORT = "5000"

$logFile = Join-Path $root "backend\target\test-prod-startup.log"
if (Test-Path $logFile) { Remove-Item -Force $logFile }

Write-Host "Launching Spring Boot JAR in production profile on port 5000..."
$proc = Start-Process -FilePath "java" -ArgumentList "-Dserver.port=5000 -Dspring.profiles.active=prod -jar `"$jarPath`"" -WorkingDirectory (Join-Path $root "backend") -RedirectStandardOutput $logFile -RedirectStandardError (Join-Path $root "backend\target\test-prod-startup-err.log") -PassThru

$started = $false
$maxSeconds = 60
$elapsed = 0

while ($elapsed -lt $maxSeconds) {
    Start-Sleep -Seconds 2
    $elapsed += 2

    $outContent = ""
    $errContent = ""
    if (Test-Path $logFile) {
        $outContent = Get-Content $logFile -Raw -ErrorAction SilentlyContinue
    }
    $errFile = Join-Path $root "backend\target\test-prod-startup-err.log"
    if (Test-Path $errFile) {
        $errContent = Get-Content $errFile -Raw -ErrorAction SilentlyContinue
    }

    if ($outContent -match "Started PrivyChatApplication" -or $errContent -match "Started PrivyChatApplication") {
        $started = $true
        Write-Host "SUCCESS: Detected 'Started PrivyChatApplication' in $elapsed seconds!"
        break
    }
    if ($proc.HasExited) {
        Write-Host "Process exited early with exit code: $($proc.ExitCode)"
        break
    }
    Write-Host "Waiting for startup... ($elapsed/$maxSeconds s)"
}

Write-Host ""
Write-Host "=================== Health Endpoint Probe ==================="
if ($started) {
    try {
        $healthResponse = Invoke-RestMethod -Uri "http://localhost:5000/" -Method Get -TimeoutSec 5
        Write-Host "GET http://localhost:5000/ Response: $($healthResponse | ConvertTo-Json -Compress)"
    } catch {
        Write-Host "Health probe error: $_"
    }

    try {
        $health2 = Invoke-RestMethod -Uri "http://localhost:5000/health" -Method Get -TimeoutSec 5
        Write-Host "GET http://localhost:5000/health Response: $($health2 | ConvertTo-Json -Compress)"
    } catch {
        Write-Host "Health probe error: $_"
    }
} else {
    Write-Host "Application did not reach 'Started PrivyChatApplication'."
}

Write-Host ""
Write-Host "=================== Startup Log Output ==================="
if (Test-Path $logFile) {
    Write-Host "--- STDOUT (Last 50 lines) ---"
    Get-Content $logFile -Tail 50
}
$errFile = Join-Path $root "backend\target\test-prod-startup-err.log"
if (Test-Path $errFile) {
    Write-Host "--- STDERR (Last 50 lines) ---"
    Get-Content $errFile -Tail 50
}

# Cleanup
if (-not $proc.HasExited) {
    Write-Host ""
    Write-Host "Stopping test process (PID: $($proc.Id))..."
    Stop-Process -Id $proc.Id -Force
}
