package com.example.buskrutracker.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * PermissionHelper - Mengelola izin GPS & Location
 * Handle: Fine Location, Coarse Location, Background Location, Notification
 */
public class PermissionHelper {

    // Request codes
    public static final int REQUEST_LOCATION_PERMISSION = 100;
    public static final int REQUEST_BACKGROUND_LOCATION = 101;
    public static final int REQUEST_NOTIFICATION_PERMISSION = 102;

    private final Activity activity;
    private final Context context;

    // ============================================
    // CONSTRUCTOR
    // ============================================
    public PermissionHelper(Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }

    // ============================================
    // CHECK PERMISSIONS
    // ============================================

    /**
     * Cek apakah GPS Location sudah diizinkan
     */
    public boolean isLocationPermissionGranted() {
        boolean fineLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        return fineLocation && coarseLocation;
    }

    /**
     * Cek apakah Background Location sudah diizinkan (Android 10+)
     */
    public boolean isBackgroundLocationGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Tidak perlu di Android < 10
    }

    /**
     * Cek apakah Notification permission granted (Android 13+)
     */
    public boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Tidak perlu di Android < 13
    }

    /**
     * Cek apakah GPS sudah aktif
     */
    public boolean isGpsEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Cek semua permission (Location + Background + Notification)
     */
    public boolean hasAllPermissions() {
        return isLocationPermissionGranted()
                && isBackgroundLocationGranted()
                && isNotificationPermissionGranted();
    }

    // ============================================
    // REQUEST PERMISSIONS
    // ============================================

    /**
     * Minta izin Location (Fine + Coarse)
     */
    public void requestLocationPermission() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        ActivityCompat.requestPermissions(
                activity,
                permissions,
                REQUEST_LOCATION_PERMISSION
        );
    }

    /**
     * Minta izin Background Location (Android 10+)
     * ⚠️ Harus setelah Fine Location granted!
     */
    public void requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!isBackgroundLocationGranted()) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        REQUEST_BACKGROUND_LOCATION
                );
            }
        }
    }

    /**
     * Minta izin Notification (Android 13+)
     */
    public void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isNotificationPermissionGranted()) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
            }
        }
    }

    /**
     * Minta semua permission sekaligus
     * Step 1: Location → Step 2: Background → Step 3: Notification
     */
    public void requestAllPermissions() {
        // Step 1: Minta Location permission dulu
        if (!isLocationPermissionGranted()) {
            requestLocationPermission();
        }
        // Step 2: Setelah Location granted, minta Background
        else if (!isBackgroundLocationGranted()) {
            requestBackgroundLocationPermission();
        }
        // Step 3: Terakhir minta Notification
        else if (!isNotificationPermissionGranted()) {
            requestNotificationPermission();
        }
    }

    // ============================================
    // HANDLE PERMISSION RESULT
    // ============================================

    /**
     * Handle hasil permission request
     * Panggil ini di onRequestPermissionsResult()
     */
    public boolean handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // Location granted, sekarang minta Background
                requestBackgroundLocationPermission();
                return true;
            }
            return false;
        }
        else if (requestCode == REQUEST_BACKGROUND_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Background granted, sekarang minta Notification
                requestNotificationPermission();
                return true;
            }
            return false;
        }
        else if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }

        return false;
    }

    // ============================================
    // CHECK RATIONALE
    // ============================================

    /**
     * Cek apakah perlu tampilkan penjelasan permission
     */
    public boolean shouldShowLocationRationale() {
        return ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
        );
    }

    public boolean shouldShowBackgroundRationale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            );
        }
        return false;
    }

    // ============================================
    // NAVIGATION TO SETTINGS
    // ============================================

    /**
     * Buka halaman Settings GPS
     */
    public void openGpsSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivity(intent);
    }

    /**
     * Buka halaman App Settings (untuk enable permission manual)
     */
    public void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }

    // ============================================
    // HELPER MESSAGES
    // ============================================

    /**
     * Get permission rationale message
     */
    public String getLocationRationaleMessage() {
        return "Aplikasi membutuhkan izin lokasi untuk melacak posisi bus secara real-time. " +
                "Izin ini diperlukan agar penumpang bisa melihat lokasi bus Anda.";
    }

    public String getBackgroundRationaleMessage() {
        return "Izin lokasi latar belakang diperlukan agar tracking tetap berjalan " +
                "meskipun aplikasi tidak sedang dibuka. Pilih 'Izinkan sepanjang waktu'.";
    }

    public String getNotificationRationaleMessage() {
        return "Izin notifikasi diperlukan untuk menampilkan status tracking " +
                "dan informasi perjalanan Anda.";
    }

    public String getGpsDisabledMessage() {
        return "GPS tidak aktif. Silakan aktifkan GPS untuk melanjutkan tracking.";
    }
}