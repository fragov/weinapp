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
import android.util.Log;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * TODO consider turning this into a singleton
 */
public class GPS implements GPSDataSender {

    public static final String FAILED_RESULT = "$No Result";
    public static final String NO_DEVICE = "No device found.";
    public static final String LCDT = "LogCatDemoTag";
    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private static final ArrayList<DeviceInfoWrapper> possibleDevicesList = new ArrayList<>();

    private static GPSDataReceiver gpsDataReceiver;
    private long pollingInterval = -1;

    private LatLng lastKnownLatLng;
    private boolean hasPos = false;

    private byte[] bytes;
    private static final int TIMEOUT = 1000;
    private static final int BUFFLEN = 1024;
    private int baudRate = 4800;

    private UsbManager manager;
    private UsbDevice device;

    private UsbInterface intf;
    private UsbDeviceConnection connection;
    private UsbEndpoint endpoint;

    private boolean isInit = false;
    private boolean hasEndpoint = false;
    private boolean isPollerSet = false;

    //TODO use Android to get this information
    private boolean hasPermission = false;

    private Context context;

    private final BroadcastReceiver attachReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();

            loggah("attachReceiver.onReceive()",true);

            if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if(device != null) {
                    //PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    //manager.requestPermission(device, permissionIntent);
                    loggah("THIS IS THE BUG!!!", false);
                    onCreate();
                    loggah("ATTACHED - not null", false);
                } else {
                    loggah("ATTACHED - null", false);
                }
            }
        }
    };

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
                    loggah("DETACHED - not null", false);
                } else {
                    loggah("DETACHED - null", false);
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
                    //UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        init();
                        hasPermission = true;
                    } else {
                        loggah("Permission denied for device.", true);
                    }
                }
            }
        }
    };

    public GPS(Context context) {
        this.context = context;

        possibleDevicesList.add(new DeviceInfoWrapper(0x067B, 0x2303));

        manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        IntentFilter intentFilter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(permissionReceiver, intentFilter);

        IntentFilter intentFilter2 = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        context.registerReceiver(attachReceiver, intentFilter2);

        IntentFilter intentFilter3 = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(detachReceiver, intentFilter3);

        loggah("Receivers maybe registered.", false);

        this.onCreate();
    }

    public LatLng getLastKnownLatLng() {
        if(!isPollerSet) setLastKnownLatLng();
        return this.lastKnownLatLng;
    }

    private void setLastKnownLatLng() {
        String gga = getFirstGGAString();
        loggah("GGA String - " + gga, false);
        if(gga.equals(FAILED_RESULT)) //TODO make this;
        lastKnownLatLng = ggaToLatLng(gga);
        loggah("Coordinates: " + lastKnownLatLng.getLatitude() + "," + lastKnownLatLng.getLongitude(), false);
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

    public String getFirstGGAString() {
        String data = getSomeData();

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

    private String getSomeData() {
        if(device == null) {
            return NO_DEVICE;
        }
        bytes = new byte[BUFFLEN];

        //connection.bulkTransfer(endpoint, bytes, bytes.length, TIMEOUT);

        long then = System.currentTimeMillis();

        StringBuilder raw = new StringBuilder();
        //TODO make this more efficient
        for(int i=0; i<BUFFLEN; i++) {
            char c = (char) readFromGPS();
            raw.append(c);
        }

        long now = System.currentTimeMillis() - then;
        loggah("Reading Time: " + now, true);

        return raw.toString();
    }

    private int readIndex = 0;
    private int readCount = 0;
    private boolean hasRead = false;
    private int recLen = 0;

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

    private class DeviceInfoWrapper {
        public int vendorId;
        public int productId;

        public DeviceInfoWrapper(int vendorId, int productId) {
            this.vendorId = vendorId;
            this.productId = productId;
        }

        public boolean equals (DeviceInfoWrapper diw) {
            return ((this.vendorId == diw.vendorId) && (this.productId == diw.productId));
        }
    }

    private void onCreate() {
        //TODO test all connected devices with nested loops
        Iterator<UsbDevice> iterator = manager.getDeviceList().values().iterator();
        if(iterator.hasNext()) {
            device = iterator.next();
        }

        boolean nullHoldsAtOnCreate = false;
        boolean emptyDeviceListHoldsAtOnCreate = false;

        if(device == null) nullHoldsAtOnCreate = true;
        if(manager.getDeviceList().isEmpty()) emptyDeviceListHoldsAtOnCreate = true;
        if(nullHoldsAtOnCreate || emptyDeviceListHoldsAtOnCreate) {
            loggah("Maybe no device at startup: " + nullHoldsAtOnCreate + ", " + emptyDeviceListHoldsAtOnCreate, false);
            return;
        }

        DeviceInfoWrapper currDiw = new DeviceInfoWrapper(device.getVendorId(), device.getProductId());
        boolean supportedDeviceConnected = false;
        for(DeviceInfoWrapper diw: possibleDevicesList) {
            if(diw.equals(currDiw)) {
                supportedDeviceConnected = true;
                break;
            }
        }
        if(!supportedDeviceConnected) {
            //TODO make Error handling
            loggah("No supported device connected: " + currDiw.vendorId + ", " + currDiw.productId, false);
        }

        if(!hasPermission) {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
            manager.requestPermission(device, permissionIntent);
        }
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
        if(device != null) {
            device = null;
        } else {
            loggah("Device is null on closit().",  false);
        }
        isInit = false;
        hasEndpoint = false;
        hasPermission = false;
    }

    public void onDestroy() {
        if(attachReceiver != null) {
            context.unregisterReceiver(attachReceiver);
            loggah("unregistered BR 0", false);
        }
        if(detachReceiver != null) {
            context.unregisterReceiver(detachReceiver);
            loggah("unregistered BR 1", false);
        }
        if(permissionReceiver != null) {
            context.unregisterReceiver(permissionReceiver);
            loggah("unregistered BR 2", false);
        }
        closit();
        loggah("super.onDestroy() to be called.", false);
        //TODO don't forget super.onDestroy();
    }

    private void init() {
        if(!isInit) {
            isInit = true;

            this.onCreate();

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

    private int ctWrapper(int requestType, int request, int value, int index, byte[] buffer, int lenght, int timeout) {
        int res = connection.controlTransfer(requestType, request, value, index, buffer, lenght, timeout);
        if(res < 0) Log.e(LCDT, "ctErrorBLABWAAH");
        return res;
    }

    private void resetPorts() {
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

    public String getDeviceMetaData() {
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
        int v = device.getVendorId();
        stringBuilder.append(Integer.toHexString(v));
        stringBuilder.append('\n');

        stringBuilder.append("Product ID: ");
        int p = device.getProductId();
        stringBuilder.append(Integer.toHexString(p));
        stringBuilder.append('\n');

        LatLng res = ggaToLatLng(getFirstGGAString());
        stringBuilder.append("Latitude: " + res.getLatitude() + "\n");
        stringBuilder.append("Longitude: " + res.getLongitude() + "\n");

        return stringBuilder.toString();
    }

    public String getDeviceListToString() {
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

    public boolean hasDevice() {
        return device != null;
    }

    public void loggah (String mess, boolean toast) {
        Log.d(LCDT, mess);
        if(toast) {
            Toast.makeText(context, mess, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public GPSDataReceiver getReceiver() {
        return this.gpsDataReceiver;
    }

    @Override
    public void registerReceiver(GPSDataReceiver gpsDataReceiver) {
        //we could do this with an ArrayList, but we can only use it once
        this.gpsDataReceiver = gpsDataReceiver;
    }

    @Override
    public void setPollingInterval(long millis) {
        this.pollingInterval = millis;
    }

    @Override
    public void updateReceivers() {
        //we could do this with an ArrayList, but we can only use it once
        this.gpsDataReceiver.onUSBGPSLocationChanged(getLastKnownLatLng());
    }

    @Override
    public void startPolling() {
        //TODO human-generated method stub
    }

    @Override
    public void stopPolling() {
        //TODO human-generated method stub
    }

    private class PollingClock implements Runnable{
        long wait;
        boolean stopFlag;

        public PollingClock(long time) {
            this.wait = time;
            stopFlag = false;
        }

        @Override
        public void run() {
            while(!stopFlag) {
                //TODO set this in another Thread
                //GPS.this.setLastKnownLatLng();
                try {
                    Thread.sleep(this.wait);
                    //if errors, try
                    //this.wait(this.wait);
                } catch (InterruptedException e) {
                    Log.e(LCDT, "Couldn't wait in PollingClock.");
                    this.stop();
                }
                updateReceivers();
            }
        }

        public void stop () {
            this.stopFlag = true;
        }
    }
}
