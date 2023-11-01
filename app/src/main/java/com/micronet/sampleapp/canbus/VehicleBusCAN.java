/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

/////////////////////////////////////////////////////////////
// VehicleBusCAN:
//  Handles the setup/teardown of threads the control the CAN bus, and communications to/from
/////////////////////////////////////////////////////////////

// API TODO:
// cancel write (do I have to close socket from another thread?)
// cancel read (like when shutting down the socket)


package com.micronet.sampleapp.canbus;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

import com.micronet.canbus.CanbusFrameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;



public class VehicleBusCAN {

    private static final String TAG = "VehicleBusCAN" ; // for logging

    public static String BUS_NAME = "CAN";

    public static int DEFAULT_CAN_NUMBER = 2; //Todo: Updated default value for CanBus Setting, It's String now

    static CANReadRunnable canReadRunnable; // thread for reading

    VehicleBusWrapper busWrapper;

    Context context;
    private HandlerThread writeHandlerThread;
    private HandlerThread readHandlerThread;


    public VehicleBusCAN(Context context) {
        busWrapper = VehicleBusWrapper.getInstance();
        VehicleBusWrapper.canNumber = DEFAULT_CAN_NUMBER;
        this.context = context;
    }

    //////////////////////////////////////////////////////
    // start() : starts the threads to listen and send CAN frames
    //  CAN will start up in one of three modes:
    //      1) Auto-detect (if selected by the function parameter
    //      2) Confirmed (if we previously received frames at this bitrate and haven't switched bitrates or restarted app since)
    //      3) Unconfirmed (all others .. this will start up in listen mode until a frame is received)
    ///////////////////////////////////////////////////////
    public boolean start(int initial_bitrate, boolean listenerMode, VehicleBusWrapper.CANHardwareFilter[] hardwareFilters, int canNumber, ArrayList<VehicleBusHW.CANFlowControl> flowControls) {

        // close any prior socket that still exists
        stop(); // stop any threads and sockets already running

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        busWrapper.setCharacteristics(listenerMode, initial_bitrate, hardwareFilters, canNumber, flowControls);
        if (listenerMode) {
            if (!busWrapper.start(BUS_NAME, busReadyReadOnlyCallback, null)) {
                Log.e(TAG, "Error starting bus with bus wrapper.");
                return false;
            }
        } else {
            if (!busWrapper.start(BUS_NAME, busReadyReadWriteCallback, null)) {
                Log.e(TAG, "Error starting bus with bus wrapper.");
                return false;
            }
        }
        return true;
    } // start()







    ///////////////////////////////////////////////////////
    // stop()
    //  called on shutdown
    ///////////////////////////////////////////////////////
    public void stop() {
        busWrapper.stop(BUS_NAME);

        if (canReadRunnable != null)
            canReadRunnable.cancelThread = true;
        if (readHandlerThread!=null)
            readHandlerThread.quit();
        if (writeHandlerThread !=null) {
            context.unregisterReceiver(sendReceiver);
            writeHandlerThread.quit();
        }

    } // stop()


    // run()
    ///////////////////////////////////////////////////////////
    // busReadyReadWriteCallback()
    //  This is called when a normal socket is ready
    ///////////////////////////////////////////////////////////
    private final Runnable busReadyReadWriteCallback = () -> {
        try {
//                Log.v(TAG, "busReadyReadWriteCallback()");
            startReading();
            startWriting();
//                Log.v(TAG, "busReadyReadWriteCallback() END");
        } catch (Exception e) {
            Log.e(TAG + ".busReadyReadWriteCallback", "Exception: " + e, e);
        }
    }; // busReadyReadWriteCallback()


    // run()
    ///////////////////////////////////////////////////////////
    // busReadyReadOnlyCallback()
    //  This is called when a listen-only socket is ready
    ///////////////////////////////////////////////////////////
    private final Runnable busReadyReadOnlyCallback = () -> {
        try {
//                Log.v(TAG, "busReadyReadOnlyCallback ()");
            startReading();
        } catch (Exception e) {
            Log.e(TAG + ".busReadyReadOnlyCallback ", "Exception: " + e, e);
        }
    }; // busReadyReadOnlyCallback ()


