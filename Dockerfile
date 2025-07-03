# Multi-stage build per supportare diverse architetture (Mac M3 → server)
# Rileva automaticamente l'architettura target
FROM openjdk:8-jre-slim

# Installa dipendenze necessarie per Chrome Headless e utilità di sistema
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libcups2 \
    libdbus-1-3 \
    libdrm2 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libx11-xcb1 \
    libxcomposite1 \
    libxcursor1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxi6 \
    libxrandr2 \
    libxrender1 \
    libxss1 \
    libxtst6 \
    xdg-utils \
    libgbm1 \
    libxshmfence1 \
    libglib2.0-0 \
    libgconf-2-4 \
    libxrandr2 \
    libasound2 \
    libpangocairo-1.0-0 \
    libatk1.0-0 \
    libcairo-gobject2 \
    libgtk-3-0 \
    libgdk-pixbuf2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Scarica e installa Chrome Headless Shell - LINK CORRETTI dal sito ufficiale
# Usa la versione Stable 136.0.7103.49 per Linux64
RUN CHROME_VERSION="136.0.7103.49" \
    && echo "Downloading Chrome Headless Shell ${CHROME_VERSION}..." \
    && wget -q -O /tmp/chrome-headless-shell.zip \
        "https://storage.googleapis.com/chrome-for-testing-public/${CHROME_VERSION}/linux64/chrome-headless-shell-linux64.zip" \
    && cd /tmp \
    && unzip chrome-headless-shell.zip \
    && chmod +x chrome-headless-shell-linux64/chrome-headless-shell \
    && mv chrome-headless-shell-linux64/chrome-headless-shell /usr/local/bin/ \
    && rm -rf /tmp/chrome-headless-shell* \
    && echo "Chrome Headless Shell ${CHROME_VERSION} installed successfully" \
    && ls -la /usr/local/bin/chrome-headless-shell \
    && /usr/local/bin/chrome-headless-shell --version

# Crea utente non-root per sicurezza
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Crea directory per l'applicazione
WORKDIR /app

# Crea directory per uploads e conversioni temporanee
RUN mkdir -p /app/uploads /app/temp && \
    chown -R appuser:appuser /app

# Copia il JAR dell'applicazione dal target locale
COPY target/*.jar app.jar

# Copia eventuali file di configurazione delle risorse
COPY src/main/resources/ /app/resources/

# Crea directory lib per Chrome Headless (se necessario per il ChromeManager)
RUN mkdir -p /app/lib/linux && \
    ln -s /usr/local/bin/chrome-headless-shell /app/lib/linux/chrome-headless-shell

# Imposta le variabili d'ambiente
ENV CHROME_PATH=/usr/local/bin/chrome-headless-shell
ENV JAVA_OPTS="-Xmx4096m -Djava.security.egd=file:/dev/./urandom"
ENV APP_UPLOAD_DIR=/app/uploads

# Cambia ownership per sicurezza e Chrome
RUN chown -R appuser:appuser /app \
    && chown appuser:appuser /usr/local/bin/chrome-headless-shell

# Cambia utente
USER appuser

# Espone la porta dell'applicazione
EXPOSE 8080

# Healthcheck per verificare lo stato dell'applicazione
HEALTHCHECK --interval=120s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/converter/status || exit 1

# Comando di avvio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]