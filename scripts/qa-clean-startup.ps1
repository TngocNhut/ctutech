$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$pgBin=if($env:PG_BIN){$env:PG_BIN}else{'C:\Program Files\PostgreSQL\17\bin'}
$dbName='luma_clean_'+(Get-Date -Format 'yyyyMMddHHmmss')
$previousPassword=$env:PGPASSWORD
$process=$null
try {
 $env:PGPASSWORD=[IO.File]::ReadAllText((Join-Path $root '.local/db-password.txt')).Trim()
 & "$pgBin\createdb.exe" -h 127.0.0.1 -p 55432 -U luma -O luma $dbName
 if($LASTEXITCODE -ne 0){throw 'Cannot create isolated clean smoke database'}
 $java=Join-Path $env:USERPROFILE '.jdks/ms-21.0.8/bin/java.exe'
 $arguments=@('-Djdk.net.unixdomain.tmpdir=.local/tmp','-jar','target/luma-tech-0.1.0-SNAPSHOT.jar',"--spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/$dbName",'--server.port=8083','--app.scheduling.enabled=false')
 $process=Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $root '.local/clean-startup.log') -RedirectStandardError (Join-Path $root '.local/clean-startup-error.log')
 $ready=$false
 for($i=0;$i -lt 90;$i++){
  if($process.HasExited){throw 'Clean app exited; inspect clean-startup logs'}
  try{$response=Invoke-WebRequest 'http://127.0.0.1:8083/' -TimeoutSec 2;if($response.StatusCode -eq 200){$ready=$true;break}}catch{}
  Start-Sleep -Milliseconds 500
 }
 if(-not $ready){throw 'Clean startup timeout'}
 $migrations=& "$pgBin\psql.exe" -h 127.0.0.1 -p 55432 -U luma -d $dbName -tAc 'SELECT count(*) FROM flyway_schema_history WHERE success'
 if($migrations.Trim() -ne '29'){throw 'Expected all 29 migrations'}
 $counts=& "$pgBin\psql.exe" -h 127.0.0.1 -p 55432 -U luma -d $dbName -tAc 'SELECT count(*) FILTER (WHERE p."DuongDan"=''dien-thoai''),count(*) FILTER (WHERE p."DuongDan"=''laptop''),count(*) FILTER (WHERE p."DuongDan"=''phu-kien'') FROM "SanPham" s JOIN "PhanLoai" p USING ("MaPhanLoai") WHERE s."DangHienThi"'
 if($LASTEXITCODE -ne 0 -or $counts.Trim() -ne '65|65|50'){throw 'Clean seed must contain 65/65/50 public models'}
 foreach($route in @('/san-pham?loai=phu-kien','/san-pham?loai=laptop','/san-pham?loai=dien-thoai','/san-pham','/san-pham/iphone-13?sku=1','/login','/quyen-rieng-tu')){if((Invoke-WebRequest ('http://127.0.0.1:8083'+$route)).StatusCode -ne 200){throw "Failed $route"}}
 Write-Output "Clean startup passed: 29 migrations, seed/catalogue/detail/auth/privacy; isolated database $dbName retained."
}finally{
 if($process -and -not $process.HasExited){$process.Kill();$process.WaitForExit()}
 $env:PGPASSWORD=$previousPassword
}
