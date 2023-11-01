/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.sampleapp.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

import com.micronet.canbus.CanbusFrameType;
import com.micronet.sampleapp.R;
import com.micronet.sampleapp.activities.MainActivity;
import com.micronet.sampleapp.canbus.VehicleBusCAN;
import com.micronet.sampleapp.canbus.VehicleBusHW;
import com.micronet.sampleapp.canbus.VehicleBusWrapper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Canbus Fragment Class
 */
public class CanbusFragment extends Fragment implements OnClickListener, AdapterView.OnItemSelectedListener {

    private final String TAG = "CanbusFragment";
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
    int currentPort = 2;
    int currentBitrate = 250000;
    boolean termination = true;
    boolean listenerMode = false;
    boolean canOpened = false;
    int dockState = -1;
    int can_fd = -1;
    public static MainActivity mainActivity;
    String allReceivedData = "";
    byte[] data;
    boolean swcEnabled = false;
    private int counter=0;
    VehicleBusWrapper vehicleBusWrapper;

    public CanbusFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        ArrayAdapter canAdapter = new ArrayAdapter(requireContext(), R.layout.spinner_item, canbusPortList);
        canList.setAdapter(canAdapter);

        bitrateList = rootView.findViewById(R.id.bitrateList);
        bitrateList.setOnItemSelectedListener(this);
        ArrayAdapter bitrateAdapter = new ArrayAdapter(requireContext(), R.layout.spinner_item, canbusBitrateList);
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
                    data = dataToSend.getText().toString().getBytes();
                    sendData(data);
                }
                break;

        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.canList) {
            String portName = parent.getItemAtPosition(position).toString();
            if (portName.contains("CAN0")) {
                currentPort = 2;
            } else {
                currentPort = 3;
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
        //TODO Send intent to write frame
        byte[] toSend;
        if(data.length <= 8){
            toSend=data;
        } else {
            toSend=new byte[8];
            System.arraycopy(data,0,toSend,0,8);
        }
        Intent intent = new Intent("com.micronet.sampleapp.canframe_send");
        intent.putExtra("ID", 0x18FFFFFF);
        intent.putExtra("DATA", toSend);
        intent.putExtra("IS_EXTENDED", true);
        requireContext().sendBroadcast(intent);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.micronet.sampleapp.canframe_received".equals(intent.getAction())){
                counter++;
                int id = intent.getIntExtra("ID", -1);
                byte[] data = intent.getByteArrayExtra("DATA");
                String type = intent.getBooleanExtra("IS_EXTENDED",false)?"E":"S";
                receivedData.setText(type + String.format(":%1$08X:", id) + new String(data));
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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

        VehicleBusWrapper.CANHardwareFilter[] filter = new VehicleBusHW.CANHardwareFilter[]{
                new VehicleBusHW.CANHardwareFilter(0,0, VehicleBusHW.CANFrameType.EXTENDED),
                new VehicleBusHW.CANHardwareFilter(0,0, VehicleBusHW.CANFrameType.STANDARD)
        };

        VehicleBusCAN can = new VehicleBusCAN(requireContext(),false);
        can.start(currentBitrate,listenerMode,filter,currentPort,null);
        requireContext().registerReceiver(receiver, new IntentFilter("com.micronet.sampleapp.canframe_received"));
        enableConfigView(false);
        return 0;
    }

    public void closeCanbus() {
        if (vehicleBusWrapper!=null){
            vehicleBusWrapper.stop("CAN");
            vehicleBusWrapper=null;
            try{
                requireContext().unregisterReceiver(receiver);
            } catch (IllegalArgumentException ignore){}
            canOpened=false;
        }
        enableConfigView(true);
    }

    public void clearData() {
        allReceivedData = "";
        receivedData.setText(allReceivedData);
        Log.e("Counter",""+counter);
        counter=0;
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