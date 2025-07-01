#!/bin/bash
# build-and-debug.sh

set -e

echo "🚀 Building File Converter with Chrome Debug"

# 1. Build del progetto
echo "📦 Building Maven project..."
mvn clean package -DskipTests

# 2. Build Docker image
echo "🔨 Building Docker image..."
docker build -t file-converter:latest .

# 3. Stop e start container
echo "🔄 Restarting container..."
docker-compose down 2>/dev/null || true
docker-compose up -d

# 4. Aspetta avvio
echo "⏳ Waiting for service..."
sleep 10

# 5. DEBUG: Verifica Chrome nel container
echo "🔍 Debugging Chrome in container..."

echo "📋 Available Chrome/Chromium binaries:"
docker exec file-converter-app find /usr/bin -name "*chrome*" -o -name "*chromium*" 2>/dev/null || echo "None found"

echo "📋 Environment variables:"
docker exec file-converter-app env | grep -E "(CHROME|JAVA)" || echo "No Chrome env vars"

echo "📋 Chrome version test:"
docker exec file-converter-app /usr/bin/chrome --version 2>/dev/null || \
docker exec file-converter-app /usr/bin/google-chrome-stable --version 2>/dev/null || \
docker exec file-converter-app /usr/bin/chromium --version 2>/dev/null || \
echo "❌ No working Chrome found"

echo "📋 Chrome with args test:"
docker exec file-converter-app /usr/bin/chrome --headless --disable-gpu --no-sandbox --version 2>/dev/null || \
echo "❌ Chrome with args failed"

# 6. Test API
echo "🧪 Testing API..."
sleep 5

STATUS=$(curl -s http://localhost:8080/api/converter/status 2>/dev/null | jq -r '.status' 2>/dev/null || echo "FAILED")
if [ "$STATUS" = "active" ]; then
    echo "✅ API Status: OK"
else
    echo "❌ API Status: FAILED"
    echo "📋 Recent container logs:"
    docker logs file-converter-app --tail 20
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
echo "   📊 Container logs: docker logs file-converter-app -f"
echo "   🔍 Enter container: docker exec -it file-converter-app bash"
echo "   🧪 Test Chrome: docker exec file-converter-app /usr/bin/chrome --version"