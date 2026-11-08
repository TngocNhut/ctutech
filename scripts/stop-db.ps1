$root = Split-Path $PSScriptRoot -Parent
$pgBin = if ($env:PG_BIN) { $env:PG_BIN } else { 'C:\Program Files\PostgreSQL\17\bin' }
& "$pgBin\pg_ctl.exe" -D (Join-Path $root '.local/postgres') -m fast -w stop
