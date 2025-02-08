package gtc.dcc.put0.core.utils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;
    private static final String BASE_URL = "https://put0.onrender.com/api/";

    private static OkHttpClient getClient() {
        return new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                String token = SharedPreferenceManager.getToken();  // Obtener el token desde SharedPreferenceManager

                // Solo agregar el token si no es el endpoint de login&&user y si existe un token
                if ((token != null && !request.url().encodedPath().equals("/auth/login")) && !(request.method().equalsIgnoreCase("POST"))) {
                    request = request.newBuilder()
                            .addHeader("Authorization", "Bearer " + token)
                            .build();
                }
                return chain.proceed(request);
            }
        }).build();
    }

    public static Retrofit getInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
