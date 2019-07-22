/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.sampleapp.fragments;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.widget.Toast;
import com.micronet.sampleapp.BuildConfig;
import com.micronet.sampleapp.R;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AboutFragment extends Fragment implements OnClickListener {

    private static final String TAG = "AboutFragment";
    private View rootView;
    private static final String PROP_MCU_VERSION = "hw.build.version.mcu";
    private static final String PROP_FPGA_VERSION = "hw.build.version.fpga";
    EditText waitTime;
    Button setAlarm;
    String waitingTimeInMinutes = "0";
    public static final int REQUEST_CODE = 101;


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
        txtAbout.setText(String.format("Tab8 Sample App v %s\n" +
            "Copyright © 2018 Micronet Inc.\n", BuildConfig.VERSION_NAME));

        updateInfoText();
        waitTime = rootView.findViewById(R.id.waitingTime);
        setAlarm = rootView.findViewById(R.id.setAlarm);
        setAlarm.setOnClickListener(this);
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


    Runnable updateTextRunnable = new Runnable() {
        @Override
        public void run() {
            updateInfoText();
        }
    };

    @SuppressLint("HardwareIds")
    public void updateInfoText() {

        String mcuVersion = ((getSystemProperty(PROP_MCU_VERSION).equalsIgnoreCase("Unknown")) ? "Unknown" : getSystemProperty(PROP_MCU_VERSION));
        String fpgaVersion = ((getSystemProperty(PROP_FPGA_VERSION).equalsIgnoreCase("Unknown")) ? "Unknown"
            : getFPGAVersion(getSystemProperty(PROP_FPGA_VERSION)));
        TextView txtDeviceInfo = rootView.findViewById(R.id.txtDeviceInfo);
        txtDeviceInfo.setText(String.format("OS Version: %s \n" +
                "MCU Version: %s\n" +
                "FPGA Version: %s\n" +
                "Android Build Version: %s\n" +
                "Device Model: %s\n" +
                "Serial: %s\n",
            Build.DISPLAY, mcuVersion, fpgaVersion, Build.VERSION.RELEASE, Build.MODEL,
            Build.SERIAL));
    }

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

    private static String getFPGAVersion(String hex) {
        String s = "";
        s = s + (char) Integer.parseInt(hex.substring(0, 2), 16) + ".";
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


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.setAlarm) {
            Log.d(TAG, "Set Alarm");
            if (TextUtils.isEmpty(waitTime.getText().toString())) {
                Toast.makeText(getContext(), "Enter number of minutes", Toast.LENGTH_LONG).show();
            } else {
                waitingTimeInMinutes = waitTime.getText().toString();
                long waitMillisec = Integer.parseInt(waitingTimeInMinutes) * 60 * 1000;
                AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(getContext().ALARM_SERVICE);
                Intent intent = new Intent(getContext(), AlarmReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                //alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + waitMillisec, pendingIntent);
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(System.currentTimeMillis() + waitMillisec, pendingIntent), pendingIntent);
            }

        }
    }

}
