#!/bin/bash
# deploy-bytebridge.sh - Linux/macOS Shell Version
set -e

echo ""
echo "🚀 ByteBridge Enterprise Deployment (Linux/macOS)"
echo "This requires GitHub access to pull the private container image"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    echo "Download from: https://www.docker.com/get-started"
    echo ""
    exit 1
fi
echo "✅ Docker found"

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    echo ""
    exit 1
fi
echo "✅ Docker Compose found"

echo ""
echo "🔐 GitHub Authentication Required"
read -p "GitHub Username: " USERNAME

# Secure token input (hidden)
echo -n "GitHub Personal Access Token (will be hidden): "
read -s TOKEN
echo ""

echo ""
echo "🔑 Authenticating with GitHub Container Registry..."

# Login to GitHub Container Registry
echo "$TOKEN" | docker login ghcr.io -u "$USERNAME" --password-stdin

if [ $? -ne 0 ]; then
    echo "❌ Authentication failed!"
    echo "Please check your GitHub credentials and token permissions."
    echo ""
    echo "Token requirements:"
    echo "- read:packages"
    echo "- Possibly repo access if repository is private"
    echo ""
    exit 1
fi
echo "✅ Authentication successful!"

echo ""
# Check if docker-compose-public.yml exists
if [ ! -f "docker-compose-public.yml" ]; then
    echo "❌ docker-compose-public.yml not found in current directory!"
    echo "Current directory: $(pwd)"
    echo "Please ensure docker-compose-public.yml is in the same folder as this script."
    echo ""
    ls -la *.yml 2>/dev/null || echo "No .yml files found"
    echo ""
    exit 1
fi

echo "🚀 Deploying ByteBridge..."
echo ""

# Pull latest images with better error handling
echo "📦 Pulling latest images..."
if ! docker-compose -f "docker-compose-public.yml" pull; then
    echo ""
    echo "❌ Failed to pull images!"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "1. Check if you have access to the repository"
    echo "2. Verify the image name in docker-compose-public.yml"
    echo "3. Check your internet connection"
    echo "4. Try manual pull: docker pull postgres:13-alpine"
    echo ""
    echo "📋 Attempting manual pull of postgres..."
    
    if ! docker pull postgres:13-alpine; then
        echo "❌ Cannot pull postgres image. Check internet connection."
    else
        echo "✅ Postgres pulled successfully. Retrying full pull..."
        if ! docker-compose -f "docker-compose-public.yml" pull; then
            echo "❌ Still failing to pull images."
            exit 1
        fi
    fi
fi
echo "✅ Images pulled successfully!"

echo ""
# Start services
echo "🚀 Starting services..."
if ! docker-compose -f "docker-compose-public.yml" up -d; then
    echo ""
    echo "❌ Failed to start services!"
    echo ""
    echo "🔍 Check logs:"
    docker-compose -f "docker-compose-public.yml" logs
    echo ""
    exit 1
fi

echo ""
echo "✅ ByteBridge deployed successfully!"
echo ""
echo "🌐 Web Service: http://localhost:8080"
echo "🗄️ Database: localhost:5432"
echo ""
echo "⏳ Services are starting up... Please wait 30-60 seconds before testing."
echo ""
echo "🧪 Test the service (wait a minute first):"
echo "  Open browser: http://localhost:8080"
echo "  Or run: curl http://localhost:8080/api/converter/status"
echo ""
echo "🔧 Management commands:"
echo "  docker-compose -f docker-compose-public.yml logs webservice"
echo "  docker-compose -f docker-compose-public.yml logs postgres"
echo "  docker-compose -f docker-compose-public.yml down"
echo "  docker-compose -f docker-compose-public.yml up -d"
echo ""

# Clear sensitive variables
unset TOKEN
unset USERNAME

echo "Press Enter to close..."
read