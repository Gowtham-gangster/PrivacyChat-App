param(
    [string]$TargetZip = ""
)

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = (Resolve-Path "$PSScriptRoot\..").Path
$backendDir = Join-Path $root "backend"
$targetDir = Join-Path $backendDir "target"
$staging = Join-Path $targetDir "eb-bundle"

if ([string]::IsNullOrWhiteSpace($TargetZip)) {
    $TargetZip = Join-Path $targetDir "privychat-eb-deployment.zip"
}

Write-Host "=========================================================="
Write-Host "Creating POSIX-compliant Elastic Beanstalk Deployment Bundle"
Write-Host "=========================================================="
Write-Host "Project Root : $root"
Write-Host "Target ZIP   : $TargetZip"

# 1. Clean and setup staging
if (Test-Path $staging) {
    Remove-Item -Recurse -Force $staging
}
$nginxDir = Join-Path $staging ".platform\nginx\conf.d"
New-Item -ItemType Directory -Path $nginxDir -Force | Out-Null

# 2. Locate built JAR
$builtJar = Join-Path $targetDir "privychat-backend-1.0.0.jar"
if (-not (Test-Path $builtJar)) {
    throw "Executable JAR not found at $builtJar. Please run 'mvn package' first."
}

# 3. Copy to application.jar at staging root
$appJar = Join-Path $staging "application.jar"
Copy-Item $builtJar -Destination $appJar
Write-Host "Copied $builtJar -> application.jar ($([math]::Round((Get-Item $appJar).Length / 1MB, 2)) MB)"

# 4. Write Procfile with strict LF
$procfilePath = Join-Path $staging "Procfile"
$procfileContent = "web: java -Dserver.port=`${PORT:-5000} -Dspring.profiles.active=`${SPRING_PROFILES_ACTIVE:-prod} -Djava.security.egd=file:/dev/./urandom -jar application.jar`n"
[System.IO.File]::WriteAllBytes($procfilePath, [System.Text.Encoding]::UTF8.GetBytes($procfileContent))
Write-Host "Written Procfile with POSIX LF line endings"

# 5. Write .platform/nginx/conf.d/proxy.conf with strict LF
$proxyConfPath = Join-Path $nginxDir "proxy.conf"
$proxyContent = "client_max_body_size 50M;`nproxy_connect_timeout 60s;`nproxy_send_timeout 3600s;`nproxy_read_timeout 3600s;`n"
[System.IO.File]::WriteAllBytes($proxyConfPath, [System.Text.Encoding]::UTF8.GetBytes($proxyContent))
Write-Host "Written .platform/nginx/conf.d/proxy.conf with POSIX LF line endings"

# 6. Build ZIP with strict POSIX forward slash entries
if (Test-Path $TargetZip) {
    Remove-Item -Force $TargetZip
}

$zipStream = [System.IO.File]::Open($TargetZip, [System.IO.FileMode]::Create)
$zipArchive = New-Object System.IO.Compression.ZipArchive($zipStream, [System.IO.Compression.ZipArchiveMode]::Create)

function Add-FileToZip($archive, [string]$filePath, [string]$entryName) {
    # CRITICAL: Force POSIX forward slashes regardless of OS
    $posixName = $entryName.Replace("\", "/")
    Write-Host "Adding ZIP entry: $posixName"
    
    $entry = $archive.CreateEntry($posixName, [System.IO.Compression.CompressionLevel]::Optimal)
    $entryStream = $entry.Open()
    $fileStream = [System.IO.File]::OpenRead($filePath)
    $fileStream.CopyTo($entryStream)
    $fileStream.Dispose()
    $entryStream.Dispose()
}

Add-FileToZip $zipArchive $appJar "application.jar"
Add-FileToZip $zipArchive $procfilePath "Procfile"
Add-FileToZip $zipArchive $proxyConfPath ".platform/nginx/conf.d/proxy.conf"

$zipArchive.Dispose()
$zipStream.Dispose()

$zipItem = Get-Item $TargetZip
Write-Host "Successfully generated ZIP: $($zipItem.FullName) ($([math]::Round($zipItem.Length / 1MB, 2)) MB)"

# 7. Verification: Inspect raw entry paths
Write-Host ""
Write-Host "=========================================================="
Write-Host "VERIFICATION: Inspecting ZIP Entries"
Write-Host "=========================================================="

$readArchive = [System.IO.Compression.ZipFile]::OpenRead($TargetZip)
$hasBackslash = $false
$foundAppJar = $false
$foundProcfile = $false
$foundProxyConf = $false

foreach ($entry in $readArchive.Entries) {
    $name = $entry.FullName
    $len = $entry.Length
    $containsBs = $name.Contains("\")
    Write-Host "Entry: '$name' (Size: $len bytes) | Contains '\': $containsBs"
    if ($containsBs) { $hasBackslash = $true }
    if ($name -eq "application.jar") { $foundAppJar = $true }
    if ($name -eq "Procfile") { $foundProcfile = $true }
    if ($name -eq ".platform/nginx/conf.d/proxy.conf") { $foundProxyConf = $true }
}
$readArchive.Dispose()

if ($hasBackslash) {
    throw "VERIFICATION FAILED: ZIP contains backslashes!"
}
if (-not $foundAppJar) {
    throw "VERIFICATION FAILED: application.jar missing from root!"
}
if (-not $foundProcfile) {
    throw "VERIFICATION FAILED: Procfile missing from root!"
}
if (-not $foundProxyConf) {
    throw "VERIFICATION FAILED: .platform/nginx/conf.d/proxy.conf missing!"
}

# 8. Test extract verification
Write-Host ""
Write-Host "=========================================================="
Write-Host "VERIFICATION: Testing ZIP extraction"
Write-Host "=========================================================="
$testExtractDir = Join-Path $targetDir "eb-verify-extract"
if (Test-Path $testExtractDir) {
    Remove-Item -Recurse -Force $testExtractDir
}
[System.IO.Compression.ZipFile]::ExtractToDirectory($TargetZip, $testExtractDir)
$extractedFiles = Get-ChildItem -Path $testExtractDir -Recurse | Where-Object { -not $_.PSIsContainer }
foreach ($f in $extractedFiles) {
    $rel = $f.FullName.Substring($testExtractDir.Length + 1)
    Write-Host "Extracted: $rel ($($f.Length) bytes)"
}
Remove-Item -Recurse -Force $testExtractDir

Write-Host ""
Write-Host "ALL VERIFICATIONS PASSED: Bundle is ready for AWS Elastic Beanstalk (Linux)."
