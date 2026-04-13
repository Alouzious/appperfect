package com.example.pitchperfect.api;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static final String BASE_URL = "https://pitch-perfect-api.onrender.com/api/";
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;
    private static PersistentCookieJar cookieJar = null;

    public static ApiService getClient(Context context) {
        if (apiService == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Initialize cookie jar for automatic cookie persistence
            if (cookieJar == null) {
                cookieJar = new PersistentCookieJar(context.getApplicationContext());
            }

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .cookieJar(cookieJar)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }

    /**
     * Get the cookie jar instance for clearing cookies on logout
     */
    public static PersistentCookieJar getCookieJar() {
        return cookieJar;
    }

    /**
     * Reset the API client (used for logout)
     */
    public static void resetClient() {
        retrofit = null;
        apiService = null;
        if (cookieJar != null) {
            cookieJar.clearCookies();
        }
    }
}