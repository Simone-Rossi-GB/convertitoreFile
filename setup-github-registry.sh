# ====================================
# STEP-BY-STEP: GITHUB CONTAINER REGISTRY
# ====================================

# =====================================
# STEP 1: CREA PERSONAL ACCESS TOKEN
# =====================================
echo "🔑 STEP 1: Create GitHub Personal Access Token"
echo "1. Vai su: https://github.com/settings/tokens"
echo "2. Click: Generate new token (classic)"
echo "3. Seleziona questi scopes:"
echo "   ✅ write:packages"
echo "   ✅ read:packages"
echo "   ✅ delete:packages"
echo "   ✅ repo (se vuoi gestire anche il codice)"
echo "4. Copia il token generato (salvalo in modo sicuro!)"
echo ""
read -p "Hai creato il token? (y/n): " token_ready

# =====================================
# STEP 2: LOGIN A GITHUB CONTAINER REGISTRY
# =====================================
echo ""
echo "🔐 STEP 2: Login to GitHub Container Registry"
read -p "GitHub Username: " GITHUB_USERNAME
read -sp "GitHub Token (incolla qui): " GITHUB_TOKEN
echo ""

echo $GITHUB_TOKEN | docker login ghcr.io -u $GITHUB_USERNAME --password-stdin

if [ $? -eq 0 ]; then
    echo "✅ Login successful!"
else
    echo "❌ Login failed! Check your credentials."
    exit 1
fi

# =====================================
# STEP 3: TAG E PUSH DELL'IMMAGINE
# =====================================
echo ""
echo "🏷️ STEP 3: Tag and Push Image"

# Verifica che l'immagine locale esista
if ! docker images | grep -q "bytebridge.*latest"; then
    echo "❌ Local image 'bytebridge:latest' not found!"
    echo "Build it first with: docker build --platform linux/amd64 -t bytebridge:latest ."
    exit 1
fi

# Tag per GitHub Container Registry
docker tag bytebridge:latest ghcr.io/$GITHUB_USERNAME/bytebridge:latest
docker tag bytebridge:latest ghcr.io/$GITHUB_USERNAME/bytebridge:1.0

echo "🚀 Pushing to GitHub Container Registry..."
docker push ghcr.io/$GITHUB_USERNAME/bytebridge:latest
docker push ghcr.io/$GITHUB_USERNAME/bytebridge:1.0

echo "✅ Images pushed successfully!"

# =====================================
# STEP 4: CREA DOCKER-COMPOSE PUBBLICO
# =====================================
echo ""
echo "📄 STEP 4: Create public docker-compose.yml"

cat > docker-compose-public.yml << EOF
services:
  postgres:
    image: postgres:13-alpine
    container_name: bytebridge-postgres
    environment:
      POSTGRES_DB: bytebridge
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: bytebridge
      PGDATA: /var/lib/postgresql/data/pgdata
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    networks:
      - bytebridge-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U admin -d bytebridge"]
      interval: 30s
      timeout: 10s
      retries: 5

  webservice:
    image: ghcr.io/$GITHUB_USERNAME/bytebridge:latest
    container_name: bytebridge
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://bytebridge-postgres:5432/bytebridge
      SPRING_DATASOURCE_USERNAME: admin
      SPRING_DATASOURCE_PASSWORD: bytebridge
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.PostgreSQLDialect
      SPRING_JPA_SHOW_SQL: true
      SERVER_ADDRESS: 0.0.0.0
      SERVER_PORT: 8080
      APP_UPLOAD_DIR: /app/uploads
      CHROME_PATH: /usr/bin/google-chrome
      JAVA_OPTS: -Xmx4096m -Djava.security.egd=file:/dev/./urandom
      LOGGING_LEVEL_WEBSERVICE: INFO
    volumes:
      - uploads_volume:/app/uploads
      - temp_volume:/app/temp
      - logs_volume:/app/logs
    ports:
      - "8080:8080"
    networks:
      - bytebridge-network
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

volumes:
  postgres_data:
    driver: local
  uploads_volume:
    driver: local
  temp_volume:
    driver: local
  logs_volume:
    driver: local

networks:
  bytebridge-network:
    driver: bridge
EOF

echo "✅ docker-compose-public.yml created!"

# =====================================
# STEP 5: SCRIPT DI DEPLOY PER UTENTI
# =====================================
echo ""
echo "📋 STEP 5: Create deployment script for users"

cat > deploy-bytebridge.sh << 'EOF'
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
EOF

chmod +x deploy-bytebridge.sh

echo "✅ deploy-bytebridge.sh created!"

# =====================================
# STEP 6: ISTRUZIONI FINALI
# =====================================
echo ""
echo "🎉 SETUP COMPLETED!"
echo ""
echo "📦 Your images are now available at:"
echo "   ghcr.io/$GITHUB_USERNAME/bytebridge:latest"
echo "   ghcr.io/$GITHUB_USERNAME/bytebridge:1.0"
echo ""
echo "🔄 NEXT STEPS:"
echo "1. Upload docker-compose-public.yml to your GitHub repository"
echo "2. Upload deploy-bytebridge.sh to your GitHub repository"
echo "3. Set package visibility to PRIVATE (see instructions below)"
echo "4. Share deploy script with authorized users"
echo ""
echo "🔒 TO MAKE PACKAGE PRIVATE:"
echo "1. Go to: https://github.com/$GITHUB_USERNAME?tab=packages"
echo "2. Click on 'bytebridge' package"
echo "3. Package settings → Change visibility → Private"
echo ""
echo "👥 TO GIVE ACCESS TO TEAM MEMBERS:"
echo "1. Go to your GitHub repository settings"
echo "2. Manage access → Invite collaborators"
echo "3. Give them 'Read' access (minimum for pulling images)"
echo ""
echo "🚀 USERS CAN NOW DEPLOY WITH:"
echo "./deploy-bytebridge.sh"