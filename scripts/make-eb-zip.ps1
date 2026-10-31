Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = $PSScriptRoot + "\.."
$staging = Join-Path $root "backend\target\eb-bundle"

if (Test-Path $staging) {
    Remove-Item -Recurse -Force $staging
}

$nginxDir = Join-Path $staging ".platform\nginx\conf.d"
New-Item -ItemType Directory -Path $nginxDir -Force | Out-Null

Copy-Item (Join-Path $root "backend\Procfile") -Destination $staging
Copy-Item (Join-Path $root "backend\target\privychat-backend-1.0.0.jar") -Destination $staging
Copy-Item (Join-Path $root "backend\.platform\nginx\conf.d\proxy.conf") -Destination $nginxDir

$zipPath = Join-Path $root "backend\target\privychat-eb-deployment.zip"
if (Test-Path $zipPath) {
    Remove-Item -Force $zipPath
}

[System.IO.Compression.ZipFile]::CreateFromDirectory((Resolve-Path $staging).Path, $zipPath)

$item = Get-Item $zipPath
Write-Host "Created bundle: $($item.FullName) ($([math]::Round($item.Length / 1MB, 2)) MB)"

