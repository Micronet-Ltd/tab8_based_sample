/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.sampleapp.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import com.micronet.sampleapp.R;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * GPIO Fragment Class
 */
public class InputsOutputsLedsFragment extends Fragment implements OnCheckedChangeListener {

    private final String TAG = "InputOutputLedFragment";
    private View rootView;
    private int dockState = -1;
    int cradleType = -1;
    protected int mInputNum = -1;
    protected int mInputValue = -1;

    TextView input0;
    TextView input1;
    TextView input2;
    TextView input3;
    TextView input4;
    TextView input5;
    TextView input6;
    TextView input7;

//    Class SystemProperties;
//    Method getProp = null;
//    String boardType;

    private CameraManager camManager;
    public static final String vInputAction = "android.intent.action.VINPUTS_CHANGED";
    public static final String dockAction = "android.intent.action.DOCK_EVENT";


    public InputsOutputsLedsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(dockAction);
        intentFilter.addAction(vInputAction);

        getActivity().registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_inputs_outputs_leds, container, false);
//        getProp();
//        try {
//            boardType = getProp.invoke(SystemProperties, new Object[]{"persist.vendor.board.config"}).toString();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
        //Outputs
        int[] outputValueList = getOutputsState();
        Switch out0 = rootView.findViewById(R.id.switchOutput0);
        out0.setChecked((outputValueList[0] == 1) ? true : false);
        out0.setOnCheckedChangeListener(this);
        Switch out1 = rootView.findViewById(R.id.switchOutput1);
        out1.setChecked((outputValueList[1] == 1) ? true : false);
        out1.setOnCheckedChangeListener(this);
        Switch out2 = rootView.findViewById(R.id.switchOutput2);
        out2.setChecked((outputValueList[2] == 1) ? true : false);
        out2.setOnCheckedChangeListener(this);
        Switch out3 = rootView.findViewById(R.id.switchOutput3);
        out3.setChecked((outputValueList[3] == 1) ? true : false);
        out3.setOnCheckedChangeListener(this);

        //Inputs
        input0 = rootView.findViewById(R.id.input0);
        input1 = rootView.findViewById(R.id.input1);
        input2 = rootView.findViewById(R.id.input2);
        input3 = rootView.findViewById(R.id.input3);
        input4 = rootView.findViewById(R.id.input4);
        input5 = rootView.findViewById(R.id.input5);
        input6 = rootView.findViewById(R.id.input6);
        input7 = rootView.findViewById(R.id.input7);
        updateInputValues();
        //Lights
        Switch switchFlashLightButton = rootView.findViewById(R.id.switchFlashLight);
        switchFlashLightButton.setChecked(false);
        switchFlashLightButton.setOnCheckedChangeListener(this);
        Switch switchMcuLightButton = rootView.findViewById(R.id.switchMcuLight);
        switchMcuLightButton.setChecked(false);
        switchMcuLightButton.setOnCheckedChangeListener(this);
        Switch switchNotificationLightButton = rootView.findViewById(R.id.switchNotificationLight);
        switchNotificationLightButton.setChecked(false);
        switchNotificationLightButton.setOnCheckedChangeListener(this);
        Switch switchIRLightButton = rootView.findViewById(R.id.switchIRLight);
        switchIRLightButton.setChecked(false);
        switchIRLightButton.setOnCheckedChangeListener(this);

        return rootView;
    }

