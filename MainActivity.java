package com.jarvis.automation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_RUNTIME_PERMISSIONS = 100;
    private TextView statusText;

    // Runtime (dangerous) permissions this app needs.
    private final String[] requiredPermissions = buildRequiredPermissions();

    private static String[] buildRequiredPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.RECORD_AUDIO);
        perms.add(Manifest.permission.CALL_PHONE);
        perms.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT);
            perms.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return perms.toArray(new String[0]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        Button grantPermissionsBtn = findViewById(R.id.btnGrantPermissions);
        Button openNotificationAccessBtn = findViewById(R.id.btnNotificationAccess);
        Button startServiceBtn = findViewById(R.id.btnStartService);
        Button stopServiceBtn = findViewById(R.id.btnStopService);

        grantPermissionsBtn.setOnClickListener(v -> requestMissingPermissions());
        openNotificationAccessBtn.setOnClickListener(v -> openNotificationAccessSettings());
        startServiceBtn.setOnClickListener(v -> startAutomationService());
        stopServiceBtn.setOnClickListener(v -> stopAutomationService());

        refreshStatus();
    }

    private void requestMissingPermissions() {
        List<String> missing = new ArrayList<>();
        for (String p : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }
        if (missing.isEmpty()) {
            Toast.makeText(this, "All runtime permissions already granted", Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityCompat.requestPermissions(this, missing.toArray(new String[0]), REQ_RUNTIME_PERMISSIONS);
    }

    private void openNotificationAccessSettings() {
        // WhatsApp auto-reply requires the user to explicitly enable this listener.
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private void startAutomationService() {
        Intent serviceIntent = new Intent(this, BackgroundAutomationService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        setServiceEnabledPref(true);
        refreshStatus();
    }

    private void stopAutomationService() {
        stopService(new Intent(this, BackgroundAutomationService.class));
        setServiceEnabledPref(false);
        refreshStatus();
    }

    private void setServiceEnabledPref(boolean enabled) {
        getSharedPreferences(ServiceStatePrefs.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(ServiceStatePrefs.KEY_SERVICE_ENABLED, enabled)
                .apply();
    }

    private void refreshStatus() {
        boolean allGranted = true;
        for (String p : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        statusText.setText(allGranted
                ? "Runtime permissions: granted\nDon't forget Notification Access for auto-reply."
                : "Runtime permissions: missing. Tap 'Grant Permissions'.");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshStatus();
    }
}
