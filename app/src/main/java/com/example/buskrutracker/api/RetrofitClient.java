package com.example.buskrutracker.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    // ⚠️ GANTI DENGAN IP KOMPUTER ANDA!
    // Cara cek IP: Buka CMD → ketik: ipconfig
    // Cari "IPv4 Address" di bagian WiFi/Ethernet
    private static final String BASE_URL = "http://192.168.1.100:8000/"; // ⭐ GANTI IP INI

    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    /**
     * Get Retrofit instance (Singleton)
     */
    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // HTTP Logging Interceptor (untuk debug)
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // OkHttp Client dengan timeout & logging
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(loggingInterceptor)
                    .build();

            // Build Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * Get API Service instance
     */
    public static ApiService getApiService() {
        if (apiService == null) {
            apiService = getRetrofitInstance().create(ApiService.class);
        }
        return apiService;
    }
}