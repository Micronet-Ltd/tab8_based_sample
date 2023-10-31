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
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.micronet.canbus.CanbusFrameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;



public class VehicleBusCAN {

    private static final String TAG = "ATS-VBS-CAN"; // for logging


    public static String BUS_NAME = "CAN";

    public static final int PF_REQUEST = 0xEA;              // make a request of another node
    public static final int PF_CLAIMED_ADDRESS = 0xEE;      // claim an address

    public static final int PF_CONNECTION_MANAGE = 0xEC;      // manage a connection
    public static final int PF_CONNECTION_DATA = 0xEB;

    public static int DEFAULT_BITRATE = 250000; // a default to use if bitrate is not specified (and used as 1rst option for auto-detect)
    public static int DEFAULT_CAN_NUMBER = 2; //Todo: Updated default value for CanBus Setting, It's String now

    static final int SAFETY_MAX_OUTGOING_QUEUE_SIZE = 10; // just make sure this queue doesn't ever keep growing forever

    static CANWriteRunnable canWriteRunnable; // thread for writing
    static CANReadRunnable canReadRunnable; // thread for reading


    Handler callbackHandler = null; // the handler that the runnable will be posted to

    Runnable readyRxRunnable = null; // runnable to be posted to handler when the bus is ready for transmit/receive
    Runnable readyTxRunnable = null; // runnable to be posted to handler when the bus is ready for transmit/receive


    final List<VehicleBusWrapper.CANFrame> incomingList = Collections.synchronizedList(new ArrayList<>());
    final List<VehicleBusWrapper.CANFrame> outgoingList = Collections.synchronizedList(new ArrayList<>());


    VehicleBusWrapper busWrapper;

    Context context;
    int myAddress=0xFE;


    int confirmedBusBitrate = 0; // set to a bitrate that we know is working so we can skip listen-only mode
    int confirmedCanNumber = 0;


    public VehicleBusCAN(Context context) {
        busWrapper = VehicleBusWrapper.getInstance();
        VehicleBusWrapper.isUnitTesting = false;
        VehicleBusWrapper.canNumber = DEFAULT_CAN_NUMBER;
        this.context = context;
    }

    public VehicleBusCAN(Context context, boolean isUnitTesting) {
        busWrapper = VehicleBusWrapper.getInstance();
        VehicleBusWrapper.isUnitTesting = isUnitTesting;
        VehicleBusWrapper.canNumber = DEFAULT_CAN_NUMBER;
        this.context = context;
        for (int i=0; i< MAX_TP_CONNECTIONS; i++ )
            connections[i] = new TpConnection();
    }


    public static class CanPacket {

        // The 29 bit ID forms these values
        int priority = 6;
        int source_address;
        int protocol_format; // protocol_format and destination address (16 bits total) form the PGN
        int destination_address; // or protocol_specific

        // The data
        byte[] data = new byte[] {-1, -1, -1, -1, -1, -1, -1, -1}; // there are up to 8 bytes in a packet (set to 0xFF by default)


        void setPGN(int pgn) {
            protocol_format = ((pgn >>> 8) & 0xFF);
            destination_address = (pgn & 0xFF);
        }

        int getPGN() {
            if (protocol_format < 240) // lower byte is always 0 if protocol_format < 240
                return (protocol_format << 8);
            else
                return (protocol_format << 8) + destination_address;
        }


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
        if (!busWrapper.start(BUS_NAME, busReadyReadOnlyCallback, null)) {
            Log.e(TAG, "Error starting bus with bus wrapper.");
            return false;
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
        if (canWriteRunnable != null)
            canWriteRunnable.cancelThread = true;

    } // stop()


    ///////////////////////////////////////////////////////
    // stopAll()
    //  just provides access to the wrapper's stopAll call,
    //  It is better to call this before stop() if we know we will be stopping all buses
    ///////////////////////////////////////////////////////
    public void stopAll() {
        busWrapper.stopAll();
    }


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

        canReadRunnable = new CANReadRunnable(canSocket);

        // If we aren't unit testing, then start the thread
        if (!VehicleBusWrapper.isUnitTesting) {
            Thread clientThread = new Thread(canReadRunnable);
            clientThread.start();
        }

        return true;
    } // startReading()


    ///////////////////////////////////////////////////////////
    // startWriting()
    //  starts a new write thread after a read thread is previously opened
    ///////////////////////////////////////////////////////////
    boolean startWriting() {

        VehicleBusWrapper.CANSocket canSocket = null;

        canSocket = busWrapper.getCANSocket();

        if ( canSocket == null) return false;

        // Safety: make sure we cancel any previous thread if we are starting a new one
        if (canWriteRunnable != null)
            canWriteRunnable.cancelThread = true;

        canWriteRunnable = new CANWriteRunnable(canSocket);

        // If we aren't unit testing, then start the thread
        if (!VehicleBusWrapper.isUnitTesting) {
            Thread clientThread = new Thread(canWriteRunnable);
            clientThread.start();
        }

        return true;
    } // startWriting()


