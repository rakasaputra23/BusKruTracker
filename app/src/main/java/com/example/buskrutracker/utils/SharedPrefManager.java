package com.example.buskrutracker.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.buskrutracker.models.Kru;
import com.google.gson.Gson;

/**
 * SharedPrefManager - Mengelola data lokal aplikasi
 * Menyimpan: Token, User Data, Perjalanan Aktif
 */
public class SharedPrefManager {

    private static final String PREF_NAME = "BusKruTrackerPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER = "user";
    private static final String KEY_PERJALANAN_ID = "perjalanan_id";
    private static final String KEY_IS_TRACKING = "is_tracking";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private static SharedPrefManager instance;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private final Gson gson;

    // ============================================
    // SINGLETON PATTERN
    // ============================================
    private SharedPrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        gson = new Gson();
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context.getApplicationContext());
        }
        return instance;
    }

    // ============================================
    // LOGIN & LOGOUT
    // ============================================

    /**
     * Simpan data login (token + user)
     */
    public void saveLoginData(String token, Kru kru) {
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_USER, gson.toJson(kru));
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Hapus semua data (logout)
     */
    public void logout() {
        editor.clear();
        editor.apply();
    }

    /**
     * Cek apakah user sudah login
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // ============================================
    // TOKEN MANAGEMENT
    // ============================================

    /**
     * Get token (dengan format Bearer)
     */
    public String getToken() {
        String token = sharedPreferences.getString(KEY_TOKEN, null);
        return token != null ? "Bearer " + token : null;
    }

    /**
     * Get raw token (tanpa Bearer)
     */
    public String getRawToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    /**
     * Simpan token
     */
    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    // ============================================
    // USER DATA
    // ============================================

    /**
     * Get data user (Kru)
     */
    public Kru getUser() {
        String userJson = sharedPreferences.getString(KEY_USER, null);
        if (userJson != null) {
            return gson.fromJson(userJson, Kru.class);
        }
        return null;
    }

    /**
     * Update data user
     */
    public void updateUser(Kru kru) {
        editor.putString(KEY_USER, gson.toJson(kru));
        editor.apply();
    }

    /**
     * Get user ID
     */
    public int getUserId() {
        Kru kru = getUser();
        return kru != null ? kru.getId() : 0;
    }

    /**
     * Get username
     */
    public String getUsername() {
        Kru kru = getUser();
        return kru != null ? kru.getUsername() : "";
    }

    /**
     * Get nama driver
     */
    public String getDriverName() {
        Kru kru = getUser();
        return kru != null ? kru.getDriver() : "";
    }

    // ============================================
    // PERJALANAN MANAGEMENT
    // ============================================

    /**
     * Simpan ID perjalanan aktif
     */
    public void savePerjalanId(int perjalanId) {
        editor.putInt(KEY_PERJALANAN_ID, perjalanId);
        editor.apply();
    }

    /**
     * Get ID perjalanan aktif
     */
    public int getPerjalanId() {
        return sharedPreferences.getInt(KEY_PERJALANAN_ID, 0);
    }

    /**
     * Hapus ID perjalanan (saat selesai)
     */
    public void clearPerjalanId() {
        editor.remove(KEY_PERJALANAN_ID);
        editor.apply();
    }

    // ============================================
    // TRACKING STATUS
    // ============================================

    /**
     * Set status tracking (aktif/tidak)
     */
    public void setTracking(boolean isTracking) {
        editor.putBoolean(KEY_IS_TRACKING, isTracking);
        editor.apply();
    }

    /**
     * Cek apakah sedang tracking
     */
    public boolean isTracking() {
        return sharedPreferences.getBoolean(KEY_IS_TRACKING, false);
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Cek apakah ada perjalanan aktif
     */
    public boolean hasActivePerjalanan() {
        return getPerjalanId() > 0 && isTracking();
    }

    /**
     * Save custom data (untuk keperluan lain)
     */
    public void saveString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public void saveInt(String key, int value) {
        editor.putInt(key, value);
        editor.apply();
    }

    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public void saveBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }
}