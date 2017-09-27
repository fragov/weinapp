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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * This class should provide everything necessary for an Activity to receive GPS data from
 * a USB GPS device.
 *
 * TODO change ugly names (onUSBGPSLocationChanged)
 *
 * TODO check for consistency in checking device, connection, etc.
 *
 * TODO handle detachment when Threads are still running
 *
 * TODO reduce time to read data (especially with no reception) because only one GGA/sec from sensor
 *
 * TODO look at log.txt on Desktop for random, weirdly corrupted and error-causing examples...
 *
 * TODO handle slowdown despite Threads (maybe too many synchronized?)
 *
 * TODO handle crash on ending Activity while Thread is running
 *
 * TODO handle crash on removing USB GPS device while Thread is running
 *
 * TODO make GPS useable without restart...
 *
 * TODO make more comments than Javadocs
 *
 */
public class GPS implements GPSDataSender {

    //-------------FINAL VARIABLES------------------------------------------------------------------

    /**
     * String to test for wrong result when extracting NMEA sentences.
     */
    private static final String FAILED_RESULT = "$No Result";

    /**
     * String to give back if no device was found.
     */
    private static final String NO_DEVICE = "No device found.";

    /**
     * A tag for finding things in the log.
     */
    private static final String LCDT = "LogCatDemoTag";

    /**
     * Name of the Action of the Intent which should be received by permissionReceiver.
     */
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    /**
     * A static list to check for supported devices.
     */
    private static final ArrayList<DeviceInfoWrapper> possibleDevicesList = new ArrayList<>();

    //-------------VARIABLES RELATED TO MULTITHREADING AND DATA TRANSFER----------------------------

    /**
     * This is the GPS singleton. Singletons are useful if we only want one instance of a class,
     * like in this case.
     */
    private static GPS gpsSingleton;

    /**
     * This is the GPSDataReceiver which receives updates if the location has changed.
     *
     * TODO can be replaced with a List of GPSDataReceivers in the future
     */
    private static GPSDataReceiver gpsDataReceiver;

    /**
     * Variable where the polling interval is stored (in milliseconds).
     */
    private long pollingInterval = -1;

    /**
     * Variable for access to the PollingClock Runnable.
     */
    private PollingClock pollingClock;

    /**
     * This is used to store the last known location.
     */
    private LatLng lastKnownLatLng;

    //-------------VARIABLES RELATED TO USB CONNECTION AND DATA READING-----------------------------

    /**
     * Buffer for the read data.
     */
    private byte[] bytes;

    /**
     * Reading timeout.
     */
    private static final int TIMEOUT = 1000;

    /**
     * Buffer length (important to consider if read freshest data has to be read).
     */
    private static final int BUFFLEN = 1024;

    /**
     * Technical stuff to receive the right characters.
     */
    private static final int baudRate = 4800;

    /**
     * Use this to get devices an establish a connection.
     */
    private UsbManager manager;

    /**
     * This this the GPS USB device in use.
     */
    private UsbDevice device;

    /**
     * This represents the connection to the device.
     */
    private UsbDeviceConnection connection;

    /**
     * Technical stuff.
     */
    private UsbInterface intf;

    /**
     * Technical stuff
     */
    private UsbEndpoint endpoint;

    //-------------FLAGS TO REPRESENT CURRENT STATE-------------------------------------------------

    /**
     * true, if connection is initialized.
     */
    private boolean isInit = false;

    /**
     * true, if we have an endpoint. Important to know before trying to read data.
     */
    private boolean hasEndpoint = false;

    /**
     * check if we currently receive updates from the poller
     */
    private boolean isPollerSet = false;

    /**
     * This indicates if there is a permission to access the USB device.
     *
     * TODO this is a simple way, but I should use Android to get this information
     */
    private boolean hasPermission = false;

    /**
     * Flag shows if this Object has to die.
     */
    private boolean almostDead = false;

    //-------------APPLICATION STUFF----------------------------------------------------------------

    /**
     * This is not an Activity, so a context has to be stored somewhere.
     */
    private static Context context;

    /**
     * This BroadcastReceiver is used to detect if a USB device is attached.
     */
    private final BroadcastReceiver attachReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();

            loggah("attachReceiver.onReceive()",false);
            loggah("USB device attached.", true);

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

    /**
     * This BroadcastReceiver is used to detect if a USB device is detached.
     */
    private final BroadcastReceiver detachReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();

            loggah("detachReceiver.onReceive()",false);
            loggah("USB device detached", true);

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

    /**
     * This BroadcastReceiver is used to establish a connection if the permission to access the
     * USB device was granted.
     */
    private final BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            loggah("permissionReceiver.onReceive()",false);

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

