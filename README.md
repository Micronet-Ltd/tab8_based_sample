Micronet Sample Application

This app show how to use Inputs, outputs, Lights, get all permissions (without ask from user to get runtime permissions), Canbus, RS232, J1708, get device type, 
get cradle/ignition state, get info about device, override buttons action (one button on smartcam and two buttons on smarttab):

1) function getPermissions()
	You need to sign the app wwith platform key to get all permissions
	call to this function to get all permissions that listed in manifest (include runtime permissions)

2) function setLight(int color, String id) 
	this function receive color and light id and turn on/off led on device:
	smartcam IR Led - LIGHT_ID_BACKLIGHT
	smartcam mcu Led - LIGHT_ID_KEYBOARD
	Notification Led - LIGHT_ID_BATTERY (for red) and LIGHT_ID_NOTIFICATIONS (for blue and green)
	
3) function getOutputsState()
	get current outputs values
	
4) function setOutput(int outputId, boolean on)
	set/clear (unset) specific output

5) function getInput(int inputNumber)
	get specific input value
	
6) use importClasses once to import all classes (reflection), and then 
	use configAndOpenCan() function to open canbus; closeCanbus() for close canbus
	
7) updateInfoText() function in AboutFragment shows how to get information about device (serial number, MCU version, FPGA version, OS version, Build version and Device model)

8) function getDeviceType() in MainActivity shows how to get device type (smartcam basic, smartcam enhanced, smarttab full, smarttab basic, smarttab standalone (without cradle))

9) function updateCradleIgnState() shows how to get cradle type and ignition state

10) PortsFragment shows how to work with RS232 and J1708

11) functions onKeyDown(int keyCode, KeyEvent event) and  onKeyUp(int keyCode, KeyEvent event) shows how to override buttons:
	one button on smartcam (keyCode KeyEvent.KEYCODE_WINDOW),
	two buttons on smarttab (keyCode KeyEvent.KEYCODE_F1 and KeyEvent.KEYCODE_F2)