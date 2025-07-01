FROM openjdk:8-jre-slim

# Installa dipendenze base con retry
RUN for i in 1 2 3; do \
    apt-get update && \
    apt-get install -y \
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
        libxss1 \
        libgbm1 \
        xdg-utils \
    && rm -rf /var/lib/apt/lists/* \
    && break || \
    (echo "Attempt $i failed, retrying..." && sleep 10); \
done

# Rileva architettura e installa browser appropriato con retry
RUN ARCH=$(dpkg --print-architecture) && \
    echo "Detected architecture: $ARCH" && \
    for i in 1 2 3; do \
        if [ "$ARCH" = "amd64" ]; then \
            echo "Installing Chrome for AMD64..." && \
            wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - && \
            echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list && \
            apt-get update && \
            apt-get install -y google-chrome-stable && \
            ln -sf /usr/bin/google-chrome-stable /usr/bin/chrome && \
            break; \
        else \
            echo "Installing Chromium for ARM64..." && \
            apt-get update && \
            apt-get install -y chromium chromium-driver && \
            ln -sf /usr/bin/chromium /usr/bin/chrome && \
            break; \
        fi || \
        (echo "Browser install attempt $i failed, retrying..." && sleep 10); \
    done && \
    rm -rf /var/lib/apt/lists/*

# Crea utente non-root
RUN useradd -ms /bin/bash appuser && \
    mkdir -p /app /app/temp /app/logs && \
    chown -R appuser:appuser /app

WORKDIR /app

# Copia il JAR
COPY target/file-type-converter-*.jar app.jar
RUN chown appuser:appuser app.jar

# Script di avvio ottimizzato
RUN echo '#!/bin/bash\n\
set -e\n\
\n\
echo "=== AVVIO CONTAINER ==="\n\
\n\
# Rileva browser disponibile\n\
if [ -f "/usr/bin/google-chrome-stable" ]; then\n\
    export CHROME_PATH="/usr/bin/google-chrome-stable"\n\
    echo "Using Google Chrome Stable"\n\
elif [ -f "/usr/bin/google-chrome" ]; then\n\
    export CHROME_PATH="/usr/bin/google-chrome"\n\
    echo "Using Google Chrome"\n\
elif [ -f "/usr/bin/chromium" ]; then\n\
    export CHROME_PATH="/usr/bin/chromium"\n\
    echo "Using Chromium"\n\
else\n\
    echo "ERROR: No browser found!"\n\
    echo "Available binaries:"\n\
    ls -la /usr/bin/ | grep -E "(chrome|chromium)" || echo "None found"\n\
    exit 1\n\
fi\n\
\n\
# Test browser\n\
echo "Testing browser..."\n\
$CHROME_PATH --version || { echo "Browser test failed"; exit 1; }\n\
\n\
# Imposta argomenti Chrome ottimizzati\n\
export CHROME_ARGS="--headless --disable-gpu --no-sandbox --disable-dev-shm-usage --disable-background-timer-throttling --disable-renderer-backgrounding --disable-features=TranslateUI --remote-debugging-port=0"\n\
\n\
echo "Starting application with browser: $CHROME_PATH"\n\
echo "Java opts: $JAVA_OPTS"\n\
exec java $JAVA_OPTS -Dserver.address=0.0.0.0 -jar app.jar' > start.sh && \
chmod +x start.sh && \
chown appuser:appuser start.sh

# Cambia a utente non-root
USER appuser

EXPOSE 8080

# Environment variables
ENV JAVA_OPTS="-Xmx5g -Xms4096m"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/converter/status || exit 1

# Avvio
CMD ["./start.sh"]