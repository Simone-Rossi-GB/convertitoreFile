package Converters;

/**
 * Classe per gestire il contesto di esecuzione dei convertitori
 */
public class ConverterContext {
    public enum Environment {
        GUI,
        WEBSERVICE
    }
    
    private static final ThreadLocal<Environment> currentEnvironment = new ThreadLocal<Environment>() {
        @Override
        protected Environment initialValue() {
            return Environment.GUI; // Default a GUI
        }
    };
    
    public static void setEnvironment(Environment env) {
        currentEnvironment.set(env);
    }
    
    public static Environment getEnvironment() {
        return currentEnvironment.get();
    }
    
    public static boolean isGuiEnvironment() {
        return getEnvironment() == Environment.GUI;
    }
    
    public static boolean isWebServiceEnvironment() {
        return getEnvironment() == Environment.WEBSERVICE;
    }
    
    public static void clearEnvironment() {
        currentEnvironment.remove();
    }
}