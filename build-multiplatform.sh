#!/bin/bash

# Script per build multi-architettura da Mac M3 per server Linux

set -e

echo "🔧 Build multi-architettura per ByteBridge WebService"
echo "📍 Building da: $(uname -m) ($(uname -s))"
echo "🎯 Target: Server Linux/AMD64"

# Verifica che Docker buildx sia disponibile
if ! docker buildx version >/dev/null 2>&1; then
    echo "❌ Docker buildx non disponibile. Installalo per supporto multi-architettura."
    exit 1
fi

# Crea builder multi-platform se non esiste
BUILDER_NAME="bytebridge-builder"
if ! docker buildx inspect $BUILDER_NAME >/dev/null 2>&1; then
    echo "🏗️  Creazione builder multi-platform: $BUILDER_NAME"
    docker buildx create --name $BUILDER_NAME --driver docker-container --bootstrap
fi

# Usa il builder multi-platform
docker buildx use $BUILDER_NAME

echo "🔨 Compilazione Java..."
if [ ! -f "target/*.jar" ]; then
    echo "⚠️  JAR non trovato in target/. Compilo con Maven..."
    mvn clean package -DskipTests
else
    echo "✅ JAR trovato in target/"
fi

echo "🐳 Build Docker per architettura server (linux/amd64)..."

# Build solo per linux/amd64 (architettura server)
# Anche se building da Mac M3, il target è sempre linux/amd64
docker buildx build \
    --platform linux/amd64 \
    --file Dockerfile \
    --tag bytebridge-webservice:latest \
    --tag bytebridge-webservice:$(date +%Y%m%d-%H%M%S) \
    --load \
    .

echo "✅ Build completata!"
echo "🏷️  Tag creati:"
echo "   - bytebridge-webservice:latest"
echo "   - bytebridge-webservice:$(date +%Y%m%d-%H%M%S)"

echo ""
echo "🚀 Per avviare l'applicazione:"
echo "   docker-compose up -d"
echo ""
echo "🔍 Per verificare l'immagine:"
echo "   docker image ls bytebridge-webservice"
echo ""
echo "🧪 Per testare Chrome nel container:"
echo "   docker run --rm bytebridge-webservice:latest /usr/local/bin/chrome-headless-shell --version"