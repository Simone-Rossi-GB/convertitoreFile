# deploy-bytebridge.ps1 - Windows PowerShell Version
Write-Host "🚀 ByteBridge Enterprise Deployment (Windows)" -ForegroundColor Green
Write-Host "This requires GitHub access to pull the private container image" -ForegroundColor Yellow
Write-Host ""

# Check if Docker is installed
try {
    $dockerVersion = docker --version
    Write-Host "✅ Docker found: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker is not installed. Please install Docker Desktop first." -ForegroundColor Red
    Write-Host "Download from: https://www.docker.com/products/docker-desktop" -ForegroundColor Yellow
    exit 1
}

# Check if Docker Compose is installed
try {
    $composeVersion = docker-compose --version
    Write-Host "✅ Docker Compose found: $composeVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker Compose is not installed. Please install Docker Desktop first." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🔐 GitHub Authentication Required" -ForegroundColor Cyan

# Get GitHub credentials
$username = Read-Host "GitHub Username"
$secureToken = Read-Host "GitHub Personal Access Token" -AsSecureString
$token = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureToken))

Write-Host ""
Write-Host "🔑 Authenticating with GitHub Container Registry..." -ForegroundColor Cyan

# Login to GitHub Container Registry
$loginProcess = Start-Process -FilePath "docker" -ArgumentList "login", "ghcr.io", "-u", $username, "--password-stdin" -NoNewWindow -PassThru -RedirectStandardInput -InputObject $token -Wait

if ($loginProcess.ExitCode -eq 0) {
    Write-Host "✅ Authentication successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🚀 Deploying ByteBridge..." -ForegroundColor Green

    # Check if docker-compose-public.yml exists
    if (-not (Test-Path "docker-compose-public.yml")) {
        Write-Host "❌ docker-compose-public.yml not found in current directory!" -ForegroundColor Red
        Write-Host "Please ensure docker-compose-public.yml is in the same folder as this script." -ForegroundColor Yellow
        exit 1
    }

    try {
        # Pull latest images
        Write-Host "📦 Pulling latest images..." -ForegroundColor Cyan
        docker-compose -f docker-compose-public.yml pull

        # Start services
        Write-Host "🚀 Starting services..." -ForegroundColor Cyan
        docker-compose -f docker-compose-public.yml up -d

        Write-Host ""
        Write-Host "✅ ByteBridge deployed successfully!" -ForegroundColor Green
        Write-Host "🌐 Web Service: http://localhost:8080" -ForegroundColor Yellow
        Write-Host "🗄️ Database: localhost:5432" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "🧪 Test the service:" -ForegroundColor Cyan
        Write-Host "curl http://localhost:8080/api/converter/status" -ForegroundColor White
        Write-Host ""
        Write-Host "⏳ Services are starting up... Please wait 30-60 seconds before testing." -ForegroundColor Yellow

    } catch {
        Write-Host "❌ Deployment failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }

} else {
    Write-Host "❌ Authentication failed!" -ForegroundColor Red
    Write-Host "Please check your GitHub credentials and token permissions." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Token requirements:" -ForegroundColor Cyan
    Write-Host "- read:packages" -ForegroundColor White
    Write-Host "- Possibly repo access if repository is private" -ForegroundColor White
    exit 1
}

# Clear sensitive variables
$token = $null
$secureToken = $null

Write-Host ""
Write-Host "🔧 Management commands:" -ForegroundColor Cyan
Write-Host "docker-compose -f docker-compose-public.yml logs webservice  # View app logs" -ForegroundColor White
Write-Host "docker-compose -f docker-compose-public.yml logs postgres    # View DB logs" -ForegroundColor White
Write-Host "docker-compose -f docker-compose-public.yml down             # Stop all services" -ForegroundColor White
Write-Host "docker-compose -f docker-compose-public.yml up -d            # Start all services" -ForegroundColor White