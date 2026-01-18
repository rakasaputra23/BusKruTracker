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

/**
 * FirebaseManager - Kelola data bus di Firebase Realtime Database
 * Struktur: buses/bus_{id}/plateNumber, class, route, capacity, currentPassengers,
 *           driver, status, kondisi, routePolyline, location, track[], eta, totalDistance
 */
public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    private static final int MAX_TRACK_POINTS = 10;

    // Ganti dengan DATABASE URL Anda dari Firebase Console
    private static final String DATABASE_URL = "https://buskrutracker-default-rtdb.asia-southeast1.firebasedatabase.app/";

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
     * Initialize bus di Firebase dengan struktur lengkap
     */
    public void initializeBus(int perjalanId,
                              String plateNumber,
                              String busClass,
                              String route,
                              int capacity,
                              String driver,
                              String routePolyline) {

        String busKey = "bus_" + perjalanId;
        DatabaseReference busRef = databaseRef.child("buses").child(busKey);

        Map<String, Object> busData = new HashMap<>();
        busData.put("plateNumber", plateNumber);
        busData.put("class", busClass);
        busData.put("route", route);
        busData.put("capacity", capacity);
        busData.put("currentPassengers", 0);
        busData.put("driver", driver);
        busData.put("status", "active");
        busData.put("routePolyline", routePolyline);
        busData.put("kondisi", "lancar"); // ⭐ Kondisi default
        busData.put("kondisiUpdate", getCurrentTimestamp()); // ⭐ Timestamp kondisi

        // Location
        Map<String, Object> location = new HashMap<>();
        location.put("latitude", 0.0);
        location.put("longitude", 0.0);
        location.put("speed", 0.0);
        location.put("lastUpdate", getCurrentTimestamp());
        busData.put("location", location);

        // Track array
        busData.put("track", new ArrayList<>());

        // ETA
        Map<String, Object> eta = new HashMap<>();
        eta.put("remainingDistance", 0.0);
        eta.put("remainingTime", 0);
        eta.put("estimatedArrival", "");
        busData.put("eta", eta);

        // Total distance
        busData.put("totalDistance", 0.0);

        busRef.setValue(busData)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Bus initialized: " + busKey))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to initialize bus: " + e.getMessage()));

        trackHistory.clear();
    }

    // ============================================
    // UPDATE LOCATION WITH TRACK
    // ============================================

    /**
     * Update location + track array (last 10 points)
     */
    public void updateLocationWithTrack(int perjalanId,
                                        double latitude,
                                        double longitude,
                                        float speed,
                                        double totalDistance) {

        String busKey = "bus_" + perjalanId;
        DatabaseReference busRef = databaseRef.child("buses").child(busKey);

        // Update location
        Map<String, Object> location = new HashMap<>();
        location.put("latitude", latitude);
        location.put("longitude", longitude);
        location.put("speed", (double) speed);
        location.put("lastUpdate", getCurrentTimestamp());

        busRef.child("location").setValue(location);

        // Add to track history
        Map<String, Double> trackPoint = new HashMap<>();
        trackPoint.put("lat", latitude);
        trackPoint.put("lng", longitude);

        trackHistory.add(trackPoint);

        // Keep only last 10 points
        if (trackHistory.size() > MAX_TRACK_POINTS) {
            trackHistory.remove(0);
        }

        // Update track array
        busRef.child("track").setValue(new ArrayList<>(trackHistory));

        // Update total distance
        busRef.child("totalDistance").setValue(totalDistance);
    }

    // ============================================
    // UPDATE ETA
    // ============================================

    /**
     * Update ETA information
     */
    public void updateETA(int perjalanId,
                          double remainingDistanceKm,
                          int remainingTimeMinutes,
                          String estimatedArrival) {

        String busKey = "bus_" + perjalanId;
        DatabaseReference etaRef = databaseRef.child("buses").child(busKey).child("eta");

        Map<String, Object> eta = new HashMap<>();
        eta.put("remainingDistance", remainingDistanceKm);
        eta.put("remainingTime", remainingTimeMinutes);
        eta.put("estimatedArrival", estimatedArrival);

        etaRef.setValue(eta);
    }

    // ============================================
    // UPDATE PASSENGERS
    // ============================================

    /**
     * Update current passenger count
     */
    public void updatePassengers(int perjalanId, int currentPassengers) {
        String busKey = "bus_" + perjalanId;
        DatabaseReference busRef = databaseRef.child("buses").child(busKey);

        busRef.child("currentPassengers").setValue(currentPassengers);
    }

    // ============================================
    // UPDATE STATUS
    // ============================================

    /**
     * Update bus status (active, stopped, completed)
     */
    public void updateStatus(int perjalanId, String status) {
        String busKey = "bus_" + perjalanId;
        DatabaseReference busRef = databaseRef.child("buses").child(busKey);

        busRef.child("status").setValue(status);
    }

    // ============================================
    // UPDATE KONDISI BUS ⭐⭐⭐
    // ============================================

    /**
     * Update kondisi bus (lancar, macet, mogok)
     */
    public void updateKondisi(int perjalanId, String kondisi) {
        String busKey = "bus_" + perjalanId;
        DatabaseReference busRef = databaseRef.child("buses").child(busKey);

        Map<String, Object> kondisiData = new HashMap<>();
        kondisiData.put("kondisi", kondisi);
        kondisiData.put("kondisiUpdate", getCurrentTimestamp());

        busRef.updateChildren(kondisiData)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Kondisi updated: " + kondisi))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to update kondisi: " + e.getMessage()));
    }

    // ============================================
    // CLEAR BUS DATA
    // ============================================

    /**
     * Clear/remove bus data from Firebase
     */
    public void clearBusData(int perjalanId) {
        String busKey = "bus_" + perjalanId;
        DatabaseReference busRef = databaseRef.child("buses").child(busKey);

        busRef.removeValue()
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