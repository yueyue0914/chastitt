@echo off
setlocal
cd /d "%~dp0"

set "MVN=%~dp0.tools\apache-maven-3.9.6\bin\mvn.cmd"
if not exist "%MVN%" (
  echo Maven not found. Run setup-maven.ps1 first.
  exit /b 1
)

"%MVN%" spring-boot:run %*
