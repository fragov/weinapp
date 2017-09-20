package com.wein3.weinapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class GPS extends AppCompatActivity {

    private Button getLatLong;
    private TextView textViewLat;
    private TextView textViewLong;

    private LatLng lastKnownLatLng;
    private boolean hasPos = false;

    private byte[] bytes;
    private static int TIMEOUT = 1000;
    private boolean forceClaim = true;

    private UsbInterface intf;
    private UsbDeviceConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        getLatLong = (Button) findViewById(R.id.getLatLong);
        textViewLat = (TextView) findViewById(R.id.textViewLat);
        textViewLong = (TextView) findViewById(R.id.textViewLong);

        getLatLong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LatLng loc = GPS.this.getLastKnownLatLng();
                String s = "Not yet implemented";

                getLatLong.setText(s);
                textViewLat.setText(s);
                textViewLong.setText(s);

                UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

                /*
                HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                Set<String> set = deviceList.keySet();
                Iterator<String> it = set.iterator();

                StringBuilder sb = new StringBuilder();
                while(it.hasNext()) {
                    sb.append(it.next());
                    sb.append('\n');
                }

                StringBuilder sbMeta = new StringBuilder();
                sbMeta.append("HashMap - size: " + deviceList.size() + "\n");
                sbMeta.append("Set - size: " + set.size());

                textViewLat.setText(sb);
                textViewLong.setText(sbMeta);
                */

                StringBuilder stringBuilder = new StringBuilder();

                Intent intent = getIntent();
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if(device == null) return;
                stringBuilder.append("Device ID: ");
                stringBuilder.append(device.getDeviceId());
                stringBuilder.append('\n');
                stringBuilder.append("Device Name: ");
                stringBuilder.append(device.getDeviceName());
                stringBuilder.append('\n');
                /*
                stringBuilder.append("Device S/N ");
                stringBuilder.append(device.getSerialNumber());
                stringBuilder.append('\n');
                */
                stringBuilder.append("Device Class: ");
                stringBuilder.append(device.getDeviceClass());
                stringBuilder.append('\n');
                stringBuilder.append("Device Subclass: ");
                stringBuilder.append(device.getDeviceSubclass());
                stringBuilder.append('\n');
                stringBuilder.append("Device Protocol: ");
                stringBuilder.append(device.getDeviceProtocol());
                stringBuilder.append('\n');
                stringBuilder.append("Vendor ID: ");
                stringBuilder.append(device.getVendorId());
                stringBuilder.append('\n');
                /*
                stringBuilder.append("Vendor Name: ");
                stringBuilder.append(device.getManufacturerName());
                stringBuilder.append('\n');
                */
                stringBuilder.append("Product ID: ");
                stringBuilder.append(device.getProductId());
                stringBuilder.append('\n');
                /*
                stringBuilder.append("Product Name: ");
                stringBuilder.append(device.getProductName());
                stringBuilder.append('\n');
                */

                textViewLat.setText(stringBuilder);

                /*
                //Please add nullptr check
                textViewLat.setText(Double.toString(loc.getLatitude()));
                textViewLong.setText(Double.toString(loc.getLongitude()));
                */

                bytes = new byte[1000];

                //https://developer.android.com/guide/topics/connectivity/usb/host.html#working-d
                intf = device.getInterface(0);
                UsbEndpoint endpoint = intf.getEndpoint(0);
                connection = manager.openDevice(device);
                connection.claimInterface(intf, forceClaim);
                connection.bulkTransfer(endpoint, bytes, bytes.length, TIMEOUT);

                //I should put this into the Manifest
                BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action  = intent.getAction();

                        if(UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                            if(device != null) {
                                connection.releaseInterface(intf);
                                connection.close();
                                Toast.makeText(getApplicationContext(), "DETACHED - not null", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "DETACHED - null", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                };

                StringBuilder raw = new StringBuilder();
                for(int i=0; i<bytes.length; i++) {
                    char c = (char) bytes[i];
                    raw.append(c);
                }
                textViewLong.setText(raw);
            }
        });
    }

    public LatLng getLastKnownLatLng() {
        return this.lastKnownLatLng;
    }

    private void closit() {
        Toast.makeText(getApplicationContext(), "Not yet implemented", Toast.LENGTH_SHORT).show();
    }
}
