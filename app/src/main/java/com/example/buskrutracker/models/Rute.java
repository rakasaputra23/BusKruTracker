package com.example.buskrutracker.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Rute {

    @SerializedName("id")
    private int id;

    @SerializedName("nama_rute")
    private String namaRute;

    @SerializedName("kota_asal")
    private String kotaAsal;

    @SerializedName("kota_tujuan")
    private String kotaTujuan;

    @SerializedName("polyline")
    private String polyline;

    @SerializedName("track_coordinates")
    private List<TrackCoordinate> trackCoordinates;

    @SerializedName("jarak")
    private String jarak;

    @SerializedName("estimasi_waktu")
    private int estimasiWaktu;

    // Inner class untuk track coordinates
    public static class TrackCoordinate {
        @SerializedName("lat")
        private double lat;

        @SerializedName("lng")
        private double lng;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }
    }

    // Constructor
    public Rute() {}

    public Rute(int id, String namaRute, String kotaAsal, String kotaTujuan,
                String polyline, String jarak, int estimasiWaktu) {
        this.id = id;
        this.namaRute = namaRute;
        this.kotaAsal = kotaAsal;
        this.kotaTujuan = kotaTujuan;
        this.polyline = polyline;
        this.jarak = jarak;
        this.estimasiWaktu = estimasiWaktu;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNamaRute() {
        return namaRute;
    }

    public void setNamaRute(String namaRute) {
        this.namaRute = namaRute;
    }

    public String getKotaAsal() {
        return kotaAsal;
    }

    public void setKotaAsal(String kotaAsal) {
        this.kotaAsal = kotaAsal;
    }

    public String getKotaTujuan() {
        return kotaTujuan;
    }

    public void setKotaTujuan(String kotaTujuan) {
        this.kotaTujuan = kotaTujuan;
    }

    public String getPolyline() {
        return polyline;
    }

    public void setPolyline(String polyline) {
        this.polyline = polyline;
    }

    public List<TrackCoordinate> getTrackCoordinates() {
        return trackCoordinates;
    }

    public void setTrackCoordinates(List<TrackCoordinate> trackCoordinates) {
        this.trackCoordinates = trackCoordinates;
    }

    public String getJarak() {
        return jarak;
    }

    public void setJarak(String jarak) {
        this.jarak = jarak;
    }

    public int getEstimasiWaktu() {
        return estimasiWaktu;
    }

    public void setEstimasiWaktu(int estimasiWaktu) {
        this.estimasiWaktu = estimasiWaktu;
    }

    // Display method untuk Spinner
    @Override
    public String toString() {
        return namaRute;
    }
}