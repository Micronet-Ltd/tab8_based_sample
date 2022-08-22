package com.micronet.sampleapp.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.micronet.sampleapp.R;
import com.micronet.sampleapp.SerialPort;
import com.micronet.sampleapp.activities.MainActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PortsFragment extends Fragment implements OnClickListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = "PortsFragment";
    private View rootView;
    protected SerialPort mSerialPort = null;
    protected OutputStream mOutputStream;
    protected InputStream mInputStream;
    protected ReadThread mReadThread;
    byte[] send;
    String receivedDataValue = null;
    String allReceivedData = "";
    boolean rs232Enabled = true;
    public static MainActivity mainActivity;

    String[] RSPortList = {"/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2", "/dev/ttyUSB3"};
    String[] RSPortListCamEnh = {"/dev/ttyMICRONET_ACCEL"};
    String[] RSPortListBasicCradle = {"/dev/ttyMSM1"};
    String[] baudrateList = {"300", "600", "1200", "1800", "2400", "4800", "9600", "19200", "38400", "57600", "115200"};
    Spinner serialPortRS232;
    Spinner baudrateRS232;
    EditText customDataRS232;
    TextView receivedDataRS232;
    Button sendDataRs232;
    Button clearDataRs232;

    String selectedPort = getRsPort()[0];
    String selectedBaudrate = baudrateList[0];
    ArrayAdapter serialPortAdapterRS232;
    EditText waitTime;
    Button setAlarm;
    String waitingTimeInMinutes = "0";
    public static final int REQUEST_CODE = 101;

    public PortsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStop() {
        closeSerialPort();
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_ports, container, false);
        mainActivity = (MainActivity) getActivity();
        MainActivity.setViewAndChildrenEnabled(rootView.findViewById(R.id.RS232), true);
        enablePort("rs", false);

        // settings for RS232
        serialPortRS232 = rootView.findViewById(R.id.SerialPortListRS232);
        serialPortRS232.setOnItemSelectedListener(this);
        serialPortAdapterRS232 = new ArrayAdapter(getActivity(), R.layout.spinner_item, getRsPort());
        serialPortRS232.setAdapter(serialPortAdapterRS232);

        baudrateRS232 = rootView.findViewById(R.id.baudrateListRS232);
        baudrateRS232.setOnItemSelectedListener(this);
        ArrayAdapter baudrateAdapterRS232 = new ArrayAdapter(getActivity(), R.layout.spinner_item, baudrateList);
        baudrateRS232.setAdapter(baudrateAdapterRS232);

        customDataRS232 = rootView.findViewById(R.id.customDataRS232);
        sendDataRs232 = rootView.findViewById(R.id.sendDataRS232);
        sendDataRs232.setOnClickListener(this);

        receivedDataRS232 = rootView.findViewById(R.id.receivedDataRS232);
        clearDataRs232 = rootView.findViewById(R.id.clearDataRS232);
        clearDataRs232.setOnClickListener(this);

        updatePorts();

        waitTime = rootView.findViewById(R.id.waitingTime);
        setAlarm = rootView.findViewById(R.id.setAlarm);
        setAlarm.setOnClickListener(this);
        return rootView;
    }

    public void updatePorts() {
        rootView.findViewById(R.id.RS232).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.rs232Disabled).setVisibility(View.INVISIBLE);
        if (rs232Enabled) {
            serialPortAdapterRS232 = new ArrayAdapter(getActivity(), R.layout.spinner_item, getRsPort());
            serialPortRS232.setAdapter(serialPortAdapterRS232);
            selectedPort = getRsPort()[0];
            selectedBaudrate = baudrateList[0];
        }
        if (mSerialPort == null) {
            openSerialPort(selectedPort);
            if (mSerialPort != null) {
                mSerialPort.config(Integer.parseInt(selectedBaudrate));
                mReadThread = new ReadThread();
                mReadThread.start();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onDestroy() {
        if (mReadThread != null) {
            mReadThread.interrupt();
        }
        closeSerialPort();
        mSerialPort = null;
        super.onDestroy();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendDataRS232:
                if (TextUtils.isEmpty(customDataRS232.getText().toString())) {
                    Toast.makeText(getContext(), "Enter data!", Toast.LENGTH_LONG).show();
                } else {
                    send = customDataRS232.getText().toString().getBytes();
                    sendData(send);
                }
                break;
            case R.id.clearDataRS232:
                clearData();
                break;
            case R.id.setAlarm:
                startAlarm();
                break;
        }
    }

    public void startAlarm() {
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.SerialPortListRS232) {
            selectedPort = parent.getItemAtPosition(position).toString();
            if (MainActivity.dockState > 0) {
                if (mSerialPort != null) {
                    closeSerialPort();
                }
                openSerialPort(selectedPort);
                if (mSerialPort != null) {
                    mSerialPort.config(Integer.parseInt(selectedBaudrate));
                    mReadThread = new ReadThread();
                    mReadThread.start();
                }
            }
        } else if (parent.getId() == R.id.baudrateListRS232) {
            selectedBaudrate = parent.getItemAtPosition(position).toString();
            if (MainActivity.dockState > 0) {
                if (mSerialPort != null) {
                    mSerialPort.config(Integer.parseInt(selectedBaudrate));
                }
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                int size;
                try {
                    byte[] buffer = new byte[64];
                    if (mInputStream == null) {
                        return;
                    }
                    size = mInputStream.read(buffer);
                    if (size > 0) {
                        onDataReceived(buffer, size);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

            }
        }
    }

    public void onDataReceived(final byte[] buffer, int size) {
        try {
            receivedDataValue = new String(buffer, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        allReceivedData = allReceivedData + receivedDataValue.substring(0, size);

        mainActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (rs232Enabled) {
                    receivedDataRS232.setText(allReceivedData);
                }
            }
        });

    }

    public void openSerialPort(String path) {
        try {
            mSerialPort = new SerialPort(new File(path), 0);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
        } catch (IOException e) {
            mSerialPort = null;
            e.printStackTrace();
        }
    }

    public void closeSerialPort() {
        if (mReadThread != null) {
            mReadThread.interrupt();
        }
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
    }

    public void sendData(byte[] data) {
        if (mSerialPort != null) {
            if (mOutputStream != null) {
                try {
                    mOutputStream.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                return;
            }
        }
    }

    public void clearData() {
        allReceivedData = "";
        if (rs232Enabled) {
            receivedDataRS232.setText(allReceivedData);
        }
    }

    public void enablePort(String port, boolean enable) {
        try {
            @SuppressWarnings("rawtypes")
            Class SerialService = Class.forName("com.android.server.serial.SerialService");
            Constructor<?> constructor = SerialService.getConstructor();
            Object enablePortInstanse = constructor.newInstance();
            Method enableRs485 = SerialService.getMethod("enableRs485");
            Method disableRs485 = SerialService.getMethod("disableRs485");
            if (port.equals("rs")) {
                if (enable) {
                    enableRs485.invoke(enablePortInstanse);
                } else {
                    disableRs485.invoke(enablePortInstanse);
                }
            }
        } catch (IllegalArgumentException iAE) {
            throw iAE;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        }
    }

    private String[] getRsPort(){
//        if (MainActivity.devType == MainActivity.SMARTCAM_BASIC)
            return RSPortListBasicCradle;
//        else if (MainActivity.devType == MainActivity.SMARTCAM_ENHANCED)
//            return RSPortListCamEnh;
//        else
            //return RSPortList;
    }
}