    ///////////////////////////////////////////////
    // isWriteReady()
    //  Are we capable of writing frames to the CAN bus
    //      (checks if the bus socket is setup, not that there is something at the other end at correct bitrate)
    ///////////////////////////////////////////////

    public boolean isWriteReady() {
        try {
            if ((canWriteRunnable != null) &&
                    (canWriteRunnable.isReady)) return true;
        } catch (Exception ignore) {}

        return false;
    }

    ///////////////////////////////////////////////
    // isReadReady()
    //  Are we capable of reading frames from the CAN bus
    //      (checks if the bus socket is setup, not that there is something at the other end at correct bitrate)
    ///////////////////////////////////////////////
    public boolean isReadReady() {
        try {
            if ((canReadRunnable != null) &&
                    (canReadRunnable.isReady)) return true;
        } catch (Exception e) {
            // DO nothing
        }

        return false;
    }



    ///////////////////////////////////////////////////////////////////
    // abortTransmits()
    //  stop attempting to send any Tx packets in progress (maybe our address was changed, etc..)
    ///////////////////////////////////////////////////////////////////
    void abortTransmits() {

        // TODO: kill any frames in the CAN queue (must happen within 50 ms)
        // Is this implemented in CAN API yet?

        // kill any frames in our queue
        synchronized (outgoingList) {
            outgoingList.clear();
        }
    } // abortTransmits

    ///////////////////////////////////////////////////////////////////
    // receiveFrame() : called by CAN thread when something is received
    ///////////////////////////////////////////////////////////////////
    void receiveFrame(VehicleBusWrapper.CANFrame frame) {
        Intent intent = new Intent("com.micronet.sampleapp.canframe");
        intent.putExtra("ID", frame.getId());
        intent.putExtra("DATA", frame.getData());
        intent.putExtra("IS_EXTENDED", frame.getType()== CanbusFrameType.EXTENDED);
        context.sendBroadcast(intent);
    } // receiveFrame()


