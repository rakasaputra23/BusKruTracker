package com.example.buskrutracker.services;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    private static final int MAX_TRACK_POINTS = 10;

    private static final String DATABASE_URL =
            "https://buskrutracker-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private DatabaseReference databaseRef;
    private SimpleDateFormat dateFormat;
    private List<Map<String, Double>> trackHistory;

    public FirebaseManager() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance(DATABASE_URL);
            databaseRef = database.getReference();
            trackHistory = new ArrayList<>();

            dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            Log.d(TAG, "FirebaseManager initialized");
        } catch (Exception e) {
            Log.e(TAG, "FirebaseManager initialization error: " + e.getMessage());
        }
    }

    // ============================================
    // INITIALIZE BUS
    // ============================================

    /**
     * ⭐ UPDATED: Tambah tarif & totalPassengersBoarded
     */
    public void initializeBus(int perjalanId,
                              String namaBus,
                              String plateNumber,
                              String busClass,
                              String route,
                              int capacity,
                              String driver,
                              String routePolyline,
                              double tarif) {           // ⭐ PARAMETER BARU

        String busKey = "bus_" + perjalanId;
        DatabaseReference busRef = databaseRef.child("buses").child(busKey);

        Map<String, Object> busData = new HashMap<>();
        busData.put("namaBus",        namaBus);
        busData.put("plateNumber",    plateNumber);
        busData.put("class",          busClass);
        busData.put("route",          route);
        busData.put("capacity",       capacity);
        busData.put("currentPassengers",       0);
        busData.put("totalPassengersBoarded",  0);   // ⭐ FIELD BARU
        busData.put("tarif",          tarif);         // ⭐ FIELD BARU
        busData.put("driver",         driver);
        busData.put("status",         "active");
        busData.put("routePolyline",  routePolyline);
        busData.put("kondisi",        "lancar");
        busData.put("kondisiUpdate",  getCurrentTimestamp());

        // Location
        Map<String, Object> location = new HashMap<>();
        location.put("latitude",   0.0);
        location.put("longitude",  0.0);
        location.put("speed",      0.0);
        location.put("lastUpdate", getCurrentTimestamp());
        busData.put("location", location);

        // Track array
        busData.put("track", new ArrayList<>());

        // ETA
        Map<String, Object> eta = new HashMap<>();
        eta.put("remainingDistance",  0.0);
        eta.put("remainingTime",      0);
        eta.put("estimatedArrival",   "");
        busData.put("eta", eta);

        // Total distance
        busData.put("totalDistance", 0.0);

        busRef.setValue(busData)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Bus initialized: " + busKey
                                + " | " + namaBus
                                + " tarif=" + tarif))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to initialize bus: " + e.getMessage()));

        trackHistory.clear();
    }

    // ============================================
    // UPDATE LOCATION WITH TRACK
    // ============================================

    public void updateLocationWithTrack(int perjalanId,
                                        double latitude,
                                        double longitude,
                                        float speed,
                                        double totalDistance) {

        String busKey = "bus_" + perjalanId;
        DatabaseReference busRef = databaseRef.child("buses").child(busKey);

        Map<String, Object> location = new HashMap<>();
        location.put("latitude",   latitude);
        location.put("longitude",  longitude);
        location.put("speed",      (double) speed);
        location.put("lastUpdate", getCurrentTimestamp());
        busRef.child("location").setValue(location);

        Map<String, Double> trackPoint = new HashMap<>();
        trackPoint.put("lat", latitude);
        trackPoint.put("lng", longitude);
        trackHistory.add(trackPoint);

        if (trackHistory.size() > MAX_TRACK_POINTS) {
            trackHistory.remove(0);
        }

        busRef.child("track").setValue(new ArrayList<>(trackHistory));
        busRef.child("totalDistance").setValue(totalDistance);
    }

    // ============================================
    // UPDATE ETA
    // ============================================

    public void updateETA(int perjalanId,
                          double remainingDistanceKm,
                          int remainingTimeMinutes,
                          String estimatedArrival) {

        String busKey = "bus_" + perjalanId;
        DatabaseReference etaRef = databaseRef.child("buses").child(busKey).child("eta");

        Map<String, Object> eta = new HashMap<>();
        eta.put("remainingDistance", remainingDistanceKm);
        eta.put("remainingTime",     remainingTimeMinutes);
        eta.put("estimatedArrival",  estimatedArrival);

        etaRef.setValue(eta);
    }

    // ============================================
    // UPDATE PASSENGERS
    // ============================================

    /**
     * Update penumpang yang sedang ada di dalam bus (bisa naik/turun).
     */
    public void updatePassengers(int perjalanId, int currentPassengers) {
        String busKey = "bus_" + perjalanId;
        databaseRef.child("buses").child(busKey)
                .child("currentPassengers")
                .setValue(currentPassengers);
    }

    /**
     * ⭐ BARU — Update akumulasi penumpang yang NAIK (tidak pernah berkurang).
     * Dipanggil setiap kali ada penumpang boarding baru.
     */
    public void updateBoardedPassengers(int perjalanId, int totalBoarded) {
        String busKey = "bus_" + perjalanId;
        databaseRef.child("buses").child(busKey)
                .child("totalPassengersBoarded")
                .setValue(totalBoarded)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "totalPassengersBoarded updated: " + totalBoarded))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to update boarded: " + e.getMessage()));
    }

    // ============================================
    // UPDATE STATUS
    // ============================================

    public void updateStatus(int perjalanId, String status) {
        String busKey = "bus_" + perjalanId;
        databaseRef.child("buses").child(busKey)
                .child("status")
                .setValue(status);
    }

    // ============================================
    // UPDATE KONDISI BUS
    // ============================================

    public void updateKondisi(int perjalanId, String kondisi) {
        String busKey = "bus_" + perjalanId;
        DatabaseReference busRef = databaseRef.child("buses").child(busKey);

        Map<String, Object> kondisiData = new HashMap<>();
        kondisiData.put("kondisi",       kondisi);
        kondisiData.put("kondisiUpdate", getCurrentTimestamp());

        busRef.updateChildren(kondisiData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Kondisi updated: " + kondisi))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to update kondisi: " + e.getMessage()));
    }

    // ============================================
    // CLEAR BUS DATA
    // ============================================

    public void clearBusData(int perjalanId) {
        String busKey = "bus_" + perjalanId;
        databaseRef.child("buses").child(busKey)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Bus data cleared: " + busKey);
                    trackHistory.clear();
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to clear bus data: " + e.getMessage()));
    }

    // ============================================
    // HELPER
    // ============================================

    private String getCurrentTimestamp() {
        return dateFormat.format(new Date());
    }
}