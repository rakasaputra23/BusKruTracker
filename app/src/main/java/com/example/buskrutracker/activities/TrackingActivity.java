package com.example.buskrutracker.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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

/**
 * TrackingActivity - Activity untuk monitoring perjalanan bus realtime
 * ⭐ UPDATED: Support untuk display nama bus
 */
public class TrackingActivity extends AppCompatActivity {

    // TextViews
    private TextView tvOrigin, tvDestination;
    private TextView tvBusInfo;  // ⭐ OPTIONAL: Untuk display nama bus + plat nomor
    private TextView tvJumlahPenumpang, tvKapasitas;
    private TextView tvSpeed, tvDistance, tvDuration;

    // Buttons (LinearLayout karena custom design)
    private LinearLayout btnTambahPenumpang, btnKurangPenumpang;
    private LinearLayout btnStatusLancar, btnStatusMacet, btnStatusMogok;
    private LinearLayout btnAkhiriPerjalanan;

    // CardViews untuk status buttons
    private CardView cardStatusLancar, cardStatusMacet, cardStatusMogok;

    private ProgressBar progressBar;

    private ApiService apiService;
    private SharedPrefManager prefManager;

    private int perjalanId;
    private String namaBus;        // ⭐ FIELD BARU
    private String armadaNomor;
    private String ruteNama;
    private String origin = "SBY";
    private String destination = "MADIUN";

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
            int updateCount = intent.getIntExtra("update_count", 0);

            // Calculate duration
            durasiMenit = updateCount / 12; // approx (update setiap 5 detik)

            updateLocationUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // ⭐ Get data dari intent
        perjalanId = getIntent().getIntExtra("perjalanan_id", 0);
        namaBus = getIntent().getStringExtra("nama_bus");           // ⭐ AMBIL NAMA BUS
        armadaNomor = getIntent().getStringExtra("armada_nomor");
        ruteNama = getIntent().getStringExtra("rute_nama");

        // Parse rute jika formatnya "Origin - Destination"
        parseRuteNama();