//    public void getProp(){
//        try {
//
//            @SuppressWarnings("rawtypes")
//            Class sp = Class.forName("android.os.SystemProperties");
//            SystemProperties = sp;
//            getProp = SystemProperties.getMethod("get", new Class[]{String.class});
//
//        } catch (IllegalArgumentException iAE) {
//            throw iAE;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        // Register for local broadcasts
        Context context = getContext();
        if (context != null) {
        }

        updateCradleIgnState();
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        // getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
//        getActivity().unregisterReceiver(mReceiver);
        Log.d(TAG, "onStop");
    }

    private void updateCradleIgnState() {
        String cradleStateMsg, ignitionStateMsg;

        switch (dockState) {
            case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                cradleStateMsg = getString(R.string.not_in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_unknown_state_text);
                break;
            case Intent.EXTRA_DOCK_STATE_DESK:
            case Intent.EXTRA_DOCK_STATE_LE_DESK:
            case Intent.EXTRA_DOCK_STATE_HE_DESK:
                cradleStateMsg = getString(R.string.in_cradle_state_text) + "(" + cradleType(cradleType) + ")";
                ignitionStateMsg = getString(R.string.ignition_off_state_text);
                break;
            case Intent.EXTRA_DOCK_STATE_CAR:
                cradleStateMsg = getString(R.string.in_cradle_state_text) + "(" + cradleType(cradleType) + ")";
                ignitionStateMsg = getString(R.string.ignition_on_state_text);
                break;
            default:
                /* this state indicates un-defined docking state */
                cradleStateMsg = getString(R.string.not_in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_unknown_state_text);
                break;
        }

        TextView cradleStateTextview = rootView.findViewById(R.id.textViewCradleState1);
        TextView ignitionStateTextview = rootView.findViewById(R.id.textViewIgnitionState1);
        cradleStateTextview.setText(cradleStateMsg);
        ignitionStateTextview.setText(ignitionStateMsg);
        //updateInputValues();
    }

    private void setOutput(int outputId, boolean on) {
        try {
            @SuppressWarnings("rawtypes")
            Class InputOutputService = Class.forName("com.android.server.vinputs.InputOutputService");
            Method setMcuOutputs = InputOutputService.getMethod("setMcuOutputs", new Class[]{int[].class, int.class});
            Constructor<?> constructor = InputOutputService.getConstructor();
            Object ioServiceInstance = constructor.newInstance();
            int[] listOfOutputs = {outputId};
            int value = on ? 1 : 0;
            setMcuOutputs.invoke(ioServiceInstance, listOfOutputs, value);
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

    private int[] getOutputsState() {
        int[] lst = null;
        try {
            @SuppressWarnings("rawtypes")
            Class InputOutputService = Class.forName("com.android.server.vinputs.InputOutputService");
            Method getMcuOutputsState = InputOutputService.getMethod("getMcuOutputsState");
            Constructor<?> constructor = InputOutputService.getConstructor();
            Object ioServiceInstance = constructor.newInstance();
            String result = (String) getMcuOutputsState.invoke(ioServiceInstance);
            lst = getOutputsSetList(result);
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
        return lst;
    }

    private int[] getOutputsSetList(String res) {
        int[] result = {0, 0, 0, 0};
        char[] temp = Integer.toBinaryString(Integer.parseInt(res, 16)).toCharArray();
        for (int i = 0; i < temp.length; i++) {
            result[i] = Integer.parseInt(String.valueOf(temp[temp.length - 1 - i]));
        }
        return result;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            goAsync();
            Log.d(TAG, "dock or input action received");
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case dockAction:
                        dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
                        cradleType = intent.getIntExtra("DockValue", -1);
                        updateCradleIgnState();
                        Log.d(TAG, "Dock event received: " + dockState + ", value: " + cradleType);
                        break;
                    case vInputAction:
                        mInputNum = intent.getIntExtra("VINPUT_NUM", -1);
                        mInputValue = intent.getIntExtra("VINPUT_VALUE", -1);
                        if (Integer.toString(mInputValue) != null) {
                            getInputView(mInputNum).setText(((mInputValue == 0) ? "OFF (" : "ON (") + Integer.toString(mInputValue) + ")");
                        } else {
                            getInputView(mInputNum).setText("OFF (0)");
                        }
                        Log.d(TAG, "Vinput event received. Input number: " + mInputNum + ". InputValue: " + mInputValue);
                        break;
                }
            }


        }
    };

    /**
     * Receive input number return input value
     */
    public int getInput(int inputNumber) {
        int inputValue = -1;
        try {
            @SuppressWarnings("rawtypes")
            Class InputOutputService = Class.forName("com.android.server.vinputs.InputOutputService");
            Method readInput = InputOutputService.getMethod("readInput", int.class);
            Constructor<?> constructor = InputOutputService.getConstructor();
            Object ioServiceInstance = constructor.newInstance();
            inputValue = (int) readInput.invoke(ioServiceInstance, inputNumber);
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
        return inputValue;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switchOutput0:
                setOutput(0, isChecked);
                break;
            case R.id.switchOutput1:
                setOutput(1, isChecked);
                break;
            case R.id.switchOutput2:
                setOutput(2, isChecked);
                break;
            case R.id.switchOutput3:
                setOutput(3, isChecked);
                break;
            case R.id.switchFlashLight:
                setFlashLight(isChecked);
                break;
            case R.id.switchMcuLight:
                setMcuLight(isChecked);
                break;
            case R.id.switchNotificationLight:
                if (isChecked) {
                    setNotificationLed(0xFFFFFFFF);
                } else {
                    setNotificationLed(0x00000000);
                }
                break;
            case R.id.switchIRLight:
                if (isChecked) {
                    setIRLed(true);
                } else {
                    setIRLed(false);
                }
                break;
        }
    }

    private TextView getInputView(int inputNum) {
        TextView inputView = null;
        switch (inputNum) {
            case 0:
                inputView = input0;
                break;
            case 1:
                inputView = input1;
                break;
            case 2:
                inputView = input2;
                break;
            case 3:
                inputView = input3;
                break;
            case 4:
                inputView = input4;
                break;
            case 5:
                inputView = input5;
                break;
            case 6:
                inputView = input6;
                break;
            case 7:
                inputView = input7;
                break;
        }
        return inputView;
    }

    public void setFlashLight(boolean setLight) {
        try {
            camManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
            String cameraId = null; // Usually front camera is at 0 position.
            if (camManager != null) {
                cameraId = camManager.getCameraIdList()[0];
                camManager.setTorchMode(cameraId, setLight); //true = turn on; false = turn off
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, e.toString());
        }
    }

    public void setMcuLight(boolean setLight) {
        int color;
        if (setLight) {
            color = 0xFFFFFFFF;
        } else {
            color = 0xFF000000;
        }
        setMcuLed(color);
    }

    public void setMcuLed(int color) {
        try {
            @SuppressWarnings("rawtypes")

            Class<?> LightsManager = Class.forName("com.android.server.lights.LightsManager");
            Class<?> Light = Class.forName("com.android.server.lights.Light");
//            Field lightId;
//            if (boardType.equals("smartcam")){
//                lightId = LightsManager.getField(LIGHT_ID_ALARM);
//            } else {
//                lightId = LightsManager.getField("LIGHT_ID_KEYBOARD");
//            }
            Field lightId = LightsManager.getField("LIGHT_ID_KEYBOARD");
            Constructor<?> constructor = LightsManager.getConstructor(Context.class);
            Object lightsManagerInstance = constructor.newInstance(getContext());
            Method getLight = LightsManager.getMethod("getLight", int.class);
            Method setBrightness = Light.getMethod("setBrightness", int.class);
            Object lightInstance = getLight.invoke(lightsManagerInstance, lightId.get(lightsManagerInstance));
            setBrightness.invoke(lightInstance, color);

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
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * returns String value (from input_all)
     */

    public String getAllInputs() {
        String inputsValue = "";
        try {
            @SuppressWarnings("rawtypes")
            Class InputOutputService = Class.forName("com.android.server.vinputs.InputOutputService");
            Method readAllInputs = InputOutputService.getMethod("readAllInputs");
            Constructor<?> constructor = InputOutputService.getConstructor();
            Object ioServiceInstance = constructor.newInstance();
            inputsValue = (String) readAllInputs.invoke(ioServiceInstance);
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
        return inputsValue;
    }

    private void updateInputValues() {
        input0.setText(((getInput(0) == 0) ? "OFF (" : "ON (") + getInput(0) + ")");
        input1.setText(((getInput(1) == 0) ? "OFF (" : "ON (") + getInput(1) + ")");
        input2.setText(((getInput(2) == 0) ? "OFF (" : "ON (") + getInput(2) + ")");
        input3.setText(((getInput(3) == 0) ? "OFF (" : "ON (") + getInput(3) + ")");
        input4.setText(((getInput(4) == 0) ? "OFF (" : "ON (") + getInput(4) + ")");
        input5.setText(((getInput(5) == 0) ? "OFF (" : "ON (") + getInput(5) + ")");
        input6.setText(((getInput(6) == 0) ? "OFF (" : "ON (") + getInput(6) + ")");
        input7.setText(((getInput(7) == 0) ? "OFF (" : "ON (") + getInput(7) + ")");
    }

    public String cradleType(int dockValue) {
        String res = "Basic";
        String temp = Integer.toBinaryString(dockValue);
        if ((temp.length() >= 3) && (temp.charAt(temp.length() - 3) == '1')) {
            res = "Smart";
        }
        return res;
    }

    public void setNotificationLed(int color) {
        try {
            @SuppressWarnings("rawtypes")

            Class<?> LightsManager = Class.forName("com.android.server.lights.LightsManager");
            Class<?> Light = Class.forName("com.android.server.lights.Light");
            Field lightId = LightsManager.getField("LIGHT_ID_NOTIFICATIONS");
            Constructor<?> constructor = LightsManager.getConstructor(Context.class);
            Object lightsManagerInstance = constructor.newInstance(getContext());
            Method getLight = LightsManager.getMethod("getLight", int.class);
            Method setBrightness = Light.getMethod("setBrightness", int.class);
            Object lightInstance = getLight.invoke(lightsManagerInstance, lightId.get(lightsManagerInstance));
            setBrightness.invoke(lightInstance, color);

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
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public void setIRLed(boolean on) {
        try {
            @SuppressWarnings("rawtypes")

            Class<?> LightsManager = Class.forName("com.android.server.lights.LightsManager");
            Class<?> Light = Class.forName("com.android.server.lights.Light");
            Field lightId = LightsManager.getField("LIGHT_ID_BACKLIGHT");
            Constructor<?> constructor = LightsManager.getConstructor(Context.class);
            Object lightsManagerInstance = constructor.newInstance(getContext());
            Method getLight = LightsManager.getMethod("getLight", int.class);
            Method setIRLed = Light.getMethod("setIRLed", boolean.class);
            Object lightInstance = getLight.invoke(lightsManagerInstance, lightId.get(lightsManagerInstance));
            setIRLed.invoke(lightInstance, on);

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
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

}
