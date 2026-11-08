$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
& (Join-Path $PSScriptRoot 'start-db.ps1')
$jdk21 = Join-Path $env:USERPROFILE '.jdks/ms-21.0.8'
if (Test-Path (Join-Path $jdk21 'bin/java.exe')) {
    $env:JAVA_HOME = $jdk21
    $env:PATH = "$jdk21\bin;$env:PATH"
}
$previousMavenHome=$env:MAVEN_USER_HOME
$env:MAVEN_USER_HOME=Join-Path $root '.local/maven-home'
Push-Location $root
try {
    New-Item -ItemType Directory -Force '.local/tmp' | Out-Null
    & (Join-Path $root 'mvnw.cmd') '-Dmaven.repo.local=.local/m2' -ntp package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'Không đóng gói được ứng dụng.' }
    & java '-Djdk.net.unixdomain.tmpdir=.local/tmp' -jar (Join-Path $root 'target/luma-tech-0.1.0-SNAPSHOT.jar')
    if ($LASTEXITCODE -ne 0) { throw 'Ứng dụng không khởi động được.' }
} finally { $env:MAVEN_USER_HOME=$previousMavenHome; Pop-Location }
