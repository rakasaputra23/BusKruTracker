package com.example.buskrutracker.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.buskrutracker.R;
import com.example.buskrutracker.api.ApiService;
import com.example.buskrutracker.api.RetrofitClient;
import com.example.buskrutracker.models.ApiResponse;
import com.example.buskrutracker.models.Armada;
import com.example.buskrutracker.models.Kru;
import com.example.buskrutracker.models.Perjalanan;
import com.example.buskrutracker.models.Rute;
import com.example.buskrutracker.services.GpsTrackingService;
import com.example.buskrutracker.utils.PermissionHelper;
import com.example.buskrutracker.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PersiapanActivity - Pilih Armada & Rute, Mulai Perjalanan
 * Design: White background dengan card selection
 */
public class PersiapanActivity extends AppCompatActivity {

    private TextView tvNamaKru, tvStatus;
    private Spinner spinnerArmada, spinnerRute;
    private Button btnMulaiPerjalanan;
    private ProgressBar progressBar;

    private ApiService apiService;
    private SharedPrefManager prefManager;
    private PermissionHelper permissionHelper;

    private List<Armada> armadaList = new ArrayList<>();
    private List<Rute> ruteList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_persiapan);

        // Initialize
        initViews();
        initServices();
        loadUserData();
        setupClickListeners();

        // Load data dari API
        loadArmadaData();
        loadRuteData();
    }

    private void initViews() {
        tvNamaKru = findViewById(R.id.tv_nama_kru);
        tvStatus = findViewById(R.id.tv_status);
        spinnerArmada = findViewById(R.id.spinner_armada);
        spinnerRute = findViewById(R.id.spinner_rute);
        btnMulaiPerjalanan = findViewById(R.id.btn_mulai_perjalanan);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initServices() {
        apiService = RetrofitClient.getApiService();
        prefManager = SharedPrefManager.getInstance(this);
        permissionHelper = new PermissionHelper(this);
    }

    private void loadUserData() {
        Kru kru = prefManager.getUser();
        if (kru != null) {
            tvNamaKru.setText("Halo, " + kru.getDriver() + "!");
            tvStatus.setText("● Online");
        }
    }

    private void setupClickListeners() {
        btnMulaiPerjalanan.setOnClickListener(v -> checkPermissionsAndStart());
    }

    // ============================================
    // LOAD DATA DARI API
    // ============================================

    private void loadArmadaData() {
        String token = prefManager.getToken();

        Call<ApiResponse<List<Armada>>> call = apiService.getArmada(token);
        call.enqueue(new Callback<ApiResponse<List<Armada>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Armada>>> call,
                                   Response<ApiResponse<List<Armada>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Armada>> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        armadaList = apiResponse.getData();
                        populateArmadaSpinner();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Armada>>> call, Throwable t) {
                Toast.makeText(PersiapanActivity.this,
                        "Gagal load armada: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRuteData() {
        String token = prefManager.getToken();

        Call<ApiResponse<List<Rute>>> call = apiService.getRute(token);
        call.enqueue(new Callback<ApiResponse<List<Rute>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Rute>>> call,
                                   Response<ApiResponse<List<Rute>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Rute>> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        ruteList = apiResponse.getData();
                        populateRuteSpinner();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Rute>>> call, Throwable t) {
                Toast.makeText(PersiapanActivity.this,
                        "Gagal load rute: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ============================================
    // POPULATE SPINNERS
    // ============================================

    private void populateArmadaSpinner() {
        ArrayAdapter<Armada> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                armadaList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerArmada.setAdapter(adapter);
    }

    private void populateRuteSpinner() {
        ArrayAdapter<Rute> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                ruteList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRute.setAdapter(adapter);
    }

    // ============================================
    // PERMISSIONS & START PERJALANAN
    // ============================================

    private void checkPermissionsAndStart() {
        // Check permissions
        if (!permissionHelper.hasAllPermissions()) {
            showPermissionDialog();
            return;
        }

        // Check GPS enabled
        if (!permissionHelper.isGpsEnabled()) {
            showGpsDialog();
            return;
        }

        // Mulai perjalanan
        mulaiPerjalanan();
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Izin Diperlukan")
                .setMessage(permissionHelper.getLocationRationaleMessage())
                .setPositiveButton("Berikan Izin", (dialog, which) -> {
                    permissionHelper.requestAllPermissions();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showGpsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("GPS Tidak Aktif")
                .setMessage(permissionHelper.getGpsDisabledMessage())
                .setPositiveButton("Buka Settings", (dialog, which) -> {
                    permissionHelper.openGpsSettings();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // ============================================
    // MULAI PERJALANAN
    // ============================================

    private void mulaiPerjalanan() {
        // Validation
        if (spinnerArmada.getSelectedItem() == null) {
            Toast.makeText(this, "Pilih armada terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spinnerRute.getSelectedItem() == null) {
            Toast.makeText(this, "Pilih rute terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        Armada selectedArmada = (Armada) spinnerArmada.getSelectedItem();
        Rute selectedRute = (Rute) spinnerRute.getSelectedItem();

        // Show loading
        setLoading(true);

        // Prepare request
        Map<String, Integer> data = new HashMap<>();
        data.put("armada_id", selectedArmada.getId());
        data.put("rute_id", selectedRute.getId());

        String token = prefManager.getToken();

        // API Call
        Call<ApiResponse<Perjalanan>> call = apiService.mulaiPerjalanan(token, data);
        call.enqueue(new Callback<ApiResponse<Perjalanan>>() {
            @Override
            public void onResponse(Call<ApiResponse<Perjalanan>> call,
                                   Response<ApiResponse<Perjalanan>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Perjalanan> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        Perjalanan perjalanan = apiResponse.getData();
                        handlePerjalananStarted(perjalanan, selectedArmada, selectedRute);
                    } else {
                        Toast.makeText(PersiapanActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Perjalanan>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(PersiapanActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handlePerjalananStarted(Perjalanan perjalanan, Armada armada, Rute rute) {
        // Save perjalanan ID
        prefManager.savePerjalanId(perjalanan.getId());

        // Start GPS Service
        Kru kru = prefManager.getUser();
        Intent serviceIntent = GpsTrackingService.createStartIntent(
                this,
                perjalanan.getId(),
                armada.getPlatNomor(),
                rute.getNamaRute(),
                kru.getDriver()
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Navigate to Tracking
        Intent intent = new Intent(PersiapanActivity.this, TrackingActivity.class);
        intent.putExtra("perjalanan_id", perjalanan.getId());
        intent.putExtra("armada_nomor", armada.getPlatNomor());
        intent.putExtra("rute_nama", rute.getNamaRute());
        startActivity(intent);
        finish();
    }

    // ============================================
    // PERMISSION RESULT
    // ============================================

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissionHelper.handlePermissionResult(requestCode, permissions, grantResults)) {
            Toast.makeText(this, "Izin diberikan!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Izin ditolak. Tracking tidak dapat dimulai.", Toast.LENGTH_LONG).show();
        }
    }

    // ============================================
    // UI HELPERS
    // ============================================

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnMulaiPerjalanan.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnMulaiPerjalanan.setEnabled(true);
        }
    }
}