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
import com.example.buskrutracker.models.MulaiPerjalananResponse;
import com.example.buskrutracker.models.Perjalanan;
import com.example.buskrutracker.models.Rute;
import com.example.buskrutracker.models.SelesaiPerjalananResponse;
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

public class PersiapanActivity extends AppCompatActivity {

    private TextView tvNamaKru, tvStatus, tvButtonText;
    private Spinner spinnerArmada, spinnerRute;
    private LinearLayout btnMulaiPerjalanan;
    private ProgressBar progressBar;

    private ApiService apiService;
    private SharedPrefManager prefManager;
    private PermissionHelper permissionHelper;

    private List<Armada> armadaList = new ArrayList<>();
    private List<Rute>   ruteList   = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_persiapan);

        initViews();
        initServices();
        loadUserData();
        setupClickListeners();

        loadArmadaData();
        loadRuteData();

        checkActiveTrip();
    }

    private void initViews() {
        tvNamaKru          = findViewById(R.id.tv_nama_kru);
        tvStatus           = findViewById(R.id.tv_status);
        tvButtonText       = findViewById(R.id.tv_button_text);
        spinnerArmada      = findViewById(R.id.spinner_armada);
        spinnerRute        = findViewById(R.id.spinner_rute);
        btnMulaiPerjalanan = findViewById(R.id.btn_mulai_perjalanan);
        progressBar        = findViewById(R.id.progress_bar);
    }

    private void initServices() {
        apiService       = RetrofitClient.getApiService();
        prefManager      = SharedPrefManager.getInstance(this);
        permissionHelper = new PermissionHelper(this);
    }

    private void loadUserData() {
        Kru kru = prefManager.getUser();
        if (kru != null) {
            tvNamaKru.setText("Halo, " + getNamaSapaan(kru.getDriver()) + "!");
        } else {
            tvNamaKru.setText("Halo, Kru!");
        }
        tvStatus.setText("Online");
    }

    private String getNamaSapaan(String namaLengkap) {
        if (namaLengkap != null && !namaLengkap.isEmpty()) {
            return "Pak " + namaLengkap.split(" ")[0];
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
    // CEK PERJALANAN AKTIF SAAT BUKA APP
    // ============================================

    private void checkActiveTrip() {
        String token = prefManager.getToken();

        apiService.getPerjalananAktif(token).enqueue(new Callback<ApiResponse<Perjalanan>>() {
            @Override
            public void onResponse(Call<ApiResponse<Perjalanan>> call,
                                   Response<ApiResponse<Perjalanan>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    showActiveTripDialog(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Perjalanan>> call, Throwable t) {
                // Silent fail
            }
        });
    }

    private void showActiveTripDialog(Perjalanan perjalanan) {
        String busInfo  = "";
        String ruteInfo = "";

        if (perjalanan.getArmada() != null) {
            Armada a = perjalanan.getArmada();
            busInfo = (a.getNamaBus() != null && !a.getNamaBus().isEmpty())
                    ? a.getNamaBus() + " (" + a.getPlatNomor() + ")"
                    : a.getPlatNomor();
        }
        if (perjalanan.getRute() != null) {
            ruteInfo = perjalanan.getRute().getNamaRute();
        }

        String finalBusInfo  = busInfo.isEmpty()  ? "ID " + perjalanan.getArmadaId()  : busInfo;
        String finalRuteInfo = ruteInfo.isEmpty() ? "ID " + perjalanan.getRuteId()    : ruteInfo;

        new AlertDialog.Builder(this)
                .setTitle("🚍 Ada Perjalanan Aktif")
                .setMessage("Ditemukan perjalanan yang belum diselesaikan:\n\n"
                        + "🚌 Bus  : " + finalBusInfo  + "\n"
                        + "📍 Rute : " + finalRuteInfo + "\n\n"
                        + "Pilih tindakan:")
                .setCancelable(false)
                .setPositiveButton("▶ Lanjutkan", (dialog, which) -> {
                    prefManager.savePerjalanId(perjalanan.getId());
                    prefManager.setTracking(true);

                    Intent intent = new Intent(PersiapanActivity.this, TrackingActivity.class);
                    intent.putExtra("perjalanan_id", perjalanan.getId());

                    if (perjalanan.getArmada() != null) {
                        intent.putExtra("nama_bus",     perjalanan.getArmada().getNamaBus());
                        intent.putExtra("armada_nomor", perjalanan.getArmada().getPlatNomor());
                    }
                    if (perjalanan.getRute() != null) {
                        intent.putExtra("rute_nama", perjalanan.getRute().getNamaRute());
                    }

                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                })
                .setNegativeButton("✖ Batalkan Perjalanan Lama", (dialog, which) ->
                        cancelOldTrip(perjalanan.getId()))
                .show();
    }

    private void cancelOldTrip(int perjalanId) {
        setLoading(true);
        showToast("⏳ Membatalkan perjalanan lama...");

        String token = prefManager.getToken();

        Map<String, Object> data = new HashMap<>();
        data.put("perjalanan_id",        perjalanId);
        data.put("total_penumpang",       0);
        data.put("total_penumpang_naik",  0);
        data.put("jarak_tempuh",          0.0);
        data.put("catatan",               "Dibatalkan otomatis oleh sistem (app restart)");

        apiService.selesaiPerjalanan(token, data)
                .enqueue(new Callback<ApiResponse<SelesaiPerjalananResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<SelesaiPerjalananResponse>> call,
                                           Response<ApiResponse<SelesaiPerjalananResponse>> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            prefManager.clearPerjalanId();
                            prefManager.setTracking(false);
                            showToast("✓ Perjalanan lama dibatalkan. Silakan mulai yang baru.");
                        } else {
                            showToast("❌ Gagal membatalkan. Hubungi admin.");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<SelesaiPerjalananResponse>> call,
                                          Throwable t) {
                        setLoading(false);
                        showToast("❌ Koneksi error: " + t.getMessage());
                    }
                });
    }

    // ============================================
    // LOAD DATA DARI API
    // ============================================

    private void loadArmadaData() {
        String token = prefManager.getToken();
        apiService.getArmada(token).enqueue(new Callback<ApiResponse<List<Armada>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Armada>>> call,
                                   Response<ApiResponse<List<Armada>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    armadaList = response.body().getData();
                    populateArmadaSpinner();
                } else {
                    showToast("⚠️ Data armada kosong");
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
        apiService.getRute(token).enqueue(new Callback<ApiResponse<List<Rute>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Rute>>> call,
                                   Response<ApiResponse<List<Rute>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    ruteList = response.body().getData();
                    populateRuteSpinner();
                } else {
                    showToast("⚠️ Data rute kosong");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Rute>>> call, Throwable t) {
                showToast("❌ Koneksi error: " + t.getMessage());
            }
        });
    }

    private void populateArmadaSpinner() {
        ArrayAdapter<Armada> adapter = new ArrayAdapter<>(
                this, R.layout.spinner_item_armada, R.id.tv_plat_nomor, armadaList);
        adapter.setDropDownViewResource(R.layout.spinner_item_armada);
        spinnerArmada.setAdapter(adapter);
    }

    private void populateRuteSpinner() {
        ArrayAdapter<Rute> adapter = new ArrayAdapter<>(
                this, R.layout.spinner_item_rute, R.id.tv_nama_rute, ruteList);
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
                .setPositiveButton("Berikan Izin", (d, w) ->
                        permissionHelper.requestAllPermissions())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showGpsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🛰️ GPS Tidak Aktif")
                .setMessage(permissionHelper.getGpsDisabledMessage())
                .setPositiveButton("Buka Settings", (d, w) ->
                        permissionHelper.openGpsSettings())
                .setNegativeButton("Batal", null)
                .show();
    }

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
        Rute   selectedRute   = (Rute)   spinnerRute.getSelectedItem();
        showConfirmationDialog(selectedArmada, selectedRute);
    }

    private void showConfirmationDialog(Armada armada, Rute rute) {
        String busInfo = (armada.getNamaBus() != null && !armada.getNamaBus().isEmpty())
                ? armada.getNamaBus() + " (" + armada.getPlatNomor() + ")"
                : armada.getPlatNomor();

        new AlertDialog.Builder(this)
                .setTitle("🚀 Mulai Perjalanan?")
                .setMessage("Bus   : " + busInfo           + "\n"
                        +   "Kelas : " + armada.getKelas() + "\n"
                        +   "Rute  : " + rute.getNamaRute())
                .setPositiveButton("Ya, Mulai", (d, w) -> startPerjalanan(armada, rute))
                .setNegativeButton("Batal", null)
                .show();
    }

    // ============================================
    // START PERJALANAN
    // ============================================

    private void startPerjalanan(Armada selectedArmada, Rute selectedRute) {
        setLoading(true);

        Map<String, Integer> data = new HashMap<>();
        data.put("armada_id", selectedArmada.getId());
        data.put("rute_id",   selectedRute.getId());

        String token = prefManager.getToken();

        apiService.mulaiPerjalanan(token, data)
                .enqueue(new Callback<ApiResponse<MulaiPerjalananResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<MulaiPerjalananResponse>> call,
                                           Response<ApiResponse<MulaiPerjalananResponse>> response) {
                        setLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<MulaiPerjalananResponse> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                Perjalanan perjalanan = apiResponse.getData().getPerjalanan();
                                if (perjalanan != null) {
                                    handlePerjalananStarted(
                                            perjalanan, selectedArmada, selectedRute);
                                } else {
                                    showToast("❌ Data perjalanan tidak valid");
                                }
                            } else {
                                showToast("❌ " + apiResponse.getMessage());
                            }
                        } else {
                            showToast("❌ Gagal memulai perjalanan");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<MulaiPerjalananResponse>> call,
                                          Throwable t) {
                        setLoading(false);
                        showToast("❌ Error: " + t.getMessage());
                    }
                });
    }

    private void handlePerjalananStarted(Perjalanan perjalanan,
                                         Armada armada,
                                         Rute rute) {
        prefManager.savePerjalanId(perjalanan.getId());
        prefManager.setTracking(true);

        Kru kru = prefManager.getUser();

        // ⭐ FIXED: pakai getTarifHarga() karena tarif adalah nested object
        double tarif = rute.getTarifHarga();

        Intent serviceIntent = GpsTrackingService.createStartIntent(
                this,
                perjalanan.getId(),
                armada.getNamaBus(),
                armada.getPlatNomor(),
                armada.getKelas(),
                armada.getKapasitas(),
                rute.getNamaRute(),
                rute.getPolyline(),
                kru.getDriver(),
                tarif
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        showToast("✓ Perjalanan dimulai!");

        btnMulaiPerjalanan.postDelayed(() -> {
            Intent intent = new Intent(PersiapanActivity.this, TrackingActivity.class);
            intent.putExtra("perjalanan_id", perjalanan.getId());
            intent.putExtra("nama_bus",      armada.getNamaBus());
            intent.putExtra("armada_nomor",  armada.getPlatNomor());
            intent.putExtra("rute_nama",     rute.getNamaRute());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 500);
    }

    // ============================================
    // PERMISSION RESULT
    // ============================================

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionHelper.handlePermissionResult(
                requestCode, permissions, grantResults)) {
            showToast("✓ Izin diberikan!");
            if (!permissionHelper.isGpsEnabled()) showGpsDialog();
        } else {
            showToast("❌ Izin ditolak. Tracking tidak dapat dimulai.");
        }
    }

    // ============================================
    // HELPER
    // ============================================

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
                .setPositiveButton("Ya, Logout", (d, w) -> {
                    prefManager.logout();
                    Intent intent = new Intent(PersiapanActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}