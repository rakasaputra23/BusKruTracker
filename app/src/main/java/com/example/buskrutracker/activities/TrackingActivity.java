package com.example.buskrutracker.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import com.example.buskrutracker.models.SelesaiPerjalananResponse;
import com.example.buskrutracker.services.GpsTrackingService;
import com.example.buskrutracker.utils.SharedPrefManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * TrackingActivity
 *
 * ⭐ FIX 1 — totalPassengersBoarded disimpan ke SharedPreferences
 *   agar tidak reset ke 0 saat activity di-recreate (rotasi layar,
 *   app ke background, dll). Ini penyebab utama total_penumpang_naik
 *   selalu 0 di database.
 *
 * ⭐ FIX 2 — akhiriPerjalanan() sekarang pakai
 *   Call<ApiResponse<SelesaiPerjalananResponse>> sehingga summary
 *   (total_pendapatan, tarif_per_orang, penumpang_naik) bisa dibaca
 *   dan diteruskan ke LaporanActivity via intent.
 *
 * KEY SharedPreferences yang dipakai:
 *   "boarded_<perjalanId>"  → int, akumulasi penumpang naik
 */
public class TrackingActivity extends AppCompatActivity {

    // Nama file SharedPreferences khusus tracking
    private static final String PREF_TRACKING = "tracking_state";

    private TextView tvOrigin, tvDestination;
    private TextView tvJumlahPenumpang, tvKapasitas;
    private TextView tvSpeed, tvDistance, tvDuration;

    private LinearLayout btnTambahPenumpang, btnKurangPenumpang;
    private LinearLayout btnStatusLancar, btnStatusMacet, btnStatusMogok;
    private LinearLayout btnAkhiriPerjalanan;

    private CardView cardStatusLancar, cardStatusMacet, cardStatusMogok;
    private ProgressBar progressBar;

    private ApiService apiService;
    private SharedPrefManager prefManager;
    private SharedPreferences trackingPrefs;   // ⭐ untuk simpan totalPassengersBoarded

    // Data dari intent
    private int perjalanId;
    private String namaBus;
    private String armadaNomor;
    private String ruteNama;
    private String origin      = "SBY";
    private String destination = "MADIUN";

    // State penumpang
    private int jumlahPenumpang    = 0;   // penumpang saat ini di dalam bus
    private int kapasitas          = 40;
    private String kondisiTerakhir = "lancar";

    // ⭐ Akumulasi boarding — disimpan ke SharedPreferences
    private int totalPassengersBoarded = 0;

