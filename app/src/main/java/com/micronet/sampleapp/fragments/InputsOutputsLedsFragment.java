/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.sampleapp.fragments;

import android.Manifest;
import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import com.micronet.sampleapp.R;
import com.micronet.sampleapp.activities.MainActivity;
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

    TextView input0;
    TextView input1;
    TextView input2;
    TextView input3;
    TextView input4;
    TextView input5;
    TextView input6;
    TextView input7;

    Switch out0;
    Switch out1;
    Switch out2;
    Switch out3;

    boolean isRedChecked = false;
    boolean isGreenChecked = false;
    boolean isBlueChecked = false;

    private CameraManager camManager;

    public InputsOutputsLedsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPermissions();
        Log.d(TAG, "onCreate");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_inputs_outputs_leds, container, false);

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

        //Outputs

        out0 = rootView.findViewById(R.id.switchOutput0);
        out1 = rootView.findViewById(R.id.switchOutput1);
        out2 = rootView.findViewById(R.id.switchOutput2);
        out3 = rootView.findViewById(R.id.switchOutput3);
        out0.setOnCheckedChangeListener(this);
        out1.setOnCheckedChangeListener(this);
        out2.setOnCheckedChangeListener(this);
        out3.setOnCheckedChangeListener(this);
        updateOutputState();

        //Lights
        Switch switchFlashLightButton = rootView.findViewById(R.id.switchFlashLight);
        switchFlashLightButton.setChecked(false);
        switchFlashLightButton.setOnCheckedChangeListener(this);
        Switch switchMcuLightButton = rootView.findViewById(R.id.switchMcuLight);
        switchMcuLightButton.setChecked(false);
        switchMcuLightButton.setOnCheckedChangeListener(this);
        Switch switchIRLightButton = rootView.findViewById(R.id.switchIRLight);
        switchIRLightButton.setChecked(false);
        switchIRLightButton.setOnCheckedChangeListener(this);
        CheckBox red = rootView.findViewById(R.id.checkbox_red);
        CheckBox green = rootView.findViewById(R.id.checkbox_green);
        CheckBox blue = rootView.findViewById(R.id.checkbox_blue);
        red.setChecked(false);
        green.setChecked(false);
        blue.setChecked(false);
        red.setOnCheckedChangeListener(this);
        green.setOnCheckedChangeListener(this);
        blue.setOnCheckedChangeListener(this);
        setNotificationLight(isRedChecked, isGreenChecked, isBlueChecked);
        if (MainActivity.getBoardType() < 2) {
            MainActivity.setViewAndChildrenEnabled(rootView.findViewById(R.id.mcuLayout), false);
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        updateCradleIgnState();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    public void updateCradleIgnState() {
        String cradleStateMsg, ignitionStateMsg, devTypeMsg;

        devTypeMsg = MainActivity.getDevTypeMessage(MainActivity.getDeviceType());
        switch (MainActivity.dockState) {
            case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                cradleStateMsg = getString(R.string.not_in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_unknown_state_text);
                break;
            case Intent.EXTRA_DOCK_STATE_DESK:
            case Intent.EXTRA_DOCK_STATE_LE_DESK:
            case Intent.EXTRA_DOCK_STATE_HE_DESK:
                cradleStateMsg = getString(R.string.in_cradle_state_text) + "(" + MainActivity.getCradleType(MainActivity.cradleType) + ")";
                ignitionStateMsg = getString(R.string.ignition_off_state_text);
                break;
            case Intent.EXTRA_DOCK_STATE_CAR:
                cradleStateMsg = getString(R.string.in_cradle_state_text) + "(" + MainActivity.getCradleType(MainActivity.cradleType) + ")";
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
        TextView devTypeTextView = rootView.findViewById(R.id.textViewDevType);
        cradleStateTextview.setText(cradleStateMsg);
        ignitionStateTextview.setText(ignitionStateMsg);
        devTypeTextView.setText(devTypeMsg);
    }

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

    public void updateInputState(int inputNum, int inputValue) {
        if (Integer.toString(inputValue) != null) {
            getInputView(inputNum).setText(((inputValue == 0) ? "OFF (" : "ON (") + Integer.toString(inputValue) + ")");
        } else {
            getInputView(inputNum).setText("OFF (0)");
        }
        Log.d(TAG, "Vinput event received. Input number: " + inputNum + ". InputValue: " + inputValue);
    }

    public void updateInputValues() {
        if (MainActivity.devType == MainActivity.SMARTTAB_STAND_ALONE) {
            rootView.findViewById(R.id.inputsView).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.inputsUnavaliable).setVisibility(View.VISIBLE);
        } else {
            rootView.findViewById(R.id.inputsUnavaliable).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.inputsView).setVisibility(View.VISIBLE);
            input0.setText(((getInput(0) == 0) ? "OFF (" : "ON (") + getInput(0) + ")");
            input1.setText(((getInput(1) == 0) ? "OFF (" : "ON (") + getInput(1) + ")");
            if (MainActivity.devType == MainActivity.SMARTTAB_CRADLE_ENHANCED) {
                input2.setText(((getInput(2) == 0) ? "OFF (" : "ON (") + getInput(2) + ")");
                input3.setText(((getInput(3) == 0) ? "OFF (" : "ON (") + getInput(3) + ")");
                input4.setText(((getInput(4) == 0) ? "OFF (" : "ON (") + getInput(4) + ")");
                input5.setText(((getInput(5) == 0) ? "OFF (" : "ON (") + getInput(5) + ")");
                input6.setText(((getInput(6) == 0) ? "OFF (" : "ON (") + getInput(6) + ")");
                input7.setText(((getInput(7) == 0) ? "OFF (" : "ON (") + getInput(7) + ")");
            } else {
                rootView.findViewById(R.id.in2).setVisibility(View.INVISIBLE);
                rootView.findViewById(R.id.in3).setVisibility(View.INVISIBLE);
                rootView.findViewById(R.id.in4).setVisibility(View.INVISIBLE);
                rootView.findViewById(R.id.in5).setVisibility(View.INVISIBLE);
                rootView.findViewById(R.id.in6).setVisibility(View.INVISIBLE);
                rootView.findViewById(R.id.in7).setVisibility(View.INVISIBLE);
            }
        }

    }

    public void updateOutputState() {
        if (MainActivity.devType == MainActivity.SMARTTAB_STAND_ALONE) {
            rootView.findViewById(R.id.outputsView).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.outputsUnavaliable).setVisibility(View.VISIBLE);
        } else {
            rootView.findViewById(R.id.outputsUnavaliable).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.outputsView).setVisibility(View.VISIBLE);
            int[] outputValueList = getOutputsState();
            if (outputValueList.length >= 1) {
                out0.setChecked((outputValueList[0] == 1) ? true : false);
            }
            if (outputValueList.length >= 2) {
                out1.setChecked((outputValueList[1] == 1) ? true : false);
            }
            if (MainActivity.devType == MainActivity.SMARTTAB_CRADLE_ENHANCED) {
                if (outputValueList.length >= 3) {
                    out2.setChecked((outputValueList[2] == 1) ? true : false);
                }
                if (outputValueList.length >= 4) {
                    out3.setChecked((outputValueList[3] == 1) ? true : false);
                }
            } else {
                rootView.findViewById(R.id.out2).setVisibility(View.INVISIBLE);
                rootView.findViewById(R.id.out3).setVisibility(View.INVISIBLE);
            }
        }

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
        char[] temp = Integer.toBinaryString(Integer.parseInt(res, 16)).toCharArray();
        int[] result = new int[temp.length];
        for (int i = 0; i < temp.length; i++) {
            result[i] = Integer.parseInt(String.valueOf(temp[temp.length - 1 - i]));
        }
        return result;
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
            case R.id.switchIRLight:
                if (isChecked) {
                    setLight(0xFFFFFFFF, "LIGHT_ID_BACKLIGHT");
                } else {
                    if (MainActivity.devType == MainActivity.SMARTCAM_BASIC || MainActivity.devType == MainActivity.SMARTCAM_ENHANCED) {
                        setLight(0x00000000, "LIGHT_ID_BACKLIGHT");
                    } else {
                        setLight(0x80808080, "LIGHT_ID_BACKLIGHT");
                    }
                }
                break;
            case R.id.checkbox_red:
                isRedChecked = isChecked;
                setNotificationLight(isRedChecked, isGreenChecked, isBlueChecked);
                break;
            case R.id.checkbox_green:
                isGreenChecked = isChecked;
                setNotificationLight(isRedChecked, isGreenChecked, isBlueChecked);
                break;
            case R.id.checkbox_blue:
                isBlueChecked = isChecked;
                setNotificationLight(isRedChecked, isGreenChecked, isBlueChecked);
                break;
        }
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
        setLight(color, "LIGHT_ID_KEYBOARD");
    }

    public void setNotificationLight(boolean isRedChecked, boolean isGreenChecked, boolean isBlueChecked) {
        int lightColor = (0xFF << 24) | ((isRedChecked ? 0xFF : 0x00) << 16) | ((isGreenChecked ? 0xFF : 0x00) << 8) | (isBlueChecked ? 0xFF : 0x00);
        setLight(lightColor, "LIGHT_ID_BATTERY");
        setLight(lightColor, "LIGHT_ID_NOTIFICATIONS");
    }

    public void setLight(int color, String id) {
        try {
            @SuppressWarnings("rawtypes")

            Class<?> LightsManager = Class.forName("com.android.server.lights.LightsManager");
            Class<?> Light = Class.forName("com.android.server.lights.Light");
            Field lightId = LightsManager.getField(id);
            Constructor<?> constructor = LightsManager.getConstructor(Context.class);
            Object lightsManagerInstance = constructor.newInstance(getContext());
            Method getLight = LightsManager.getMethod("getLight", int.class);
            Method setColor = Light.getMethod("setColor", int.class);
            setColor.setAccessible(true);
            Object lightInstance = getLight.invoke(lightsManagerInstance, lightId.get(lightsManagerInstance));
            setColor.invoke(lightInstance, color);
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

    public void getPermissions() {
        try {
            @SuppressWarnings("rawtypes")

            Class<?> PermissionsManager = Class.forName("com.android.server.permissions.PermissionsManager");
            Constructor<?> constructor = PermissionsManager.getConstructor();
            Object getPerms = constructor.newInstance();
            Method getPermissions = PermissionsManager.getMethod("getPermissions",String.class , Context.class);
            getPermissions.setAccessible(true);
            getPermissions.invoke(getPerms, "com.micronet.sampleapp", getContext());
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

}
