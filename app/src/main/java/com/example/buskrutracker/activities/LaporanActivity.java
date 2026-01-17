package com.example.buskrutracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.buskrutracker.R;

/**
 * LaporanActivity - Laporan Selesai Perjalanan
 * Design: White clean dengan summary card
 */
public class LaporanActivity extends AppCompatActivity {

    private TextView tvTotalPenumpang, tvJarakTempuh, tvDurasi;
    private Button btnKembaliHome;

    private int totalPenumpang;
    private double jarakTempuh;
    private int durasiMenit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_laporan);

        // Get data dari intent
        totalPenumpang = getIntent().getIntExtra("total_penumpang", 0);
        jarakTempuh = getIntent().getDoubleExtra("jarak_tempuh", 0.0);
        durasiMenit = getIntent().getIntExtra("durasi_menit", 0);

        // Initialize
        initViews();
        displayLaporan();
        setupClickListeners();
    }

    private void initViews() {
        tvTotalPenumpang = findViewById(R.id.tv_total_penumpang);
        tvJarakTempuh = findViewById(R.id.tv_jarak_tempuh);
        tvDurasi = findViewById(R.id.tv_durasi);
        btnKembaliHome = findViewById(R.id.btn_kembali_home);
    }

    private void displayLaporan() {
        // Total Penumpang
        tvTotalPenumpang.setText(totalPenumpang + " Orang");

        // Jarak Tempuh
        tvJarakTempuh.setText(String.format("%.0f KM", jarakTempuh));

        // Durasi
        int hours = durasiMenit / 60;
        int minutes = durasiMenit % 60;
        tvDurasi.setText(String.format("%d Jam %d Mnt", hours, minutes));
    }

    private void setupClickListeners() {
        btnKembaliHome.setOnClickListener(v -> kembaliKeHome());
    }

    private void kembaliKeHome() {
        Intent intent = new Intent(LaporanActivity.this, PersiapanActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Redirect to home
        kembaliKeHome();
    }
}