package com.upivoice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Service auto-restarts via NotificationListenerService binding
            // This receiver ensures the app is registered after reboot
        }
    }
}
