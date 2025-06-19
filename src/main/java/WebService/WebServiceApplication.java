package WebService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class WebServiceApplication {

    private static ConfigurableApplicationContext context;

    public static void startWebService() {
        if (context == null || !context.isActive()) {
            context = SpringApplication.run(WebServiceApplication.class);
            System.out.println("Web Service avviato su porta 8080");
        }
    }

    public static void stopWebService() {
        if (context != null && context.isActive()) {
            context.close();
            System.out.println("Web Service fermato");
        }
    }

    public static boolean isRunning() {
        return context != null && context.isActive();
    }

    public static void main(String[] args) {
        startWebService();
    }
}