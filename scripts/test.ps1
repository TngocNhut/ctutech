$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
& (Join-Path $PSScriptRoot 'start-db.ps1')
$pgBin = if ($env:PG_BIN) { $env:PG_BIN } else { 'C:\Program Files\PostgreSQL\17\bin' }
$oldPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = [IO.File]::ReadAllText((Join-Path $root '.local/db-password.txt')).Trim()
    $exists = & "$pgBin\psql.exe" -h 127.0.0.1 -p 55432 -U luma -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='luma_test'"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot connect to the project database server.' }
    if ($exists -ne '1') {
        & "$pgBin\createdb.exe" -h 127.0.0.1 -p 55432 -U luma -O luma luma_test
        if ($LASTEXITCODE -ne 0) { throw 'Cannot create the isolated test database.' }
    }
} finally { $env:PGPASSWORD=$oldPassword }
$jdk21=Join-Path $env:USERPROFILE '.jdks/ms-21.0.8'
if(Test-Path (Join-Path $jdk21 'bin/java.exe')) {
    $env:JAVA_HOME=$jdk21
    $env:PATH="$jdk21\bin;$env:PATH"
}
$previousMavenHome=$env:MAVEN_USER_HOME
$env:MAVEN_USER_HOME=Join-Path $root '.local/maven-home'
Push-Location $root
try {
    New-Item -ItemType Directory -Force .local/tmp | Out-Null
    & (Join-Path $root 'mvnw.cmd') '-Dmaven.repo.local=.local/m2' -B -ntp test
    if($LASTEXITCODE -ne 0) { throw 'Integration tests failed.' }
} finally { $env:MAVEN_USER_HOME=$previousMavenHome; Pop-Location }
