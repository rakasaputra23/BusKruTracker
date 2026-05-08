package com.example.buskrutracker.models;

import com.google.gson.annotations.SerializedName;

/**
 * MulaiPerjalananResponse
 *
 * Model untuk memetakan response baru dari POST /api/kru/perjalanan/mulai.
 *
 * Response sebelumnya (lama):
 *   { "data": { ...perjalanan fields... } }
 *
 * Response sekarang (baru):
 *   {
 *     "data": {
 *       "perjalanan":      { ...perjalanan fields... },
 *       "firebase_bus_id": "bus_1",
 *       "tarif_berlaku":   15000
 *     }
 *   }
 *
 * Cara pakai di ApiService:
 *   Call<ApiResponse<MulaiPerjalananResponse>> mulaiPerjalanan(...)
 *
 * Cara akses di Activity:
 *   MulaiPerjalananResponse resp = apiResponse.getData();
 *   Perjalanan perjalanan       = resp.getPerjalanan();
 *   String firebaseBusId        = resp.getFirebaseBusId();
 *   double tarifBerlaku         = resp.getTarifBerlaku();
 */
public class MulaiPerjalananResponse {

    @SerializedName("perjalanan")
    private Perjalanan perjalanan;

    @SerializedName("firebase_bus_id")
    private String firebaseBusId;

    @SerializedName("tarif_berlaku")
    private double tarifBerlaku;

    // Constructor
    public MulaiPerjalananResponse() {}

    // Getters & Setters
    public Perjalanan getPerjalanan() { return perjalanan; }
    public void setPerjalanan(Perjalanan perjalanan) { this.perjalanan = perjalanan; }

    public String getFirebaseBusId() { return firebaseBusId; }
    public void setFirebaseBusId(String firebaseBusId) { this.firebaseBusId = firebaseBusId; }

    public double getTarifBerlaku() { return tarifBerlaku; }
    public void setTarifBerlaku(double tarifBerlaku) { this.tarifBerlaku = tarifBerlaku; }
}