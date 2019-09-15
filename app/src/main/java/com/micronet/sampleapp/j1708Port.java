package com.micronet.sampleapp;

import android.util.Log;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class j1708Port {

    private static final String TAG = "j1708Port";

    /*
     * Do not remove or rename the field mFd: it is used by native method close();
     */
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    public j1708Port(File device, int flags) throws SecurityException, IOException {

        /* Check access permission */
        Log.d(TAG, "==> SerialPort before \" can read \" ========================================");
        if (!device.canRead() || !device.canWrite()) {
//            try {
//                Log.d(TAG, "==> SerialPort before no permission ========================================");
//                /* Missing read/write permission, trying to chmod the file */
//                Process su;
//                su = Runtime.getRuntime().exec("/system/xbin/su");
//                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"
//                    + "exit\n";
//                su.getOutputStream().write(cmd.getBytes());
//                if ((su.waitFor() != 0) || !device.canRead()
//                    || !device.canWrite()) {
//                    throw new SecurityException();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                throw new SecurityException();
//            }
            Log.d(TAG, "ERROR: Can't read and/or write from/to serial port " + device.toString());
            Log.d(TAG, "==> SerialPort after no permission ========================================");
        }
        Log.d(TAG, "==> SerialPort before open ========================================");
        mFd = DeviceOpen(device.getAbsolutePath(), flags);
        if (mFd == null) {
            Log.d(TAG, "==> SerialPort mFd = null ========================================");
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
        Log.d(TAG, "native opened successfully========================================");
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    // Getters and setters
    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    // JNI
    //private native static FileDescriptor open(String path, int flags);
    private native static FileDescriptor DeviceOpen(String path, int flags);
   // public native static void config(int baudrate);
    public native void close();
    //    public native void read();
    static {
        System.loadLibrary("Serial_port");
    }
}


