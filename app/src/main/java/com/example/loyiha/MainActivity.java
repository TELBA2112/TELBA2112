package com.example.loyiha;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int MESSAGE_READ = 1;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    private ConnectedThread connectedThread;

    private TextView receivedTextView;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receivedTextView = findViewById(R.id.receivedTextView);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not enabled. Please enable Bluetooth and restart the app.", Toast.LENGTH_LONG).show();
            finish();
        }

        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("HC-05")) { // Change to your HC-05 device name
                    try {
                        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard UUID for SPP
                        btSocket = device.createRfcommSocketToServiceRecord(uuid);
                        btSocket.connect();
                        outStream = btSocket.getOutputStream();
                        connectedThread = new ConnectedThread(btSocket);
                        connectedThread.start();
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Error connecting to Arduino", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    break;
                }
            }
        }

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connectedThread != null) {
                    connectedThread.write("1".getBytes()); // Send '1' to start sending data from Arduino
                }
            }
        });

        Button finishButton = findViewById(R.id.finishButton);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connectedThread != null) {
                    connectedThread.write("0".getBytes()); // Send '0' to stop sending data from Arduino
                }
            }
        });
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error in InputStream", Toast.LENGTH_SHORT).show();
            }

            mmInStream = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error writing to Arduino", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final Handler handler = new Handler(new Handler.Callback() {
        public boolean handleMessage(Message msg) {
            if (msg.what == MESSAGE_READ) {
                String readMessage = (String) msg.obj;
                receivedTextView.append(readMessage);
            }
            return true;
        }
    });
}
