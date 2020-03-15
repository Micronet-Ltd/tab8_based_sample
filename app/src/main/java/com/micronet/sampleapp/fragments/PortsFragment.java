package com.micronet.sampleapp.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.micronet.sampleapp.R;
import com.micronet.sampleapp.SerialPort;
import com.micronet.sampleapp.j1708Port;
import com.micronet.sampleapp.activities.MainActivity;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PortsFragment extends Fragment implements OnClickListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = "PortsFragment";
    private View rootView;
    protected SerialPort mSerialPort = null;
    protected OutputStream mOutputStream;
    protected InputStream mInputStream;
    protected ReadThread mReadThread;
    protected j1708Port mSerialPortJ1708 = null;
    protected OutputStream mOutputStreamj1708;
    protected InputStream mInputStreamj1708;
    protected ReadThreadJ1708 mReadThreadJ1708;
    byte[] send;
    String receivedDataValue = null;
    String allReceivedData = "";
    String allReceivedDataJ1708 = "";
    String receivedDataValueJ1708 = null;
    boolean rs232Enabled = true;
    public static MainActivity mainActivity;

    String[] RSPortList = {"/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2", "/dev/ttyUSB3"};
    String[] RSPortListCamEnh = {"/dev/ttyMICRONET_ACCEL"};
    String[] RSPortListBasicCradle = {"/dev/ttyHS0"};
    String[] baudrateList = {"300", "600", "1200", "1800", "2400", "4800", "9600", "19200", "38400", "57600", "115200"};
    String[] actionsList = {"turn on flash", "turn off flash"};
    //String j1708Port = "/dev/ttyMICRONET_J1708";
    String j1708Port = "/dev/j1708";
    RadioGroup jGroup;
    Spinner serialPortRS232;
    Spinner baudrateRS232;
    EditText customDataRS232;
    TextView receivedDataRS232;
    Button sendDataRs232;
    Button clearDataRs232;

    Spinner actions;

    EditText customDataJ1708;
    TextView receivedDataJ1708;
    Button sendDataJ1708;
    Button clearDataJ1708;

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
        closeSerialPortJ1708();
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
        MainActivity.setViewAndChildrenEnabled(rootView.findViewById(R.id.J1708), true);
        enablePort("j", true);

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

        // settings for custom button Action
        actions = rootView.findViewById(R.id.customButtonActions);
        actions.setOnItemSelectedListener(this);
        ArrayAdapter actionsAdapter = new ArrayAdapter(getActivity(), R.layout.spinner_item, actionsList);
        actions.setAdapter(actionsAdapter);
        TextView text = rootView.findViewById(R.id.textForCustomButton);
        if (MainActivity.getBoardType() < 2) {
            text.setText("select action for custom button \\n (F1 - first from the left");
        } else {
            text.setText("select action for custom button");
        }

        jGroup = rootView.findViewById(R.id.jGroup);
        jGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.j1708En) {
                    enablePort("j", true);
                    MainActivity.setViewAndChildrenEnabled(rootView.findViewById(R.id.J1708), true);
                    openSerialPortJ1708();
                    if (mSerialPortJ1708 != null) {

                        mReadThreadJ1708 = new ReadThreadJ1708();
                        mReadThreadJ1708.start();
                    }

                } else {
                    clearData();
                    closeSerialPortJ1708();
                    enablePort("j", false);
                    MainActivity.setViewAndChildrenEnabled(rootView.findViewById(R.id.J1708), false);
                    MainActivity.setViewAndChildrenEnabled(rootView.findViewById(R.id.jGroup), true);
                }
            }
        });

        customDataJ1708 = rootView.findViewById(R.id.customDataJ1708);
        sendDataJ1708 = rootView.findViewById(R.id.sendDataJ1708);
        sendDataJ1708.setOnClickListener(this);

        receivedDataJ1708 = rootView.findViewById(R.id.receivedDataJ1708);
        clearDataJ1708 = rootView.findViewById(R.id.clearDataJ1708);
        clearDataJ1708.setOnClickListener(this);

        updatePorts();

        waitTime = rootView.findViewById(R.id.waitingTime);
        setAlarm = rootView.findViewById(R.id.setAlarm);
        setAlarm.setOnClickListener(this);
        return rootView;
    }

    public void updatePorts() {
        if (MainActivity.devType == MainActivity.SMARTTAB_STAND_ALONE) {
            closeSerialPort();
            closeSerialPortJ1708();
            rootView.findViewById(R.id.RS232).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.rs232Disabled).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.J1708).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.j1708Disabled).setVisibility(View.VISIBLE);
        } else {
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
            if (MainActivity.devType == MainActivity.SMARTCAM_BASIC || MainActivity.devType == MainActivity.SMARTTAB_CRADLE_BASIC) {
                rootView.findViewById(R.id.J1708).setVisibility(View.INVISIBLE);
                rootView.findViewById(R.id.j1708Disabled).setVisibility(View.VISIBLE);
            } else {
                rootView.findViewById(R.id.J1708).setVisibility(View.VISIBLE);
                rootView.findViewById(R.id.j1708Disabled).setVisibility(View.INVISIBLE);
            }
            if (mSerialPortJ1708 == null) {
                openSerialPortJ1708();
                if (mSerialPortJ1708 != null) {
                    mReadThreadJ1708 = new ReadThreadJ1708();
                    mReadThreadJ1708.start();
                }
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
        closeSerialPortJ1708();
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
            case R.id.sendDataJ1708:
                if (TextUtils.isEmpty(customDataJ1708.getText().toString())) {
                    Toast.makeText(getContext(), "Enter data!", Toast.LENGTH_LONG).show();
                } else {
                    send = mainActivity.toBinary(customDataJ1708.getText().toString());
                    sendDataJ1708(send);
                }
                break;
            case R.id.clearDataRS232:
                clearData();
                break;
            case R.id.clearDataJ1708:
                clearDataJ1708();
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
        } else if (parent.getId() == R.id.customButtonActions) {
            mainActivity.actionId = position;
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

    public void onDataReceivedJ1708(byte[] buffer, int size) {
        receivedDataValueJ1708 = "";
        int len = Integer.parseInt(Integer.toHexString(buffer[0] & 0xFF), 16);
        for (int i = 1; i <= len; i++) {
            if (Integer.toHexString(buffer[i]).length() < 2) {
                receivedDataValueJ1708 = receivedDataValueJ1708 + "0" + Integer.toHexString(buffer[i]);
            } else {
                receivedDataValueJ1708 = receivedDataValueJ1708 + Integer.toHexString(buffer[i] & 0xFF);
            }
        }

        allReceivedDataJ1708 = allReceivedDataJ1708 + receivedDataValueJ1708;

        mainActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                receivedDataJ1708.setText(allReceivedDataJ1708);
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

    public void openSerialPortJ1708() {
        try {

            mSerialPortJ1708 = new j1708Port(new File(j1708Port), 0);
            mOutputStreamj1708 = mSerialPortJ1708.getOutputStream();
            mInputStreamj1708 = mSerialPortJ1708.getInputStream();
        } catch (IOException e) {
            mSerialPortJ1708 = null;
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

    public void closeSerialPortJ1708() {
        if (mReadThreadJ1708 != null) {
            mReadThreadJ1708.interrupt();
        }
        if (mSerialPortJ1708 != null) {
            mSerialPortJ1708.close();
            mSerialPortJ1708 = null;
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

    public void sendDataJ1708(byte[] data) {
        if (mSerialPortJ1708 != null) {
            if (mOutputStreamj1708 != null) {
                try {
                    mOutputStreamj1708.write(data);
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

    public void clearDataJ1708() {
        allReceivedDataJ1708 = "";
        receivedDataJ1708.setText(allReceivedDataJ1708);
    }

    public void enablePort(String port, boolean enable) {
        try {
            @SuppressWarnings("rawtypes")
            Class SerialService = Class.forName("com.android.server.serial.SerialService");
            Constructor<?> constructor = SerialService.getConstructor();
            Object enablePortInstanse = constructor.newInstance();
            Method enableRs485 = SerialService.getMethod("enableRs485");
            Method disableRs485 = SerialService.getMethod("disableRs485");
            Method enableJ1708 = SerialService.getMethod("enableJ1708");
            Method disableJ1708 = SerialService.getMethod("disableJ1708");
            if (port.equals("rs")) {
                if (enable) {
                    enableRs485.invoke(enablePortInstanse);
                } else {
                    disableRs485.invoke(enablePortInstanse);
                }
            } else {
                if (enable) {
                    enableJ1708.invoke(enablePortInstanse);
                } else {
                    disableJ1708.invoke(enablePortInstanse);
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

    private class ReadThreadJ1708 extends Thread {

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                int size;
                try {
                    byte[] buffer = new byte[64];
                    if (mInputStreamj1708 == null) {
                        return;
                    }
                    size = mInputStreamj1708.read(buffer);
                    if (size > 0) {
                        onDataReceivedJ1708(buffer, size);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

            }
        }
    }

    private String[] getRsPort(){
        if (MainActivity.devType == MainActivity.SMARTCAM_BASIC)
            return RSPortListBasicCradle;
        else if (MainActivity.devType == MainActivity.SMARTCAM_ENHANCED)
            return RSPortListCamEnh;
        else return RSPortList;
    }
}
