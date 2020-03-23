# Micronet Sample Application

This app show how to use inputs, outputs, lights, get all permissions (without ask from user to get runtime permissions), Canbus, RS232, J1708, get device type, get cradle/ignition state, get info about device, override buttons action (one button on SmartCam and two buttons on SmartTab):

### Micronet Hardware API

Below is a list of functions that show how to get information/permissions from the device.

**Note**: These functions are built into the OS and are used via reflection. To use these functions in your application, your application must be signed with the platform key.

1) function getPermissions(): You **need** to sign the app with platform key to get all permissions. Call this function to get all permissions that listed in manifest (include runtime permissions).

2) function setLight(int color, String id): This function sets turns on/off an Led and also sets the color of the led on the device. Possibly inputs for the id are: 
	- SmartCam IR Led - LIGHT_ID_BACKLIGHT
	- SmartCam MCU Led - LIGHT_ID_KEYBOARD
	- Notification Led - LIGHT_ID_BATTERY (for red) and LIGHT_ID_NOTIFICATIONS (for blue and green)
	
3) function getOutputsState(): Gets the current outputs values.
	
4) function setOutput(int outputId, boolean on): Set/clear specific output on device.

5) function getInput(int inputNumber): Get specific input value. Value with be 0 if it is low and >0 if it is high.

6) function getDeviceType(): This function in MainActivity shows how to get device type (smartcam basic, smartcam enhanced, smarttab full, smarttab basic, smarttab standalone (without cradle)).

7) functions onKeyDown(int keyCode, KeyEvent event) and  onKeyUp(int keyCode, KeyEvent event) shows how to override buttons:
	- one button on SmartCam (keyCode KeyEvent.KEYCODE_WINDOW),
	- two buttons on SmartTab (keyCode KeyEvent.KEYCODE_F1 and KeyEvent.KEYCODE_F2)

8) The app uses importClasses once to import all classes (reflection), and then use configAndOpenCan() function to open canbus; closeCanbus() for close canbus.

9) function updateCradleIgnState(): This function shows how to get cradle type and ignition state.

10) The updateInfoText() function in AboutFragment shows how to get information about device (serial number, MCU version, FPGA version, OS version, Build version and Device model).

11) The PortsFragment shows how to work with RS232 and J1708.

