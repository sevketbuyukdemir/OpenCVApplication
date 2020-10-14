package com.capstonebau.opencvapplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class BluetoothConnectionEraser {
    // SDP Record name
    private static final String NAME = "BluetoothAndroidJson";
    // UUID for this application. A class that represents an immutable universally unique
    // identifier (UUID). A UUID represents a 128-bit value.
    private static final UUID MY_UUID = UUID.fromString("0032101-0320-1006-6532-02456F9A3BJK");
    // Constants for Bluetooth Connection
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Constants for Bluetooth Connection States
    private int bluetooth_state; // Bluetooth State holder variable
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    // Bluetooth Objects
    private final BluetoothAdapter bluetooth_adapter;
    private final Handler bluetooth_handler;
    private AcceptThread bluetooth_accept_thread;
    private ConnectThread bluetooth_connect_thread;
    private ConnectedThread bluetooth_connected_thread;
    // Robot erasing status
    public boolean isOk = false;

    public boolean isOk() {
        return isOk;
    }

    public void sendJsonFile (JSONObject json_file) {
        if (this.getState() != BluetoothConnectionEraser.STATE_CONNECTED) {
            return;
        }
        if (json_file.isNull("F") && json_file.isNull("B")
                && json_file.isNull("B") && json_file.isNull("L")) {
            // File is empty -
        } else {
            byte[] json = json_file.toString().getBytes();
            this.write(json);
        }
    }

    public void connectDevice(BluetoothEraserDevice bluetoothEraserDevice) {
        String address = bluetoothEraserDevice.getDevice_address();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            BluetoothDevice device = adapter.getRemoteDevice(address);
            this.connect(device);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public BluetoothConnectionEraser(Context context, Handler handler) {
        bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
        bluetooth_state = STATE_NONE;
        bluetooth_handler = handler;
    }

    private synchronized void setState(int state) {
        bluetooth_state = state;
        bluetooth_handler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return bluetooth_state;
    }

    public synchronized void start() {
        if (bluetooth_connect_thread != null) {
            bluetooth_connect_thread.cancel();
            bluetooth_connect_thread = null;
        }

        if (bluetooth_connected_thread != null) {
            bluetooth_connected_thread.cancel();
            bluetooth_connected_thread = null;
        }

        setState(STATE_LISTEN);

        if (bluetooth_accept_thread == null) {
            bluetooth_accept_thread = new AcceptThread();
            bluetooth_accept_thread.start();
        }
    }

    private synchronized void connect(BluetoothDevice device) {
        if (bluetooth_state == STATE_CONNECTING) {
            if (bluetooth_connect_thread != null) {
                bluetooth_connect_thread.cancel();
                bluetooth_connect_thread = null;
            }
        }

        if (bluetooth_connected_thread != null) {
            bluetooth_connected_thread.cancel();
            bluetooth_connected_thread = null;
        }

        bluetooth_connect_thread = new ConnectThread(device);
        bluetooth_connect_thread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        if (bluetooth_connect_thread != null) {
            bluetooth_connect_thread.cancel();
            bluetooth_connect_thread = null;
        }

        if (bluetooth_connected_thread != null) {
            bluetooth_connected_thread.cancel();
            bluetooth_connected_thread = null;
        }

        if (bluetooth_accept_thread != null) {
            bluetooth_accept_thread.cancel();
            bluetooth_accept_thread = null;
        }

        bluetooth_connected_thread = new ConnectedThread(socket, socketType);
        bluetooth_connected_thread.start();

        Message msg = bluetooth_handler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString("Connected", device.getName());
        msg.setData(bundle);
        bluetooth_handler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        if (bluetooth_accept_thread != null) {
            bluetooth_accept_thread.cancel();
            bluetooth_accept_thread = null;
        }

        if (bluetooth_connect_thread != null) {
            bluetooth_connect_thread.cancel();
            bluetooth_connect_thread = null;
        }

        if (bluetooth_connected_thread != null) {
            bluetooth_connected_thread.cancel();
            bluetooth_connected_thread = null;
        }

        setState(STATE_NONE);
    }

    private void connectionFailed() {
        Message msg = bluetooth_handler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("Toast", "Unable to connect device");
        msg.setData(bundle);
        bluetooth_handler.sendMessage(msg);
        BluetoothConnectionEraser.this.start();
    }

    private void connectionLost() {
        Message msg = bluetooth_handler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("Toast", "Device connection was lost");
        msg.setData(bundle);
        bluetooth_handler.sendMessage(msg);
        BluetoothConnectionEraser.this.start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetooth_adapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread" + mSocketType);
            BluetoothSocket socket = null;
            while (bluetooth_state != STATE_CONNECTED) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothConnectionEraser.this) {
                        switch (bluetooth_state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice(), mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run() {
            bluetooth_adapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    e.printStackTrace();
                }
                connectionFailed();
                return;
            }
            synchronized (BluetoothConnectionEraser.this) {
                bluetooth_connect_thread = null;
            }
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String s = bytes + "";
                    JSONObject jsonObject = new JSONObject(s);
                    if (jsonObject.getString("status").equals("O")) {
                        isOk = true;
                    } else {
                        isOk = false;
                    }
                    bluetooth_handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    connectionLost();
                    BluetoothConnectionEraser.this.start();
                    break;
                } catch (JSONException e) {
                    // Wrong format for JSON file.
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                bluetooth_handler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (bluetooth_state != STATE_CONNECTED) {
                return;
            }
            r = bluetooth_connected_thread;
        }
        r.write(out);
    }

}