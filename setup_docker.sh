#!/bin/bash
# build-and-debug.sh

set -e

echo "🚀 Building ByteBridge with Chrome Debug"

# 1. Build del progetto
echo "📦 Building Maven project..."
mvn clean package -DskipTests

# 2. Build Docker image
echo "🔨 Building Docker image..."
docker build --platform linux/amd64 -t bytebridge:latest .

# 3. Stop e start container
echo "🔄 Restarting container..."
docker-compose down 2>/dev/null || true
docker-compose up -d --no-build

# 4. Aspetta avvio completo dell'applicazione
echo "⏳ Waiting for service..."
sleep 20  # Aumentato perché l'app sta ancora caricando

# 5. Attesa con retry per l'API
echo "⏳ Waiting for API to be ready..."
for i in {1..12}; do
    echo "API Check attempt $i/12..."
    STATUS=$(curl -s http://localhost:8080/api/converter/status 2>/dev/null | jq -r '.status' 2>/dev/null || echo "FAILED")
    if [ "$STATUS" = "active" ]; then
        echo "✅ API is ready!"
        break
    else
        if [ $i -eq 12 ]; then
            echo "❌ API not ready after 2 minutes"
        else
            echo "   API not ready yet, waiting..."
            sleep 10
        fi
    fi
done

# 5. DEBUG: Verifica Chrome nel container
echo "🔍 Debugging Chrome in container..."

echo "📋 Available Chrome/Chromium binaries:"
docker exec bytebridge find /usr/bin -name "*chrome*" -o -name "*chromium*" 2>/dev/null || echo "None found"

echo "📋 Environment variables:"
docker exec bytebridge env | grep -E "(CHROME|JAVA)" || echo "No Chrome env vars"

echo "📋 Chrome version test:"
docker exec bytebridge /usr/bin/google-chrome --version 2>/dev/null || \
docker exec bytebridge /usr/bin/google-chrome-stable --version 2>/dev/null || \
docker exec bytebridge /usr/bin/chromium --version 2>/dev/null || \
echo "❌ No working Chrome found"

echo "📋 Chrome with args test:"
docker exec bytebridge /usr/bin/google-chrome --headless --disable-gpu --no-sandbox --version 2>/dev/null || \
docker exec bytebridge /usr/bin/google-chrome-stable --headless --disable-gpu --no-sandbox --version 2>/dev/null || \
echo "❌ Chrome with args failed"

# 6. Test API finale
echo "🧪 Final API test..."

STATUS=$(curl -s http://localhost:8080/api/converter/status 2>/dev/null | jq -r '.status' 2>/dev/null || echo "FAILED")
if [ "$STATUS" = "active" ]; then
    echo "✅ API Status: OK"
else
    echo "❌ API Status: FAILED"
    echo "📋 Recent container logs:"
    docker logs bytebridge --tail 20
fi

# 7. Test conversions endpoint
echo "🧪 Testing conversions endpoint..."
CONVERSIONS=$(curl -s http://localhost:8080/api/converter/conversions/html 2>/dev/null || echo "FAILED")
if [[ "$CONVERSIONS" == *"pdf"* ]]; then
    echo "✅ Conversions: PDF available"
else
    echo "❌ Conversions: PDF not available"
    echo "📋 Response: $CONVERSIONS"
fi

echo ""
echo "🎉 Build completed!"
echo "📊 Service URL: http://localhost:8080"
echo ""
echo "🔧 Debug commands:"
echo "   📊 Container logs: docker logs bytebridge -f"
echo "   🔍 Enter container: docker exec -it bytebridge bash"
echo "   🧪 Test Chrome: docker exec bytebridge /usr/bin/google-chrome --version"