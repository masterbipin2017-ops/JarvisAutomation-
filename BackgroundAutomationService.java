package com.jarvis.automation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Persistent foreground service. Owns:
 *  - the sticky notification that keeps the process alive
 *  - shake-to-torch detection (accelerometer)
 *  - the voice command manager (started/stopped by Bluetooth trigger)
 *
 * This is a normal, user-initiated foreground service — it only runs after the
 * user explicitly taps "Start" in MainActivity, and shows a persistent notification
 * the whole time it's alive, as required by Android 8+.
 */
public class BackgroundAutomationService extends Service implements SensorEventListener {

    private static final String TAG = "JarvisService";
    private static final String CHANNEL_ID = "jarvis_automation_channel";
    private static final int NOTIFICATION_ID = 42;

    // Shake detection tuning
    private static final float SHAKE_THRESHOLD_G = 2.7f; // in units of g
    private static final int SHAKE_DEBOUNCE_MS = 1200;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTimestamp = 0;
    private boolean torchOn = false;

    private VoiceCommandManager voiceCommandManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification("Jarvis is active"));

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        voiceCommandManager = new VoiceCommandManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case Actions.ACTION_HEADSET_CONNECTED:
                    onHeadsetConnected();
                    break;
                case Actions.ACTION_HEADSET_DISCONNECTED:
                    onHeadsetDisconnected();
                    break;
                default:
                    break;
            }
        }
        // START_STICKY: ask the OS to recreate the service with a null intent
        // if it's killed under memory pressure, rather than treating this as
        // "immune to Android's memory model" (that's not something any app can be).
        return START_STICKY;
    }

    private void onHeadsetConnected() {
        updateNotification("VIP headset connected — voice control armed");
        voiceCommandManager.startListening();
    }

    private void onHeadsetDisconnected() {
        updateNotification("Jarvis is active");
        voiceCommandManager.stopListening();
    }

    // ---- Shake-to-torch ----

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        double gForce = Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;

        if (gForce > SHAKE_THRESHOLD_G) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTimestamp > SHAKE_DEBOUNCE_MS) {
                lastShakeTimestamp = now;
                toggleTorch();
            }
        }
    }

    private void toggleTorch() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) return;
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds.length == 0) return;
            String camId = cameraIds[0];
            torchOn = !torchOn;
            cameraManager.setTorchMode(camId, torchOn);
            Log.d(TAG, "Torch toggled: " + torchOn);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to toggle torch", e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* no-op */ }

    // ---- Notification plumbing ----

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Jarvis Automation", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps Jarvis automation running in the background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Jarvis")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        if (voiceCommandManager != null) voiceCommandManager.stopListening();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // not a bound service
    }
}
