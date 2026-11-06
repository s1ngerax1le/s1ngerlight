package com.onyx.decoled;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            LedController controller = new LedController(context);
            if (controller.isBootSync()) {
                controller.applyState();
            }
        }
    }
}
