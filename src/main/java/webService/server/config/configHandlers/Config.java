package webService.server.config.configHandlers;

public class Config {

        private Data data;

        // Costruttore vuoto obbligatorio
        public Config() {
        }

        // Getter e setter
        public Data getData() {
                return data;
        }

        public void setData(Data data) {
                this.data = data;
        }

        @Override
        public String toString() {
                return "Config{" +
                        "data=" + data +
                        '}';
        }

        // Classe interna statica per i dati
        public static class Data {
                private String destinationFormat;
                private String password;
                private boolean protectedOutput;
                private boolean union;
                private boolean zippedOutput;
                private String watermark;
                private boolean multipleConversion;
                private String token;

                // Costruttore vuoto obbligatorio
                public Data() {
                }

                // Getter e setter
                public String getDestinationFormat() {
                        return destinationFormat;
                }

                public void setDestinationFormat(String destinationFormat) {
                        this.destinationFormat = destinationFormat;
                }

                public String getPassword() {
                        return password;
                }

                public String getToken() {
                        return token;
                }

                public void setPassword(String password) {
                        this.password = password;
                }

                public boolean isProtectedOutput() {
                        return protectedOutput;
                }

                public void setProtectedOutput(boolean protectedOutput) {
                        this.protectedOutput = protectedOutput;
                }

                public boolean isUnion() {
                        return union;
                }

                public void setUnion(boolean union) {
                        this.union = union;
                }

                public boolean isZippedOutput() {
                        return zippedOutput;
                }

                public void setZippedOutput(boolean zippedOutput) {
                        this.zippedOutput = zippedOutput;
                }

                public String getWatermark() {
                        return watermark;
                }

                public void getToken(String token) {
                        this.token = token;
                }

                public void setWatermark(String watermark) {
                        this.watermark = watermark;
                }

                public boolean isMultipleConversion() {
                        return multipleConversion;
                }

                public void setMultipleConversion(boolean multipleConversion) {
                        this.multipleConversion = multipleConversion;
                }
        }
}