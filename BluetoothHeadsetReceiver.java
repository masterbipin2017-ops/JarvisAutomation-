package com.jarvis.automation;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.content.ContextCompat;

/**
 * Listens for ACL connect/disconnect events and forwards a filtered trigger
 * to BackgroundAutomationService when the *specific* configured headset
 * (see VipConfig.TRUSTED_DEVICE_NAME) connects or disconnects.
 *
 * Filtering by name/address (rather than reacting to any Bluetooth device)
 * avoids accidentally arming voice control / calling from an untrusted device.
 */
public class BluetoothHeadsetReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (!isTrustedDevice(context, device)) {
            return; // ignore every other Bluetooth device
        }

        Intent serviceIntent = new Intent(context, BackgroundAutomationService.class);

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            serviceIntent.setAction(Actions.ACTION_HEADSET_CONNECTED);
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            serviceIntent.setAction(Actions.ACTION_HEADSET_DISCONNECTED);
        } else {
            return;
        }

        ContextCompat.startForegroundService(context, serviceIntent);
    }

    private boolean isTrustedDevice(Context context, BluetoothDevice device) {
        if (device == null) return false;
        // BLUETOOTH_CONNECT is required on API 31+ to read device.getName().
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.BLUETOOTH_CONNECT)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        String name = device.getName();
        String address = device.getAddress();
        return VipConfig.TRUSTED_DEVICE_NAME.equalsIgnoreCase(name)
                || VipConfig.TRUSTED_DEVICE_ADDRESS.equalsIgnoreCase(address);
    }
}