    // Stats dari GPS service
    private double totalJarak  = 0.0;
    private int    durasiMenit = 0;
    private float  speedKmh   = 0.0f;

    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            speedKmh    = intent.getFloatExtra("speed", 0);
            totalJarak  = intent.getDoubleExtra("distance", 0);
            int updateCount = intent.getIntExtra("update_count", 0);
            durasiMenit = updateCount / 12;
            updateLocationUI();
        }
    };

    // ============================================
    // LIFECYCLE
    // ============================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        perjalanId  = getIntent().getIntExtra("perjalanan_id", 0);
        namaBus     = getIntent().getStringExtra("nama_bus");
        armadaNomor = getIntent().getStringExtra("armada_nomor");
        ruteNama    = getIntent().getStringExtra("rute_nama");

        parseRuteNama();
        initViews();
        initServices();

        // ⭐ Restore totalPassengersBoarded dari SharedPreferences
        restoreBoardedCount();

        setupUI();
        setupClickListeners();
        loadPerjalanAktif();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(locationReceiver, new IntentFilter("GPS_LOCATION_UPDATE"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(locationReceiver); }
        catch (IllegalArgumentException ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(locationReceiver); }
        catch (IllegalArgumentException ignored) {}
    }

    // ============================================
    // INIT
    // ============================================

    private void parseRuteNama() {
        if (ruteNama == null) return;
        String sep = ruteNama.contains("→") ? "→" : "-";
        if (ruteNama.contains(sep)) {
            String[] parts = ruteNama.split(sep);
            if (parts.length >= 2) {
                origin      = parts[0].trim().toUpperCase();
                destination = parts[1].trim().toUpperCase();
            }
        }
    }

    private void initViews() {
        tvOrigin      = findViewById(R.id.tv_origin);
        tvDestination = findViewById(R.id.tv_destination);

        tvJumlahPenumpang = findViewById(R.id.tv_jumlah_penumpang);
        tvKapasitas       = findViewById(R.id.tv_kapasitas);

        tvSpeed    = findViewById(R.id.tv_speed);
        tvDistance = findViewById(R.id.tv_distance);
        tvDuration = findViewById(R.id.tv_duration);

        btnTambahPenumpang = findViewById(R.id.btn_tambah_penumpang);
        btnKurangPenumpang = findViewById(R.id.btn_kurang_penumpang);
        btnStatusLancar    = findViewById(R.id.btn_status_lancar);
        btnStatusMacet     = findViewById(R.id.btn_status_macet);
        btnStatusMogok     = findViewById(R.id.btn_status_mogok);
        btnAkhiriPerjalanan = findViewById(R.id.btn_akhiri_perjalanan);
        progressBar         = findViewById(R.id.progress_bar);
    }

    private void initServices() {
        apiService    = RetrofitClient.getApiService();
        prefManager   = SharedPrefManager.getInstance(this);
        trackingPrefs = getSharedPreferences(PREF_TRACKING, Context.MODE_PRIVATE);
    }

    // ============================================
    // ⭐ PERSIST BOARDED COUNT
    // ============================================

    /**
     * Simpan totalPassengersBoarded ke SharedPreferences.
     * Key pakai perjalanId supaya tidak bocor ke trip lain.
     */
    private void saveBoardedCount() {
        trackingPrefs.edit()
                .putInt("boarded_" + perjalanId, totalPassengersBoarded)
                .apply();
    }

    /**
     * Restore totalPassengersBoarded dari SharedPreferences.
     * Dipanggil di onCreate — kalau activity recreate, nilai tidak hilang.
     */
    private void restoreBoardedCount() {
        totalPassengersBoarded = trackingPrefs.getInt("boarded_" + perjalanId, 0);
    }

    /**
     * Hapus data setelah perjalanan selesai supaya tidak menumpuk.
     */
    private void clearBoardedCount() {
        trackingPrefs.edit()
                .remove("boarded_" + perjalanId)
                .apply();
    }

    // ============================================
    // SETUP UI
    // ============================================

    private void setupUI() {
        tvOrigin.setText(origin);
        tvDestination.setText(destination);

        if (getSupportActionBar() != null) {
            String title = (namaBus != null && !namaBus.isEmpty()) ? namaBus : armadaNomor;
            getSupportActionBar().setTitle("🚍 " + title);
            getSupportActionBar().setSubtitle(armadaNomor + " • " + ruteNama);
        }

        updatePenumpangUI();
        setActiveStatusButton(btnStatusLancar);
    }

    private void setupClickListeners() {
        btnTambahPenumpang.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            tambahPenumpang();
        });
        btnKurangPenumpang.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            kurangPenumpang();
        });
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
        btnAkhiriPerjalanan.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            showAkhiriDialog();
        });
    }

    // ============================================
    // LOAD PERJALANAN AKTIF
    // ============================================

    private void loadPerjalanAktif() {
        String token = prefManager.getToken();

        apiService.getPerjalananAktif(token).enqueue(new Callback<ApiResponse<Perjalanan>>() {
            @Override
            public void onResponse(Call<ApiResponse<Perjalanan>> call,
                                   Response<ApiResponse<Perjalanan>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {

                    Perjalanan p = response.body().getData();
                    jumlahPenumpang = p.getTotalPenumpang();
                    kondisiTerakhir = p.getKondisiTerakhir() != null
                            ? p.getKondisiTerakhir() : "lancar";

                    updatePenumpangUI();
                    setActiveStatusByKondisi(kondisiTerakhir);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Perjalanan>> call, Throwable t) { }
        });
    }

    // ============================================
    // PENUMPANG COUNTER
    // ============================================

    private void tambahPenumpang() {
        if (jumlahPenumpang < kapasitas) {
            jumlahPenumpang++;

            // ⭐ Tambah boarding counter DAN langsung simpan ke SharedPreferences
            totalPassengersBoarded++;
            saveBoardedCount();

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

            // ⭐ TIDAK mengurangi totalPassengersBoarded —
            //   penumpang turun ≠ batal naik. Counter tetap.

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
        startService(GpsTrackingService.createPassengerUpdateIntent(
                this, perjalanId, jumlahPenumpang));
    }

    private void updatePenumpangToServer() {
        String token = prefManager.getToken();
        Map<String, Integer> data = new HashMap<>();
        data.put("perjalanan_id",  perjalanId);
        data.put("total_penumpang", jumlahPenumpang);

        apiService.updatePenumpang(token, data).enqueue(new Callback<ApiResponse<Perjalanan>>() {
            @Override
            public void onResponse(Call<ApiResponse<Perjalanan>> call,
                                   Response<ApiResponse<Perjalanan>> response) { }
            @Override
            public void onFailure(Call<ApiResponse<Perjalanan>> call, Throwable t) { }
        });
    }

    // ============================================
    // UPDATE KONDISI BUS
    // ============================================

    private void updateKondisi(String kondisi, LinearLayout activeButton) {
        kondisiTerakhir = kondisi;
        setActiveStatusButton(activeButton);
        startService(GpsTrackingService.createKondisiUpdateIntent(this, perjalanId, kondisi));

        String token = prefManager.getToken();
        Map<String, Object> data = new HashMap<>();
        data.put("perjalanan_id", perjalanId);
        data.put("kondisi",       kondisi);

        apiService.updateKondisi(token, data).enqueue(new Callback<ApiResponse<Perjalanan>>() {
            @Override
            public void onResponse(Call<ApiResponse<Perjalanan>> call,
                                   Response<ApiResponse<Perjalanan>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    String emoji = kondisi.equals("lancar") ? "✓"
                            : kondisi.equals("macet") ? "⚠" : "🔧";
                    Toast.makeText(TrackingActivity.this,
                            emoji + " Status: " + kondisi.toUpperCase(),
                            Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Perjalanan>> call, Throwable t) { }
        });
    }

    private void setActiveStatusButton(LinearLayout activeButton) {
        btnStatusLancar.setAlpha(0.7f);
        btnStatusMacet.setAlpha(0.7f);
        btnStatusMogok.setAlpha(0.7f);
        activeButton.setAlpha(1.0f);
        activeButton.animate()
                .scaleX(1.05f).scaleY(1.05f).setDuration(150)
                .withEndAction(() -> activeButton.animate()
                        .scaleX(1.0f).scaleY(1.0f).setDuration(150).start())
                .start();
    }

    private void setActiveStatusByKondisi(String kondisi) {
        switch (kondisi.toLowerCase()) {
            case "macet":  setActiveStatusButton(btnStatusMacet);  break;
            case "mogok":  setActiveStatusButton(btnStatusMogok);  break;
            default:       setActiveStatusButton(btnStatusLancar); break;
        }
    }

    // ============================================
    // UPDATE LOCATION UI
    // ============================================

    private void updateLocationUI() {
        if (tvSpeed != null && tvSpeed.getVisibility() == View.VISIBLE)
            tvSpeed.setText(String.format("%.1f km/h", speedKmh));
        if (tvDistance != null && tvDistance.getVisibility() == View.VISIBLE)
            tvDistance.setText(String.format("%.2f km", totalJarak));
        if (tvDuration != null && tvDuration.getVisibility() == View.VISIBLE)
            tvDuration.setText(String.format("%dj %dm", durasiMenit / 60, durasiMenit % 60));
    }

    // ============================================
    // AKHIRI PERJALANAN
    // ============================================

    private void showAkhiriDialog() {
        String busInfo = (namaBus != null && !namaBus.isEmpty())
                ? namaBus + " (" + armadaNomor + ")" : armadaNomor;

        new AlertDialog.Builder(this)
                .setTitle("⬛ Akhiri Perjalanan?")
                .setMessage("Apakah Anda yakin ingin mengakhiri perjalanan ini?\n\n"
                        + "🚍 Bus: "             + busInfo               + "\n"
                        + "👥 Penumpang naik: "  + totalPassengersBoarded + " orang\n"
                        + "🪑 Saat ini di bus: " + jumlahPenumpang        + " orang\n"
                        + "📍 Jarak tempuh: "    + String.format("%.2f km", totalJarak))
                .setPositiveButton("Ya, Akhiri", (dialog, which) -> akhiriPerjalanan())
                .setNegativeButton("Batal", null)
                .show();
    }

    /**
     * ⭐ FIXED:
     *  1. Kirim totalPassengersBoarded (yang sudah di-persist) sebagai total_penumpang_naik
     *  2. Gunakan SelesaiPerjalananResponse untuk baca summary dari server
     *  3. Teruskan summary ke LaporanActivity via intent
     */
    private void akhiriPerjalanan() {
        setLoading(true);

        String token = prefManager.getToken();

        Map<String, Object> data = new HashMap<>();
        data.put("perjalanan_id",        perjalanId);
        data.put("total_penumpang",      jumlahPenumpang);
        data.put("total_penumpang_naik", totalPassengersBoarded);  // ⭐ nilai yang benar
        data.put("jarak_tempuh",         totalJarak);
        data.put("catatan",              "Perjalanan selesai");

        // ⭐ Tipe diubah ke SelesaiPerjalananResponse
        apiService.selesaiPerjalanan(token, data)
                .enqueue(new Callback<ApiResponse<SelesaiPerjalananResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<SelesaiPerjalananResponse>> call,
                                           Response<ApiResponse<SelesaiPerjalananResponse>> response) {
                        setLoading(false);

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getData() != null) {

                            SelesaiPerjalananResponse resp = response.body().getData();
                            handlePerjalananSelesai(resp);

                        } else {
                            String msg = (response.body() != null)
                                    ? response.body().getMessage()
                                    : "Gagal mengakhiri perjalanan";
                            Toast.makeText(TrackingActivity.this,
                                    "❌ " + msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<SelesaiPerjalananResponse>> call,
                                          Throwable t) {
                        setLoading(false);
                        Toast.makeText(TrackingActivity.this,
                                "❌ Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handlePerjalananSelesai(SelesaiPerjalananResponse resp) {
        // ⭐ Bersihkan counter yang disimpan di SharedPreferences
        clearBoardedCount();

        // Hentikan GPS service
        startService(GpsTrackingService.createStopIntent(this));

        // Bersihkan sesi
        prefManager.clearPerjalanId();
        prefManager.setTracking(false);

        // ⭐ Ambil summary dari response server
        int penumpangNaik      = totalPassengersBoarded;
        double totalPendapatan = 0.0;
        double tarifPerOrang   = 0.0;
        int durasiJam          = durasiMenit / 60;
        int durasiSisa         = durasiMenit % 60;

        if (resp.getSummary() != null) {
            SelesaiPerjalananResponse.Summary s = resp.getSummary();
            penumpangNaik    = s.getPenumpangNaik();
            totalPendapatan  = s.getTotalPendapatan();
            tarifPerOrang    = s.getTarifPerOrang();
            durasiJam        = s.getDurasiJam();
            durasiSisa       = s.getDurasiMenit();
        }

        // Navigasi ke LaporanActivity dengan data lengkap
        Intent intent = new Intent(TrackingActivity.this, LaporanActivity.class);
        intent.putExtra("perjalanan_id",        perjalanId);
        intent.putExtra("nama_bus",             namaBus);
        intent.putExtra("armada_nomor",         armadaNomor);
        intent.putExtra("rute_nama",            ruteNama);
        intent.putExtra("total_penumpang",      jumlahPenumpang);
        intent.putExtra("total_penumpang_naik", penumpangNaik);       // ⭐ boarding count
        intent.putExtra("total_pendapatan",     totalPendapatan);     // ⭐ pendapatan
        intent.putExtra("tarif_per_orang",      tarifPerOrang);       // ⭐ tarif
        intent.putExtra("jarak_tempuh",         totalJarak);
        intent.putExtra("durasi_jam",           durasiJam);
        intent.putExtra("durasi_menit_sisa",    durasiSisa);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "✓ Perjalanan berhasil diakhiri", Toast.LENGTH_SHORT).show();
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnAkhiriPerjalanan.setEnabled(!isLoading);
        btnAkhiriPerjalanan.setAlpha(isLoading ? 0.5f : 1.0f);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Peringatan")
                .setMessage("Perjalanan masih aktif!\n\nGunakan tombol 'Akhiri Perjalanan' untuk keluar dengan aman.")
                .setPositiveButton("Mengerti", null)
                .show();
    }
}