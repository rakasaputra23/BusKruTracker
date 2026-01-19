package com.example.buskrutracker.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.buskrutracker.R;
import com.example.buskrutracker.activities.TrackingActivity;
import com.example.buskrutracker.utils.ETACalculator;
import com.example.buskrutracker.utils.PolylineUtils;
import com.example.buskrutracker.utils.SharedPrefManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GpsTrackingService - Enhanced dengan struktur Firebase yang benar
 * ⭐ UPDATED: Tambah namaBus support
 */
public class GpsTrackingService extends Service {

    private static final String TAG = "GpsTrackingService";
    private static final String CHANNEL_ID = "gps_tracking_channel";
    private static final int NOTIFICATION_ID = 1001;

    // Location update interval
    private static final long UPDATE_INTERVAL = 5000; // 5 detik
    private static final long FASTEST_INTERVAL = 3000; // 3 detik
    private static final float MIN_DISTANCE = 5.0f; // 5 meter

    // ETA update interval (setiap 30 detik)
    private static final long ETA_UPDATE_INTERVAL = 30000;

    // Intent Actions
    public static final String ACTION_START_TRACKING = "START_TRACKING";
    public static final String ACTION_STOP_TRACKING = "STOP_TRACKING";
    public static final String ACTION_UPDATE_PASSENGERS = "UPDATE_PASSENGERS";
    public static final String ACTION_UPDATE_KONDISI = "UPDATE_KONDISI";

    // Intent Extras
    public static final String EXTRA_PERJALANAN_ID = "perjalanan_id";
    public static final String EXTRA_ARMADA_ID = "armada_id";
    public static final String EXTRA_RUTE_ID = "rute_id";
    public static final String EXTRA_KRU_ID = "kru_id";

    // Data tracking
    private int perjalanId;
    private String namaBus;        // ⭐ FIELD BARU
    private String armadaNomor;
    private String kelas;
    private int kapasitas;
    private String ruteNama;
    private String polyline;
    private String kruNama;
    private double destLat;
    private double destLng;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseManager firebaseManager;
    private ETACalculator etaCalculator;
    private SharedPrefManager prefManager;

    // Tracking data
    private double totalJarak = 0.0;
    private Location lastLocation;
    private long startTime;
    private long lastETAUpdate = 0;
    private int updateCount = 0;
    private boolean isTracking = false;

    // Full track history untuk MySQL
    private List<Map<String, Double>> fullTrackHistory;

    // ============================================
    // SERVICE LIFECYCLE
    // ============================================

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firebaseManager = new FirebaseManager();
        etaCalculator = new ETACalculator();
        prefManager = SharedPrefManager.getInstance(this);
        fullTrackHistory = new ArrayList<>();

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if (ACTION_START_TRACKING.equals(action)) {
                handleStartTracking(intent);
            } else if (ACTION_STOP_TRACKING.equals(action)) {
                handleStopTracking();
            } else if (ACTION_UPDATE_PASSENGERS.equals(action)) {
                handleUpdatePassengers(intent);
            } else if (ACTION_UPDATE_KONDISI.equals(action)) {
                handleUpdateKondisi(intent);
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed");

        stopLocationUpdates();

        if (etaCalculator != null) {
            etaCalculator.shutdown();
        }

        if (firebaseManager != null && perjalanId > 0) {
            firebaseManager.clearBusData(perjalanId);
        }

        if (prefManager != null) {
            prefManager.setTracking(false);
        }

        isTracking = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ============================================
    // START TRACKING
    // ============================================

    private void handleStartTracking(Intent intent) {
        perjalanId = intent.getIntExtra(EXTRA_PERJALANAN_ID, 0);
        namaBus = intent.getStringExtra("nama_bus");           // ⭐ AMBIL NAMA BUS
        armadaNomor = intent.getStringExtra("armada_nomor");
        kelas = intent.getStringExtra("kelas");
        kapasitas = intent.getIntExtra("kapasitas", 40);
        ruteNama = intent.getStringExtra("rute_nama");
        polyline = intent.getStringExtra("polyline");
        kruNama = intent.getStringExtra("kru_nama");

        if (perjalanId == 0 || polyline == null || polyline.isEmpty()) {
            Log.e(TAG, "Invalid data! Cannot start tracking.");
            stopSelf();
            return;
        }

        // Extract destination dari polyline
        LatLng destination = PolylineUtils.getDestination(polyline);
        if (destination != null) {
            destLat = destination.latitude;
            destLng = destination.longitude;
        }

        // Reset data
        totalJarak = 0.0;
        lastLocation = null;
        startTime = System.currentTimeMillis();
        lastETAUpdate = 0;
        updateCount = 0;
        isTracking = true;
        fullTrackHistory.clear();

        // Update SharedPreferences
        prefManager.savePerjalanId(perjalanId);
        prefManager.setTracking(true);

        // ⭐ Initialize bus di Firebase dengan namaBus
        firebaseManager.initializeBus(
                perjalanId,
                namaBus,          // ⭐ PARAMETER BARU
                armadaNomor,
                kelas,
                ruteNama,
                kapasitas,
                kruNama,
                polyline
        );

        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification("Memulai tracking...", 0, 0));

        // Setup & start location updates
        setupLocationCallback();
        startLocationUpdates();

        // ⭐ Log untuk debugging
        Log.d(TAG, "Tracking started for: " + namaBus + " (" + armadaNomor + ")");
    }

