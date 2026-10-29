# PrivyChat Android Release Keystore Generator
# Generates a production-grade 4096-bit RSA keystore for APK signing.

param(
    [string]$KeystorePath = "android-app/app/privychat-release.keystore",
    [string]$Alias = "privychat",
    [string]$DName = "CN=PrivyChat Release, OU=Security, O=PrivyChat, C=US",
    [int]$ValidityDays = 10000
)

Write-Host "=== PrivyChat Android Release Keystore Generator ===" -ForegroundColor Cyan

# Check if keytool is available
$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool) {
    if ($env:JAVA_HOME) {
        $keytoolPath = Join-Path $env:JAVA_HOME "bin\keytool.exe"
        if (Test-Path $keytoolPath) {
            $keytool = $keytoolPath
        }
    }
}

if (-not $keytool) {
    Write-Error "keytool was not found in PATH or JAVA_HOME. Please ensure JDK 17+ is installed."
    exit 1
}

if (Test-Path $KeystorePath) {
    Write-Warning "Keystore already exists at: $KeystorePath"
    $overwrite = Read-Host "Do you want to overwrite it? (y/N)"
    if ($overwrite -ne "y") {
        Write-Host "Aborted." -ForegroundColor Yellow
        exit 0
    }
    Remove-Item $KeystorePath -Force
}

$parentDir = Split-Path -Parent $KeystorePath
if ($parentDir -and -not (Test-Path $parentDir)) {
    New-Item -ItemType Directory -Path $parentDir -Force | Out-Null
}

$storePassword = Read-Host "Enter Keystore Password (min 8 characters)" -AsSecureString
$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($storePassword)
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)

if ($plainPassword.Length -lt 8) {
    Write-Error "Password must be at least 8 characters long."
    exit 1
}

Write-Host "Generating 4096-bit RSA release signing key..." -ForegroundColor Cyan

& $keytool -genkeypair `
    -v `
    -keystore $KeystorePath `
    -alias $Alias `
    -keyalg RSA `
    -keysize 4096 `
    -sigalg SHA256withRSA `
    -validity $ValidityDays `
    -dname $DName `
    -storepass $plainPassword `
    -keypass $plainPassword

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Keystore generated successfully at: $KeystorePath" -ForegroundColor Green
    Write-Host "Alias: $Alias"
    Write-Host "Validity: $ValidityDays days"
    Write-Host ""
    Write-Host "Certificate Fingerprint (SHA-256):" -ForegroundColor Cyan
    & $keytool -list -v -keystore $KeystorePath -alias $Alias -storepass $plainPassword | Select-String "SHA256:"
} else {
    Write-Error "Failed to generate release keystore."
    exit 1
}

