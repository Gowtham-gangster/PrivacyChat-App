$envVars = @{}
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z0-9_]+)\s*=\s*(.*)$') {
        $envVars[$matches[1]] = $matches[2].Trim()
    }
}
$url = $envVars['DATABASE_URL']
$user = $envVars['DATABASE_USERNAME']
$pass = $envVars['DATABASE_PASSWORD']

Write-Host "Migrating DB with user: $user"

$pinfo = New-Object System.Diagnostics.ProcessStartInfo
$pinfo.FileName = "cmd.exe"
$pinfo.WorkingDirectory = (Join-Path (Get-Location) "backend")
$pinfo.Arguments = "/c mvn flyway:migrate `"-Dflyway.url=$url`" `"-Dflyway.user=$user`" `"-Dflyway.password=$pass`""
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

