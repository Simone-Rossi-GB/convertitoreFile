#!/bin/bash
echo "🚀 ByteBridge Enterprise Deployment"
echo "This requires GitHub access to pull the private container image"
echo ""

# Check if docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if docker-compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

echo "🔐 GitHub Authentication Required"
read -p "GitHub Username: " USERNAME
read -sp "GitHub Personal Access Token: " TOKEN
echo ""

# Login to GitHub Container Registry
echo "🔑 Authenticating with GitHub Container Registry..."
echo $TOKEN | docker login ghcr.io -u $USERNAME --password-stdin

if [ $? -eq 0 ]; then
    echo "✅ Authentication successful!"
    echo ""
    echo "🚀 Deploying ByteBridge..."

    # Pull latest images
    docker-compose -f docker-compose-public.yml pull

    # Start services
    docker-compose -f docker-compose-public.yml up -d

    echo ""
    echo "✅ ByteBridge deployed successfully!"
    echo "🌐 Web Service: http://localhost:8080"
    echo "🗄️ Database: localhost:5432"
    echo ""
    echo "🧪 Test the service:"
    echo "curl http://localhost:8080/api/converter/status"

else
    echo "❌ Authentication failed!"
    echo "Please check your GitHub credentials and token permissions."
    echo ""
    echo "Token requirements:"
    echo "- read:packages"
    echo "- Possibly repo access if repository is private"
    exit 1
fi
