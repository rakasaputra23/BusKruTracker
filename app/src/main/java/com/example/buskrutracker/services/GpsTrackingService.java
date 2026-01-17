package com.example.buskrutracker.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import com.example.buskrutracker.utils.SharedPrefManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * GpsTrackingService - Background GPS Tracking dengan Foreground Service
 * Terintegrasi dengan SharedPrefManager & FirebaseManager
 */
public class GpsTrackingService extends Service {

    private static final String TAG = "GpsTrackingService";
    private static final String CHANNEL_ID = "gps_tracking_channel";
    private static final int NOTIFICATION_ID = 1001;

    // Location update interval
    private static final long UPDATE_INTERVAL = 5000; // 5 detik
    private static final long FASTEST_INTERVAL = 3000; // 3 detik
    private static final float MIN_DISTANCE = 5.0f; // 5 meter (untuk avoid spam update)

    // Intent Actions
    public static final String ACTION_START_TRACKING = "START_TRACKING";
    public static final String ACTION_STOP_TRACKING = "STOP_TRACKING";
    public static final String EXTRA_PERJALANAN_ID = "perjalanan_id";
    public static final String EXTRA_ARMADA_NOMOR = "armada_nomor";
    public static final String EXTRA_RUTE_NAMA = "rute_nama";
    public static final String EXTRA_KRU_NAMA = "kru_nama";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseManager firebaseManager;
    private SharedPrefManager prefManager;

    private int perjalanId;
    private String armadaNomor;
    private String ruteNama;
    private String kruNama;

    private double totalJarak = 0.0;
    private Location lastLocation;
    private long startTime;
    private int updateCount = 0;

    private boolean isTracking = false;

