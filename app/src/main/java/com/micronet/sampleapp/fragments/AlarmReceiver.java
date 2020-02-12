package com.micronet.sampleapp.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | //todo: change to partial_wake_lock (but it is not turn on the screen)
            PowerManager.ACQUIRE_CAUSES_WAKEUP |
            PowerManager.ON_AFTER_RELEASE,"wakeup");
        wl.acquire();
        Toast.makeText(context, "Alarm Triggered!!!", Toast.LENGTH_SHORT).show();
        Log.d("AlarmReceiver", "Intent received");
        wl.release();
    }
}