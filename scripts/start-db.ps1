$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$pgBin = if ($env:PG_BIN) { $env:PG_BIN } else { 'C:\Program Files\PostgreSQL\17\bin' }
$localDir = Join-Path $root '.local'
$dataDir = Join-Path $localDir 'postgres'
New-Item -ItemType Directory -Force $localDir | Out-Null
$passwordFile = Join-Path $localDir 'db-password.txt'
if (-not (Test-Path $passwordFile)) {
    [IO.File]::WriteAllText($passwordFile, [Guid]::NewGuid().ToString('N'))
}
$password = [IO.File]::ReadAllText($passwordFile).Trim()
if (-not (Test-Path (Join-Path $dataDir 'PG_VERSION'))) {
    & "$pgBin\initdb.exe" -D $dataDir -U luma --encoding=UTF8 --locale=C --auth=scram-sha-256 --pwfile=$passwordFile
    if ($LASTEXITCODE -ne 0) { throw 'Không khởi tạo được PostgreSQL riêng của dự án.' }
}
& "$pgBin\pg_ctl.exe" -D $dataDir status *> $null
if ($LASTEXITCODE -ne 0) {
    & "$pgBin\pg_ctl.exe" -D $dataDir -o '-p 55432 -h 127.0.0.1' -l (Join-Path $localDir 'postgres.log') -w start
    if ($LASTEXITCODE -ne 0) { throw 'Không khởi động được PostgreSQL; xem .local/postgres.log.' }
}
$oldPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $password
    $exists = & "$pgBin\psql.exe" -h 127.0.0.1 -p 55432 -U luma -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='luma_shop'"
    if ($LASTEXITCODE -ne 0) { throw 'Không kết nối được PostgreSQL của dự án.' }
    if ($exists -ne '1') {
        & "$pgBin\createdb.exe" -h 127.0.0.1 -p 55432 -U luma -O luma luma_shop
        if ($LASTEXITCODE -ne 0) { throw 'Không tạo được database luma_shop.' }
    }
} finally { $env:PGPASSWORD = $oldPassword }
$configDir = Join-Path $root 'config'
$configFile = Join-Path $configDir 'application-local.properties'
New-Item -ItemType Directory -Force $configDir | Out-Null
if (-not (Test-Path $configFile)) {
    Copy-Item (Join-Path $configDir 'application-local.example.properties') $configFile
}
$config = Get-Content -LiteralPath $configFile | Where-Object { $_ -notmatch '^spring\.datasource\.(url|username|password)=' }
$config += 'spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/luma_shop'
$config += 'spring.datasource.username=luma'
$config += "spring.datasource.password=$password"
[IO.File]::WriteAllLines($configFile, $config, [Text.UTF8Encoding]::new($false))
Write-Host 'PostgreSQL sẵn sàng tại 127.0.0.1:55432/luma_shop. Cấu hình bí mật lưu trong config/application-local.properties.'