        initViews();
        initServices();
        setupUI();
        setupClickListeners();
        loadPerjalanAktif();
    }

    private void parseRuteNama() {
        if (ruteNama != null && ruteNama.contains("-")) {
            String[] parts = ruteNama.split("-");
            if (parts.length >= 2) {
                origin = parts[0].trim().toUpperCase();
                destination = parts[1].trim().toUpperCase();
            }
        } else if (ruteNama != null && ruteNama.contains("→")) {
            String[] parts = ruteNama.split("→");
            if (parts.length >= 2) {
                origin = parts[0].trim().toUpperCase();
                destination = parts[1].trim().toUpperCase();
            }
        }
    }

    private void initViews() {
        // Header
        tvOrigin = findViewById(R.id.tv_origin);
        tvDestination = findViewById(R.id.tv_destination);

        // ⭐ OPTIONAL: TextView untuk display nama bus
        // Uncomment jika ada di layout
        // tvBusInfo = findViewById(R.id.tv_bus_info);

        // Counter
        tvJumlahPenumpang = findViewById(R.id.tv_jumlah_penumpang);
        tvKapasitas = findViewById(R.id.tv_kapasitas);

        // Stats (optional - visibility gone by default)
        tvSpeed = findViewById(R.id.tv_speed);
        tvDistance = findViewById(R.id.tv_distance);
        tvDuration = findViewById(R.id.tv_duration);

        // Penumpang buttons (LinearLayout)
        btnTambahPenumpang = findViewById(R.id.btn_tambah_penumpang);
        btnKurangPenumpang = findViewById(R.id.btn_kurang_penumpang);

        // Status buttons (LinearLayout)
        btnStatusLancar = findViewById(R.id.btn_status_lancar);
        btnStatusMacet = findViewById(R.id.btn_status_macet);
        btnStatusMogok = findViewById(R.id.btn_status_mogok);

        // Bottom action
        btnAkhiriPerjalanan = findViewById(R.id.btn_akhiri_perjalanan);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initServices() {
        apiService = RetrofitClient.getApiService();
        prefManager = SharedPrefManager.getInstance(this);
    }

    private void setupUI() {
        // Set rute
        tvOrigin.setText(origin);
        tvDestination.setText(destination);

        // ⭐ Set bus info (OPTIONAL - jika ada TextView di layout)
        // if (tvBusInfo != null) {
        //     if (namaBus != null && !namaBus.isEmpty()) {
        //         tvBusInfo.setText(namaBus + " • " + armadaNomor);
        //     } else {
        //         tvBusInfo.setText(armadaNomor);
        //     }
        // }

        // Set action bar title dengan nama bus (OPTIONAL)
        if (getSupportActionBar() != null) {
            if (namaBus != null && !namaBus.isEmpty()) {
                getSupportActionBar().setTitle("🚍 " + namaBus);
                getSupportActionBar().setSubtitle(armadaNomor + " • " + ruteNama);
            } else {
                getSupportActionBar().setTitle("🚍 " + armadaNomor);
                getSupportActionBar().setSubtitle(ruteNama);
            }
        }

        // Set penumpang
        updatePenumpangUI();

        // Set status default (Lancar)
        setActiveStatusButton(btnStatusLancar);
    }

    private void setupClickListeners() {
        // Penumpang controls
        btnTambahPenumpang.setOnClickListener(v -> {
            // Haptic feedback
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            tambahPenumpang();
        });

        btnKurangPenumpang.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            kurangPenumpang();
        });

        // Status kondisi
        btnStatusLancar.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            updateKondisi("lancar", btnStatusLancar);
        });

        btnStatusMacet.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            updateKondisi("macet", btnStatusMacet);
        });

        btnStatusMogok.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            updateKondisi("mogok", btnStatusMogok);
        });

        // Akhiri perjalanan
        btnAkhiriPerjalanan.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            showAkhiriDialog();
        });
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("GPS_LOCATION_UPDATE");
        registerReceiver(locationReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(locationReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver already unregistered
        }
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
            Toast.makeText(this, "⚠️ Kapasitas penuh!", Toast.LENGTH_SHORT).show();
        }
    }

    private void kurangPenumpang() {
        if (jumlahPenumpang > 0) {
            jumlahPenumpang--;
            updatePenumpangUI();
            updatePenumpangToService();
            updatePenumpangToServer();
        } else {
            Toast.makeText(this, "⚠️ Penumpang sudah 0", Toast.LENGTH_SHORT).show();
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
    // UPDATE KONDISI BUS
    // ============================================

    private void updateKondisi(String kondisi, LinearLayout activeButton) {
        kondisiTerakhir = kondisi;
        setActiveStatusButton(activeButton);

        // 1. Update ke Firebase via Service (REALTIME)
        updateKondisiToService(kondisi);

        // 2. Update ke MySQL via API (PERSISTENT)
        updateKondisiToServer(kondisi);
    }

    /**
     * Update kondisi ke Firebase via Service
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
     * Update kondisi ke MySQL via API
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
                    String emoji = kondisi.equals("lancar") ? "✓" :
                            kondisi.equals("macet") ? "⚠" : "🔧";
                    Toast.makeText(TrackingActivity.this,
                            emoji + " Status: " + kondisi.toUpperCase(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Perjalanan>> call, Throwable t) {
                // Silent fail
            }
        });
    }

    /**
     * Set active status button dengan animasi alpha
     */
    private void setActiveStatusButton(LinearLayout activeButton) {
        // Reset semua ke inactive (alpha 0.7)
        btnStatusLancar.setAlpha(0.7f);
        btnStatusMacet.setAlpha(0.7f);
        btnStatusMogok.setAlpha(0.7f);

        // Set active button ke full opacity
        activeButton.setAlpha(1.0f);

        // Optional: Animate scale
        activeButton.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(150)
                .withEndAction(() -> {
                    activeButton.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    /**
     * Set active status berdasarkan kondisi string
     */
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
        if (tvSpeed != null && tvSpeed.getVisibility() == View.VISIBLE) {
            tvSpeed.setText(String.format("%.1f km/h", speedKmh));
        }

        if (tvDistance != null && tvDistance.getVisibility() == View.VISIBLE) {
            tvDistance.setText(String.format("%.2f km", totalJarak));
        }

        if (tvDuration != null && tvDuration.getVisibility() == View.VISIBLE) {
            int hours = durasiMenit / 60;
            int minutes = durasiMenit % 60;
            tvDuration.setText(String.format("%dj %dm", hours, minutes));
        }
    }

    // ============================================
    // AKHIRI PERJALANAN
    // ============================================

    private void showAkhiriDialog() {
        // ⭐ Display nama bus di dialog (jika ada)
        String busInfo = namaBus != null && !namaBus.isEmpty() ?
                namaBus + " (" + armadaNomor + ")" : armadaNomor;

        new AlertDialog.Builder(this)
                .setTitle("⬛ Akhiri Perjalanan?")
                .setMessage("Apakah Anda yakin ingin mengakhiri perjalanan ini?\n\n" +
                        "🚍 Bus: " + busInfo + "\n" +
                        "📊 Total Penumpang: " + jumlahPenumpang + "\n" +
                        "📍 Jarak Tempuh: " + String.format("%.2f km", totalJarak))
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
                                "❌ " + apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(TrackingActivity.this,
                            "❌ Gagal mengakhiri perjalanan",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(TrackingActivity.this,
                        "❌ Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handlePerjalananSelesai() {
        // Stop GPS tracking service
        Intent stopIntent = GpsTrackingService.createStopIntent(this);
        startService(stopIntent);

        // Clear preferences
        prefManager.clearPerjalanId();
        prefManager.setTracking(false);

        // Navigate to laporan
        Intent intent = new Intent(TrackingActivity.this, LaporanActivity.class);
        intent.putExtra("perjalanan_id", perjalanId);
        intent.putExtra("nama_bus", namaBus);  // ⭐ Pass nama bus ke laporan
        intent.putExtra("armada_nomor", armadaNomor);
        intent.putExtra("total_penumpang", jumlahPenumpang);
        intent.putExtra("jarak_tempuh", totalJarak);
        intent.putExtra("durasi_menit", durasiMenit);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

        // Success toast
        Toast.makeText(this, "✓ Perjalanan berhasil diakhiri", Toast.LENGTH_SHORT).show();
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnAkhiriPerjalanan.setEnabled(false);
            btnAkhiriPerjalanan.setAlpha(0.5f);
        } else {
            progressBar.setVisibility(View.GONE);
            btnAkhiriPerjalanan.setEnabled(true);
            btnAkhiriPerjalanan.setAlpha(1.0f);
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Peringatan")
                .setMessage("Perjalanan masih aktif!\n\nGunakan tombol 'Akhiri Perjalanan' untuk keluar dengan aman.")
                .setPositiveButton("Mengerti", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(locationReceiver);
        } catch (IllegalArgumentException e) {
            // Already unregistered
        }
    }
}