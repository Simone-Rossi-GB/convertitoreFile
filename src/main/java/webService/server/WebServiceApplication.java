package webService.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@SpringBootApplication
public class WebServiceApplication {

    private static ConfigurableApplicationContext context;
    private static final Logger logger = LogManager.getLogger(WebServiceApplication.class);

    public static void startWebService() {
        if (context == null || !context.isActive()) {
            context = SpringApplication.run(WebServiceApplication.class);
            logger.trace("Web Service: Web Service avviato su porta 8080");
        }
    }

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