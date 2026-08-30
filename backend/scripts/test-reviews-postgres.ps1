param(
    [string]$PostgresBin = 'C:\Program Files\PostgreSQL\18\bin',
    [int]$Port = 55439
)
$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$initDb = Join-Path $PostgresBin 'initdb.exe'
$pgCtl = Join-Path $PostgresBin 'pg_ctl.exe'
if (!(Test-Path -LiteralPath $initDb -PathType Leaf) -or !(Test-Path -LiteralPath $pgCtl -PathType Leaf)) {
    throw 'Pass -PostgresBin pointing to an installed PostgreSQL bin directory.'
}
if ($Port -lt 1024 -or $Port -gt 65535) { throw 'Use a non-privileged TCP port.' }
$testRoot = Join-Path $backendRoot ('target\reviews-postgres-' + [guid]::NewGuid().ToString('N'))
if (!$testRoot.StartsWith($backendRoot + '\target\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid test directory.' }
New-Item -ItemType Directory -Path $testRoot | Out-Null
$dataDir = Join-Path $testRoot 'data'
$testLog = Join-Path $testRoot 'postgres.log'
$previousUrl = $env:BOOKNEST_TEST_DATABASE_URL
$previousUser = $env:BOOKNEST_TEST_DATABASE_USER
$previousPassword = $env:BOOKNEST_TEST_DATABASE_PASSWORD
$started = $false
Push-Location $backendRoot
try {
    & $initDb -D $dataDir -U booknest_test -A trust --encoding=UTF8 --locale=C
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL initialization failed.' }
    # Local-only, short-lived cluster with synthetic data; never uses the application database.
    & $pgCtl -D $dataDir -l $testLog -o "-h 127.0.0.1 -p $Port" -w start
    if ($LASTEXITCODE -ne 0) { throw 'Test PostgreSQL failed to start.' }
    $started = $true
    $env:BOOKNEST_TEST_DATABASE_URL = "jdbc:postgresql://127.0.0.1:$Port/postgres"
    $env:BOOKNEST_TEST_DATABASE_USER = 'booknest_test'
    $env:BOOKNEST_TEST_DATABASE_PASSWORD = ''
    & .\mvnw.cmd test
    if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed.' }
} finally {
    $env:BOOKNEST_TEST_DATABASE_URL = $previousUrl
    $env:BOOKNEST_TEST_DATABASE_USER = $previousUser
    $env:BOOKNEST_TEST_DATABASE_PASSWORD = $previousPassword
    if ($started) {
        & $pgCtl -D $dataDir -m fast -w stop
        if ($LASTEXITCODE -ne 0) { Write-Warning "Could not stop test PostgreSQL at $dataDir" }
    }
    Pop-Location
    Write-Host "Test database and logs retained under $testRoot"
}
