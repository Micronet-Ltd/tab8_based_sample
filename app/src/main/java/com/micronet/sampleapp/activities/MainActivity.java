/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.sampleapp.activities;

import static java.lang.Character.isDigit;
import static java.lang.Character.isUpperCase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.view.KeyEvent;
import com.micronet.sampleapp.R;
import com.micronet.sampleapp.fragments.AboutFragment;
import com.micronet.sampleapp.fragments.CanbusFragment;
import com.micronet.sampleapp.fragments.InputsOutputsLedsFragment;
import com.micronet.sampleapp.fragments.PortsFragment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SmartSampleApp";
    public static final int SMARTTAB_STAND_ALONE = 0;
    public static final int SMARTTAB_CRADLE_BASIC = 1;
    public static final int SMARTTAB_CRADLE_ENHANCED = 2;
    public static final int SMARTCAM_BASIC = 3;
    public static final int SMARTCAM_ENHANCED = 4;
    public static final String vInputAction = "android.intent.action.VINPUTS_CHANGED";
    public static final String dockAction = "android.intent.action.DOCK_EVENT";
    public static int dockState = -1;
    public static int actionId = 0;
    private CameraManager camManager;
    public static int devType = -1;
    public static int cradleType = -1;
    public int mInputNum = -1;
    public int mInputValue = -1;
    InputsOutputsLedsFragment ioLedFragment = new InputsOutputsLedsFragment();
    AboutFragment aboutFragment = new AboutFragment();
    PortsFragment portsFragment = new PortsFragment();
    CanbusFragment canbusFragment = new CanbusFragment();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewPager viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        devType = getDeviceType();
        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(dockAction);
        intentFilter.addAction(vInputAction);
        this.registerReceiver(mReceiver, intentFilter);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            goAsync();
            Log.d(TAG, "dock or input action received");
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case dockAction:
                        dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
                        cradleType = intent.getIntExtra("DockValue", -1);
                        devType = getDeviceType();
                        if (ioLedFragment.isAdded()) {
                            ioLedFragment.updateCradleIgnState();
                            ioLedFragment.updateInputValues();
                            ioLedFragment.updateOutputState();
                        }
                        Log.d(TAG, "Dock event received: " + dockState + ", value: " + getCradleType(cradleType));
                        break;
                    case vInputAction:
                        mInputNum = intent.getIntExtra("VINPUT_NUM", -1);
                        mInputValue = intent.getIntExtra("VINPUT_VALUE", -1);
                        if (ioLedFragment.isAdded()) {
                            ioLedFragment.updateInputState(mInputNum, mInputValue);
                        }
                        break;
                }
            }


        }
    };

    public static String getCradleType(int dockValue) {
        String res = "Basic";
        String temp = Integer.toBinaryString(dockValue);
        if ((temp.length() >= 3) && (temp.charAt(temp.length() - 3) == '1')) {
            res = "Enhanced";
        }
        return res;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("AAAAAAAA key", "keycode: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && actionId != 0) {
            event.startTracking();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event.isTracking() && !event.isCanceled()) {
                switch (actionId) {
                    case 1:
                        setFlashLight(true);
                        break;
                    case 2:
                        setFlashLight(false);
                        break;
                }
                return true;
            }

        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "Configuration changed: " + newConfig.toString());
    }


    public static synchronized int getDockState() {
        return dockState;
    }


    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(ioLedFragment, "io+leds");
        adapter.addFragment(aboutFragment, "Info");
        adapter.addFragment(portsFragment, "Ports+other");
        adapter.addFragment(canbusFragment, "Canbus");
        viewPager.setAdapter(adapter);
    }

    public void setFlashLight(boolean setLight) {
        try {
            camManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
            String cameraId = null; // Usually front camera is at 0 position.
            if (camManager != null) {
                cameraId = camManager.getCameraIdList()[0];
                camManager.setTorchMode(cameraId, setLight); //true = turn on; false = turn off
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, e.toString());
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {

        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        private ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

        private void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }
    }

    public byte[] toBinary(String str) {
        char[] messChar = str.toCharArray();
        byte[] result;
        if (messChar.length == 1) {
            result = new byte[messChar.length];
            char first = '0';
            char second = messChar[0];
            Integer twoDigits = ((charToHex(first) << 4) | charToHex(second));
            result[0] = twoDigits.byteValue();
        } else {
            result = new byte[messChar.length / 2];
            for (int i = 0; i < messChar.length - 1; i = i + 2) {
                char first = messChar[i];
                char second = messChar[i + 1];
                Integer twoDigits = ((charToHex(first) << 4) | charToHex(second));
                result[i / 2] = twoDigits.byteValue();
            }
        }

        return result;
    }

    public int charToHex(char c) {
        int res;
        if (isDigit(c)) {
            res = (int) c - 0x30;
        } else {
            if (isUpperCase(c)) {
                res = 9 + ((int) c - 0x40);
            } else {
                res = 9 + ((int) c - 0x60);
            }
        }
        return res;
    }

    public int getBoardType() {
        int ret = 0;
        ret = Integer.parseInt(getSystemProperty("hw.board.id"));
        return ret;
    }

    public int getDeviceType() {
        int ret = 0;
        int boardType = getBoardType();
        if (dockState == Intent.EXTRA_DOCK_STATE_UNDOCKED) {
            return SMARTTAB_STAND_ALONE;
        }
        switch (boardType) {
            case 0:
                return SMARTTAB_CRADLE_ENHANCED;
            case 1:
                return SMARTTAB_CRADLE_BASIC;
            case 2:
                return SMARTCAM_BASIC;
            case 3:
                return SMARTCAM_ENHANCED;
        }
        return -1;
    }

    //todo change to SystemProperties and remove this function from AboutFragment
    private static String getSystemProperty(String propertyName) {
        String propertyValue = "Unknown";

        try {
            Process getPropProcess = Runtime.getRuntime().exec("getprop " + propertyName);
            BufferedReader osRes = new BufferedReader(new InputStreamReader(getPropProcess.getInputStream()));
            propertyValue = osRes.readLine();
            osRes.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return propertyValue;
    }
}
