package com.example.buskrutracker.models;

import com.google.gson.annotations.SerializedName;

public class Perjalanan {

    @SerializedName("id")
    private int id;

    @SerializedName("kru_id")
    private int kruId;

    @SerializedName("armada_id")
    private int armadaId;

    @SerializedName("rute_id")
    private int ruteId;

    @SerializedName("waktu_mulai")
    private String waktuMulai;

    @SerializedName("waktu_selesai")
    private String waktuSelesai;

    @SerializedName("total_penumpang")
    private int totalPenumpang;

    @SerializedName("jarak_tempuh")
    private String jarakTempuh;

    @SerializedName("durasi_menit")
    private int durasiMenit;

    @SerializedName("status")
    private String status;

    @SerializedName("kondisi_terakhir")
    private String kondisiTerakhir;

    @SerializedName("catatan")
    private String catatan;

    @SerializedName("kru")
    private Kru kru;

    @SerializedName("armada")
    private Armada armada;

    @SerializedName("rute")
    private Rute rute;

    // Constructor
    public Perjalanan() {}

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getKruId() {
        return kruId;
    }

    public void setKruId(int kruId) {
        this.kruId = kruId;
    }

    public int getArmadaId() {
        return armadaId;
    }

    public void setArmadaId(int armadaId) {
        this.armadaId = armadaId;
    }

    public int getRuteId() {
        return ruteId;
    }

    public void setRuteId(int ruteId) {
        this.ruteId = ruteId;
    }

    public String getWaktuMulai() {
        return waktuMulai;
    }

    public void setWaktuMulai(String waktuMulai) {
        this.waktuMulai = waktuMulai;
    }

    public String getWaktuSelesai() {
        return waktuSelesai;
    }

    public void setWaktuSelesai(String waktuSelesai) {
        this.waktuSelesai = waktuSelesai;
    }

    public int getTotalPenumpang() {
        return totalPenumpang;
    }

    public void setTotalPenumpang(int totalPenumpang) {
        this.totalPenumpang = totalPenumpang;
    }

    public String getJarakTempuh() {
        return jarakTempuh;
    }

    public void setJarakTempuh(String jarakTempuh) {
        this.jarakTempuh = jarakTempuh;
    }

    public int getDurasiMenit() {
        return durasiMenit;
    }

    public void setDurasiMenit(int durasiMenit) {
        this.durasiMenit = durasiMenit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getKondisiTerakhir() {
        return kondisiTerakhir;
    }

    public void setKondisiTerakhir(String kondisiTerakhir) {
        this.kondisiTerakhir = kondisiTerakhir;
    }

    public String getCatatan() {
        return catatan;
    }

    public void setCatatan(String catatan) {
        this.catatan = catatan;
    }

    public Kru getKru() {
        return kru;
    }

    public void setKru(Kru kru) {
        this.kru = kru;
    }

    public Armada getArmada() {
        return armada;
    }

    public void setArmada(Armada armada) {
        this.armada = armada;
    }

    public Rute getRute() {
        return rute;
    }

    public void setRute(Rute rute) {
        this.rute = rute;
    }
}