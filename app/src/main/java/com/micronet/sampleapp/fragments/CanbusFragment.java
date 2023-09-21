/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.sampleapp.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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
import com.micronet.sampleapp.activities.MainActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Canbus Fragment Class
 */
public class CanbusFragment extends Fragment implements OnClickListener, AdapterView.OnItemSelectedListener {

    private View rootView;
    Spinner bitrateList;
    RadioGroup listenerModeGroup;
    Button openCan;
    Button closeCan;
    EditText hwFilterIds;
    EditText hwMaskTypes;
    EditText swIdFltr;
    TextView receivedData;
    Button clearData;
    Button sendData;
    EditText dataToSend;
    String[] canbusBitrateList = {"125Kbit", "250Kbit", "500Kbit", "800Kbit", "1Mbit"};
    int[] canbusBitrateListValues = {125000, 250000, 500000, 800000, 1000000};
    int currentBitrate = 125000;
    boolean listenerMode = false;

    public static MainActivity mainActivity;
    String receivedDataValue = null;
    protected ReadThread mReadThread;



    Class<?> canServiceClass;
    Method bitrate;
    Method mode;
    Method link;
    Method open;
    Method bind;
    Method close;
    Method config;
    Method send;
    Method receiveMsg;
    Method mask;
    Object canService;

    int socket1;
    int socket2;
    int idx1;
    int idx2;
    int totalDropped;

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

        hwFilterIds = rootView.findViewById(R.id.hw_filter_ids);
        swIdFltr = rootView.findViewById(R.id.sw_id_filter);
        hwMaskTypes = rootView.findViewById(R.id.hw_mask_types);

        bitrateList = rootView.findViewById(R.id.bitrateList);
        bitrateList.setOnItemSelectedListener(this);
        ArrayAdapter<String> bitrateAdapter = new ArrayAdapter<>(requireActivity(), R.layout.spinner_item, canbusBitrateList);
        bitrateList.setAdapter(bitrateAdapter);

        listenerModeGroup = rootView.findViewById(R.id.listenerModeGroup);
        listenerModeGroup.setOnCheckedChangeListener((group, checkedId) -> listenerMode = checkedId != R.id.lisModeOff);

