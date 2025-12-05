package com.parental.callrecorder;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String PREFS_NAME = "CallRecorderPrefs";
    private static final String PREF_MONITORING_ACTIVE = "monitoring_active";
    
    private TextView statusText;
    private Button startButton;
    private SharedPreferences prefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        statusText = findViewById(R.id.statusText);
        startButton = findViewById(R.id.startButton);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Έλεγχος αν το monitoring είναι ήδη ενεργό
        if (isMonitoringActive()) {
            startMonitoringService();
            finish(); // Κλείνει το UI αμέσως
            return;
        }
        
        setupUI();
        checkDeviceCapability();
    }
    
    private void setupUI() {
        startButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                startMonitoring();
            } else {
                requestPermissions();
            }
        });
    }
    
    private void startMonitoring() {
        // Αποθήκευση ότι το monitoring είναι ενεργό
        prefs.edit().putBoolean(PREF_MONITORING_ACTIVE, true).apply();
        
        // Έναρξη service
        startMonitoringService();
        
        // Μήνυμα επιτυχίας
        Toast.makeText(this, "✅ Monitoring started! App will run in background.", Toast.LENGTH_LONG).show();
        
        // Κλείνει το UI
        finish();
    }
    
    private void startMonitoringService() {
        Intent serviceIntent = new Intent(this, CallRecordingService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
    
    private boolean isMonitoringActive() {
        return prefs.getBoolean(PREF_MONITORING_ACTIVE, false);
    }
    
    private boolean checkPermissions() {
        String[] requiredPermissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.FOREGROUND_SERVICE
        };
        
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    private void requestPermissions() {
        String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.POST_NOTIFICATIONS
        };
        
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }
    
    private void checkDeviceCapability() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String model = Build.MODEL;
        int sdk = Build.VERSION.SDK_INT;
        
        String message = "📱 Device: " + manufacturer + " " + model + 
                        "\n🤖 Android: " + sdk + " (API " + Build.VERSION.SDK_INT + ")" +
                        "\n\n⚠️ Note: Call recording may be limited on Android 10+" +
                        "\n\nClick 'Start Monitoring' to begin. App will run in background.";
        
        statusText.setText(message);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                startMonitoring();
            } else {
                Toast.makeText(this, "❌ Permissions denied. App cannot function.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
