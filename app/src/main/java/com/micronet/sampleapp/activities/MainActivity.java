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
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import com.micronet.sampleapp.R;
import com.micronet.sampleapp.fragments.AboutFragment;
import com.micronet.sampleapp.fragments.CanbusFragment;
import com.micronet.sampleapp.fragments.InputsOutputsLedsFragment;
import com.micronet.sampleapp.fragments.PortsFragment;

import java.lang.reflect.Method;
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
    public static int actionId = 1;
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
                        if (aboutFragment.isAdded()) {
                            aboutFragment.updateInfoText();
                        }
                        if (portsFragment.isAdded()) {
                            portsFragment.updatePorts();
                        }
                        if (canbusFragment.isAdded()) {
                            canbusFragment.updateCanbus();
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
        if ((keyCode == KeyEvent.KEYCODE_F1 && MainActivity.getBoardType() < 2) || (keyCode == KeyEvent.KEYCODE_WINDOW && MainActivity.getBoardType() >= 2)) {
            event.startTracking();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_F1 && MainActivity.getBoardType() < 2) || (keyCode == KeyEvent.KEYCODE_WINDOW && MainActivity.getBoardType() >= 2)) {
            if (event.isTracking() && !event.isCanceled()) {
                switch (actionId) {
                    case 0:
                        setFlashLight(true);
                        break;
                    case 1:
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
    protected void onStop() {
        unregisterReceiver(mReceiver);
        super.onStop();
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

    public static int getBoardType() {
        int ret;
        String s = getSystemProperty("hw.board.id");
        if (s == null || s.equals(""))
            return 0;
        ret = Integer.parseInt(s);
        Log.d(TAG, "board id: " + ret);
        return ret;
    }

    public static int getDeviceType() {
        int boardType = getBoardType();

        if (dockState == Intent.EXTRA_DOCK_STATE_UNDOCKED || dockState == -1) {
            return SMARTTAB_STAND_ALONE;
        }
        switch (boardType) {
            case 0:
                return SMARTTAB_CRADLE_ENHANCED;
            case 1:
                return SMARTTAB_CRADLE_BASIC;
            case 2:
                return SMARTCAM_BASIC;
            case 6:
                return SMARTCAM_ENHANCED;
        }
        return -1;
    }

    public static String getDevTypeMessage(int devType) {

        switch (devType) {
            case 0:
                return "Tab8 standalone";
            case 1:
                return "Tab8-LowCost";
            case 2:
                return "TAB8 LTE";
            case 3:
                return "SmartCam (basic)";
            case 4:
                return "SmartCam (full)";
        }
        return "Unknown";
    }

    public static String getSystemProperty(String propName) {
        String propValue = "";
        try {
            @SuppressWarnings("rawtypes")
            Class sp = Class.forName("android.os.SystemProperties");
            Class SystemProperties = sp;
            Method getProp = SystemProperties.getMethod("get", new Class[]{String.class});
            propValue = getProp.invoke(SystemProperties, new Object[]{propName}).toString();
        } catch (IllegalArgumentException iAE) {
            throw iAE;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return propValue;
    }

    public static void setViewAndChildrenEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                setViewAndChildrenEnabled(child, enabled);
            }
        }
    }
}
