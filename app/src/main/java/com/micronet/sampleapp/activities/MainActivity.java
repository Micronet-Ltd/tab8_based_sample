/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.sampleapp.activities;

import static java.lang.Character.isDigit;
import static java.lang.Character.isUpperCase;

import android.content.Context;
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
import android.widget.Toast;
import com.micronet.sampleapp.R;
import com.micronet.sampleapp.fragments.AboutFragment;
import com.micronet.sampleapp.fragments.CanbusFragment;
import com.micronet.sampleapp.fragments.InputsOutputsLedsFragment;
import com.micronet.sampleapp.fragments.PortsFragment;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SmartSampleApp";
    private static int dockState = -1;
    public static int actionId = 0;
    private CameraManager camManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewPager viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && actionId !=0) {
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

//        switch (keyCode) {
//            case KeyEvent.KEYCODE_VOLUME_UP:
//                if (event.isTracking() && !event.isCanceled()) {
//                    switch (actionId) {
//                        case 0:
//                            //todo
//                            Log.d("AAA",  "action position = 0");
//                            break;
//                        case 1:
//                            //todo
//                            Log.d("AAA",  "action position = 1");
//                            break;
//                        case 2:
//                            //todo
//                            Log.d("AAA",  "action position = 2");
//                            break;
//                    }
//                }
//                return true;
//        }
//        return super.onKeyUp(keyCode, event);
    }

//    @Override
//    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
//        return false;
//    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        adapter.addFragment(new InputsOutputsLedsFragment(), "io+leds");
        adapter.addFragment(new AboutFragment(), "Info");
        adapter.addFragment(new PortsFragment(), "Ports+other");
        adapter.addFragment(new CanbusFragment(), "Canbus");
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
//        Log.d("AAAA", Arrays.toString(result));

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
}
