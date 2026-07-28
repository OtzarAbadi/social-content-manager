# Copy this file to backend/run-backend.ps1. The copied file is ignored by Git.
# Put local credentials in backend/.env (copied from .env.example), never in this script.
# All values in this example are placeholders; the backend loads .env automatically.

$backendDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $backendDirectory

# Quoted arguments prevent PowerShell/Maven from treating the Spring profile as a lifecycle phase.
& .\mvnw.cmd "-Dspring-boot.run.profiles=production" "spring-boot:run"
