# Dockerfile per strategia 1 - multi-platform
FROM openjdk:8-jre-slim

# Installa dipendenze base
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    unzip \
    ca-certificates \
    gnupg \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libatspi2.0-0 \
    libcups2 \
    libdbus-1-3 \
    libdrm2 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libwayland-client0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxkbcommon0 \
    libxrandr2 \
    xdg-utils \
    && rm -rf /var/lib/apt/lists/*

# Rileva architettura e installa browser appropriato
RUN ARCH=$(dpkg --print-architecture) && \
    echo "Detected architecture: $ARCH" && \
    if [ "$ARCH" = "amd64" ]; then \
        echo "Installing Chrome for AMD64..." && \
        wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - && \
        echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list && \
        apt-get update && \
        apt-get install -y google-chrome-stable; \
    else \
        echo "Installing Chromium for ARM64..." && \
        apt-get update && \
        apt-get install -y chromium chromium-driver; \
    fi && \
    rm -rf /var/lib/apt/lists/*

# Crea utente non-root
RUN useradd -ms /bin/bash appuser && \
    mkdir -p /app && \
    chown appuser:appuser /app

USER appuser
WORKDIR /app

# Copia il JAR
COPY target/file-type-converter-*.jar app.jar

# Directory temporanee
RUN mkdir -p temp/uploads temp/outputs logs

EXPOSE 8080

# Environment variables
ENV JAVA_OPTS="-Xmx1g -Xms512m"

# Script di avvio che rileva il browser
RUN echo '#!/bin/bash\n\
if [ -f "/usr/bin/google-chrome" ]; then\n\
    export CHROME_PATH="/usr/bin/google-chrome"\n\
    echo "Using Google Chrome"\n\
elif [ -f "/usr/bin/chromium" ]; then\n\
    export CHROME_PATH="/usr/bin/chromium"\n\
    echo "Using Chromium"\n\
else\n\
    echo "No browser found!"\n\
    exit 1\n\
fi\n\
java $JAVA_OPTS -Dserver.address=0.0.0.0 -jar app.jar' > start.sh && \
chmod +x start.sh

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/converter/status || exit 1

# Avvio
CMD ["./start.sh"]