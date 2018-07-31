package com.example.pques_000.comunicacionbt;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by pques_000 on 07/05/2016.
 */
public class ThreadConectado extends Thread {
    private static final String tag = "debug";
    protected static final int MESSAGE_READ = 1;

    private Handler BT_Handler;
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    public ThreadConectado(BluetoothSocket socket,Handler _handler) {
        BT_Handler=_handler;
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;


        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer;
        int bytes;

        try {
            buffer = new byte[1024];
            bytes = mmInStream.read(buffer);

            BT_Handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                    .sendToTarget();


        } catch (IOException e) {
            Log.e(tag, "segundo conect:error de conexion");
        }

    }

    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
