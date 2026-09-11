# Download portable Maven into JAVA/.tools (no global install needed)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$tools = Join-Path $root ".tools"
$mavenHome = Join-Path $tools "apache-maven-3.9.6"
$mvn = Join-Path $mavenHome "bin\mvn.cmd"

if (Test-Path $mvn) {
  Write-Host "Maven already present: $mvn"
  & $mvn -v
  exit 0
}

New-Item -ItemType Directory -Force -Path $tools | Out-Null
$zip = Join-Path $tools "maven.zip"
$url = "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"
Write-Host "Downloading Maven 3.9.6 ..."
Invoke-WebRequest -Uri $url -OutFile $zip
Expand-Archive -Path $zip -DestinationPath $tools -Force
Remove-Item $zip -Force
& $mvn -v
Write-Host ""
Write-Host "OK. Start backend with: .\run-backend.cmd"
