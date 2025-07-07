# deploy-bytebridge.ps1 - PowerShell Version
# Enable strict error handling
$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "🚀 ByteBridge Enterprise Deployment (PowerShell)" -ForegroundColor Green
Write-Host "This requires GitHub access to pull the private container image"
Write-Host ""

# Check if Docker is installed
try {
    $null = Get-Command docker -ErrorAction Stop
    Write-Host "✅ Docker found" -ForegroundColor Green
}
catch {
    Write-Host "❌ Docker is not installed. Please install Docker Desktop first." -ForegroundColor Red
    Write-Host "Download from: https://www.docker.com/products/docker-desktop"
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

# Check if Docker Compose is installed
try {
    $null = Get-Command docker-compose -ErrorAction Stop
    Write-Host "✅ Docker Compose found" -ForegroundColor Green
}
catch {
    Write-Host "❌ Docker Compose is not installed. Please install Docker Desktop first." -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "🔐 GitHub Authentication Required" -ForegroundColor Yellow
$USERNAME = Read-Host "GitHub Username"

# Secure token input (hidden)
$TOKEN = Read-Host "GitHub Personal Access Token (will be hidden)" -AsSecureString
$TOKEN_PLAIN = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($TOKEN))

Write-Host ""
Write-Host "🔑 Authenticating with GitHub Container Registry..." -ForegroundColor Yellow

# Login to GitHub Container Registry
try {
    $TOKEN_PLAIN | docker login ghcr.io -u $USERNAME --password-stdin
    if ($LASTEXITCODE -ne 0) {
        throw "Docker login failed"
    }
    Write-Host "✅ Authentication successful!" -ForegroundColor Green
}
catch {
    Write-Host "❌ Authentication failed!" -ForegroundColor Red
    Write-Host "Please check your GitHub credentials and token permissions."
    Write-Host ""
    Write-Host "Token requirements:"
    Write-Host "- read:packages"
    Write-Host "- Possibly repo access if repository is private"
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
# Check if docker-compose-public.yml exists
if (-not (Test-Path "docker-compose-public.yml")) {
    Write-Host "❌ docker-compose-public.yml not found in current directory!" -ForegroundColor Red
    Write-Host "Current directory: $(Get-Location)"
    Write-Host "Please ensure docker-compose-public.yml is in the same folder as this script."
    Write-Host ""
    Get-ChildItem -Filter "*.yml" -ErrorAction SilentlyContinue | Format-Table Name, Length, LastWriteTime
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "🚀 Deploying ByteBridge..." -ForegroundColor Green
Write-Host ""

# Pull latest images with better error handling
Write-Host "📦 Pulling latest images..." -ForegroundColor Yellow
try {
    docker-compose -f "docker-compose-public.yml" pull
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to pull images"
    }
    Write-Host "✅ Images pulled successfully!" -ForegroundColor Green
}
catch {
    Write-Host ""
    Write-Host "❌ Failed to pull images!" -ForegroundColor Red
    Write-Host ""
    Write-Host "🔍 Troubleshooting:" -ForegroundColor Yellow
    Write-Host "1. Check if you have access to the repository"
    Write-Host "2. Verify the image name in docker-compose-public.yml"
    Write-Host "3. Check your internet connection"
    Write-Host "4. Try manual pull: docker pull postgres:13-alpine"
    Write-Host ""
    Write-Host "📋 Attempting manual pull of postgres..." -ForegroundColor Yellow
    
    try {
        docker pull postgres:13-alpine
        if ($LASTEXITCODE -ne 0) {
            throw "Postgres pull failed"
        }
        Write-Host "✅ Postgres pulled successfully. Retrying full pull..." -ForegroundColor Green
        docker-compose -f "docker-compose-public.yml" pull
        if ($LASTEXITCODE -ne 0) {
            throw "Full pull still failing"
        }
    }
    catch {
        Write-Host "❌ Cannot pull postgres image. Check internet connection." -ForegroundColor Red
        Write-Host ""
        Read-Host "Press Enter to exit"
        exit 1
    }
}

Write-Host ""
# Start services
Write-Host "🚀 Starting services..." -ForegroundColor Yellow
try {
    docker-compose -f "docker-compose-public.yml" up -d
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to start services"
    }
}
catch {
    Write-Host ""
    Write-Host "❌ Failed to start services!" -ForegroundColor Red
    Write-Host ""
    Write-Host "🔍 Check logs:" -ForegroundColor Yellow
    docker-compose -f "docker-compose-public.yml" logs
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "✅ ByteBridge deployed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "🌐 Web Service: http://localhost:8080" -ForegroundColor Cyan
Write-Host "🗄️ Database: localhost:5432" -ForegroundColor Cyan
Write-Host ""
Write-Host "⏳ Services are starting up... Please wait 30-60 seconds before testing." -ForegroundColor Yellow
Write-Host ""
Write-Host "🧪 Test the service (wait a minute first):" -ForegroundColor Yellow
Write-Host "  Open browser: http://localhost:8080"
Write-Host "  Or run: curl http://localhost:8080/api/converter/status"
Write-Host ""
Write-Host "🔧 Management commands:" -ForegroundColor Yellow
Write-Host "  docker-compose -f docker-compose-public.yml logs webservice"
Write-Host "  docker-compose -f docker-compose-public.yml logs postgres"
Write-Host "  docker-compose -f docker-compose-public.yml down"
Write-Host "  docker-compose -f docker-compose-public.yml up -d"
Write-Host ""

# Clear sensitive variables
$TOKEN_PLAIN = $null
$TOKEN = $null
$USERNAME = $null

Write-Host "Press Enter to close..."
Read-Host