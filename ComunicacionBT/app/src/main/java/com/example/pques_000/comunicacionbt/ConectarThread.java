package com.example.pques_000.comunicacionbt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by pques_000 on 07/05/2016.
 */
public class ConectarThread  extends Thread{
    private static final String tag = "debug";
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int ERROR_CONNECTION=2;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothSocket BT_Socket;
    private final BluetoothDevice BT_Device;
    private BluetoothAdapter BT_Adapter;
    private Handler BT_Handler;

        public ConectarThread(BluetoothDevice device,BluetoothAdapter _bt_adapter, Handler _handler) {
            BT_Adapter= _bt_adapter;
            BT_Handler=_handler;
            BluetoothSocket tmp_socket = null;
            BT_Device = device;
            try {
               //tmp_socket = BT_Device.createRfcommSocketToServiceRecord(MY_UUID);  // Te pide la contraseña
               tmp_socket = BT_Device.createInsecureRfcommSocketToServiceRecord(MY_UUID);   // No te pide la contraseña
            } catch (IOException e) {
                Log.e(tag, "Fallo en el socket");

            }
            BT_Socket = tmp_socket;
        }

    public void run() {

        BT_Adapter.cancelDiscovery();
        Log.e(tag, "funcion run");
        try {
            BT_Socket.connect();
        } catch (IOException connectException) {
            Log.e(tag, "fallo en la conexion del socket");
            BT_Handler.obtainMessage(ERROR_CONNECTION).sendToTarget();

            try {
                BT_Socket.close();

            } catch (IOException closeException) { }
            return;
        }
        BT_Handler.obtainMessage(SUCCESS_CONNECT, BT_Socket).sendToTarget();

    }



    public void cancel() {
        try {
            BT_Socket.close();
        } catch (IOException e) {
        }
    }


 }

