$workDir = (Join-Path (Get-Location) "backend")
$pinfo = New-Object System.Diagnostics.ProcessStartInfo
$pinfo.FileName = "cmd.exe"
$pinfo.WorkingDirectory = $workDir
$pinfo.Arguments = "/c mvn package -DskipTests"
$pinfo.UseShellExecute = $false

$p = [System.Diagnostics.Process]::Start($pinfo)
$p.WaitForExit()
Write-Host "Exit Code: $($p.ExitCode)"