    // ============================================
    // SERVICE LIFECYCLE
    // ============================================

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firebaseManager = new FirebaseManager();
        prefManager = SharedPrefManager.getInstance(this);

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
            }
        }

        return START_STICKY; // Service akan restart jika di-kill system
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed");

        stopLocationUpdates();

        // Clear Firebase data
        if (firebaseManager != null && perjalanId > 0) {
            firebaseManager.clearBusLocation(perjalanId);
        }

        // Update SharedPreferences
        if (prefManager != null) {
            prefManager.setTracking(false);
        }

        isTracking = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Tidak menggunakan binding
    }

    // ============================================
    // START & STOP TRACKING
    // ============================================

    private void handleStartTracking(Intent intent) {
        // Get data dari intent
        perjalanId = intent.getIntExtra(EXTRA_PERJALANAN_ID, 0);
        armadaNomor = intent.getStringExtra(EXTRA_ARMADA_NOMOR);
        ruteNama = intent.getStringExtra(EXTRA_RUTE_NAMA);
        kruNama = intent.getStringExtra(EXTRA_KRU_NAMA);

        if (perjalanId == 0) {
            Log.e(TAG, "Perjalanan ID is 0! Cannot start tracking.");
            stopSelf();
            return;
        }

        Log.d(TAG, "Starting tracking for Perjalanan ID: " + perjalanId);

        // Reset data
        totalJarak = 0.0;
        lastLocation = null;
        startTime = System.currentTimeMillis();
        updateCount = 0;
        isTracking = true;

        // Update SharedPreferences
        prefManager.savePerjalanId(perjalanId);
        prefManager.setTracking(true);

        // Update Firebase dengan info bus
        firebaseManager.updateBusInfo(perjalanId, armadaNomor, ruteNama, kruNama);
        firebaseManager.setBusStatus(perjalanId, "berjalan");

        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification("Memulai tracking...", 0, 0));

        // Setup & start location updates
        setupLocationCallback();
        startLocationUpdates();
    }

    private void handleStopTracking() {
        Log.d(TAG, "Stopping tracking");

        stopLocationUpdates();

        // Update status di Firebase
        if (perjalanId > 0) {
            firebaseManager.setBusStatus(perjalanId, "selesai");
            firebaseManager.clearBusLocation(perjalanId);
        }

        // Update SharedPreferences
        prefManager.setTracking(false);

        isTracking = false;

        // Stop service
        stopForeground(true);
        stopSelf();
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
            Log.d(TAG, "Location updates started successfully");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error: " + e.getMessage());
            stopSelf();
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates stopped");
        }
    }

    // ============================================
    // HANDLE LOCATION UPDATE
    // ============================================

    private void handleLocationUpdate(Location location) {
        if (location == null || !isTracking) return;

        double lat = location.getLatitude();
        double lng = location.getLongitude();
        float speed = location.getSpeed() * 3.6f; // m/s to km/h
        float accuracy = location.getAccuracy();

        // Skip jika akurasi terlalu buruk (> 50 meter)
        if (accuracy > 50) {
            Log.w(TAG, "Poor accuracy: " + accuracy + "m. Skipping update.");
            return;
        }

        // Calculate distance dari lokasi terakhir
        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(location);

            // Hanya tambahkan jarak jika movement > 5 meter (avoid GPS drift)
            if (distance > MIN_DISTANCE) {
                totalJarak += distance / 1000.0; // Convert to kilometers
            }
        }

        lastLocation = location;
        updateCount++;

        // Calculate duration
        long durationMillis = System.currentTimeMillis() - startTime;
        int durationMinutes = (int) (durationMillis / 60000);

        Log.d(TAG, String.format(
                "Update #%d | Loc: %.6f, %.6f | Speed: %.1f km/h | Distance: %.3f km | Accuracy: %.1fm",
                updateCount, lat, lng, speed, totalJarak, accuracy
        ));

        // Send to Firebase
        firebaseManager.updateBusLocation(perjalanId, lat, lng, speed, totalJarak);

        // Update notification
        updateNotification(
                String.format("%.1f km/h | %.2f km", speed, totalJarak),
                speed,
                totalJarak
        );

        // Broadcast location update (untuk TrackingActivity)
        broadcastLocationUpdate(lat, lng, speed, totalJarak, durationMinutes);
    }

    // ============================================
    // BROADCAST LOCATION (ke Activity)
    // ============================================

    private void broadcastLocationUpdate(double lat, double lng, float speed, double jarak, int durasi) {
        Intent intent = new Intent("GPS_LOCATION_UPDATE");
        intent.putExtra("latitude", lat);
        intent.putExtra("longitude", lng);
        intent.putExtra("speed", speed);
        intent.putExtra("distance", jarak);
        intent.putExtra("duration", durasi);
        intent.putExtra("update_count", updateCount);

        sendBroadcast(intent);
    }

    // ============================================
    // NOTIFICATION
    // ============================================

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
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Stop action
        Intent stopIntent = new Intent(this, GpsTrackingService.class);
        stopIntent.setAction(ACTION_STOP_TRACKING);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE
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
    // HELPER METHODS
    // ============================================

    public static Intent createStartIntent(Context context, int perjalanId,
                                           String armadaNomor, String ruteNama, String kruNama) {
        Intent intent = new Intent(context, GpsTrackingService.class);
        intent.setAction(ACTION_START_TRACKING);
        intent.putExtra(EXTRA_PERJALANAN_ID, perjalanId);
        intent.putExtra(EXTRA_ARMADA_NOMOR, armadaNomor);
        intent.putExtra(EXTRA_RUTE_NAMA, ruteNama);
        intent.putExtra(EXTRA_KRU_NAMA, kruNama);
        return intent;
    }

    public static Intent createStopIntent(Context context) {
        Intent intent = new Intent(context, GpsTrackingService.class);
        intent.setAction(ACTION_STOP_TRACKING);
        return intent;
    }

    // Get current tracking data
    public double getTotalJarak() {
        return totalJarak;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    public boolean isTracking() {
        return isTracking;
    }
}