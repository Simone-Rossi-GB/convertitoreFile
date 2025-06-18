package WebService.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConverterWebServiceClient {

    private final OkHttpClient client;
    private final String baseUrl;
    private final Gson gson;

    public ConverterWebServiceClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    public boolean isServiceAvailable() {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "api/converter/status")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getPossibleConversions(String extension) throws Exception {
        Request request = new Request.Builder()
                .url(baseUrl + "api/converter/conversions/" + extension)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Conversione non supportata per: " + extension);
            }

            String jsonResponse = response.body().string();
            Type listType = new TypeToken<List<String>>(){}.getType();
            return gson.fromJson(jsonResponse, listType);
        }
    }

    public ConversionResult convertFile(File sourceFile, String targetFormat) throws Exception {
        return convertFile(sourceFile, targetFormat, null, false);
    }

    public ConversionResult convertFile(File sourceFile, String targetFormat, String password) throws Exception {
        return convertFile(sourceFile, targetFormat, password, false);
    }

    public ConversionResult convertFile(File sourceFile, String targetFormat, String password, boolean mergeImages) throws Exception {

        if (!sourceFile.exists() || !sourceFile.isFile()) {
            throw new Exception("File sorgente non valido: " + sourceFile.getAbsolutePath());
        }

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", sourceFile.getName(),
                        RequestBody.create(sourceFile, MediaType.parse("application/octet-stream")))
                .addFormDataPart("targetFormat", targetFormat);

        if (password != null && !password.trim().isEmpty()) {
            builder.addFormDataPart("password", password);
        }

        if (mergeImages) {
            builder.addFormDataPart("mergeImages", "true");
        }

        Request request = new Request.Builder()
                .url(baseUrl + "api/converter/convert")
                .post(builder.build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            Map<String, Object> result = gson.fromJson(responseBody, Map.class);

            Boolean success = (Boolean) result.get("success");
            if (success != null && success) {
                return new ConversionResult(true, (String) result.get("message"), null);
            } else {
                return new ConversionResult(false, null, (String) result.get("error"));
            }
        }
    }

    public void close() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
}