    // ============================================
    // STOP TRACKING
    // ============================================

    private void handleStopTracking() {
        stopLocationUpdates();

        if (perjalanId > 0) {
            firebaseManager.updateStatus(perjalanId, "completed");
            firebaseManager.clearBusData(perjalanId);
        }

        prefManager.setTracking(false);
        isTracking = false;

        stopForeground(true);
        stopSelf();
    }

    // ============================================
    // UPDATE PASSENGERS
    // ============================================

    private void handleUpdatePassengers(Intent intent) {
        int currentPassengers = intent.getIntExtra("current_passengers", 0);
        firebaseManager.updatePassengers(perjalanId, currentPassengers);
    }

    // ============================================
    // UPDATE KONDISI
    // ============================================

    private void handleUpdateKondisi(Intent intent) {
        String kondisi = intent.getStringExtra("kondisi");

        if (kondisi != null && !kondisi.isEmpty()) {
            firebaseManager.updateKondisi(perjalanId, kondisi);
            Log.d(TAG, "Kondisi updated to: " + kondisi);
        }
    }

    // ============================================
    // LOCATION UPDATES
    // ============================================

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || !isTracking) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        handleLocationUpdate(location);
                    }
                }
            }
        };
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                UPDATE_INTERVAL
        )
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
                .setMinUpdateDistanceMeters(MIN_DISTANCE)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error: " + e.getMessage());
            stopSelf();
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // ============================================
    // HANDLE LOCATION UPDATE
    // ============================================

    private void handleLocationUpdate(Location location) {
        if (location == null || !isTracking) return;

        double lat = location.getLatitude();
        double lng = location.getLongitude();
        float speed = location.getSpeed() * 3.6f;
        float accuracy = location.getAccuracy();

        if (accuracy > 50) {
            return;
        }

        // Calculate distance
        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(location);
            if (distance > MIN_DISTANCE) {
                totalJarak += distance / 1000.0;
            }
        }

        lastLocation = location;
        updateCount++;

        // Add to full track history
        Map<String, Double> trackPoint = new HashMap<>();
        trackPoint.put("lat", lat);
        trackPoint.put("lng", lng);
        fullTrackHistory.add(trackPoint);

        // Update Firebase
        firebaseManager.updateLocationWithTrack(perjalanId, lat, lng, speed, totalJarak);

        // Update ETA setiap 30 detik
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastETAUpdate > ETA_UPDATE_INTERVAL) {
            updateETA(lat, lng, speed);
            lastETAUpdate = currentTime;
        }

        // Update notification
        updateNotification(
                String.format("%.1f km/h | %.2f km", speed, totalJarak),
                speed,
                totalJarak
        );

        // Broadcast location update
        broadcastLocationUpdate(lat, lng, speed, totalJarak);
    }

    // ============================================
    // ETA CALCULATION
    // ============================================

    private void updateETA(double currentLat, double currentLng, float currentSpeed) {
        if (destLat == 0 || destLng == 0) {
            return;
        }

        etaCalculator.calculateETA(
                currentLat, currentLng,
                destLat, destLng,
                new ETACalculator.ETACallback() {
                    @Override
                    public void onETACalculated(double remainingDistanceKm,
                                                int remainingTimeMinutes,
                                                String estimatedArrival) {
                        firebaseManager.updateETA(
                                perjalanId,
                                remainingDistanceKm,
                                remainingTimeMinutes,
                                estimatedArrival
                        );
                    }

                    @Override
                    public void onError(String error) {
                        etaCalculator.calculateETAManual(
                                currentLat, currentLng,
                                destLat, destLng,
                                currentSpeed > 0 ? currentSpeed : 60.0f,
                                new ETACalculator.ETACallback() {
                                    @Override
                                    public void onETACalculated(double remainingDistanceKm,
                                                                int remainingTimeMinutes,
                                                                String estimatedArrival) {
                                        firebaseManager.updateETA(
                                                perjalanId,
                                                remainingDistanceKm,
                                                remainingTimeMinutes,
                                                estimatedArrival
                                        );
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "Manual ETA calculation failed: " + error);
                                    }
                                }
                        );
                    }
                }
        );
    }

    // ============================================
    // BROADCAST & NOTIFICATION
    // ============================================

    private void broadcastLocationUpdate(double lat, double lng, float speed, double jarak) {
        Intent intent = new Intent("GPS_LOCATION_UPDATE");
        intent.putExtra("latitude", lat);
        intent.putExtra("longitude", lng);
        intent.putExtra("speed", speed);
        intent.putExtra("distance", jarak);
        intent.putExtra("update_count", updateCount);
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "GPS Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background GPS tracking untuk bus");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String contentText, float speed, double jarak) {
        Intent notificationIntent = new Intent(this, TrackingActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent stopIntent = new Intent(this, GpsTrackingService.class);
        stopIntent.setAction(ACTION_STOP_TRACKING);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🚍 Bus Tracker Active")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Stop",
                        stopPendingIntent
                )
                .build();
    }

    private void updateNotification(String content, float speed, double jarak) {
        Notification notification = createNotification(content, speed, jarak);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    // ============================================
    // HELPER METHODS - CREATE INTENTS
    // ============================================

    /**
     * ⭐ UPDATED: Tambah parameter namaBus
     */
    public static Intent createStartIntent(Context context,
                                           int perjalanId,
                                           String namaBus,      // ⭐ PARAMETER BARU
                                           String armadaNomor,
                                           String kelas,
                                           int kapasitas,
                                           String ruteNama,
                                           String polyline,
                                           String kruNama) {
        Intent intent = new Intent(context, GpsTrackingService.class);
        intent.setAction(ACTION_START_TRACKING);
        intent.putExtra(EXTRA_PERJALANAN_ID, perjalanId);
        intent.putExtra("nama_bus", namaBus);           // ⭐ EXTRA BARU
        intent.putExtra("armada_nomor", armadaNomor);
        intent.putExtra("kelas", kelas);
        intent.putExtra("kapasitas", kapasitas);
        intent.putExtra("rute_nama", ruteNama);
        intent.putExtra("polyline", polyline);
        intent.putExtra("kru_nama", kruNama);
        return intent;
    }

    public static Intent createStopIntent(Context context) {
        Intent intent = new Intent(context, GpsTrackingService.class);
        intent.setAction(ACTION_STOP_TRACKING);
        return intent;
    }

    public static Intent createPassengerUpdateIntent(Context context,
                                                     int perjalanId,
                                                     int currentPassengers) {
        Intent intent = new Intent(context, GpsTrackingService.class);
        intent.setAction(ACTION_UPDATE_PASSENGERS);
        intent.putExtra(EXTRA_PERJALANAN_ID, perjalanId);
        intent.putExtra("current_passengers", currentPassengers);
        return intent;
    }

    /**
     * Create intent untuk update kondisi bus
     */
    public static Intent createKondisiUpdateIntent(Context context,
                                                   int perjalanId,
                                                   String kondisi) {
        Intent intent = new Intent(context, GpsTrackingService.class);
        intent.setAction(ACTION_UPDATE_KONDISI);
        intent.putExtra(EXTRA_PERJALANAN_ID, perjalanId);
        intent.putExtra("kondisi", kondisi);
        return intent;
    }
}