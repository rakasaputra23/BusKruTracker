package com.example.buskrutracker.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
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
 * PersiapanActivity - Query data master dari database
 * Design: Modern card-based UI (Match HTML Mockup)
 */
public class PersiapanActivity extends AppCompatActivity {

    private TextView tvNamaKru, tvStatus, tvButtonText;
    private Spinner spinnerArmada, spinnerRute;
    private LinearLayout btnMulaiPerjalanan;
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
        tvButtonText = findViewById(R.id.tv_button_text);
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
            String nama = kru.getDriver();
            // Format nama dengan sapaan yang tepat
            String sapaan = getNamaSapaan(nama);
            tvNamaKru.setText("Halo, " + sapaan + "!");
            tvStatus.setText("Online");
        } else {
            tvNamaKru.setText("Halo, Kru!");
            tvStatus.setText("Online");
        }
    }

    private String getNamaSapaan(String namaLengkap) {
        // Ambil nama depan saja
        if (namaLengkap != null && !namaLengkap.isEmpty()) {
            String[] parts = namaLengkap.split(" ");
            String namaDepan = parts[0];

            // Tambahkan gelar "Pak" atau "Bu" (default Pak)
            // Bisa ditambahkan logic berdasarkan gender jika ada di database
            return "Pak " + namaDepan;
        }
        return "Kru";
    }

    private void setupClickListeners() {
        btnMulaiPerjalanan.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            checkPermissionsAndStart();
        });
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

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        armadaList = apiResponse.getData();
                        populateArmadaSpinner();
                    } else {
                        showToast("⚠️ Data armada kosong");
                    }
                } else {
                    showToast("❌ Gagal memuat data armada");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Armada>>> call, Throwable t) {
                showToast("❌ Koneksi error: " + t.getMessage());
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

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        ruteList = apiResponse.getData();
                        populateRuteSpinner();
                    } else {
                        showToast("⚠️ Data rute kosong");
                    }
                } else {
                    showToast("❌ Gagal memuat data rute");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Rute>>> call, Throwable t) {
                showToast("❌ Koneksi error: " + t.getMessage());
            }
        });
    }

    private void populateArmadaSpinner() {
        // Custom adapter for better UI
        ArrayAdapter<Armada> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_armada,
                R.id.tv_plat_nomor,
                armadaList
        );
        adapter.setDropDownViewResource(R.layout.spinner_item_armada);
        spinnerArmada.setAdapter(adapter);
    }

    private void populateRuteSpinner() {
        // Custom adapter for better UI
        ArrayAdapter<Rute> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_rute,
                R.id.tv_nama_rute,
                ruteList
        );
        adapter.setDropDownViewResource(R.layout.spinner_item_rute);
        spinnerRute.setAdapter(adapter);
    }

    // ============================================
    // PERMISSIONS & START PERJALANAN
    // ============================================

    private void checkPermissionsAndStart() {
        if (!permissionHelper.hasAllPermissions()) {
            showPermissionDialog();
            return;
        }

        if (!permissionHelper.isGpsEnabled()) {
            showGpsDialog();
            return;
        }

        mulaiPerjalanan();
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("📍 Izin Lokasi Diperlukan")
                .setMessage(permissionHelper.getLocationRationaleMessage())
                .setPositiveButton("Berikan Izin", (dialog, which) -> {
                    permissionHelper.requestAllPermissions();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showGpsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🛰️ GPS Tidak Aktif")
                .setMessage(permissionHelper.getGpsDisabledMessage())
                .setPositiveButton("Buka Settings", (dialog, which) -> {
                    permissionHelper.openGpsSettings();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // ============================================
    // MULAI PERJALANAN - AMBIL DATA DARI DATABASE
    // ============================================

    private void mulaiPerjalanan() {
        if (spinnerArmada.getSelectedItem() == null) {
            showToast("⚠️ Pilih armada terlebih dahulu");
            return;
        }

        if (spinnerRute.getSelectedItem() == null) {
            showToast("⚠️ Pilih rute terlebih dahulu");
            return;
        }

        Armada selectedArmada = (Armada) spinnerArmada.getSelectedItem();
        Rute selectedRute = (Rute) spinnerRute.getSelectedItem();

        // Show confirmation dialog
        showConfirmationDialog(selectedArmada, selectedRute);
    }

    private void showConfirmationDialog(Armada armada, Rute rute) {
        new AlertDialog.Builder(this)
                .setTitle("🚀 Mulai Perjalanan?")
                .setMessage("Armada: " + armada.getPlatNomor() + " (" + armada.getKelas() + ")\n" +
                        "Rute: " + rute.getNamaRute())
                .setPositiveButton("Ya, Mulai", (dialog, which) -> {
                    startPerjalanan(armada, rute);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void startPerjalanan(Armada selectedArmada, Rute selectedRute) {
        setLoading(true);

        Map<String, Integer> data = new HashMap<>();
        data.put("armada_id", selectedArmada.getId());
        data.put("rute_id", selectedRute.getId());

        String token = prefManager.getToken();

        Call<ApiResponse<Perjalanan>> call = apiService.mulaiPerjalanan(token, data);
        call.enqueue(new Callback<ApiResponse<Perjalanan>>() {
            @Override
            public void onResponse(Call<ApiResponse<Perjalanan>> call,
                                   Response<ApiResponse<Perjalanan>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Perjalanan> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        Perjalanan perjalanan = apiResponse.getData();
                        handlePerjalananStarted(perjalanan, selectedArmada, selectedRute);
                    } else {
                        showToast("❌ " + apiResponse.getMessage());
                    }
                } else {
                    showToast("❌ Gagal memulai perjalanan");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Perjalanan>> call, Throwable t) {
                setLoading(false);
                showToast("❌ Error: " + t.getMessage());
            }
        });
    }

    private void handlePerjalananStarted(Perjalanan perjalanan, Armada armada, Rute rute) {
        // Save perjalanan ID
        prefManager.savePerjalanId(perjalanan.getId());
        prefManager.setTracking(true);

        // Get kru data
        Kru kru = prefManager.getUser();

        // Start GPS Service dengan data DARI DATABASE
        Intent serviceIntent = GpsTrackingService.createStartIntent(
                this,
                perjalanan.getId(),          // perjalanan_id
                armada.getPlatNomor(),       // armada.plat_nomor
                armada.getKelas(),           // armada.kelas
                armada.getKapasitas(),       // armada.kapasitas
                rute.getNamaRute(),          // rute.nama_rute
                rute.getPolyline(),          // rute.polyline ⭐ DARI DATABASE
                kru.getDriver()              // kru.driver
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Success message
        showToast("✓ Perjalanan dimulai!");

        // Navigate to Tracking dengan delay smooth
        btnMulaiPerjalanan.postDelayed(() -> {
            Intent intent = new Intent(PersiapanActivity.this, TrackingActivity.class);
            intent.putExtra("perjalanan_id", perjalanan.getId());
            intent.putExtra("armada_nomor", armada.getPlatNomor());
            intent.putExtra("rute_nama", rute.getNamaRute());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

            // Smooth transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 500);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissionHelper.handlePermissionResult(requestCode, permissions, grantResults)) {
            showToast("✓ Izin diberikan!");

            // Auto-check GPS after permission granted
            if (!permissionHelper.isGpsEnabled()) {
                showGpsDialog();
            }
        } else {
            showToast("❌ Izin ditolak. Tracking tidak dapat dimulai.");
        }
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnMulaiPerjalanan.setEnabled(false);
            btnMulaiPerjalanan.setAlpha(0.6f);
            tvButtonText.setText("Memproses...");
        } else {
            progressBar.setVisibility(View.GONE);
            btnMulaiPerjalanan.setEnabled(true);
            btnMulaiPerjalanan.setAlpha(1.0f);
            tvButtonText.setText("MULAI PERJALANAN");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Logout?")
                .setMessage("Apakah Anda ingin keluar dari aplikasi?")
                .setPositiveButton("Ya, Logout", (dialog, which) -> {
                    // Clear session
                    prefManager.logout();

                    // Go to login
                    Intent intent = new Intent(PersiapanActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}