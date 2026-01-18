package com.example.buskrutracker.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.buskrutracker.R;
import com.example.buskrutracker.api.ApiService;
import com.example.buskrutracker.api.RetrofitClient;
import com.example.buskrutracker.models.ApiResponse;
import com.example.buskrutracker.models.Perjalanan;
import com.example.buskrutracker.services.GpsTrackingService;
import com.example.buskrutracker.utils.SharedPrefManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackingActivity extends AppCompatActivity {

    private TextView tvRuteAktif, tvJumlahPenumpang, tvKapasitas;
    private TextView tvSpeed, tvDistance, tvDuration;
    private Button btnTambahPenumpang, btnKurangPenumpang;
    private Button btnStatusLancar, btnStatusMacet, btnStatusMogok;
    private Button btnAkhiriPerjalanan;
    private ProgressBar progressBar;

    private ApiService apiService;
    private SharedPrefManager prefManager;

    private int perjalanId;
    private String armadaNomor;
    private String ruteNama;

    private int jumlahPenumpang = 0;
    private int kapasitas = 40;
    private String kondisiTerakhir = "lancar";

    private double totalJarak = 0.0;
    private int durasiMenit = 0;
    private float speedKmh = 0.0f;

    // BroadcastReceiver untuk terima update dari Service
    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double lat = intent.getDoubleExtra("latitude", 0);
            double lng = intent.getDoubleExtra("longitude", 0);
            speedKmh = intent.getFloatExtra("speed", 0);
            totalJarak = intent.getDoubleExtra("distance", 0);
            durasiMenit = intent.getIntExtra("duration", 0);

            updateLocationUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // Get data dari intent
        perjalanId = getIntent().getIntExtra("perjalanan_id", 0);
        armadaNomor = getIntent().getStringExtra("armada_nomor");
        ruteNama = getIntent().getStringExtra("rute_nama");

        initViews();
        initServices();
        setupUI();
        setupClickListeners();
        loadPerjalanAktif();
    }

    private void initViews() {
        tvRuteAktif = findViewById(R.id.tv_rute_aktif);
        tvJumlahPenumpang = findViewById(R.id.tv_jumlah_penumpang);
        tvKapasitas = findViewById(R.id.tv_kapasitas);
        tvSpeed = findViewById(R.id.tv_speed);
        tvDistance = findViewById(R.id.tv_distance);
        tvDuration = findViewById(R.id.tv_duration);

        btnTambahPenumpang = findViewById(R.id.btn_tambah_penumpang);
        btnKurangPenumpang = findViewById(R.id.btn_kurang_penumpang);

        btnStatusLancar = findViewById(R.id.btn_status_lancar);
        btnStatusMacet = findViewById(R.id.btn_status_macet);
        btnStatusMogok = findViewById(R.id.btn_status_mogok);

        btnAkhiriPerjalanan = findViewById(R.id.btn_akhiri_perjalanan);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initServices() {
        apiService = RetrofitClient.getApiService();
        prefManager = SharedPrefManager.getInstance(this);
    }

    private void setupUI() {
        tvRuteAktif.setText(ruteNama != null ? ruteNama : "N/A");
        updatePenumpangUI();
        setActiveStatusButton(btnStatusLancar);
    }

    private void setupClickListeners() {
        btnTambahPenumpang.setOnClickListener(v -> tambahPenumpang());
        btnKurangPenumpang.setOnClickListener(v -> kurangPenumpang());

        btnStatusLancar.setOnClickListener(v -> updateKondisi("lancar", btnStatusLancar));
        btnStatusMacet.setOnClickListener(v -> updateKondisi("macet", btnStatusMacet));
        btnStatusMogok.setOnClickListener(v -> updateKondisi("mogok", btnStatusMogok));

        btnAkhiriPerjalanan.setOnClickListener(v -> showAkhiriDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("GPS_LOCATION_UPDATE");
        registerReceiver(locationReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(locationReceiver);
    }

    // ============================================
    // LOAD PERJALANAN AKTIF
    // ============================================

    private void loadPerjalanAktif() {
        String token = prefManager.getToken();

        Call<ApiResponse<Perjalanan>> call = apiService.getPerjalananAktif(token);
        call.enqueue(new Callback<ApiResponse<Perjalanan>>() {
            @Override
            public void onResponse(Call<ApiResponse<Perjalanan>> call,
                                   Response<ApiResponse<Perjalanan>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Perjalanan> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        Perjalanan perjalanan = apiResponse.getData();
                        jumlahPenumpang = perjalanan.getTotalPenumpang();
                        kondisiTerakhir = perjalanan.getKondisiTerakhir() != null ?
                                perjalanan.getKondisiTerakhir() : "lancar";

                        updatePenumpangUI();
                        setActiveStatusByKondisi(kondisiTerakhir);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Perjalanan>> call, Throwable t) {
                // Silent fail
            }
        });
    }

    // ============================================
    // PENUMPANG COUNTER
    // ============================================

    private void tambahPenumpang() {
        if (jumlahPenumpang < kapasitas) {
            jumlahPenumpang++;
            updatePenumpangUI();
            updatePenumpangToService();
            updatePenumpangToServer();
        } else {
            Toast.makeText(this, "Kapasitas penuh!", Toast.LENGTH_SHORT).show();
        }
    }

    private void kurangPenumpang() {
        if (jumlahPenumpang > 0) {
            jumlahPenumpang--;
            updatePenumpangUI();
            updatePenumpangToService();
            updatePenumpangToServer();
        }
    }

    private void updatePenumpangUI() {
        tvJumlahPenumpang.setText(String.valueOf(jumlahPenumpang));
        tvKapasitas.setText("/" + kapasitas);
    }

    private void updatePenumpangToService() {
        Intent intent = GpsTrackingService.createPassengerUpdateIntent(
                this,
                perjalanId,
                jumlahPenumpang
        );
        startService(intent);
    }

    private void updatePenumpangToServer() {
        String token = prefManager.getToken();

        Map<String, Integer> data = new HashMap<>();
        data.put("perjalanan_id", perjalanId);
        data.put("total_penumpang", jumlahPenumpang);

        Call<ApiResponse<Perjalanan>> call = apiService.updatePenumpang(token, data);
        call.enqueue(new Callback<ApiResponse<Perjalanan>>() {
            @Override
            public void onResponse(Call<ApiResponse<Perjalanan>> call,
                                   Response<ApiResponse<Perjalanan>> response) {
                // Silent update
            }

            @Override
            public void onFailure(Call<ApiResponse<Perjalanan>> call, Throwable t) {
                // Silent fail
            }
        });
    }

    // ============================================
    // UPDATE KONDISI BUS ⭐⭐⭐
    // ============================================

    private void updateKondisi(String kondisi, Button activeButton) {
        kondisiTerakhir = kondisi;
        setActiveStatusButton(activeButton);

        // ⭐ 1. Update ke Firebase via Service (REALTIME)
        updateKondisiToService(kondisi);

        // ⭐ 2. Update ke MySQL via API (PERSISTENT)
        updateKondisiToServer(kondisi);
    }

    /**
     * ⭐ Update kondisi ke Firebase via Service
     */
    private void updateKondisiToService(String kondisi) {
        Intent intent = GpsTrackingService.createKondisiUpdateIntent(
                this,
                perjalanId,
                kondisi
        );
        startService(intent);
    }

    /**
     * ⭐ Update kondisi ke MySQL via API
     */
    private void updateKondisiToServer(String kondisi) {
        String token = prefManager.getToken();

        Map<String, Object> data = new HashMap<>();
        data.put("perjalanan_id", perjalanId);
        data.put("kondisi", kondisi);

        Call<ApiResponse<Perjalanan>> call = apiService.updateKondisi(token, data);
        call.enqueue(new Callback<ApiResponse<Perjalanan>>() {
            @Override
            public void onResponse(Call<ApiResponse<Perjalanan>> call,
                                   Response<ApiResponse<Perjalanan>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(TrackingActivity.this,
                            "Status: " + kondisi.toUpperCase(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Perjalanan>> call, Throwable t) {
                // Silent fail
            }
        });
    }

    private void setActiveStatusButton(Button activeButton) {
        btnStatusLancar.setAlpha(0.5f);
        btnStatusMacet.setAlpha(0.5f);
        btnStatusMogok.setAlpha(0.5f);
        activeButton.setAlpha(1.0f);
    }

    private void setActiveStatusByKondisi(String kondisi) {
        switch (kondisi.toLowerCase()) {
            case "macet":
                setActiveStatusButton(btnStatusMacet);
                break;
            case "mogok":
                setActiveStatusButton(btnStatusMogok);
                break;
            default:
                setActiveStatusButton(btnStatusLancar);
                break;
        }
    }

    // ============================================
    // UPDATE LOCATION UI
    // ============================================

    private void updateLocationUI() {
        tvSpeed.setText(String.format("%.1f km/h", speedKmh));
        tvDistance.setText(String.format("%.2f km", totalJarak));

        int hours = durasiMenit / 60;
        int minutes = durasiMenit % 60;
        tvDuration.setText(String.format("%dj %dm", hours, minutes));
    }

    // ============================================
    // AKHIRI PERJALANAN
    // ============================================

    private void showAkhiriDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Akhiri Perjalanan?")
                .setMessage("Apakah Anda yakin ingin mengakhiri perjalanan ini?")
                .setPositiveButton("Ya, Akhiri", (dialog, which) -> akhiriPerjalanan())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void akhiriPerjalanan() {
        setLoading(true);

        String token = prefManager.getToken();

        Map<String, Object> data = new HashMap<>();
        data.put("perjalanan_id", perjalanId);
        data.put("total_penumpang", jumlahPenumpang);
        data.put("jarak_tempuh", totalJarak);
        data.put("catatan", "Perjalanan selesai");

        Call<ApiResponse<Map<String, Object>>> call = apiService.selesaiPerjalanan(token, data);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                   Response<ApiResponse<Map<String, Object>>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        handlePerjalananSelesai();
                    } else {
                        Toast.makeText(TrackingActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(TrackingActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handlePerjalananSelesai() {
        Intent stopIntent = GpsTrackingService.createStopIntent(this);
        startService(stopIntent);

        prefManager.clearPerjalanId();
        prefManager.setTracking(false);

        Intent intent = new Intent(TrackingActivity.this, LaporanActivity.class);
        intent.putExtra("total_penumpang", jumlahPenumpang);
        intent.putExtra("jarak_tempuh", totalJarak);
        intent.putExtra("durasi_menit", durasiMenit);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnAkhiriPerjalanan.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnAkhiriPerjalanan.setEnabled(true);
        }
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Gunakan tombol 'Akhiri Perjalanan' untuk keluar", Toast.LENGTH_SHORT).show();
    }
}