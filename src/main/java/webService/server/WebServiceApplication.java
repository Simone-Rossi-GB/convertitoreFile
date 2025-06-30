package webService.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import webService.server.configuration.configHandlers.serverConfig.ConfigData;
import webService.server.configuration.configHandlers.serverConfig.ConfigInstance;
import webService.server.configuration.configHandlers.serverConfig.ConfigReader;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;

@SpringBootApplication
public class WebServiceApplication {

    private static ConfigurableApplicationContext context;
    private static final Logger logger = LogManager.getLogger(WebServiceApplication.class);

    /*
     * funzione chiamata per controllare se c'è un webservice attivo e
     * se non c'è istanzia e avvia il contesto di esecuzione dell'applicazione webservice
     * context --> applicazione webservice
     */


    public static void startWebService() {
        try {
            // Carica da classpath
            InputStream configStream = WebServiceApplication.class.getClassLoader()
                    .getResourceAsStream("serverConfig.json");

            if (configStream != null) {
                // Crea un file temporaneo
                File tempConfigFile = File.createTempFile("serverConfig", ".json");
                tempConfigFile.deleteOnExit();

                // Usa Apache Commons IO per copiare
                try (FileOutputStream fos = new FileOutputStream(tempConfigFile)) {
                    IOUtils.copy(configStream, fos);
                }

                ConfigInstance ci = new ConfigInstance(tempConfigFile);
                ConfigData.update(ci);
                logger.info("Configurazione caricata dalle risorse");
            } else {
                logger.warn("File serverConfig.json non trovato nelle risorse");
            }
        } catch (Exception e) {
            logger.error("Errore nel caricamento configurazione: {}", e.getMessage());
        }

        if (context == null || !context.isActive()) {
            context = SpringApplication.run(WebServiceApplication.class);
            logger.trace("Web Service: Web Service avviato su porta 8080");
        }
    }


    /*
     * funzione chiamata per controllare se c'è un webservice attivo e di terminarlo
     * context --> applicazione webservice
     */
    public static void stopWebService() {
        if (context != null && context.isActive()) {
            context.close();
            logger.trace("Web Service: Web Service fermato");
        }
    }

    public static boolean isRunning() {
        return context != null && context.isActive();
    }

    public static void main(String[] args) {
        startWebService();
    }
}