package com.example.buskrutracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.buskrutracker.R;

/**
 * LaporanActivity - Laporan Selesai Perjalanan
 * Design: Minimalist white dengan success icon, summary card
 * Mengikuti design UI/UX dari HTML mockup
 */
public class LaporanActivity extends AppCompatActivity {

    private TextView tvTotalPenumpang, tvJarakTempuh, tvDurasi;
    private Button btnKembaliHome;
    private CardView successCard;

    private int totalPenumpang;
    private double jarakTempuh;
    private int durasiMenit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_laporan);

        // Hide action bar untuk full screen experience
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Get data dari intent
        totalPenumpang = getIntent().getIntExtra("total_penumpang", 42);
        jarakTempuh = getIntent().getDoubleExtra("jarak_tempuh", 145.0);
        durasiMenit = getIntent().getIntExtra("durasi_menit", 195); // 3 jam 15 menit

        // Initialize views
        initViews();

        // Display data
        displayLaporan();

        // Setup interactions
        setupClickListeners();

        // Animate entrance
        animateEntrance();
    }

    private void initViews() {
        tvTotalPenumpang = findViewById(R.id.tv_total_penumpang);
        tvJarakTempuh = findViewById(R.id.tv_jarak_tempuh);
        tvDurasi = findViewById(R.id.tv_durasi);
        btnKembaliHome = findViewById(R.id.btn_kembali_home);
        successCard = findViewById(R.id.success_icon_card);
    }

    private void displayLaporan() {
        // Total Penumpang
        tvTotalPenumpang.setText(totalPenumpang + " Orang");

        // Jarak Tempuh - format dengan 0 desimal
        tvJarakTempuh.setText(String.format("%.0f KM", jarakTempuh));

        // Durasi - format jam dan menit
        int hours = durasiMenit / 60;
        int minutes = durasiMenit % 60;
        tvDurasi.setText(String.format("%d Jam %d Mnt", hours, minutes));
    }

    private void setupClickListeners() {
        btnKembaliHome.setOnClickListener(v -> {
            // Animate button press
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .withEndAction(this::kembaliKeHome)
                                .start();
                    })
                    .start();
        });
    }

    private void animateEntrance() {
        // Animate success card dengan scale
        if (successCard != null) {
            successCard.setScaleX(0f);
            successCard.setScaleY(0f);
            successCard.setAlpha(0f);
            successCard.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(200)
                    .start();
        }

        // Animate content dengan fade in dari bawah
        View contentLayout = findViewById(R.id.summary_card);
        if (contentLayout != null) {
            contentLayout.setTranslationY(50);
            contentLayout.setAlpha(0f);
            contentLayout.animate()
                    .translationY(0)
                    .alpha(1f)
                    .setDuration(600)
                    .setStartDelay(400)
                    .start();
        }

        // Animate button
        if (btnKembaliHome != null) {
            btnKembaliHome.setTranslationY(50);
            btnKembaliHome.setAlpha(0f);
            btnKembaliHome.animate()
                    .translationY(0)
                    .alpha(1f)
                    .setDuration(600)
                    .setStartDelay(600)
                    .start();
        }
    }

    private void kembaliKeHome() {
        // Kembali ke halaman persiapan (home)
        Intent intent = new Intent(LaporanActivity.this, PersiapanActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent back, redirect to home instead
        kembaliKeHome();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup jika diperlukan
    }
}