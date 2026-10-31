$envLines = Get-Content '.env'
$hostName = ''
$port = 587
$user = ''
$pass = ''
foreach ($l in $envLines) {
    if ($l -match '^\s*SMTP_HOST=(.+)$') { $hostName = $matches[1].Trim() }
    if ($l -match '^\s*SMTP_PORT=(.+)$') { $port = [int]$matches[1].Trim() }
    if ($l -match '^\s*SMTP_USERNAME=(.+)$') { $user = $matches[1].Trim() }
    if ($l -match '^\s*SMTP_PASSWORD=(.+)$') { $pass = $matches[1].Trim() }
}
Write-Output "Testing SMTP connectivity to ${hostName}:${port} with user ${user}..."
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $tcp.Connect($hostName, $port)
    Write-Output "TCP connection to ${hostName}:${port} succeeded."
    $tcp.Close()
    
    $smtp = New-Object System.Net.Mail.SmtpClient($hostName, $port)
    $smtp.EnableSsl = $true
    $smtp.Credentials = New-Object System.Net.NetworkCredential($user, $pass)
    
    $msg = New-Object System.Net.Mail.MailMessage
    $msg.From = New-Object System.Net.Mail.MailAddress($user, 'PrivyChat Security')
    $msg.To.Add($user)
    $msg.Subject = 'PrivyChat SMTP Verification Test - Acceptance Check'
    $msg.Body = 'This is an automated SMTP verification test confirming that PrivyChat backend can dispatch emails via your configured SMTP server.'
    
    $smtp.Send($msg)
    Write-Output "SUCCESS: SMTP server accepted message for delivery."
} catch {
    Write-Output "FAILED: $($_.Exception.Message)"
    if ($_.Exception.InnerException) {
        Write-Output "INNER: $($_.Exception.InnerException.Message)"
    }
}

