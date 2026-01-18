package com.example.buskrutracker;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class BusTrackerApplication extends Application {

    private static final String TAG = "BusTrackerApp";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this);

            // Enable offline persistence
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);

            Log.d(TAG, "Application initialized");

        } catch (Exception e) {
            Log.e(TAG, "Initialization error: " + e.getMessage());
        }
    }
}