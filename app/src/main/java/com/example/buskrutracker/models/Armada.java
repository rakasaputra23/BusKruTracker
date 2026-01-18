package com.example.buskrutracker.models;

import com.google.gson.annotations.SerializedName;

public class Armada {

    @SerializedName("id")
    private int id;

    @SerializedName("nama_bus")
    private String namaBus;

    @SerializedName("plat_nomor")
    private String platNomor;

    @SerializedName("kelas")
    private String kelas;

    @SerializedName("kapasitas")
    private int kapasitas;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Constructor
    public Armada() {}

    public Armada(int id, String namaBus, String platNomor, String kelas, int kapasitas, String status) {
        this.id = id;
        this.namaBus = namaBus;
        this.platNomor = platNomor;
        this.kelas = kelas;
        this.kapasitas = kapasitas;
        this.status = status;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNamaBus() {
        return namaBus;
    }

    public void setNamaBus(String namaBus) {
        this.namaBus = namaBus;
    }

    public String getPlatNomor() {
        return platNomor;
    }

    public void setPlatNomor(String platNomor) {
        this.platNomor = platNomor;
    }

    public String getKelas() {
        return kelas;
    }

    public void setKelas(String kelas) {
        this.kelas = kelas;
    }

    public int getKapasitas() {
        return kapasitas;
    }

    public void setKapasitas(int kapasitas) {
        this.kapasitas = kapasitas;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Display method untuk Spinner
    @Override
    public String toString() {
        return namaBus + " (" + platNomor + ")";
    }
}