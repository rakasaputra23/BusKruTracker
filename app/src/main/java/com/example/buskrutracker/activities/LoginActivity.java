package com.example.buskrutracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
 * Design: Blue gradient dengan card input
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
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
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initServices() {
        apiService = RetrofitClient.getApiService();
        prefManager = SharedPrefManager.getInstance(this);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());
    }

    // ============================================
    // LOGIN LOGIC
    // ============================================

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username tidak boleh kosong");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password tidak boleh kosong");
            etPassword.requestFocus();
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
                        Toast.makeText(LoginActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Login gagal. Coba lagi.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Koneksi error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLoginSuccess(Map<String, Object> data) {
        try {
            // Parse response
            int id = ((Double) data.get("id")).intValue();
            String driver = (String) data.get("driver");
            String username = (String) data.get("username");
            String status = (String) data.get("status");
            String token = (String) data.get("token");

            // Create Kru object
            Kru kru = new Kru();
            kru.setId(id);
            kru.setDriver(driver);
            kru.setUsername(username);
            kru.setStatus(status);
            kru.setToken(token);

            // Save to SharedPreferences
            prefManager.saveLoginData(token, kru);

            // Show success message
            Toast.makeText(this,
                    "Selamat datang, " + driver + "!",
                    Toast.LENGTH_SHORT).show();

            // Navigate to Persiapan
            goToPersiapan();

        } catch (Exception e) {
            Toast.makeText(this,
                    "Error parsing data: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ============================================
    // NAVIGATION
    // ============================================

    private void goToPersiapan() {
        Intent intent = new Intent(LoginActivity.this, PersiapanActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ============================================
    // UI HELPERS
    // ============================================

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            btnLogin.setText("Memproses...");
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            btnLogin.setText("MASUK SISTEM");
        }
    }

    @Override
    public void onBackPressed() {
        // Disable back button di login screen
        // User harus login dulu
    }
}