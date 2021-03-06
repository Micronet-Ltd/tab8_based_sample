/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.sampleapp.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
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
import com.micronet.sampleapp.activities.MainActivity;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Canbus Fragment Class
 */
public class CanbusFragment extends Fragment implements OnClickListener, AdapterView.OnItemSelectedListener {

    private final String TAG = "CanbusFragment";
    public static final String dockAction = "android.intent.action.DOCK_EVENT";
    private View rootView;
    Spinner canList;
    Spinner bitrateList;
    RadioGroup terminationGroup;
    RadioGroup listenerModeGroup;
    RadioGroup swcGroup;
    Button openCan;
    Button closeCan;
    EditText ids;
    EditText types;
    EditText masks;
    TextView receivedData;
    Button clearData;
    Button sendData;
    EditText dataToSend;
    String[] canbusPortList = {"/dev/ttyCAN0", "/dev/ttyCAN1"};
    String[] canbusBitrateList = {"10Kbit", "20Kbit", "33.33Kbit", "50Kbit", "100Kbit", "125Kbit", "250Kbit", "500Kbit", "800Kbit", "1Mbit"};
    int[] canbusBitrateListValues = {10000, 20000, 33330, 50000, 100000, 125000, 250000, 500000, 800000, 1000000};
    int currentPort = 1;
    int currentBitrate = 10000;
    boolean termination = true;
    boolean listenerMode = false;
    boolean canOpened = false;
    int dockState = -1;
    int can_fd = -1;
    public static MainActivity mainActivity;
    String receivedDataValue = null;
    String allReceivedData = "";
    protected ReadThread mReadThread;
    byte[] data;
    boolean swcEnabled = false;

    Class CanbusService;
    Class CanbusHardwareFilter;
    Class CanbusFlowControl;
    Class filtersArray;
    Object canServiceInstanse;
    Object canHardwareFilterInstanse;
    Method configureAndOpenCan;
    Method closeCanMethod;


    OutputStream mOutputStream;
    InputStream mInputStream;

