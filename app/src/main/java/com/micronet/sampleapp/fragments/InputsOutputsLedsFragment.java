/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.sampleapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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

    Switch out0;
    Switch out1;

    boolean isRedChecked = false;
    boolean isGreenChecked = false;
    boolean isBlueChecked = false;
    boolean isRed1Checked = false;
    boolean isGreen1Checked = false;
    boolean isBlue1Checked = false;

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
        updateInputValues();

        //Outputs

        out0 = rootView.findViewById(R.id.switchOutput0);
        out1 = rootView.findViewById(R.id.switchOutput1);
        out0.setOnCheckedChangeListener(this);
        out1.setOnCheckedChangeListener(this);
        updateOutputState();

        //Lights
        Switch switchIRLightButton = rootView.findViewById(R.id.switchIRLight);
        switchIRLightButton.setChecked(false);
        switchIRLightButton.setOnCheckedChangeListener(this);
        CheckBox red = rootView.findViewById(R.id.checkbox_red);
        CheckBox green = rootView.findViewById(R.id.checkbox_green);
        CheckBox blue = rootView.findViewById(R.id.checkbox_blue);
        CheckBox red1 = rootView.findViewById(R.id.checkbox_red1);
        CheckBox green1 = rootView.findViewById(R.id.checkbox_green1);
        CheckBox blue1 = rootView.findViewById(R.id.checkbox_blue1);
        red.setChecked(false);
        green.setChecked(false);
        blue.setChecked(false);
        red1.setChecked(false);
        green1.setChecked(false);
        blue1.setChecked(false);
        red.setOnCheckedChangeListener(this);
        green.setOnCheckedChangeListener(this);
        blue.setOnCheckedChangeListener(this);
        red1.setOnCheckedChangeListener(this);
        green1.setOnCheckedChangeListener(this);
        blue1.setOnCheckedChangeListener(this);
        setNotificationLight(false,false,false,false,false,false);
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

        devTypeMsg = MainActivity.getDevTypeMessage();
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
        if (MainActivity.devType == MainActivity.SC200_MINIMAL || MainActivity.devType == MainActivity.SC200_MID) {
            rootView.findViewById(R.id.inputsView).setVisibility(View.INVISIBLE);
        } else {
            rootView.findViewById(R.id.inputsView).setVisibility(View.VISIBLE);
            if (MainActivity.devType == MainActivity.SC200_FULL || MainActivity.devType == MainActivity.SC200_FULL_BATTERY) {
                input0.setText(((getInput(0) == 0) ? "OFF (" : "ON (") + getInput(0) + ")");
                input1.setText(((getInput(1) == 0) ? "OFF (" : "ON (") + getInput(1) + ")");
            } else {
                rootView.findViewById(R.id.input0).setVisibility(View.INVISIBLE);
                rootView.findViewById(R.id.input1).setVisibility(View.INVISIBLE);
            }
        }

    }

    public void updateOutputState() {
        if (MainActivity.devType == MainActivity.SC200_MINIMAL || MainActivity.devType == MainActivity.SC200_MID) {
            rootView.findViewById(R.id.outputsView).setVisibility(View.INVISIBLE);
        } else {
            rootView.findViewById(R.id.outputsView).setVisibility(View.VISIBLE);
            int[] outputValueList = getOutputsState();
            if (outputValueList.length >= 1) {
                out0.setChecked((outputValueList[0] == 1) ? true : false);
            }
            if (outputValueList.length >= 2) {
                out1.setChecked((outputValueList[1] == 1) ? true : false);
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
            case R.id.switchIRLight:
                if (isChecked) {
                    setLight(0xFFFFFFFF, "LIGHT_ID_BACKLIGHT");
                } else {
                    //if (MainActivity.devType == MainActivity.SMARTCAM_BASIC || MainActivity.devType == MainActivity.SMARTCAM_ENHANCED) {
                        setLight(0x00000000, "LIGHT_ID_BACKLIGHT");
                    //} else {
                    //    setLight(0x80808080, "LIGHT_ID_BACKLIGHT");
                    //}
                }
                break;
            case R.id.checkbox_red:
                isRedChecked = isChecked;
                setNotificationLight(isRedChecked, isGreenChecked, isBlueChecked, isRed1Checked,isGreen1Checked,isBlue1Checked);
                break;
            case R.id.checkbox_green:
                isGreenChecked = isChecked;
                setNotificationLight(isRedChecked, isGreenChecked, isBlueChecked, isRed1Checked,isGreen1Checked,isBlue1Checked);
                break;
            case R.id.checkbox_blue:
                isBlueChecked = isChecked;
                setNotificationLight(isRedChecked, isGreenChecked, isBlueChecked, isRed1Checked,isGreen1Checked,isBlue1Checked);
                break;
            case R.id.checkbox_red1:
                isRed1Checked = isChecked;
                setNotificationLight(isRedChecked, isGreenChecked, isBlueChecked, isRed1Checked,isGreen1Checked,isBlue1Checked);
                break;
            case R.id.checkbox_green1:
                isGreen1Checked = isChecked;
                setNotificationLight(isRedChecked, isGreenChecked, isBlueChecked, isRed1Checked,isGreen1Checked,isBlue1Checked);
                break;
            case R.id.checkbox_blue1:
                isBlue1Checked = isChecked;
                setNotificationLight(isRedChecked, isGreenChecked, isBlueChecked, isRed1Checked,isGreen1Checked,isBlue1Checked);
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

    public void setNotificationLight(boolean red0, boolean green0, boolean blue0,boolean red1, boolean green1, boolean blue1) {
        int lightColor = (0xFF << 24) | ((red0 ? 0xFF : 0x00) << 16);
        setLight(lightColor, "LIGHT_ID_BATTERY");

        lightColor = (0x30 << 24) | ((red1 ? 0x2 : 0x00) << 16) | ((green1 ? 0x2 : 0x00) << 8) | (blue1 ? 0x2 : 0x00)
                | ((green0 ? 0x1 : 0x00) << 8) | (blue0 ? 0x1 : 0x00);
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
