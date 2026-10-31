$pinfo = New-Object System.Diagnostics.ProcessStartInfo
$pinfo.FileName = "cmd.exe"
$pinfo.WorkingDirectory = (Join-Path (Get-Location) "backend")
$pinfo.Arguments = "/c mvn test -Dtest=LiveSmtpDeliveryTest"
$pinfo.RedirectStandardOutput = $true
$pinfo.RedirectStandardError = $true
$pinfo.UseShellExecute = $false

$p = New-Object System.Diagnostics.Process
$p.StartInfo = $pinfo
$p.Start() | Out-Null
$stdout = $p.StandardOutput.ReadToEnd()
$stderr = $p.StandardError.ReadToEnd()
$p.WaitForExit()

Write-Host $stdout
if ($stderr) { Write-Host "ERR: $stderr" }
Write-Host "Exit Code: $($p.ExitCode)"

