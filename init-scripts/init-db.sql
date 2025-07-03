-- Script di inizializzazione del database PostgreSQL
-- Questo file verrà eseguito automaticamente alla prima creazione del container

-- Creazione del database se non esiste (già gestito dalle variabili d'ambiente)
CREATE DATABASE IF NOT EXISTS bytebridge;

-- Creazione dell'utente admin se non esiste (già gestito dalle variabili d'ambiente)
CREATE USER IF NOT EXISTS admin WITH PASSWORD 'bytebridge';

-- Assegnazione privilegi
GRANT ALL PRIVILEGES ON DATABASE bytebridge TO admin;

-- Estensioni utili per PostgreSQL
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Impostazioni per il database
ALTER DATABASE bytebridge SET timezone TO 'UTC';

-- Commenti informativi
COMMENT ON DATABASE bytebridge IS 'Database per il sistema ByteBridge di conversione file';

-- Crea un indice per migliorare le performance delle query di autenticazione
-- (La tabella users verrà creata automaticamente da Hibernate)

-- Log di inizializzazione
DO $$
BEGIN
    RAISE NOTICE 'Database ByteBridge inizializzato correttamente';
    RAISE NOTICE 'Utente admin configurato';
    RAISE NOTICE 'Timezone impostato su UTC';
END $$;