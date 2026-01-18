package com.example.buskrutracker.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ETACalculator - Calculate ETA using Google Directions API
 */
public class ETACalculator {

    private static final String TAG = "ETACalculator";

    // GANTI DENGAN GOOGLE MAPS API KEY ANDA
    private static final String GOOGLE_API_KEY = "AIzaSyDDDvRiEfPqb4fUMJQ2KSxAlwm5UJa4kxs";

    private static final String DIRECTIONS_API_URL =
            "https://maps.googleapis.com/maps/api/directions/json";

    private final ExecutorService executorService;

    public interface ETACallback {
        void onETACalculated(double remainingDistanceKm, int remainingTimeMinutes,
                             String estimatedArrival);
        void onError(String error);
    }

    // ============================================
    // CONSTRUCTOR
    // ============================================
    public ETACalculator() {
        executorService = Executors.newSingleThreadExecutor();
    }

    // ============================================
    // CALCULATE ETA
    // ============================================
    /**
     * Calculate ETA menggunakan Google Directions API
     *
     * @param currentLat Current latitude
     * @param currentLng Current longitude
     * @param destLat Destination latitude
     * @param destLng Destination longitude
     * @param callback Callback untuk hasil
     */
    public void calculateETA(double currentLat, double currentLng,
                             double destLat, double destLng,
                             ETACallback callback) {

        executorService.execute(() -> {
            try {
                // Build URL
                String urlString = String.format(Locale.US,
                        "%s?origin=%f,%f&destination=%f,%f&key=%s&mode=driving",
                        DIRECTIONS_API_URL,
                        currentLat, currentLng,
                        destLat, destLng,
                        GOOGLE_API_KEY
                );

                Log.d(TAG, "Fetching ETA from Google Directions API...");

                // Make HTTP request
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse response
                    parseETAResponse(response.toString(), callback);

                } else {
                    callback.onError("HTTP Error: " + responseCode);
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error calculating ETA: " + e.getMessage());
                callback.onError(e.getMessage());
            }
        });
    }

    // ============================================
    // PARSE RESPONSE
    // ============================================
    private void parseETAResponse(String jsonResponse, ETACallback callback) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            String status = json.getString("status");

            if (!"OK".equals(status)) {
                callback.onError("Directions API Error: " + status);
                return;
            }

            JSONArray routes = json.getJSONArray("routes");
            if (routes.length() == 0) {
                callback.onError("No routes found");
                return;
            }

            JSONObject route = routes.getJSONObject(0);
            JSONArray legs = route.getJSONArray("legs");
            JSONObject leg = legs.getJSONObject(0);

            // Get distance (in meters)
            JSONObject distance = leg.getJSONObject("distance");
            double distanceMeters = distance.getDouble("value");
            double distanceKm = distanceMeters / 1000.0;

            // Get duration (in seconds)
            JSONObject duration = leg.getJSONObject("duration");
            int durationSeconds = duration.getInt("value");
            int durationMinutes = durationSeconds / 60;

            // Calculate estimated arrival time
            long arrivalTimeMillis = System.currentTimeMillis() + (durationSeconds * 1000L);
            String estimatedArrival = formatTimestamp(arrivalTimeMillis);

            Log.d(TAG, String.format(
                    "ETA Calculated: %.2f km, %d min, Arrival: %s",
                    distanceKm, durationMinutes, estimatedArrival
            ));

            callback.onETACalculated(distanceKm, durationMinutes, estimatedArrival);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing ETA response: " + e.getMessage());
            callback.onError("Parse error: " + e.getMessage());
        }
    }

    // ============================================
    // FALLBACK: CALCULATE ETA MANUALLY (jika API gagal)
    // ============================================
    /**
     * Hitung ETA manual menggunakan Haversine formula
     * Digunakan sebagai fallback jika Google API gagal
     */
    public void calculateETAManual(double currentLat, double currentLng,
                                   double destLat, double destLng,
                                   float averageSpeedKmh,
                                   ETACallback callback) {

        try {
            // Calculate distance using Haversine formula
            double distance = calculateDistance(currentLat, currentLng, destLat, destLng);

            // Calculate time (assuming constant speed)
            if (averageSpeedKmh <= 0) {
                averageSpeedKmh = 60.0f; // Default 60 km/h
            }

            int durationMinutes = (int) ((distance / averageSpeedKmh) * 60);

            // Calculate arrival time
            long arrivalTimeMillis = System.currentTimeMillis() + (durationMinutes * 60000L);
            String estimatedArrival = formatTimestamp(arrivalTimeMillis);

            Log.d(TAG, String.format(
                    "Manual ETA: %.2f km, %d min (Speed: %.1f km/h)",
                    distance, durationMinutes, averageSpeedKmh
            ));

            callback.onETACalculated(distance, durationMinutes, estimatedArrival);

        } catch (Exception e) {
            callback.onError("Manual calculation error: " + e.getMessage());
        }
    }

    // ============================================
    // HAVERSINE DISTANCE CALCULATION
    // ============================================
    /**
     * Calculate distance between two coordinates using Haversine formula
     * Returns distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Earth radius in kilometers

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    // ============================================
    // HELPER METHODS
    // ============================================
    private String formatTimestamp(long timestampMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        return sdf.format(new Date(timestampMillis));
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}