package com.wein3.weinapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class GPS extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String LCDT = "LogCatDemoTag";

    private Button getLatLong;
    private TextView textViewLat;
    private TextView textViewLong;

    private LatLng lastKnownLatLng;
    private boolean hasPos = false;

    private byte[] bytes;
    private static final int TIMEOUT = 1000;
    private static final int BUFFLEN = 1000;
    private int recLen = 0;
    private boolean forceClaim = true;
    private int baudRate = 4800;

    private UsbManager manager;
    private UsbDevice device;

    private UsbInterface intf;
    private UsbDeviceConnection connection;
    private UsbEndpoint endpoint;
    private UsbEndpoint outEndpoint;

    private boolean isInit = false;
    private boolean hasEndpoint = false;
    public BroadcastReceiver broadcastReceiver;

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

                manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                //textViewLat.setText(getDeviceListToString(manager));

                Intent intent = getIntent();
                device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if(device == null) return;
                textViewLat.setText(getDeviceMetaData());

                init();
                textViewLong.setText(getSomeData());
            }
        });
    }

    public LatLng getLastKnownLatLng() {
        return this.lastKnownLatLng;
    }

    public void closit() {
        //Toast.makeText(getApplicationContext(), "Not yet implemented", Toast.LENGTH_SHORT).show();
        if(hasEndpoint && connection.releaseInterface(intf)) {
            loggah("Released interface.", false);
        }
        hasEndpoint = false;
    }

    private String getDeviceMetaData() {
        if(device == null) {
            return "No device found.";
        }
        StringBuilder stringBuilder = new StringBuilder();

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

        return stringBuilder.toString();
    }

    private String getDeviceListToString() {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Set<String> set = deviceList.keySet();
        Iterator<String> it = set.iterator();

        StringBuilder sb = new StringBuilder();
        while(it.hasNext()) {
            sb.append(it.next());
            sb.append('\n');
        }
        return sb.toString();
    }

    private String getSomeData() {
        if(device == null) {
            return "No device found.";
        }
        bytes = new byte[1000];

        connection.bulkTransfer(endpoint, bytes, bytes.length, TIMEOUT);

        StringBuilder raw = new StringBuilder();
        for(int i=0; i<256; i++) {
            char c = (char) readFromGPS();
            raw.append(c);
        }
        return raw.toString();
    }

    @Override
    protected void onDestroy() {
        if(broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
        closit();
        super.onDestroy();
    }

    private void init() {
        if(!isInit) {
            isInit = true;

            bytes = new byte[BUFFLEN];

            //https://developer.android.com/guide/topics/connectivity/usb/host.html#working-d
            /*
            intf = device.getInterface(0);
            endpoint = intf.getEndpoint(0);
            connection = manager.openDevice(device);
            connection.claimInterface(intf, forceClaim);
            */
            connection = manager.openDevice(device);
            if(connection != null) {
                loggah("Got connection. Interface count: " + device.getInterfaceCount(), true);
                intf = device.getInterface(0);
                loggah("Endpoint count: " + intf.getEndpointCount(), true);

                for(int i=0; i<intf.getEndpointCount(); i++) {
                    UsbEndpoint temp = intf.getEndpoint(i);
                    if(temp.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK && temp.getDirection() == UsbConstants.USB_DIR_IN) {
                        endpoint = temp;
                    }
                }

                if(endpoint != null) {
                    loggah("Hazn Endpoint.", false);
                    connection.claimInterface(intf, true);
                    byte[] buffer = new byte[1];
                    //https://github.com/sintrb/Android-PL2303HXA/blob/master/Android-PL2303HXA/src/com/sin/android/usb/pl2303hxa/PL2303Driver.java
                    //TODO erklären können, sonst wäre das nur abgeschrieben
                    ctWrapper(192, 1, 33924, 0, buffer, 1, TIMEOUT);
                    ctWrapper(64, 1, 1028, 0, null, 0, TIMEOUT);
                    ctWrapper(192, 1, 33924, 0, buffer, 1, TIMEOUT);
                    ctWrapper(192, 1, 33667, 0, buffer, 1, TIMEOUT);
                    ctWrapper(192, 1, 33924, 0, buffer, 1, TIMEOUT);
                    ctWrapper(64, 1, 1028, 0, null, 0, TIMEOUT);
                    ctWrapper(192, 1, 33924, 0, buffer, 1, TIMEOUT);
                    ctWrapper(192, 1, 33667, 0, buffer, 1, TIMEOUT);
                    ctWrapper(64, 1, 1028, 1, null, 0, TIMEOUT);
                    ctWrapper(64, 1, 1028, 0, null, 0, TIMEOUT);
                    ctWrapper(64, 1, 1028, 68, null, 0, TIMEOUT);

                    resetPorts();

                    hasEndpoint = true;
                }
            }

            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action  = intent.getAction();

                    loggah("onReceive()",true);

                    if(UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if(device != null) {
                            //connection.releaseInterface(intf);
                            //connection.close();
                            closit();
                            loggah("DETACHED - not null", true);
                        } else {
                            loggah("DETACHED - null", true);
                        }
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
            getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    private void loggah (String mess, boolean toast) {
        Log.d(LCDT, mess);
        if(toast) {
            Toast.makeText(getApplicationContext(), mess, Toast.LENGTH_SHORT).show();
        }
    }

    private int ctWrapper(int requestType, int request, int value, int index, byte[] buffer, int lenght, int timeout) {
        int res = connection.controlTransfer(requestType, request, value, index, buffer, lenght, timeout);
        Log.e(LCDT, "ctErrorBLABWAAH");
        return res;
    }

    public void resetPorts() {
        byte[] portSet = new byte[7];
        ctWrapper(161, 33, 0, 0, portSet, 7, TIMEOUT);
        portSet[0] = (byte) (baudRate & 0xff);
        portSet[1] = (byte) (baudRate >> 8 & 0xff);
        portSet[2] = (byte) (baudRate >> 16 & 0xff);
        portSet[3] = (byte) (baudRate >> 24 & 0xff);
        portSet[4] = 0;
        portSet[5] = 0;
        portSet[6] = 0;
        ctWrapper(33,32,0,0,portSet,7,TIMEOUT);
        ctWrapper(161,33,0,0,portSet,7,TIMEOUT);
    }

    private int readIndex = 0;
    private int readCount = 0;
    private boolean hasRead = false;

    public byte readFromGPS() {
        byte b = 0;

        if(readIndex >= readCount) {
            readCount = connection.bulkTransfer(endpoint, bytes, BUFFLEN, TIMEOUT);
            readIndex = 0;
        }
        if(readIndex < readCount) {
            b = bytes[readIndex];
            hasRead = true;
            recLen++;
            readIndex++;
        } else {
            hasRead = false;
        }

        return b;
    }

    /**
     * Called when an item in the navigation menu is selected.
     *
     * @param item The selected item
     * @return true to display the item as the selected item
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }
}
