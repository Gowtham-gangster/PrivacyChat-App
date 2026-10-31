$workDir = (Join-Path (Get-Location) "android-app")
$gradlew = (Join-Path $workDir "gradlew.bat")

$pinfo = New-Object System.Diagnostics.ProcessStartInfo
$pinfo.FileName = "cmd.exe"
$pinfo.WorkingDirectory = $workDir
$pinfo.Arguments = "/c `"$gradlew`" assembleDebug"
$pinfo.RedirectStandardOutput = $true
$pinfo.RedirectStandardError = $true
$pinfo.UseShellExecute = $false

$p = New-Object System.Diagnostics.Process
$p.StartInfo = $pinfo
$p.Start() | Out-Null
$stdout = $p.StandardOutput.ReadToEnd()
$stderr = $p.StandardError.ReadToEnd()
$p.WaitForExit()

Write-Host "STDOUT:"
Write-Host $stdout
Write-Host "STDERR:"
Write-Host $stderr
Write-Host "Exit Code: $($p.ExitCode)"

