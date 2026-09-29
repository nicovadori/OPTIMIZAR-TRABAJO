@echo off
setlocal
set "GRADLE_VERSION=8.7"
set "DIST=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"
set "CACHE=%USERPROFILE%\.gradle\wrapper\dists\gradle-%GRADLE_VERSION%-bin"
if not exist "%CACHE%\gradle-%GRADLE_VERSION%\bin\gradle.bat" (
  if not exist "%CACHE%" mkdir "%CACHE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "(New-Object Net.WebClient).DownloadFile('%DIST%','%CACHE%\gradle.zip')"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%CACHE%\gradle.zip' '%CACHE%'"
)
call "%CACHE%\gradle-%GRADLE_VERSION%\bin\gradle.bat" %*