    ///////////////////////////////////////////////////////////////////
    // sendFrame() : safe to call from a different thread than the CAN threads
    //  queues a frame to be sent by the write thread
    ///////////////////////////////////////////////////////////////////
    void sendFrame(VehicleBusWrapper.CANFrame frame) {
        synchronized (outgoingList) {
            if (outgoingList.size() < SAFETY_MAX_OUTGOING_QUEUE_SIZE) {
                outgoingList.add(frame);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////
    // clearQueues()
    //  This is used in testing to clear the incoming and outgoing frames when starting a test
    ///////////////////////////////////////////////////////////////////
    void clearQueues() {
        synchronized (incomingList) {
            incomingList.clear();
        }

        synchronized (outgoingList) {
            outgoingList.clear();
        }
    } //clearQueues()




    // We need separate threads for sending and receiving data since both are blocking operations
    ////////////////////////////////////////////////////////
    // CANWriteRunnable : this is the code that runs on another thread and
    //  handles CAN writing to bus
    ////////////////////////////////////////////////////////
    class CANWriteRunnable implements Runnable {


        volatile boolean cancelThread = false;
        volatile boolean isClosed = false;
        volatile boolean isReady = false;
        //CanbusInterface canInterface;
        VehicleBusWrapper.CANSocket canWriteSocket;

        CANWriteRunnable(VehicleBusWrapper.CANSocket socket) {
//                CanbusInterface new_canInterface) {
            canWriteSocket = socket;
        }

        public void run() {

            VehicleBusWrapper.CANFrame outFrame = null;

            while (!cancelThread) {

                // remove anything in our outgoing queues and connections
                abortTransmits();

                if (!cancelThread) {
                    // Notify the main thread that we are ready for write

                    if ((callbackHandler != null) && (readyTxRunnable != null)) {
                        callbackHandler.post(readyTxRunnable);
                    }
                    Log.v(TAG, "CAN-Write thread ready");
                    isReady = true;

                }


                while (!cancelThread) {

                    // try and send a packet
                    outFrame = null;
                    // get what we need to send
                    synchronized (outgoingList) {
                        if (outgoingList.size() > 0) {
                            outFrame = outgoingList.get(0);
                            outgoingList.remove(0);
                        }
                    }
                    if (outFrame == null) {
                        SystemClock.sleep(5); // we can wait 5 ms if nothing to send.
                    } else {
                        //Log.w(TAG, "frame --> " + String.format("%02x", outFrame.getId()) + " : " + Log.bytesToHex(outFrame.getData(), outFrame.getData().length));
                        try {
                            canWriteSocket.write(outFrame);

                            //Log.d(TAG, "Write Returns");
                        } catch (Exception e) {
                            // exceptions are expected if the interface is closed
                            Log.e(TAG, "Exception on write socket. Canceling Thread");
                            cancelThread = true;
                        }
                    }
                } // thread not canceled

            } // thread not cancelled

            isReady = false;
            Log.v(TAG, "CAN Write Thread terminated");
            isClosed = true;

        } // run
    } // CAN Write communications (runnable)





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
//            CanbusInterface new_canInterface) {
            //canInterface = new_canInterface;
            canReadSocket = new_canSocket;
        }

        public void run() {


            while (!cancelThread) {

                // also remove anything that was incoming on last bus (so we know what bus it arrived on)
                synchronized (incomingList) {
                    incomingList.clear();
                }


                VehicleBusWrapper.CANFrame inFrame = null;


                if (!cancelThread) {
                    // Notify the main thread that we are ready for read
                    if ((callbackHandler != null) && (readyRxRunnable != null)) {
                        callbackHandler.post(readyRxRunnable);
                    }
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

    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    // Transport Protocol & Connection Management
    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////


    // Max CAN frame size is 8 bytes, hence the need for Transport protocol

    public static final int TP_CM_RTS = 16; // Request
    public static final int TP_CM_CTS = 17; // Clear
    public static final int TP_CM_EOM = 19; // end of message ACK
    public static final int TP_CM_ABORT = 255;
    public static final int TP_CM_BAM = 32; // broadcast announce


    // TP Connection abort reasons per J1939
    public static final int TP_ABORT_MAXCONNECTIONS =1;
    public static final int TP_ABORT_NOSYSTEMRESOURCES =2;
    public static final int TP_ABORT_TIMEOUT =3;

    class TpConnection {
        int pgn;
        int expected_bytes;
        int expected_packets; //if this is set to 0 then the connection is "closed"
        int max_packets_per_burst;
        int source_address; // the remote address that sourced the connection
        int destination_address; // us (or the global address)
        long timeout_elapsedms; // time that this connection will expire in elapsed ms
        byte[] data;
    } // class

    public static final int MAX_TP_CONNECTIONS = 5; // maximum # of simultaneous connections
    // note: we may have a couple nodes sending multiple DTCs, and a couple nodes sending VIN responses at the same time
    public static final int MAX_TP_FRAMES_PER_BURST = 1; // maximum number of frames we will accept in each burst


    public static final int TP_TIMEOUT_MS = 1250; // 1250 ms is the maximum timout

    // this is the list of pgns that we will accept TP connections for:

    TpConnection[] connections = new TpConnection[MAX_TP_CONNECTIONS]; // list of open connections

    public static long littleEndian2Long(byte[] bytearray, int start_index, int number_of_bytes) {
        long l;
        int i;

        l = 0;
        for (i = start_index+number_of_bytes-1; i >= start_index; i--) {
            l <<= 8;
            l |= (bytearray[i] & 0xFF);
        }

        return l;
    } // littleEndian2Long

    public static void long2LittleEndian(long value, byte[] bytearray, int start_index, int number_of_bytes) {

        int i;

        for (i = start_index; i < start_index + number_of_bytes; i++) {
            bytearray[i] = (byte) (value & 0xFF);
            value >>>= 8;
        }

    }

    private void sendPacket(CanPacket packet) {
        //Log.vv(TAG, "SendPacket()");

        VehicleBusHW.CANFrame frame = packet2frame(packet);
        synchronized (outgoingList) {
            outgoingList.add(frame);
        }
        //can.sendFrame(frame);

    } // sendPacket()

    VehicleBusHW.CANFrame packet2frame(CanPacket packet) {


        // A J1939 CAN 29b frame ID looks like:
        // PRI(3) RSRV(1) DP(1) PF(8) PS(8) SA(8)

        int frameId = packet.priority;
        frameId <<= 1;
        frameId |= 0; // reserved
        frameId <<= 1;
        frameId |= 0; // Data Page
        frameId <<= 8;
        frameId |= packet.protocol_format;
        frameId <<= 8;
        frameId |= packet.destination_address; // or protocol-specific for some formats
        frameId <<= 8;
        frameId |= packet.source_address;


        return new VehicleBusHW.CANFrame(frameId,Arrays.copyOf(packet.data, 8), VehicleBusHW.CANFrameType.EXTENDED);
    } // packet2frame()

    public void sendConnectCTS(int to_address, int pgn, int max_packets, int first_packet) {

        CanPacket packet = new CanPacket();
        packet.protocol_format = PF_CONNECTION_MANAGE;
        packet.destination_address = to_address;
        packet.source_address = myAddress;

        packet.data[0] = TP_CM_CTS;
        packet.data[1] = (byte) max_packets;
        packet.data[2] = (byte) first_packet;

        // [5] .. 7] is the pgn
        long2LittleEndian(pgn, packet.data, 5, 3);

        sendPacket(packet);

    } // sendConnectCTS()

    ///////////////////////////////////////////////////////////////////
    // findOpenConnection()
    //  returns the index of an open connection if one exists.
    //  returns -1 if no open connection exists
    ///////////////////////////////////////////////////////////////////
    int findOpenConnection(int source_address, int destination_address) {
        for (int i=0 ; i< MAX_TP_CONNECTIONS; i++) {
            if ((connections[i].source_address == source_address) &&
                    (connections[i].destination_address == destination_address) &&
                    (connections[i].expected_packets != 0))  {
                return i; // index of the open connection
            }
        }

        return -1; // no open connection ID
    } // findOpenConnection()

    boolean isConnectionIdAvailable(int connectionID) {
        return connections[connectionID].expected_packets == 0;
    } // isConnectionIdAvailable()


} // CAN class
