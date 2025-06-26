package webService.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import webService.client.configuration.configHandlers.config.ConfigData;
import webService.client.configuration.configHandlers.config.ConfigInstance;
import webService.server.configuration.configHandlers.conversionContext.ConversionContextData;
import webService.server.configuration.configHandlers.conversionContext.ConversionContextInstance;

import java.io.File;

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
        //Inizializza i gestori dei file di configurazione
        ConfigInstance ci = new ConfigInstance(new File("config-files/config.json"));
        ConfigData.update(ci);
        logger.info(ci.getJsonFile().getAbsolutePath());
        logger.info(ci.getJsonFile().exists());
        ConversionContextInstance cci = new ConversionContextInstance(new File("config-files/conversionContext.json"));
        ConversionContextData.update(cci);
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