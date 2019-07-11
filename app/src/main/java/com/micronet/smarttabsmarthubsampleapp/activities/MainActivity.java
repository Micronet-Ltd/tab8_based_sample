/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.smarttabsmarthubsampleapp.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.view.WindowManager;
import com.micronet.smarttabsmarthubsampleapp.R;
import com.micronet.smarttabsmarthubsampleapp.fragments.AboutFragment;
import com.micronet.smarttabsmarthubsampleapp.fragments.Can1OverviewFragment;
import com.micronet.smarttabsmarthubsampleapp.fragments.Can2OverviewFragment;
import com.micronet.smarttabsmarthubsampleapp.fragments.CanBusFramesFragment;
import com.micronet.smarttabsmarthubsampleapp.fragments.CanbusFragment;
import com.micronet.smarttabsmarthubsampleapp.fragments.InputsOutputsLedsFragment;
import com.micronet.smarttabsmarthubsampleapp.fragments.PortsFragment;
import com.micronet.smarttabsmarthubsampleapp.receivers.DeviceStateReceiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SmartSampleApp";

    private static boolean portsAttached = false;
    private static int dockState = -1;

    private DeviceStateReceiver deviceStateReceiver = new DeviceStateReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewPager viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        // Check if tty ports are available
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager != null) {
            HashMap<String, UsbDevice> connectedDevices = usbManager.getDeviceList();
            for (UsbDevice device : connectedDevices.values()) {
                // Check if tty ports are enumerated
                if (device.getProductId() == 773 && device.getVendorId() == 5538) {
                    portsAttached = true;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Receive any dock events or usb events
        IntentFilter filters = new IntentFilter();
        filters.addAction(Intent.ACTION_DOCK_EVENT);
        filters.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filters.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(deviceStateReceiver, filters);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister receiver
        unregisterReceiver(deviceStateReceiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "Configuration changed: " + newConfig.toString());
    }

    @SuppressWarnings("unused")
    public static synchronized boolean areTtyPortsAvailable() {
        return portsAttached;
    }

    public static synchronized void setTtyPortsState(boolean state) {
        portsAttached = state;
    }

    public static synchronized int getDockState() {
        return dockState;
    }

    public static synchronized void setDockState(int state) {
        dockState = state;
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        //adapter.addFragment(new InputOutputsFragment(), "GPIOs");
        adapter.addFragment(new InputsOutputsLedsFragment(), "io+leds");
       // adapter.addFragment(new Can1OverviewFragment(), "Can1");
      //  adapter.addFragment(new Can2OverviewFragment(), "Can2");
      //  adapter.addFragment(new CanBusFramesFragment(), "CanFrames");
        //adapter.addFragment(new J1708Fragment(), "J1708");
        adapter.addFragment(new AboutFragment(), "Info");
        adapter.addFragment(new PortsFragment(), "Ports");
        adapter.addFragment(new CanbusFragment(), "Canbus");
        viewPager.setAdapter(adapter);
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
}
