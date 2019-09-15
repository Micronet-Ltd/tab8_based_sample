package com.micronet.sampleapp.fragments;

import static java.lang.Character.isDigit;
import static java.lang.Character.isUpperCase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class PortsFragment extends Fragment implements OnClickListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = "PortsFragment";
    public static final String dockAction = "android.intent.action.DOCK_EVENT";
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
    int dockState = -1;
    public static MainActivity mainActivity;

    String[] RSPortList = {"/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2", "/dev/ttyUSB3"};
    String[] RSPortListBasicCradle = {"/dev/ttyHS0"}; //todo add basic cradle case
    String[] baudrateList = {"300", "600", "1200", "1800", "2400", "4800", "9600", "19200", "38400", "57600", "115200"};
    String[] actionsList = {"volume up", "turn on flash", "turn off flash"};
//    String j1708Port = "/dev/ttyMICRONET_J1708";
    String j1708Port = "/dev/j1708";
    int j1708Baudrate = 9600;
//    RadioGroup rsGroup;
    RadioGroup jGroup;
    Spinner serialPortRS232;
    Spinner baudrateRS232;
    EditText customDataRS232;
    TextView receivedDataRS232;
    Button sendDataRs232;
    Button clearDataRs232;

//    Spinner serialPortRS485;
//    Spinner baudrateRS485;
//    EditText customDataRS485;
//    TextView receivedDataRS485;
//    Button sendDataRs485;
//    Button clearDataRs485;

    Spinner actions;

    EditText customDataJ1708;
    TextView receivedDataJ1708;
    Button sendDataJ1708;
    Button clearDataJ1708;

    String selectedPortRS232 = RSPortList[0];
    String selectedBaudrateRS232 = baudrateList[0];
//    String selectedPortRS485 = RSPortList[0];
//    String selectedBaudrateRS485 = baudrateList[0];
    String selectedPort = RSPortList[0];
    String selectedBaudrate = baudrateList[0];


    public PortsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(dockAction);
        getActivity().registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_ports, container, false);
        mainActivity = (MainActivity) getActivity();
        //setViewAndChildrenEnabled(rootView.findViewById(R.id.RS485), false);
        setViewAndChildrenEnabled(rootView.findViewById(R.id.RS232), true);
        enablePort("rs", false);
        setViewAndChildrenEnabled(rootView.findViewById(R.id.J1708), true);
        enablePort("j", true);

        // settings for RS232
        serialPortRS232 = rootView.findViewById(R.id.SerialPortListRS232);
        serialPortRS232.setOnItemSelectedListener(this);
        ArrayAdapter serialPortAdapterRS232 = new ArrayAdapter(getActivity(), R.layout.spinner_item, RSPortList);
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

//        // settings for RS485
//        serialPortRS485 = rootView.findViewById(R.id.SerialPortListRS485);
//        serialPortRS485.setOnItemSelectedListener(this);
//        ArrayAdapter serialPortAdapterRS485 = new ArrayAdapter(getActivity(), R.layout.spinner_item, RSPortList);
//        serialPortRS485.setAdapter(serialPortAdapterRS485);
//
//        baudrateRS485 = rootView.findViewById(R.id.baudrateListRS485);
//        baudrateRS485.setOnItemSelectedListener(this);
//        ArrayAdapter baudrateAdapterRS485 = new ArrayAdapter(getActivity(), R.layout.spinner_item, baudrateList);
//        baudrateRS485.setAdapter(baudrateAdapterRS485);
//
//        customDataRS485 = rootView.findViewById(R.id.customDataRS485);
//        sendDataRs485 = rootView.findViewById(R.id.sendDataRS485);
//        sendDataRs485.setOnClickListener(this);
//
//        receivedDataRS485 = rootView.findViewById(R.id.receivedDataRS485);
//        clearDataRs485 = rootView.findViewById(R.id.clearDataRS485);
//        clearDataRs485.setOnClickListener(this);

