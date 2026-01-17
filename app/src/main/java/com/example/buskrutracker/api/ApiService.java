package com.example.buskrutracker.api;

import com.example.buskrutracker.models.ApiResponse;
import com.example.buskrutracker.models.Armada;
import com.example.buskrutracker.models.Kru;
import com.example.buskrutracker.models.Perjalanan;
import com.example.buskrutracker.models.Rute;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Header;

public interface ApiService {

    // ============================================
    // 1. LOGIN
    // ============================================
    @POST("api/kru/login")
    Call<ApiResponse<Map<String, Object>>> login(
            @Body Map<String, String> credentials
    );

    // ============================================
    // 2. GET ARMADA
    // ============================================
    @GET("api/kru/armada")
    Call<ApiResponse<List<Armada>>> getArmada(
            @Header("Authorization") String token
    );

    // ============================================
    // 3. GET RUTE
    // ============================================
    @GET("api/kru/rute")
    Call<ApiResponse<List<Rute>>> getRute(
            @Header("Authorization") String token
    );

    // ============================================
    // 4. MULAI PERJALANAN
    // ============================================
    @POST("api/kru/perjalanan/mulai")
    Call<ApiResponse<Perjalanan>> mulaiPerjalanan(
            @Header("Authorization") String token,
            @Body Map<String, Integer> data
    );

    // ============================================
    // 5. UPDATE KONDISI BUS
    // ============================================
    @POST("api/kru/perjalanan/kondisi")
    Call<ApiResponse<Perjalanan>> updateKondisi(
            @Header("Authorization") String token,
            @Body Map<String, Object> data
    );

    // ============================================
    // 6. UPDATE PENUMPANG
    // ============================================
    @POST("api/kru/perjalanan/penumpang")
    Call<ApiResponse<Perjalanan>> updatePenumpang(
            @Header("Authorization") String token,
            @Body Map<String, Integer> data
    );

    // ============================================
    // 7. GET PERJALANAN AKTIF
    // ============================================
    @GET("api/kru/perjalanan/aktif")
    Call<ApiResponse<Perjalanan>> getPerjalananAktif(
            @Header("Authorization") String token
    );

    // ============================================
    // 8. SELESAI PERJALANAN
    // ============================================
    @POST("api/kru/perjalanan/selesai")
    Call<ApiResponse<Map<String, Object>>> selesaiPerjalanan(
            @Header("Authorization") String token,
            @Body Map<String, Object> data
    );

    // ============================================
    // 9. LOGOUT
    // ============================================
    @POST("api/kru/logout")
    Call<ApiResponse<Object>> logout(
            @Header("Authorization") String token
    );
}