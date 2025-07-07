@echo off
REM deploy-bytebridge.bat - Windows Batch Version

echo 🚀 ByteBridge Enterprise Deployment (Windows)
echo This requires GitHub access to pull the private container image
echo.

REM Check if Docker is installed
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker is not installed. Please install Docker Desktop first.
    echo Download from: https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)
echo ✅ Docker found

REM Check if Docker Compose is installed
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker Compose is not installed. Please install Docker Desktop first.
    pause
    exit /b 1
)
echo ✅ Docker Compose found

echo.
echo 🔐 GitHub Authentication Required
set /p USERNAME="GitHub Username: "
set /p TOKEN="GitHub Personal Access Token: "

echo.
echo 🔑 Authenticating with GitHub Container Registry...

REM Login to GitHub Container Registry
echo %TOKEN% | docker login ghcr.io -u %USERNAME% --password-stdin

if %errorlevel% neq 0 (
    echo ❌ Authentication failed!
    echo Please check your GitHub credentials and token permissions.
    echo.
    echo Token requirements:
    echo - read:packages
    echo - Possibly repo access if repository is private
    pause
    exit /b 1
)

echo ✅ Authentication successful!
echo.
echo 🚀 Deploying ByteBridge...

REM Check if docker-compose-public.yml exists
if not exist "docker-compose-public.yml" (
    echo ❌ docker-compose-public.yml not found in current directory!
    echo Please ensure docker-compose-public.yml is in the same folder as this script.
    pause
    exit /b 1
)

REM Pull latest images
echo 📦 Pulling latest images...
docker-compose -f docker-compose-public.yml pull

if %errorlevel% neq 0 (
    echo ❌ Failed to pull images!
    pause
    exit /b 1
)

REM Start services
echo 🚀 Starting services...
docker-compose -f docker-compose-public.yml up -d

if %errorlevel% neq 0 (
    echo ❌ Failed to start services!
    pause
    exit /b 1
)

echo.
echo ✅ ByteBridge deployed successfully!
echo 🌐 Web Service: http://localhost:8080
echo 🗄️ Database: localhost:5432
echo.
echo 🧪 Test the service:
echo curl http://localhost:8080/api/converter/status
echo.
echo ⏳ Services are starting up... Please wait 30-60 seconds before testing.
echo.
echo 🔧 Management commands:
echo docker-compose -f docker-compose-public.yml logs webservice
echo docker-compose -f docker-compose-public.yml logs postgres
echo docker-compose -f docker-compose-public.yml down
echo docker-compose -f docker-compose-public.yml up -d

REM Clear sensitive variables
set TOKEN=
set USERNAME=

echo.
pause