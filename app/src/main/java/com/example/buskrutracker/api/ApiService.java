package com.example.buskrutracker.api;

import com.example.buskrutracker.models.ApiResponse;
import com.example.buskrutracker.models.Armada;
import com.example.buskrutracker.models.Kru;
import com.example.buskrutracker.models.MulaiPerjalananResponse;
import com.example.buskrutracker.models.Perjalanan;
import com.example.buskrutracker.models.Rute;
import com.example.buskrutracker.models.SelesaiPerjalananResponse; // ⭐ IMPORT BARU

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Header;

public interface ApiService {

    @POST("api/kru/login")
    Call<ApiResponse<Map<String, Object>>> login(
            @Body Map<String, String> credentials
    );

    @GET("api/kru/armada")
    Call<ApiResponse<List<Armada>>> getArmada(
            @Header("Authorization") String token
    );

    @GET("api/kru/rute")
    Call<ApiResponse<List<Rute>>> getRute(
            @Header("Authorization") String token
    );

    @POST("api/kru/perjalanan/mulai")
    Call<ApiResponse<MulaiPerjalananResponse>> mulaiPerjalanan(
            @Header("Authorization") String token,
            @Body Map<String, Integer> data
    );

    @POST("api/kru/perjalanan/kondisi")
    Call<ApiResponse<Perjalanan>> updateKondisi(
            @Header("Authorization") String token,
            @Body Map<String, Object> data
    );

    @POST("api/kru/perjalanan/penumpang")
    Call<ApiResponse<Perjalanan>> updatePenumpang(
            @Header("Authorization") String token,
            @Body Map<String, Integer> data
    );

    @GET("api/kru/perjalanan/aktif")
    Call<ApiResponse<Perjalanan>> getPerjalananAktif(
            @Header("Authorization") String token
    );

    // ⭐ FIXED: Return type diubah ke SelesaiPerjalananResponse
    //   agar summary (total_pendapatan, penumpang_naik, dll) bisa diakses
    //   dengan type-safe dan diteruskan ke LaporanActivity.
    @POST("api/kru/perjalanan/selesai")
    Call<ApiResponse<SelesaiPerjalananResponse>> selesaiPerjalanan(
            @Header("Authorization") String token,
            @Body Map<String, Object> data
    );

    @POST("api/kru/logout")
    Call<ApiResponse<Object>> logout(
            @Header("Authorization") String token
    );
}