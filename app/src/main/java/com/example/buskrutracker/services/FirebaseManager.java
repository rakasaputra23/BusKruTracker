package com.example.buskrutracker.services;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * FirebaseManager - Mengelola Real-time Database Firebase
 * Digunakan untuk tracking lokasi bus secara real-time
 */
public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    private final DatabaseReference database;

    // ============================================
    // CONSTRUCTOR
    // ============================================
    public FirebaseManager() {
        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().getReference("buses");
    }

    // ============================================
    // UPDATE BUS LOCATION (Real-time)
    // ============================================
    /**
     * Update lokasi bus ke Firebase
     * Data ini akan diakses oleh aplikasi penumpang untuk tracking real-time
     */
    public void updateBusLocation(int perjalanId, double latitude, double longitude,
                                  float speed, double totalJarak) {

        String busKey = "bus_" + perjalanId;

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("perjalanan_id", perjalanId);
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        locationData.put("speed", speed);
        locationData.put("total_jarak", totalJarak);
        locationData.put("timestamp", getCurrentTimestamp());
        locationData.put("last_update", System.currentTimeMillis());

        database.child(busKey).updateChildren(locationData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Location updated successfully for: " + busKey);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update location: " + e.getMessage());
                });
    }

    // ============================================
    // SET BUS STATUS (Mulai/Selesai)
    // ============================================
    /**
     * Update status bus (berjalan/selesai/berhenti)
     */
    public void setBusStatus(int perjalanId, String status) {
        String busKey = "bus_" + perjalanId;

        Map<String, Object> statusData = new HashMap<>();
        statusData.put("status", status);
        statusData.put("timestamp", getCurrentTimestamp());

        database.child(busKey).updateChildren(statusData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Status updated: " + status);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update status: " + e.getMessage());
                });
    }

    // ============================================
    // CLEAR BUS LOCATION (When tracking stops)
    // ============================================
    /**
     * Hapus data bus dari Firebase (saat perjalanan selesai)
     */
    public void clearBusLocation(int perjalanId) {
        String busKey = "bus_" + perjalanId;

        database.child(busKey).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Bus data cleared: " + busKey);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to clear bus data: " + e.getMessage());
                });
    }

    // ============================================
    // SEND EMERGENCY ALERT
    // ============================================
    /**
     * Kirim alert darurat (kecelakaan, masalah teknis, dll)
     */
    public void sendEmergencyAlert(int perjalanId, double latitude, double longitude,
                                   String message) {

        String alertKey = "alert_" + perjalanId + "_" + System.currentTimeMillis();

        Map<String, Object> alertData = new HashMap<>();
        alertData.put("perjalanan_id", perjalanId);
        alertData.put("latitude", latitude);
        alertData.put("longitude", longitude);
        alertData.put("message", message);
        alertData.put("timestamp", getCurrentTimestamp());
        alertData.put("created_at", System.currentTimeMillis());

        database.child("alerts").child(alertKey).setValue(alertData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Emergency alert sent");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send alert: " + e.getMessage());
                });
    }

    // ============================================
    // UPDATE BUS INFO (Armada, Rute, Kru)
    // ============================================
    /**
     * Update informasi bus (armada, rute, kru)
     * Dipanggil saat mulai perjalanan
     */
    public void updateBusInfo(int perjalanId, String armadaNomor, String ruteNama,
                              String kruNama) {

        String busKey = "bus_" + perjalanId;

        Map<String, Object> busInfo = new HashMap<>();
        busInfo.put("armada_nomor", armadaNomor);
        busInfo.put("rute_nama", ruteNama);
        busInfo.put("kru_nama", kruNama);

        database.child(busKey).updateChildren(busInfo)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Bus info updated");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update bus info: " + e.getMessage());
                });
    }

    // ============================================
    // HELPER: Get Current Timestamp
    // ============================================
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // ============================================
    // GET DATABASE REFERENCE (for custom operations)
    // ============================================
    /**
     * Get Firebase Database reference untuk operasi custom
     */
    public DatabaseReference getDatabaseReference() {
        return database;
    }
}