//        rsGroup = rootView.findViewById(R.id.rsGroup);
//        rsGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                if (checkedId == R.id.rs232En) {
//                    clearData();
//                    closeSerialPort();
//                    rs232Enabled = true;
//                    enablePort("rs", false);
//                    setViewAndChildrenEnabled(rootView.findViewById(R.id.RS485), false);
//                    setViewAndChildrenEnabled(rootView.findViewById(R.id.RS232), true);
//                    selectedPort = selectedPortRS232;
//                    selectedBaudrate = selectedBaudrateRS232;
//                    openSerialPort(selectedPort);
//                    mSerialPort.config(Integer.parseInt(selectedBaudrate));
//                    mReadThread = new ReadThread();
//                    mReadThread.start();
//
//                } else {
//                    clearData();
//                    closeSerialPort();
//                    rs232Enabled = false;
//                    enablePort("rs", true);
//                    setViewAndChildrenEnabled(rootView.findViewById(R.id.RS485), true);
//                    setViewAndChildrenEnabled(rootView.findViewById(R.id.RS232), false);
//                    selectedPort = selectedPortRS485;
//                    selectedBaudrate = selectedBaudrateRS485;
//                    openSerialPort(selectedPort);
//                    mSerialPort.config(Integer.parseInt(selectedBaudrate));
//                    mReadThread = new ReadThread();
//                    mReadThread.start();
//
//                }
//            }
//        });

        jGroup = rootView.findViewById(R.id.jGroup);
        jGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.j1708En) {
                    enablePort("j", true);
                    setViewAndChildrenEnabled(rootView.findViewById(R.id.J1708), true);
                    // if (mSerialPortJ1708 == null) {
                    openSerialPortJ1708();
                    //}
                    //  if (mReadThreadJ1708.isInterrupted()) {
                    mReadThreadJ1708 = new ReadThreadJ1708();
                    mReadThreadJ1708.start();
                    // }

                } else {
                    clearData();
                    closeSerialPortJ1708();
                    enablePort("j", false);
                    setViewAndChildrenEnabled(rootView.findViewById(R.id.J1708), false);
                    setViewAndChildrenEnabled(rootView.findViewById(R.id.jGroup), true);
                }
            }
        });

        customDataJ1708 = rootView.findViewById(R.id.customDataJ1708);
        sendDataJ1708 = rootView.findViewById(R.id.sendDataJ1708);
        sendDataJ1708.setOnClickListener(this);

        receivedDataJ1708 = rootView.findViewById(R.id.receivedDataJ1708);
        clearDataJ1708 = rootView.findViewById(R.id.clearDataJ1708);
        clearDataJ1708.setOnClickListener(this);

        dockState = MainActivity.getDockState();
        if (dockState == -1 || dockState == 0) {
//            rootView.findViewById(R.id.rsGroup).setVisibility(View.INVISIBLE);
           // rootView.findViewById(R.id.allPorts).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.rs232andJ1708).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.disabled).setVisibility(View.VISIBLE);
        } else {
//            rootView.findViewById(R.id.rsGroup).setVisibility(View.VISIBLE);
//            rootView.findViewById(R.id.allPorts).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.rs232andJ1708).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.disabled).setVisibility(View.INVISIBLE);
            openSerialPort(selectedPort);
            mSerialPort.config(Integer.parseInt(selectedBaudrate));
            mReadThread = new ReadThread();
            mReadThread.start();
            openSerialPortJ1708();
            mReadThreadJ1708 = new ReadThreadJ1708();
            mReadThreadJ1708.start();
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        // Register for local broadcasts
        Context context = getContext();
        if (context != null) {
        }
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mReceiver);
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
//            case R.id.sendDataRS485:
//                if (TextUtils.isEmpty(customDataRS485.getText().toString())) {
//                    Toast.makeText(getContext(), "Enter data!", Toast.LENGTH_LONG).show();
//                } else {
//                    send = customDataRS485.getText().toString().getBytes();
//                    sendData(send);
//                }
//                break;
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
//            case R.id.clearDataRS485:
//                clearData();
//                break;
            case R.id.clearDataJ1708:
                clearDataJ1708();
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.SerialPortListRS232) {
            selectedPortRS232 = parent.getItemAtPosition(position).toString();
            selectedPort = selectedPortRS232;
            if (dockState > 0) {
                if (mSerialPort != null) {
                    closeSerialPort();
                }
                openSerialPort(selectedPort);
                mSerialPort.config(Integer.parseInt(selectedBaudrate));
                mReadThread = new ReadThread();
                mReadThread.start();
            }
        } else if (parent.getId() == R.id.baudrateListRS232) {
            selectedBaudrateRS232 = parent.getItemAtPosition(position).toString();
            selectedBaudrate = selectedBaudrateRS232;
            if (dockState > 0) {
                mSerialPort.config(Integer.parseInt(selectedBaudrate));
            }
//        } else if (parent.getId() == R.id.SerialPortListRS485) {
//            selectedPortRS485 = parent.getItemAtPosition(position).toString();
//            selectedPort = selectedPortRS485;
//            if (dockState > 0) {
//                if (mSerialPort != null) {
//                    closeSerialPort();
//                }
//                openSerialPort(selectedPort);
//                mSerialPort.config(Integer.parseInt(selectedBaudrate));
//                mReadThread = new ReadThread();
//                mReadThread.start();
//            }
//        } else if (parent.getId() == R.id.baudrateListRS485) {
//            selectedBaudrateRS485 = parent.getItemAtPosition(position).toString();
//            selectedBaudrate = selectedBaudrateRS485;
//            if (dockState > 0) {
//                mSerialPort.config(Integer.parseInt(selectedBaudrate));
//            }
        }
        else if (parent.getId() == R.id.customButtonActions){
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
//                } else {
//                    receivedDataRS485.setText(allReceivedData);
                }
            }
        });

    }

