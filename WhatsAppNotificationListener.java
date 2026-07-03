package com.jarvis.automation;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Reads WhatsApp message notifications and sends an automatic reply using the
 * notification's own "Quick Reply" RemoteInput action — the same mechanism
 * WhatsApp itself uses for reply-from-notification.
 *
 * Scope is intentionally narrow:
 *  - only notifications from the WhatsApp package are inspected
 *  - only the reply action already exposed by WhatsApp is used
 *  - no notification content is stored, logged persistently, or sent anywhere
 *    off-device
 *
 * Requires the user to manually enable this listener under
 * Settings > Apps > Special access > Notification access.
 */
public class WhatsAppNotificationListener extends NotificationListenerService {

    private static final String TAG = "WhatsAppAutoReply";
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";
    private static final String REMOTE_INPUT_RESULT_KEY = "key_text_reply"; // WhatsApp's key

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !WHATSAPP_PACKAGE.equals(sbn.getPackageName())) {
            return; // ignore every other app's notifications
        }

        Notification notification = sbn.getNotification();
        if (notification == null || notification.actions == null) return;

        for (Notification.Action action : notification.actions) {
            RemoteInput[] remoteInputs = action.getRemoteInputs();
            if (remoteInputs == null || remoteInputs.length == 0) continue;

            // This is the "Reply" quick-action WhatsApp attaches to message notifications.
            sendAutoReply(action, remoteInputs);
            break;
        }
    }

    private void sendAutoReply(Notification.Action action, RemoteInput[] remoteInputs) {
        try {
            Bundle localBundle = new Bundle();
            for (RemoteInput ri : remoteInputs) {
                localBundle.putCharSequence(ri.getResultKey(), VipConfig.AUTO_REPLY_TEXT);
            }

            Intent localIntent = new Intent();
            Bundle resultsBundle = new Bundle();
            resultsBundle.putAll(localBundle);
            RemoteInput.addResultsToIntent(remoteInputs, localIntent, resultsBundle);

            PendingIntent pendingIntent = action.actionIntent;
            if (pendingIntent != null) {
                pendingIntent.send(this, 0, localIntent);
                Log.d(TAG, "Auto-reply sent");
            }
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Failed to send auto-reply", e);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) { /* no-op */ }
}
