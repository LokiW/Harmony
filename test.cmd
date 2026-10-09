@echo off
rem Runs test.ps1 without needing to change the PowerShell script execution policy
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0test.ps1" %*