//    public void onDataReceivedJ1708(final byte[] buffer, int size) {
//        try {
//            receivedDataValueJ1708 = new String(buffer, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        allReceivedDataJ1708 = allReceivedDataJ1708 + receivedDataValueJ1708.substring(0, size);
//
//        if (allReceivedDataJ1708.length() < 300) {
//            mainActivity.runOnUiThread(new Runnable() {
//
//                @Override
//                public void run() {
//                    receivedDataJ1708.setText(allReceivedDataJ1708);
//                }
//            });
//        }
//    }

    public void onDataReceivedJ1708(byte[] buffer, int size) {
       // try {
        Log.d("AAA", Arrays.toString(buffer) + " sise = " + size);
            receivedDataValueJ1708 = "";
            int len = Integer.parseInt(Integer.toHexString(buffer[0] & 0xFF), 16);
            Log.d("AAAA", "len = " + len);
                for (int i = 1; i <= len; i++) {
                    if (Integer.toHexString(buffer[i]).length() < 2) {
                        receivedDataValueJ1708 = receivedDataValueJ1708 + "0" + Integer.toHexString(buffer[i]);
                    } else {
                        receivedDataValueJ1708 = receivedDataValueJ1708 + Integer.toHexString(buffer[i] & 0xFF);
                    }
                 //   Log.d("BBBBBB", "data received: " + receivedDataValueJ1708);
                }

            allReceivedDataJ1708 = allReceivedDataJ1708 + receivedDataValueJ1708;


            mainActivity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    receivedDataJ1708.setText(allReceivedDataJ1708);
                }
            });

//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }

    }

    private static void setViewAndChildrenEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                setViewAndChildrenEnabled(child, enabled);
            }
        }
    }

    public void openSerialPort(String path) {
        try {

            mSerialPort = new SerialPort(new File(path), 0);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openSerialPortJ1708() {
        try {

            mSerialPortJ1708 = new j1708Port(new File(j1708Port), 0);
            mOutputStreamj1708 = mSerialPortJ1708.getOutputStream();
            mInputStreamj1708 = mSerialPortJ1708.getInputStream();
          //  mSerialPortJ1708.config(j1708Baudrate);
        } catch (IOException e) {
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
//        } else {
//            receivedDataRS485.setText(allReceivedData);
        }
    }

    public void clearDataJ1708() {
        allReceivedDataJ1708 = "";
        receivedDataJ1708.setText(allReceivedDataJ1708);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            goAsync();
            Log.d(TAG, "dock action received");
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case dockAction:
                        dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
                        Log.d(TAG, "Dock event received: " + dockState);
                        if (dockState == -1 || dockState == 0) {
                            closeSerialPort();
//                            rootView.findViewById(R.id.rsGroup).setVisibility(View.INVISIBLE);
                            //rootView.findViewById(R.id.allPorts).setVisibility(View.INVISIBLE);
                            rootView.findViewById(R.id.rs232andJ1708).setVisibility(View.INVISIBLE);
                            rootView.findViewById(R.id.disabled).setVisibility(View.VISIBLE);
                        } else {
//                            rootView.findViewById(R.id.rsGroup).setVisibility(View.VISIBLE);
                            //rootView.findViewById(R.id.allPorts).setVisibility(View.VISIBLE);
                            rootView.findViewById(R.id.rs232andJ1708).setVisibility(View.VISIBLE);
                            rootView.findViewById(R.id.disabled).setVisibility(View.INVISIBLE);
                            if (rs232Enabled) {
                                selectedPort = selectedPortRS232;
                                selectedBaudrate = selectedBaudrateRS232;
//                            } else {
//                                selectedPort = selectedPortRS485;
//                                selectedBaudrate = selectedBaudrateRS485;
                            }
                            if (mSerialPort == null) {
                                //todo: add set enable?
                                openSerialPort(selectedPort);
                                mSerialPort.config(Integer.parseInt(selectedBaudrate));
                                mReadThread = new ReadThread();
                                mReadThread.start();
                            }

                            if (mSerialPortJ1708 == null) {
                                openSerialPortJ1708();
                                mReadThreadJ1708 = new ReadThreadJ1708();
                                mReadThreadJ1708.start();
                            }
                        }
                        break;
                }
            }


        }
    };

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

//    public void onDataReceivedJ1708(final byte[] buffer, int size) {
//        try {
//            receivedDataValueJ1708 = new String(buffer, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        allReceivedDataJ1708 = allReceivedDataJ1708 + receivedDataValueJ1708.substring(0, size);
//
//        if (allReceivedDataJ1708.length() < 300) {
//            mainActivity.runOnUiThread(new Runnable() {
//
//                @Override
//                public void run() {
//                    receivedDataJ1708.setText(allReceivedDataJ1708);
//                }
//            });
//        }
//    }



}
