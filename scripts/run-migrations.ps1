# PrivyChat Database Migration Runner
# Executes Flyway database migrations against PostgreSQL.

param(
    [string]$JdbcUrl = "jdbc:postgresql://localhost:5432/privychat",
    [string]$DbUser = "privychat_admin",
    [string]$DbPass = "ChangeMeSecurePassword123!"
)

Write-Host "=== PrivyChat Flyway Database Migration Runner ===" -ForegroundColor Cyan
Write-Host "Target Database: $JdbcUrl"
Write-Host "User: $DbUser"

$backendPom = "backend/pom.xml"
if (-not (Test-Path $backendPom)) {
    Write-Error "backend/pom.xml not found. Please run this script from the workspace root."
    exit 1
}

Write-Host "`nRunning Flyway migrate..." -ForegroundColor Yellow

$mvnArgs = @(
    "-f", "backend/pom.xml",
    "flyway:migrate",
    "-Dflyway.url=$JdbcUrl",
    "-Dflyway.user=$DbUser",
    "-Dflyway.password=$DbPass",
    "-Dflyway.locations=classpath:db/migration"
)

& mvn @mvnArgs

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[SUCCESS] Database migrations applied successfully!" -ForegroundColor Green
} else {
    Write-Host "`n[ERROR] Migration failed. Ensure PostgreSQL is running and accessible." -ForegroundColor Red
    exit 1
}

