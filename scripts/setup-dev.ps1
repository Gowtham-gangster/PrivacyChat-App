# PrivyChat Local Development Environment Setup Script

Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "        PrivyChat Developer Environment Setup         " -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan

# 1. Check Java JDK
Write-Host "`n[1/5] Checking Java Development Kit..." -ForegroundColor Yellow
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($javaCmd) {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "  Found: $javaVersion" -ForegroundColor Green
} else {
    Write-Host "  [WARNING] 'java' command not in PATH. Please install JDK 21+ and set JAVA_HOME." -ForegroundColor Red
}

# 2. Check Apache Maven
Write-Host "`n[2/5] Checking Apache Maven..." -ForegroundColor Yellow
$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvnCmd) {
    $mvnVersion = mvn -version | Select-Object -First 1
    Write-Host "  Found: $mvnVersion" -ForegroundColor Green
} else {
    Write-Host "  [WARNING] 'mvn' command not in PATH. Please install Maven 3.9+." -ForegroundColor Red
}

# 3. Check Docker & Docker Compose
Write-Host "`n[3/5] Checking Docker & Docker Compose..." -ForegroundColor Yellow
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if ($dockerCmd) {
    $dockerVersion = docker --version
    Write-Host "  Found: $dockerVersion" -ForegroundColor Green
} else {
    Write-Host "  [INFO] Docker not found. Required if running local PostgreSQL / Coturn via compose." -ForegroundColor Gray
}

# 4. Check Android SDK
Write-Host "`n[4/5] Checking Android SDK..." -ForegroundColor Yellow
if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) {
    Write-Host "  Found ANDROID_HOME: $env:ANDROID_HOME" -ForegroundColor Green
} elseif ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) {
    Write-Host "  Found ANDROID_SDK_ROOT: $env:ANDROID_SDK_ROOT" -ForegroundColor Green
} else {
    Write-Host "  [INFO] ANDROID_HOME environment variable not set. Needed for local Android APK compilation." -ForegroundColor Gray
}

# 5. Environment configuration (.env)
Write-Host "`n[5/5] Checking environment configuration..." -ForegroundColor Yellow
if (-not (Test-Path ".env")) {
    if (Test-Path ".env.example") {
        Copy-Item ".env.example" ".env"
        Write-Host "  Created .env from .env.example with secure default development settings." -ForegroundColor Green
    } else {
        Write-Host "  [WARNING] .env.example not found." -ForegroundColor Red
    }
} else {
    Write-Host "  .env file already exists." -ForegroundColor Green
}

Write-Host "`nPrivyChat dev setup check complete!" -ForegroundColor Cyan
Write-Host "To start the backend in dev mode:" -ForegroundColor White
Write-Host "  cd backend; mvn spring-boot:run -Dspring-boot.run.profiles=dev" -ForegroundColor Gray
Write-Host "To start Docker infrastructure:" -ForegroundColor White
Write-Host "  cd infrastructure/docker; docker compose up -d" -ForegroundColor Gray

