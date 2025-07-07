@echo off
setlocal enabledelayedexpansion
REM deploy-bytebridge.bat - Windows Batch Version FIXED

echo.
echo 🚀 ByteBridge Enterprise Deployment (Windows)
echo This requires GitHub access to pull the private container image
echo.

REM Check if Docker is installed
where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker is not installed. Please install Docker Desktop first.
    echo Download from: https://www.docker.com/products/docker-desktop
    echo.
    pause
    exit /b 1
)
echo ✅ Docker found

REM Check if Docker Compose is installed
where docker-compose >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker Compose is not installed. Please install Docker Desktop first.
    echo.
    pause
    exit /b 1
)
echo ✅ Docker Compose found

echo.
echo 🔐 GitHub Authentication Required
set /p "USERNAME=GitHub Username: "

REM Secure token input (hidden)
set "TOKEN="
set /p "TOKEN=GitHub Personal Access Token (will be hidden): "

echo.
echo 🔑 Authenticating with GitHub Container Registry...

REM Create temp file for token
echo !TOKEN! > temp_token.txt

REM Login to GitHub Container Registry using temp file
type temp_token.txt | docker login ghcr.io -u "!USERNAME!" --password-stdin

REM Store login result
set LOGIN_RESULT=%errorlevel%

REM Clean up temp file immediately
del temp_token.txt >nul 2>&1

if !LOGIN_RESULT! neq 0 (
    echo ❌ Authentication failed!
    echo Please check your GitHub credentials and token permissions.
    echo.
    echo Token requirements:
    echo - read:packages
    echo - Possibly repo access if repository is private
    echo.
    pause
    exit /b 1
)

echo ✅ Authentication successful!
echo.

REM Check if docker-compose-public.yml exists
if not exist "docker-compose-public.yml" (
    echo ❌ docker-compose-public.yml not found in current directory!
    echo Current directory: %CD%
    echo Please ensure docker-compose-public.yml is in the same folder as this script.
    echo.
    dir *.yml
    echo.
    pause
    exit /b 1
)

echo 🚀 Deploying ByteBridge...
echo.

REM Pull latest images with better error handling
echo 📦 Pulling latest images...
docker-compose -f "docker-compose-public.yml" pull

if %errorlevel% neq 0 (
    echo.
    echo ❌ Failed to pull images!
    echo.
    echo 🔍 Troubleshooting:
    echo 1. Check if you have access to the repository
    echo 2. Verify the image name in docker-compose-public.yml
    echo 3. Check your internet connection
    echo 4. Try manual pull: docker pull postgres:13-alpine
    echo.
    echo 📋 Attempting manual pull of postgres...
    docker pull postgres:13-alpine
    if %errorlevel% neq 0 (
        echo ❌ Cannot pull postgres image. Check internet connection.
    ) else (
        echo ✅ Postgres pulled successfully. Retrying full pull...
        docker-compose -f "docker-compose-public.yml" pull
    )
    echo.
    pause
    exit /b 1
)

echo ✅ Images pulled successfully!
echo.

REM Start services
echo 🚀 Starting services...
docker-compose -f "docker-compose-public.yml" up -d

if %errorlevel% neq 0 (
    echo.
    echo ❌ Failed to start services!
    echo.
    echo 🔍 Check logs:
    docker-compose -f "docker-compose-public.yml" logs
    echo.
    pause
    exit /b 1
)

echo.
echo ✅ ByteBridge deployed successfully!
echo.
echo 🌐 Web Service: http://localhost:8080
echo 🗄️ Database: localhost:5432
echo.
echo ⏳ Services are starting up... Please wait 30-60 seconds before testing.
echo.
echo 🧪 Test the service (wait a minute first):
echo   Open browser: http://localhost:8080
echo   Or run: curl http://localhost:8080/api/converter/status
echo.
echo 🔧 Management commands:
echo   docker-compose -f docker-compose-public.yml logs webservice
echo   docker-compose -f docker-compose-public.yml logs postgres  
echo   docker-compose -f docker-compose-public.yml down
echo   docker-compose -f docker-compose-public.yml up -d
echo.

REM Clear sensitive variables
set "TOKEN="
set "USERNAME="

echo Press any key to close...
pause >nul