    public CanbusFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        importClasses();
    }

    @Override
    public void onPause() {
        closeCanbus();
        super.onPause();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_canbus, container, false);
        mainActivity = (MainActivity) getActivity();

        ids = rootView.findViewById(R.id.ids);
        masks = rootView.findViewById(R.id.masks);
        types = rootView.findViewById(R.id.types);

        canList = rootView.findViewById(R.id.canList);
        canList.setOnItemSelectedListener(this);
        ArrayAdapter canAdapter = new ArrayAdapter(getActivity(), R.layout.spinner_item, canbusPortList);
        canList.setAdapter(canAdapter);

        bitrateList = rootView.findViewById(R.id.bitrateList);
        bitrateList.setOnItemSelectedListener(this);
        ArrayAdapter bitrateAdapter = new ArrayAdapter(getActivity(), R.layout.spinner_item, canbusBitrateList);
        bitrateList.setAdapter(bitrateAdapter);

        terminationGroup = rootView.findViewById(R.id.terminationGroup);
        terminationGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.termOn) {
                    termination = true;
                } else {
                    termination = false;
                }
            }
        });
        listenerModeGroup = rootView.findViewById(R.id.listenerModeGroup);
        listenerModeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lisModeOff) {
                    listenerMode = false;
                } else {
                    listenerMode = true;
                }
            }
        });
        swcGroup = rootView.findViewById(R.id.swcGroup);
        swcGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.regular) {
                    bitrateList.setEnabled(true);
                    swcEnabled = false;
                } else {
                    currentBitrate = canbusBitrateListValues[2];
                    bitrateList.setSelection(2);
                    bitrateList.setEnabled(false);
                    swcEnabled = true;
                }
            }
        });

        openCan = rootView.findViewById(R.id.openCan);
        openCan.setOnClickListener(this);
        closeCan = rootView.findViewById(R.id.closeCan);
        closeCan.setOnClickListener(this);
        closeCan.setEnabled(false);
        receivedData = rootView.findViewById(R.id.receivedData);
        clearData = rootView.findViewById(R.id.clearData);
        clearData.setOnClickListener(this);
        dataToSend = rootView.findViewById(R.id.dataToSend);
        sendData = rootView.findViewById(R.id.sendData);
        sendData.setOnClickListener(this);
        updateCanbus();
        return rootView;
    }


    @Override
    public void onDestroy() {
        if (mReadThread != null) {
            mReadThread.interrupt();
        }
        closeCanbus();
        super.onDestroy();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.openCan:
                configAndOpenCan();
                break;
            case R.id.closeCan:
                closeCanbus();
                break;
            case R.id.clearData:
                clearData();
                break;
            case R.id.sendData:
                if (TextUtils.isEmpty(dataToSend.getText().toString())) {
                    Toast.makeText(getContext(), "Enter data!", Toast.LENGTH_LONG).show();
                } else {
                    byte[] crByte = {0x0D};
                    data = dataToSend.getText().toString().getBytes();
                    byte[] allData = new byte[data.length + 1];
                    System.arraycopy(data, 0, allData, 0, data.length);
                    System.arraycopy(crByte, 0, allData, data.length, 1);
                    sendData(allData);
                }
                break;

        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.canList) {
            String portName = parent.getItemAtPosition(position).toString();
            if (portName.contains("CAN0")) {
                currentPort = 1;
            } else {
                currentPort = 2;
            }
        }
        if (parent.getId() == R.id.bitrateList) {
            currentBitrate = canbusBitrateListValues[position];
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void sendData(byte[] data) {
        if (mOutputStream != null) {
            try {
                mOutputStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        allReceivedData = receivedDataValue.substring(0, size);

        mainActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                receivedData.setText(allReceivedData);
            }
        });

    }

    public int configAndOpenCan() {
        int ret = 0;
        String[] idsStr, maskStr, typeStr;
        int[] tempIds, tempMask, tempType;

        if (TextUtils.isEmpty(ids.getText().toString())) {
            tempIds = new int[]{0x00000000, 0x00000000};
        } else {
            idsStr = ids.getText().toString().split(",");
            tempIds = new int[idsStr.length];
            for (int i = 0; i < idsStr.length; i++) {

                tempIds[i] = Integer.parseInt(idsStr[i]);
            }
        }
        if (TextUtils.isEmpty(masks.getText().toString())) {
            tempMask = new int[]{0x00000000, 0x00000000};
        } else {
            maskStr = masks.getText().toString().split(",");
            tempMask = new int[maskStr.length];
            for (int i = 0; i < maskStr.length; i++) {
                tempMask[i] = Integer.parseInt(maskStr[i]);
            }
        }
        if (TextUtils.isEmpty(types.getText().toString())) {
            tempType = new int[]{0, 1};
        } else {
            typeStr = types.getText().toString().split(",");
            tempType = new int[typeStr.length];
            for (int i = 0; i < typeStr.length; i++) {
                tempType[i] = Integer.parseInt(typeStr[i]);
            }
        }

        try {
            Constructor<?> constructor = CanbusHardwareFilter.getConstructor(int[].class, int[].class, int[].class);
            canHardwareFilterInstanse = constructor.newInstance(tempIds, tempMask, tempType);
            Object filterArr = Array.newInstance(CanbusHardwareFilter, 1);
            Array.set(filterArr, 0, canHardwareFilterInstanse);
            if (can_fd == -1)
                ret = (int) configureAndOpenCan.invoke(canServiceInstanse, listenerMode, currentBitrate, termination, filterArr, currentPort,
                    null);
            else
                ret = can_fd;

            if (ret != -1) {
                can_fd = ret;
                canOpened = true;
                enableConfigView(false);
                mInputStream = new FileInputStream(canbusPortList[currentPort - 1]);
                mOutputStream = new FileOutputStream(canbusPortList[currentPort - 1]);

                mReadThread = new ReadThread();
                mReadThread.start();
            } else {
                Toast.makeText(getContext(), "Can't open canbus", Toast.LENGTH_LONG).show();
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void closeCanbus() {
        try {
            if (mReadThread != null) {
                mReadThread.interrupt();
            }
            int ret = (int) closeCanMethod.invoke(canServiceInstanse, currentPort);
            if (ret != -1) {
                canOpened = false;
                enableConfigView(true);
                if (swcEnabled) {
                    bitrateList.setEnabled(false);
                }
                //closeCanMethod(currentPort);
                can_fd = -1;
            }
            mInputStream = null;
            mOutputStream = null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void clearData() {
        allReceivedData = "";
        receivedData.setText(allReceivedData);

    }

    private void importClasses() {
        try {
            CanbusService = Class.forName("com.android.server.serial.CanbusService");
            CanbusHardwareFilter = Class.forName("com.android.server.serial.CanbusHardwareFilter");
            CanbusFlowControl = Class.forName("com.android.server.serial.CanbusFlowControl");
            Constructor<?> constructor = CanbusService.getConstructor();
            canServiceInstanse = constructor.newInstance();
            filtersArray = Class.forName("[Lcom.android.server.serial.CanbusHardwareFilter;");
            Class flowControlArray = Class.forName("[Lcom.android.server.serial.CanbusFlowControl;");

            configureAndOpenCan = CanbusService
                .getMethod("configureAndOpenCan", boolean.class, int.class, boolean.class, filtersArray, int.class,
                    flowControlArray);
            closeCanMethod = CanbusService.getMethod("closeCan", int.class);


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

    public void enableConfigView(boolean enable) {
        MainActivity.setViewAndChildrenEnabled(rootView.findViewById(R.id.maskConfiguration), enable);
        MainActivity.setViewAndChildrenEnabled(rootView.findViewById(R.id.canConfiguration), enable);
        closeCan.setEnabled(!enable);
        openCan.setEnabled(enable);
        MainActivity.setViewAndChildrenEnabled(rootView.findViewById(R.id.dataConfiguration), !enable);
    }

    public void updateCanbus() {
        if (MainActivity.devType == MainActivity.SMARTTAB_STAND_ALONE || MainActivity.devType == MainActivity.SMARTTAB_CRADLE_BASIC
            || MainActivity.devType == MainActivity.SMARTCAM_BASIC) {
            if (dockState == -1 || dockState == 0) {
                enableConfigView(!canOpened);
                if (swcEnabled) {
                    bitrateList.setEnabled(false);
                }
                canOpened = false;
                rootView.findViewById(R.id.CanbusFragment).setVisibility(View.INVISIBLE);
                rootView.findViewById(R.id.disabledCan).setVisibility(View.VISIBLE);
            } else {
                rootView.findViewById(R.id.CanbusFragment).setVisibility(View.VISIBLE);
                rootView.findViewById(R.id.disabledCan).setVisibility(View.INVISIBLE);
            }
        }
    }
}