    /**
     * This handler is used to receive Messages from the Poller Runnable.
     */
    private final Handler pollHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            String res = message.getData().getString("mess");
            if("gotMess".equals(res)) {
                updateReceivers();
            } else {
                loggah("Almost...", false);
                updateReceivers();
            }
        }
    };

    //-------------CONSTRUCTOR----------------------------------------------------------------------

    /**
     * Most important tasks of this constructor are:
     *      -getting the UsbManager
     *      -register the BroadcastReceivers
     *
     * @param context - stores the context we need to access for certain methods
     */
    private GPS(Context context) {
        this.context = context;

        //Set supported devices list.
        //Device: Navilock NL-650US; GPS: MediaTek MT3337; RS-232 to USB: Prolific PL-2303 HXD
        possibleDevicesList.add(new DeviceInfoWrapper(0x067B, 0x2303));

        manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        IntentFilter intentFilter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(permissionReceiver, intentFilter);

        IntentFilter intentFilter2 = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        context.registerReceiver(attachReceiver, intentFilter2);

        IntentFilter intentFilter3 = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(detachReceiver, intentFilter3);

        loggah("Receivers registered.", false);

        this.onCreate();
    }

    /**
     * This method return the instance of GPS. If an instance already exist, just set the context in it.
     *
     * @param context - context used by GPS
     *
     * @return - reference to instance
     */
    public static GPS getInstance(Context context) {
        if(GPS.gpsSingleton == null) {
            GPS.gpsSingleton = new GPS(context);
        } else {
            GPS.context = context;
        }
        return GPS.gpsSingleton;
    }

    //-------------READING METHODS------------------------------------------------------------------

    /**
     * This returns the last known location. If no Poller is set, this method calls setLastKnownLatLng.
     * Please be aware that that method would be executed in the UI Thread so it is not recommended
     * to use it directly. Instead, consider using a poller.
     *
     * @return - last known location in LatLng
     */
    public LatLng getLastKnownLatLng() {
        if(!isPollerSet) setLastKnownLatLng();
        return this.lastKnownLatLng;
    }

    /**
     * This calculates the current location.
     * Please be aware that this method needs a lot of time.
     */
    private synchronized void setLastKnownLatLng() {
        String gga = getFirstGGAString();
        loggah("GGA String - " + gga, false);
        this.lastKnownLatLng = ggaToLatLng(gga);
        if(this.lastKnownLatLng != null) {
            loggah("Coordinates: " + lastKnownLatLng.getLatitude() + "," + lastKnownLatLng.getLongitude(), false);
        }
    }

    /**
     * This returns a LatLng object with the coordinates specified in the parameter, which has to be
     * a String containing an NMEA GGA sentence.
     *
     * @param gga - GGA dataset
     * @return - LatLng with location from GGA dataset
     */
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
            loggah(toasterMessage, false);
            return null;
        }
        LatLng retVal = new LatLng(lat, lng);
        try {
            double alt = Double.parseDouble(parts[1]);
            //TODO don't set wrong altitude, dude
            retVal.setAltitude(alt);
        } catch (NumberFormatException e) {
            loggah("Time Fail", false);
        }
        return retVal;
    }

    /**
     * Get the next GGA dataset from the USB GPS device.
     *
     * @return - GGA dataset
     */
    private String getFirstGGAString() {
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

    /**
     * Read BUFFLEN bytes in a buffer and turn the buffer into a String.
     *
     * @return - raw data as a String
     */
    private String getSomeData() {
        bytes = new byte[BUFFLEN];

        //connection.bulkTransfer(endpoint, bytes, bytes.length, TIMEOUT);

        long then = System.currentTimeMillis();

        StringBuilder raw = new StringBuilder();
        //TODO make this more efficient
        for(int i=0; i<BUFFLEN; i++) {
            char c = 0;
            try {
                c = (char) readFromGPS();
            } catch (GPSException e) {
                return NO_DEVICE;
            }
            raw.append(c);
        }

        long now = System.currentTimeMillis() - then;
        loggah("Reading Time: " + now, false);

        return raw.toString();
    }

    //TODO make Javadocs for read, etc.

    private int readIndex = 0;
    private int readCount = 0;
    private boolean hasRead = false;
    private int recLen = 0;

    private byte readFromGPS() throws GPSException {
        byte b = 0;

        if(readIndex >= readCount) {
            if(device == null) {
                throw new GPSException(NO_DEVICE);
            }
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

    //-------------INITIALIZING AND CLOSING---------------------------------------------------------

    /**
     * This class was written to easily store and compare important device information.
     */
    private class DeviceInfoWrapper {
        /**
         * This stores the vendor ID.
         */
        public int vendorId;

        /**
         * This stores the product ID.
         */
        public int productId;

        /**
         * This constructor sets the vendor and product IDs.
         *
         * @param vendorId - vendor ID
         * @param productId - product ID
         */
        public DeviceInfoWrapper(int vendorId, int productId) {
            this.vendorId = vendorId;
            this.productId = productId;
        }

        /**
         * This method compares this DeviceInfoWrapper with the given one.
         *
         * @param diw - given DeviceInfoWrapper
         * @return - true if the given device has the same values than this one
         */
        public boolean equals (DeviceInfoWrapper diw) {
            return ((this.vendorId == diw.vendorId) && (this.productId == diw.productId));
        }
    }

    /**
     * Used to get a USB device and ask for permission to access it.
     */
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

    /**
     * Used to close connection, remove device and reset state.
     */
    private void closit() {
        loggah("Entered closit().", false);
        this.stopPolling();
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

    /**
     * This method should be called in an Activity's onDestroy() method. It unregisters all
     * BroadcastReceivers and should disconnect everything "safely" if the Activity is closed.
     */
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

        this.almostDead = true;
        closit();

        loggah("super.onDestroy() to be called.", false);
        //TODO don't forget super.onDestroy();
    }

    /**
     * This method should establish a connection.
     */
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

    /**
     * Technical stuff required for controlling data Transfer. Wrapper is needed to check for errors.
     *
     * @param requestType - noidea lookat connection.controlTransfer()
     * @param request - noidea lookat connection.controlTransfer()
     * @param value - noidea lookat connection.controlTransfer()
     * @param index - noidea lookat connection.controlTransfer()
     * @param buffer - noidea lookat connection.controlTransfer()
     * @param lenght - noidea lookat connection.controlTransfer()
     * @param timeout - noidea lookat connection.controlTransfer()
     *
     * @return - a result value to check for errors
     */
    private int ctWrapper(int requestType, int request, int value, int index, byte[] buffer, int lenght, int timeout) {
        int res = connection.controlTransfer(requestType, request, value, index, buffer, lenght, timeout);
        if(res < 0) Log.e(LCDT, "ctErrorBLABWAAH");
        return res;
    }

    /**
     * Initialize ports. (noidea lookat)
     */
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

    //-------------METADATA SECTION-----------------------------------------------------------------

    /**
     * Get some metadata from the connected device. Useful for debugging...
     *
     * @return
     */
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

    /**
     * Get the device list as String. Useful for debugging...
     *
     * @return
     */
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

    //-------------PROVIDE CURRENT STATE TO THE ACTIVITY--------------------------------------------

    /**
     * Tells the Activity if there is a device. Use this before trying to access GPS-methods...
     *
     * @return - true if there is a device
     */
    public boolean hasDevice() {
        return device != null;
    }

    /**
     * Tells the Activity if a Poller is set. Use this before accessing getLastKnownLatLng().
     * Please be aware that the direct use of getLastKnownLatLng() is strongly discouraged.
     *
     * @return - if a Poller is set
     */
    public boolean isPollerSet() {
        return this.isPollerSet;
    }

    /**
     * Get the possible devices to integrate into UI (maybe);
     *
     * @return - possibleDevicesList
     */
    public static ArrayList<DeviceInfoWrapper> getPossibleDevicesList() {
        return GPS.possibleDevicesList;
    }

    //-------------INTERFACE METHODS----------------------------------------------------------------

    @Override
    public void registerReceiver(GPSDataReceiver gpsDataReceiver) {
        //we could do this with an ArrayList, but we can only use it once
        if(this.gpsDataReceiver == null) {
            this.gpsDataReceiver = gpsDataReceiver;
        }
    }

    @Override
    public void unRegisterReceiver(GPSDataReceiver gpsDataReceiver) {
        this.gpsDataReceiver = null;
    }

    @Override
    public void setPollingInterval(long millis) {
        if(millis > 9999) {
            this.pollingInterval = millis;
        } else {
            this.pollingInterval = 10000;
            Log.i(LCDT, "Polling interval set to 10 seconds.");
        }
    }

    @Override
    public synchronized void updateReceivers() {
        //we could do this with an ArrayList, but we can only use it once
        this.gpsDataReceiver.onUSBGPSLocationChanged(getLastKnownLatLng());
    }

    @Override
    public boolean startPolling() {
        //TODO human-generated method stub
        isPollerSet = true;

        if(this.pollingInterval > 9999) {
            this.pollingClock = new PollingClock(this.pollingInterval);
        } else {
            this.pollingClock = new PollingClock(10000);
            Log.i(LCDT, "Polling interval set to 10 seconds.");
        }
        if(hasEndpoint) {
            /*
            Thread pollingClockThread = new Thread() {
                public void run() {
                    handler.post(GPS.this.pollingClock);
                }
            };
            */

            Thread pollingClockThread = new Thread(this.pollingClock);
            pollingClockThread.start();

            //this.directPoller = new DirectPoller();
            //Thread t = new Thread(this.directPoller);
            //t.start();

            loggah("Polling started", true);
            return true;
        } else {
            loggah("Polling start failed.", true);
            this.pollingClock = null;
            this.directPoller = null;
            isPollerSet = false;
            return false;
            //TODO don't use return values. Work out repeat strategy in this class
        }
    }

    @Override
    public void stopPolling() {
        //TODO human-generated method stub
        if(this.pollingClock != null) {
            this.pollingClock.stop();
            loggah("Polling stopped.", true);
            this.pollingClock = null;
        }
        if(this.directPoller != null) {
            this.directPoller.stop();
            loggah("Polling stopped.", true);
            this.directPoller = null;
        }
        if(this.almostDead && this.isPollerSet) {
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        isPollerSet = false;
    }

    //-------------MULTITHREADING SECTION-----------------------------------------------------------

    /**
     * Originally, this Runnable was used as a clock that creates a Thread which reads data from the
     * USB GPS device in a given interval. However, if the reading time is bigger than the interval,
     * this would cause fatal errors. Therefore, it was changed to calling an Thread and waiting for
     * that Thread to notify this Thread. Without the fixed interval, the easiest way to get data
     * would be a single thread with a loop which is doing the whole work. Unfortunately, this won't
     * work as expected (see the DirectPoller class).
     */
    private class PollingClock implements Runnable{
        /**
         * This is the polling interval.
         */
        long wait;

        /**
         * This flag indicates if the loop has to stop.
         */
        boolean stopFlag;

        /**
         * This constructor is setting the polling interval and the flag.
         *
         * @param time
         */
        public PollingClock(long time) {
            this.wait = time;
            stopFlag = false;
        }

        @Override
        public void run() {
            while(!stopFlag) {
                /*
                Thread thread = new Thread() {
                    public void run() {
                        handler2.post(new Poller());
                    }
                };
                */
                Thread thread = new Thread(new Poller(this));
                thread.start();
                try {
                    //Thread.sleep(this.wait);
                    this.t_lock();
                } catch (InterruptedException e) {
                    Log.e(LCDT, "Couldn't wait in PollingClock.");
                    this.stop();
                }
            }
        }

        /**
         * This method is setting the flag, so there won't be a next loop cycle.
         */
        public void stop () {
            this.stopFlag = true;
        }

        /**
         * Wrap the wait() so it's synchronized (somehow important)
         *
         * @throws InterruptedException - if thread is interrupted
         */
        public synchronized void t_lock() throws InterruptedException {
            this.wait();
        }

        /**
         * Call notify on this Thread.
         *
         * TODO chack if synchronized is necessary
         */
        public synchronized void t_unlock() {
            this.notify();
        }
    }

    /**
     * This is a Thread to outsource the long data loading work to another Thread than the UI Thread.
     */
    private class Poller implements Runnable {

        /**
         * Stores a reference to the calling PollingClock (to call notify())
         */
        private PollingClock pc;

        /**
         * This constructor is setting the reference to pc.
         *
         * @param pc - reference
         */
        public Poller(PollingClock pc) {
            this.pc = pc;
        }

        @Override
        public void run() {
            GPS.this.setLastKnownLatLng();
            Message mess = pollHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putString("mess", "gotMess");
            mess.setData(b);
            pollHandler.sendMessage(mess);
            pc.t_unlock();
            if(GPS.this.almostDead) {
                GPS.this.notify();
            }
        }
    }

    /**
     * The direct Poller is stored here.
     */
    private DirectPoller directPoller;

    /**
     * This would be the best way to poll data without a fixes interval, but somehow it doesn't work...
     *
     * TODO : this is the more elegant way, but it doesn't really work...
     *
     * TODO make inner Javadocs
     */
    private class DirectPoller implements Runnable {
        /**
         * This flag indicates if the loop has to stop.
         */
        private boolean stopFlag = false;

        @Override
        public void run() {
            while(!stopFlag) {
                GPS.this.setLastKnownLatLng();
                Message mess = pollHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("mess", "gotMess");
                mess.setData(b);
                pollHandler.sendMessage(mess);
                if(GPS.this.almostDead) {
                    GPS.this.notify();
                }
            }
        }

        /**
         * This method is setting the flag, so there won't be a next loop cycle.
         */
        public void stop() {
            this.stopFlag = true;
        }
    }

    /**
     * Write something as a debug message into the log.
     *
     * @param mess - message to be logged
     * @param toast - if true then make a short Toast
     */
    public void loggah (String mess, boolean toast) {
        Log.d(LCDT, mess);
        if(toast) {
            Toast.makeText(context, mess, Toast.LENGTH_SHORT).show();
        }
    }
}
