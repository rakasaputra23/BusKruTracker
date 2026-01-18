package com.example.buskrutracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.buskrutracker.R;
import com.example.buskrutracker.api.ApiService;
import com.example.buskrutracker.api.RetrofitClient;
import com.example.buskrutracker.models.ApiResponse;
import com.example.buskrutracker.models.Kru;
import com.example.buskrutracker.utils.SharedPrefManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LoginActivity - Halaman Login Kru Bus
 * Design: Modern blue gradient dengan card input (Match HTML Mockup)
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private LinearLayout btnLogin;
    private TextView tvLoginText;
    private ProgressBar progressBar;

    private ApiService apiService;
    private SharedPrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize
        initViews();
        initServices();

        // Check jika sudah login
        if (prefManager.isLoggedIn()) {
            goToPersiapan();
            return;
        }

        // Setup listeners
        setupClickListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvLoginText = findViewById(R.id.tv_login_text);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initServices() {
        apiService = RetrofitClient.getApiService();
        prefManager = SharedPrefManager.getInstance(this);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            // Haptic feedback
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            handleLogin();
        });

        // Enter key di password field = submit
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            handleLogin();
            return true;
        });
    }

    // ============================================
    // LOGIN LOGIC
    // ============================================

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("ID tidak boleh kosong");
            etUsername.requestFocus();
            showToast("⚠️ Masukkan ID Pengemudi");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password tidak boleh kosong");
            etPassword.requestFocus();
            showToast("⚠️ Masukkan Kata Sandi");
            return;
        }

        // Show loading
        setLoading(true);

        // Prepare request body
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);

        // API Call
        Call<ApiResponse<Map<String, Object>>> call = apiService.login(credentials);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                   Response<ApiResponse<Map<String, Object>>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        handleLoginSuccess(apiResponse.getData());
                    } else {
                        showToast("❌ " + apiResponse.getMessage());
                    }
                } else {
                    showToast("❌ Login gagal. Periksa koneksi.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                setLoading(false);
                Log.e("LoginActivity", "Login error: ", t);
                showToast("❌ Koneksi gagal. Coba lagi.");
            }
        });
    }

    private void handleLoginSuccess(Map<String, Object> data) {
        try {
            // Response structure: data.kru + data.token
            Map<String, Object> kruData = (Map<String, Object>) data.get("kru");
            String token = data.get("token") != null ? data.get("token").toString() : "";

            if (kruData == null) {
                showToast("❌ Data kru tidak ditemukan");
                return;
            }

            // Parse kru data
            int id;
            if (kruData.get("id") instanceof Double) {
                id = ((Double) kruData.get("id")).intValue();
            } else if (kruData.get("id") instanceof Integer) {
                id = (Integer) kruData.get("id");
            } else {
                id = Integer.parseInt(kruData.get("id").toString());
            }

            String driver = kruData.get("driver") != null ? kruData.get("driver").toString() : "";
            String usernameDb = kruData.get("username") != null ? kruData.get("username").toString() : "";
            String status = kruData.get("status") != null ? kruData.get("status").toString() : "aktif";

            // Create Kru object
            Kru kru = new Kru();
            kru.setId(id);
            kru.setDriver(driver);
            kru.setUsername(usernameDb);
            kru.setStatus(status);
            kru.setToken(token);

            // Save to SharedPreferences
            prefManager.saveLoginData(token, kru);

            // Show success message dengan delay untuk smooth transition
            showToast("✓ Selamat datang, " + driver + "!");

            // Navigate to Persiapan dengan delay smooth
            btnLogin.postDelayed(this::goToPersiapan, 500);

        } catch (Exception e) {
            showToast("❌ Error: " + e.getMessage());
            Log.e("LoginActivity", "Parse Error: ", e);
        }
    }

    // ============================================
    // NAVIGATION
    // ============================================

    private void goToPersiapan() {
        Intent intent = new Intent(LoginActivity.this, PersiapanActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ============================================
    // UI HELPERS
    // ============================================

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            btnLogin.setAlpha(0.6f);
            tvLoginText.setText("Memproses...");

            // Hide keyboard
            hideKeyboard();
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            btnLogin.setAlpha(1.0f);
            tvLoginText.setText("MASUK SISTEM");
        }
    }

    private void hideKeyboard() {
        try {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Keluar Aplikasi?")
                .setMessage("Apakah Anda yakin ingin keluar?")
                .setPositiveButton("Ya", (dialog, which) -> {
                    finishAffinity(); // Close all activities
                })
                .setNegativeButton("Tidak", null)
                .show();
    }
}