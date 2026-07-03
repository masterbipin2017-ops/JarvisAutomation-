package com.jarvis.automation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.content.ContextCompat;

/**
 * Restarts the automation service after a reboot, but ONLY if the user had
 * explicitly started it before (tracked via ServiceStatePrefs). We never
 * auto-start background services a user hasn't opted into.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences(
                ServiceStatePrefs.PREFS_NAME, Context.MODE_PRIVATE);
        boolean wasRunning = prefs.getBoolean(ServiceStatePrefs.KEY_SERVICE_ENABLED, false);
        if (wasRunning) {
            ContextCompat.startForegroundService(context,
                    new Intent(context, BackgroundAutomationService.class));
        }
    }
}