    ///////////////////////////////////////////////////////////
    // startReading()
    //  starts a new read thread after a read thread is previously opened
    ///////////////////////////////////////////////////////////
    boolean startReading() {

        VehicleBusWrapper.CANSocket canSocket = null;

        canSocket = busWrapper.getCANSocket();

        if (canSocket == null) return false;

        // Safety: make sure we cancel any previous thread if we are starting a new one
        if (canReadRunnable != null)
            canReadRunnable.cancelThread = true;
        if (readHandlerThread != null) {
            readHandlerThread.quit();
        }

        canReadRunnable = new CANReadRunnable(canSocket);

        readHandlerThread = new HandlerThread("CANRead");
        readHandlerThread.start();
        Handler handler = new Handler(readHandlerThread.getLooper());
        handler.post(canReadRunnable);
        return true;
    } // startReading()

    final BroadcastReceiver sendReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.micronet.sampleapp.canframe_send".equals(intent.getAction())){
                int id = intent.getIntExtra("ID", -1);
                byte[] data = intent.getByteArrayExtra("DATA");
                boolean type = intent.getBooleanExtra("IS_EXTENDED",false);
                VehicleBusWrapper.CANFrame frame = new VehicleBusHW.CANFrame(id,data,type?VehicleBusHW.CANFrameType.EXTENDED:VehicleBusHW.CANFrameType.STANDARD);
                final VehicleBusHW.CANSocket socket= busWrapper.getCANSocket();
                if (socket!=null) {
                    socket.write(frame);
                }
            }
        }
    };


    ///////////////////////////////////////////////////////////
    // startWriting()
    //  starts a new write thread after a read thread is previously opened
    ///////////////////////////////////////////////////////////
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    boolean startWriting() {

        VehicleBusWrapper.CANSocket canSocket = null;

        canSocket = busWrapper.getCANSocket();

        if ( canSocket == null) return false;

        // Safety: make sure we cancel any previous thread if we are starting a new one
        if (writeHandlerThread != null) {
            context.unregisterReceiver(sendReceiver);
            writeHandlerThread.quit();
        }

        writeHandlerThread = new HandlerThread("CANWrite");
        writeHandlerThread.start();
        Handler writeHandler = new Handler(writeHandlerThread.getLooper());
        context.registerReceiver(sendReceiver,new IntentFilter("com.micronet.sampleapp.canframe_send"),null,writeHandler);

        return true;
    } // startWriting()



    ///////////////////////////////////////////////////////////////////
    // receiveFrame() : called by CAN thread when something is received
    ///////////////////////////////////////////////////////////////////
    void receiveFrame(VehicleBusWrapper.CANFrame frame) {
        Intent intent = new Intent("com.micronet.sampleapp.canframe_received");
        intent.putExtra("ID", frame.getId());
        intent.putExtra("DATA", frame.getData());
        intent.putExtra("IS_EXTENDED", frame.getType()== CanbusFrameType.EXTENDED);
        context.sendBroadcast(intent);
    } // receiveFrame()


    ////////////////////////////////////////////////////////
    // CANRunnable : this is the code that runs on another thread and
    //  handles CAN sending and receiving
    ////////////////////////////////////////////////////////
    class CANReadRunnable implements Runnable {


        volatile boolean cancelThread = false;
        volatile boolean isClosed = false;
        volatile boolean isReady = false;

        //CanbusInterface canInterface;
        VehicleBusWrapper.CANSocket canReadSocket;

        CANReadRunnable(VehicleBusWrapper.CANSocket new_canSocket) {
            canReadSocket = new_canSocket;
        }

        public void run() {


            while (!cancelThread) {
                VehicleBusWrapper.CANFrame inFrame;
                if (!cancelThread) {
                    Log.v(TAG, "CAN-Read thread ready" );
                    isReady = true;
                }

                while (!cancelThread)  {
                    // try and receive a packet
                    inFrame = null;
                    try {
                        inFrame = canReadSocket.read();
                    } catch (Exception e) {
                        // exceptions are expected if the interface is closed
                        Log.e(TAG, "Exception on read socket. Canceling Thread: " + e.getMessage());
                        cancelThread = true;
                    }


                    if (inFrame != null) {
                        receiveFrame(inFrame);
                    }

                } // thread not canceled


            } // thread not cancelled

            isReady = false;
            Log.v(TAG, "CAN Read Thread terminated");
            isClosed = true;

        } // run
    } // CAN Read communications (runnable)
} // CAN class
