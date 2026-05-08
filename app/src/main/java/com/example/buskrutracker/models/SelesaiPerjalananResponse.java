package com.example.buskrutracker.models;

import com.google.gson.annotations.SerializedName;

/**
 * SelesaiPerjalananResponse
 *
 * Model untuk response POST /api/kru/perjalanan/selesai.
 *
 * Struktur response dari server:
 * {
 *   "success": true,
 *   "data": {
 *     "perjalanan": { ...fields... },
 *     "summary": {
 *       "durasi_jam":       3,
 *       "durasi_menit":     15,
 *       "total_penumpang":  5,
 *       "penumpang_naik":   10,
 *       "jarak_km":         145.5,
 *       "tarif_per_orang":  15000,
 *       "total_pendapatan": 150000
 *     }
 *   }
 * }
 *
 * Cara pakai di ApiService:
 *   Call<ApiResponse<SelesaiPerjalananResponse>> selesaiPerjalanan(...)
 */
public class SelesaiPerjalananResponse {

    @SerializedName("perjalanan")
    private Perjalanan perjalanan;

    @SerializedName("summary")
    private Summary summary;

    public Perjalanan getPerjalanan() { return perjalanan; }
    public void setPerjalanan(Perjalanan perjalanan) { this.perjalanan = perjalanan; }

    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }

    // ============================================
    // INNER CLASS — Summary
    // ============================================

    public static class Summary {

        @SerializedName("durasi_jam")
        private int durasiJam;

        @SerializedName("durasi_menit")
        private int durasiMenit;

        @SerializedName("total_penumpang")
        private int totalPenumpang;

        @SerializedName("penumpang_naik")
        private int penumpangNaik;

        @SerializedName("jarak_km")
        private double jarakKm;

        @SerializedName("tarif_per_orang")
        private double tarifPerOrang;

        @SerializedName("total_pendapatan")
        private double totalPendapatan;

        public int getDurasiJam()         { return durasiJam; }
        public int getDurasiMenit()       { return durasiMenit; }
        public int getTotalPenumpang()    { return totalPenumpang; }
        public int getPenumpangNaik()     { return penumpangNaik; }
        public double getJarakKm()        { return jarakKm; }
        public double getTarifPerOrang()  { return tarifPerOrang; }
        public double getTotalPendapatan(){ return totalPendapatan; }
    }
}