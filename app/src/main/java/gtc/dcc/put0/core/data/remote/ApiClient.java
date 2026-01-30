package gtc.dcc.put0.core.data.remote;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Emulator uses 10.0.2.2 for localhost
    private static final String BASE_URL = "http://10.0.2.2:8080/";
    private static Retrofit retrofit = null;

    public static Put0ApiService getService() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(Put0ApiService.class);
    }

    public static void resetApiClient() {
        retrofit = null;
    }

    public static String getBaseUrl() {
        String ip = gtc.dcc.put0.core.utils.SharedPreferenceManager.getString("server_ip", "10.0.2.2:8080");
        // Sanitize string (remove quotes if present from legacy Gson saving)
        ip = ip.replace("\"", "").trim();

        if (!ip.startsWith("http")) {
            ip = "http://" + ip;
        }
        if (!ip.endsWith("/")) {
            ip = ip + "/";
        }
        return ip;
    }
}
