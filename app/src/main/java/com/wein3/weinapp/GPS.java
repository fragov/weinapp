package com.wein3.weinapp;

import android.app.PendingIntent;
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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class GPS extends AppCompatActivity {

    public static final String FAILED_RESULT = "$No Result";
    public static final String NO_DEVICE = "No device found.";
    public static final String LCDT = "LogCatDemoTag";
    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private Button getLatLong;
    private TextView textViewLat;
    private TextView textViewLong;
    private TextView satTime;

    private LatLng lastKnownLatLng;
    private boolean hasPos = false;

    private byte[] bytes;
    private static final int TIMEOUT = 1000;
    private static final int BUFFLEN = 1024;
    private int recLen = 0;
    private int baudRate = 4800;

    private UsbManager manager;
    private UsbDevice device;

    private UsbInterface intf;
    private UsbDeviceConnection connection;
    private UsbEndpoint endpoint;

    private boolean isInit = false;
    private boolean hasEndpoint = false;

    private final BroadcastReceiver detachReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();

            loggah("detachReceiver.onReceive()",true);

            if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
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

    private final BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            loggah("permissionReceiver.onReceive()",true);

            if(ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null) {
                            init();
                        }
                    } else {
                        loggah("Permission denied for device.", false);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        getLatLong = (Button) findViewById(R.id.getLatLong);
        textViewLat = (TextView) findViewById(R.id.textViewLat);
        textViewLong = (TextView) findViewById(R.id.textViewLong);
        satTime = (TextView) findViewById(R.id.satTime);

        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        //TODO search for right device with vendor and product id
        device = manager.getDeviceList().values().iterator().next();

        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter intentFilter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(permissionReceiver, intentFilter);

        manager.requestPermission(device, permissionIntent);

        //PendingIntent detachIntent = PendingIntent.getBroadcast(this, 0, new Intent(UsbManager.ACTION_USB_ACCESSORY_DETACHED), 0);
        IntentFilter intentFilter2 = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(detachReceiver, intentFilter2);

        getLatLong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                Intent intent = getIntent();
                device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                */
                if(device == null) {
                    loggah("No device found upon button usage.", true);
                    return;
                }

                LatLng res = getLastKnownLatLng();

                textViewLat.setText(Double.toString(res.getLatitude()));
                textViewLong.setText(Double.toString(res.getLongitude()));
                satTime.setText(Double.toString(res.getAltitude()));
            }
        });
    }

    public LatLng getLastKnownLatLng() {
        String gga = getFirstGGAString();
        loggah("GGA String - " + gga, false);
        if(gga.equals(FAILED_RESULT)) return this.lastKnownLatLng;
        lastKnownLatLng = ggaToLatLng(gga);
        loggah("Coordinates: " + lastKnownLatLng.getLatitude() + "," + lastKnownLatLng.getLongitude(), false);
        return this.lastKnownLatLng;
    }

    public void closit() {
        loggah("Entered closit().", false);
        if(connection != null) {
            if(hasEndpoint && connection.releaseInterface(intf)) {
                loggah("Released interface.", false);
                connection.close();
            }
        } else {
            loggah("Connection is null on closit().",  false);
        }
        hasEndpoint = false;
    }

    private String getDeviceMetaData() {
        if(device == null) {
            return NO_DEVICE;
        }
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Device ID: ");
        stringBuilder.append(device.getDeviceId());
        stringBuilder.append('\n');

        stringBuilder.append("Device Name: ");
        stringBuilder.append(device.getDeviceName());
        stringBuilder.append('\n');

        stringBuilder.append("Vendor ID: ");
        stringBuilder.append(device.getVendorId());
        stringBuilder.append('\n');

        stringBuilder.append("Product ID: ");
        stringBuilder.append(device.getProductId());
        stringBuilder.append('\n');

        LatLng res = ggaToLatLng(getFirstGGAString());
        stringBuilder.append("Latitude: " + res.getLatitude() + "\n");
        stringBuilder.append("Longitude: " + res.getLongitude() + "\n");

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
            return NO_DEVICE;
        }
        bytes = new byte[BUFFLEN];

        connection.bulkTransfer(endpoint, bytes, bytes.length, TIMEOUT);

        StringBuilder raw = new StringBuilder();
        //TODO make this more efficient
        for(int i=0; i<BUFFLEN; i++) {
            char c = (char) readFromGPS();
            raw.append(c);
        }
        return raw.toString();
    }

    @Override
    protected void onDestroy() {
        if(detachReceiver != null) {
            unregisterReceiver(detachReceiver);
            loggah("unregistered BR 1", false);
        }
        if(permissionReceiver != null) {
            unregisterReceiver(permissionReceiver);
            loggah("unregistered BR 2", false);
        }
        closit();
        loggah("super.onDestroy() to be called.", false);
        super.onDestroy();
    }

    private void init() {
        if(!isInit) {
            isInit = true;

            bytes = new byte[BUFFLEN];

            //https://developer.android.com/guide/topics/connectivity/usb/host.html#working-d
            connection = manager.openDevice(device);
            if(connection != null) {
                loggah("Got connection. Interface count: " + device.getInterfaceCount(), false);
                intf = device.getInterface(0);
                loggah("Endpoint count: " + intf.getEndpointCount(), false);

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
        if(res < 0) Log.e(LCDT, "ctErrorBLABWAAH");
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

    public String getFirstGGAString() {
        String data = getSomeData();

        //loggah("RECEIVED DATA: " + data, false);

        StringBuilder builder = new StringBuilder();
        String[] sentences = data.split("\\$");
        loggah("Data length: " + data.length() + "; Array Length: " + sentences.length, false);
        String result = "No Result";
        for(String sen: sentences) {
            if(sen.equals("")) continue;
            if(sen.startsWith("GPGGA")) {
                result = sen;
                break;
            }
        }
        builder.append('$');
        builder.append(result);

        return builder.toString();
    }

    public LatLng ggaToLatLng(String gga) {
        if(gga.equals(FAILED_RESULT)) {
            loggah("Got invalid information from sensor.", true);
            Log.e(LCDT, "Got invalid information from sensor.");
            return null;
        }
        String[] parts = gga.split(",");

        String toasterMessage = "";
        boolean toasted = false;

        double lat = 0;
        try {
            lat = Double.parseDouble(parts[2]);
        } catch (NumberFormatException e) {
            toasterMessage += "No Latitude";
            toasted = true;
        }
        lat /= 100;
        if(parts[3].equals("S")) {
            lat *= -1;
        }

        double lng = 0;
        try {
            lng = Double.parseDouble(parts[4]);
        } catch (NumberFormatException e) {
            if(toasted) {
                toasterMessage += " nor Longitude";
            } else {
                toasterMessage += "No Longitude";
            }
            toasted = true;
        }
        lng /= 100;
        if(parts[5].equals("W")) {
            lat *= -1;
        }

        if(toasted) {
            toasterMessage += " found.";
            loggah(toasterMessage, true);
        }
        LatLng retVal = new LatLng(lat, lng);
        try {
            double alt = Double.parseDouble(parts[1]);
            //TODO don't set wrong altitude, dude
            retVal.setAltitude(alt);
        } catch (NumberFormatException e) {
            loggah("Time Fail", true);
        }
        return retVal;
    }
}
