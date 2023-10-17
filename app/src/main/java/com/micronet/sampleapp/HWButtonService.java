package com.micronet.sampleapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.Toast;

public class HWButtonService extends Service {

    private static final String ACTION_PANIC_BUTTON = "android.intent.action.ACTION_PANIC_BUTTON";
    private static final String ACTION_PANIC_BUTTON_RELEASE = "android.intent.action.ACTION_PANIC_BUTTON_RELEASE";
    private boolean isActionButtonPressed=false;
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PANIC_BUTTON);
        intentFilter.addAction(ACTION_PANIC_BUTTON_RELEASE);
        registerReceiver(mReceiver, intentFilter);
    }



    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            goAsync();
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_PANIC_BUTTON:
                        if (!isActionButtonPressed) {
                            Toast.makeText(context, "Button Pressed", Toast.LENGTH_LONG).show();
                            isActionButtonPressed=true;
                        }
                        break;
                    case ACTION_PANIC_BUTTON_RELEASE:
                        if (isActionButtonPressed) {
                            Toast.makeText(context, "Button Released", Toast.LENGTH_LONG).show();
                            isActionButtonPressed=false;
                        }
                        break;
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}