/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.sampleapp.fragments;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.micronet.sampleapp.BuildConfig;
import com.micronet.sampleapp.R;

import com.micronet.sampleapp.activities.MainActivity;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AboutFragment extends Fragment {

    private static final String TAG = "AboutFragment";
    private View rootView;
    private static final String PROP_MCU_VERSION = "hw.build.version.mcu";
    private static final String PROP_FPGA_VERSION = "hw.build.version.fpga";

    public AboutFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_about, container, false);
        TextView txtAbout = rootView.findViewById(R.id.txtAppInfo);
        txtAbout.setText(String.format("SC200 Sample App v %s\n" +
            "Copyright © 2021 Micronet Inc.\n", BuildConfig.VERSION_NAME));

        updateInfoText();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @SuppressLint({"HardwareIds", "MissingPermission"})
    public void updateInfoText() {

//        String mcuVersion = ((MainActivity.getSystemProperty(PROP_MCU_VERSION).equalsIgnoreCase("Unknown")) ? "Unknown" : MainActivity.getSystemProperty(PROP_MCU_VERSION));
//        String fpgaVersion = ((MainActivity.getSystemProperty(PROP_FPGA_VERSION).equalsIgnoreCase("Unknown")) ? "Unknown": getFPGAVersion(MainActivity.getSystemProperty(PROP_FPGA_VERSION)));
        TextView txtDeviceInfo = rootView.findViewById(R.id.txtDeviceInfo);
//        Log.d(TAG, "mcu: " + mcuVersion + " fpga: " + fpgaVersion);
        txtDeviceInfo.setText(String.format("OS Version: %s \n" +
                "Android Build Version: %s\n" +
                "Device Model: %s\n" +
                "Serial: %s\n",
            Build.DISPLAY, Build.VERSION.RELEASE, Build.MODEL,
            Build.getSerial()));
    }

    private static String getFPGAVersion(String hex) {
        String s = "";
        s = s + (char) Integer.parseInt(hex.substring(0, 2), 16) + ".";
        if((hex.length() % 2) != 0){ //version contains some symbols
         return "Unknown";
        }
        for (int i = 2; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            s = s + Integer.parseInt(str, 16) + ".";
        }
        return s.substring(0, s.length() - 1);
    }

    /**
     * Get actual ROM (storage)
     */
    private String getRom() {
        int rom = 0;
        String fileName = "/sys/class/block/dm-1/size";
        String line = null;
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
                if (line != null) {
                    rom = ((int) (Long.parseLong(line) / 2000000.0)) + 1;
                }
            }
            bufferedReader.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return Integer.toString(rom) + "GB";
    }




}
