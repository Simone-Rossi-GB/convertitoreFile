# Dockerfile per build multi-architettura (Mac M3 → server Linux/AMD64)
FROM openjdk:8-jre-slim

# Installa dipendenze necessarie per Chrome e utilità di sistema
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
    libpangocairo-1.0-0 \
    libcairo-gobject2 \
    libgdk-pixbuf2.0-0 \
    locales \
    && rm -rf /var/lib/apt/lists/*

# Configura locale per ICU
RUN locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

# Installa Google Chrome completo (con tutti i dati ICU)
RUN wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

# Verifica installazione Chrome
RUN google-chrome --version

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

# Directory lib non più necessaria con path assoluto
# RUN mkdir -p /app/lib/linux

# Imposta le variabili d'ambiente
ENV CHROME_PATH=/usr/bin/google-chrome
ENV JAVA_OPTS="-Xmx4096m -Djava.security.egd=file:/dev/./urandom"
ENV APP_UPLOAD_DIR=/app/uploads

# Cambia ownership per sicurezza
RUN chown -R appuser:appuser /app

# Cambia utente
USER appuser

# Espone la porta dell'applicazione
EXPOSE 8080

# Healthcheck per verificare lo stato dell'applicazione
HEALTHCHECK --interval=120s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/converter/status || exit 1

# Comando di avvio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]