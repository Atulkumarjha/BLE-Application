@echo off
setlocal

where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle is required to build the Android module. Install Gradle or use Flutter to generate the standard wrapper files.
  exit /b 1
)

gradle %*