        openCan = rootView.findViewById(R.id.openCan);
        openCan.setOnClickListener((v)->configAndOpenCan());
        closeCan = rootView.findViewById(R.id.closeCan);
        closeCan.setOnClickListener((v)->closeCanbus());
        closeCan.setEnabled(false);
        receivedData = rootView.findViewById(R.id.receivedData);
        clearData = rootView.findViewById(R.id.clearData);
        clearData.setOnClickListener((v)->clearData());
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
        if (v.getId() == R.id.sendData) {
            if (TextUtils.isEmpty(dataToSend.getText().toString())) {
                Toast.makeText(getContext(), "Enter data!", Toast.LENGTH_LONG).show();
            } else {
                byte[] data = dataToSend.getText().toString().getBytes();
                int size = Math.min(data.length, 8);
                byte[] allData = new byte[size];
                System.arraycopy(data, 0, allData, 0, size);
                sendData(allData);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.bitrateList) {
            currentBitrate = canbusBitrateListValues[position];
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void sendData(byte[] data) {
        try {
            send.invoke(canService, socket2, 255, data.length, data);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            IntBuffer idBuffer =ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
            IntBuffer dlcBuffer =ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
            IntBuffer droppedBuffer =ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
            LongBuffer tsBuffer=ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).asLongBuffer();
            ByteBuffer plBuffer=ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder());

            idBuffer.rewind();
            dlcBuffer.rewind();
            droppedBuffer.rewind();
            tsBuffer.rewind();
            plBuffer.rewind();
            byte[] data = new byte[8];
            while (!isInterrupted()) {
                try {
                    receiveMsg.invoke(canService, socket1, idx1, idBuffer, dlcBuffer, tsBuffer, droppedBuffer, plBuffer);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                if (isInterrupted()) break;

                int dlcInt = dlcBuffer.get();

                System.arraycopy(plBuffer.array(), plBuffer.arrayOffset(), data,0,dlcInt);
                onDataReceived(data,dlcInt);
                totalDropped=droppedBuffer.get();

                idBuffer.rewind();
                dlcBuffer.rewind();
                droppedBuffer.rewind();
                tsBuffer.rewind();
                plBuffer.rewind();
            }
        }
    }

    public void onDataReceived(final byte[] buffer, int size) {
        receivedDataValue = new String(buffer, StandardCharsets.ISO_8859_1);
        final String allReceivedData = receivedDataValue.substring(0, size);

        mainActivity.runOnUiThread(() -> receivedData.setText(allReceivedData));

    }

    public void configAndOpenCan() {
        String[] idsStr, maskStr, typeStr;
        IntBuffer hwFilter= ByteBuffer.allocateDirect(24).order(ByteOrder.nativeOrder()).asIntBuffer();
        IntBuffer hwMask =ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).asIntBuffer();
        IntBuffer swIds = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        IntBuffer swFilter = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

        hwFilter.put(0).put(0).put(0).put(0).put(0).put(0);
        hwFilter.rewind();
        if (!TextUtils.isEmpty(hwFilterIds.getText().toString())) {
            idsStr = hwFilterIds.getText().toString().split(",");
            int size = Math.min(idsStr.length, 6);
            Log.e("Size", ""+size);
            for (int i = 0; i < size; i++) {
                hwFilter.put((int)Long.parseLong(idsStr[i],16));
                SystemClock.sleep(20);
                Log.e("HWFilter", ""+Integer.toHexString((int)Long.parseLong(idsStr[i],16)));
            }
        }
        hwMask.put(0).put(0);
        hwMask.rewind();
        if (!TextUtils.isEmpty(hwMaskTypes.getText().toString())) {
            typeStr = hwMaskTypes.getText().toString().split(",");
            int size = Math.min(typeStr.length, 2);
            for (int i = 0; i < size; i++) {
                hwMask.put((int)Long.parseLong(typeStr[i],16));
            }
        }
        if (TextUtils.isEmpty(swIdFltr.getText().toString())) {
            swIds.put(0);
            swFilter.put(0);
        } else {
            maskStr = swIdFltr.getText().toString().split(",");
            swIds.put((int)Long.parseLong(maskStr[0],16));
            swFilter.put((int)Long.parseLong(maskStr[1],16));
        }
        enableConfigView(false);
        new Thread(()->{
            try {
                link.invoke(canService, "down");
                SystemClock.sleep(200);

                bitrate.invoke(canService,currentBitrate);
                SystemClock.sleep(200);

                mode.invoke(canService, listenerMode?"listen-only":"normal");
                SystemClock.sleep(200);

                Log.e("MASK",""+mask.invoke(canService,hwMask,hwFilter));
                SystemClock.sleep(200);


                link.invoke(canService, "up");
                SystemClock.sleep(200);

                socket1 = (int) open.invoke(canService, "can0");
                socket2 = (int) open.invoke(canService, "can0");
                idx1 =(int)bind.invoke(canService, "can0", socket1);
                idx2 =(int)bind.invoke(canService, "can0", socket2);
                config.invoke(canService, "can0", swIds, swFilter, 0, 0, socket1, 0, 0);
                config.invoke(canService, "can0", swIds, swFilter, 0, 0, socket2, 0, 0);
                mReadThread= new ReadThread();
                mReadThread.start();
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                mainActivity.runOnUiThread(()->enableConfigView(true));
            }
        }).start();
    }

    public void closeCanbus() {
        try {
            if (close != null) {
                close.invoke(canService, socket1);
                close.invoke(canService, socket2);
            }
            if (link != null) {
                link.invoke(canService, "down");
            }
            enableConfigView(true);
            Log.e("Dropped", ""+totalDropped);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void clearData() {
        receivedData.setText("");

    }

    @SuppressLint("PrivateApi")
    private void importClasses() {
        try {
            canServiceClass = Class.forName("com.android.server.net.CanbusService");
            bitrate = canServiceClass.getDeclaredMethod("bitrate", int.class);
            mode = canServiceClass.getDeclaredMethod("mode", String.class);
            link = canServiceClass.getDeclaredMethod("link", String.class);
            open = canServiceClass.getDeclaredMethod("open", String.class);
            bind = canServiceClass.getDeclaredMethod("bind", String.class, int.class);
            close = canServiceClass.getDeclaredMethod("close", int.class);
            config = canServiceClass.getDeclaredMethod("config", String.class, IntBuffer.class, IntBuffer.class, int.class, int.class, int.class, int.class, int.class);
            send = canServiceClass.getDeclaredMethod("send", int.class,int.class,int.class,byte[].class);
            receiveMsg =canServiceClass.getDeclaredMethod("recvmsg", int.class, int.class, IntBuffer.class,
                    IntBuffer.class, LongBuffer.class, IntBuffer.class, ByteBuffer.class);
            mask=canServiceClass.getDeclaredMethod("mask",IntBuffer.class,IntBuffer.class);
            canService =canServiceClass.getConstructor().newInstance();


        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException | java.lang.InstantiationException e) {
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

    public void updateCanbus() {}
}
