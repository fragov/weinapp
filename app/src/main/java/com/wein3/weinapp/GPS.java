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
 * I have tried to use the existence (null or not null) of the this.device as an indicator if the
 * reading process can work. (But that's just one out of many weird concepts in this class.)
 *
 * Please be aware that this product is only a prototype. I am aware that this code is far away from
 * perfect and that there are still some major bugs (as you might see when reading through the left
 * TODOs). I tried my best, but I left so much work to do due to time limitations.
 *
 * TODO reduce time to read data (especially with no reception) because only one GGA/sec from sensor
 *
 * TODO handle crash on ending Activity while PollingClock is running
 *
 * TODO handle crash on removing USB GPS device while PollingClock is running (handle detachment when Threads are still running)
 *
 * TODO handle closit(): device is set null, but this interferes with the singleton concept
 *
 */
public class GPS implements GPSDataSender {

    //-------------FINAL VARIABLES------------------------------------------------------------------

    /**
     * <code>String</code> to test for wrong result when extracting NMEA sentences.
     */
    private static final String FAILED_RESULT = "$No Result";

    /**
     * <code>String</code> to give back if no device was found.
     */
    private static final String NO_DEVICE = "No device found.";

    /**
     * A tag for finding things in the log.
     */
    private static final String LCDT = "LogCatDemoTag";

    /**
     * Name of the <code>Action</code> of the <code>Intent</code> which should be received by
     * the <code>permissionReceiver</code>.
     */
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    /**
     * A static list to check for supported devices.
     */
    private static final ArrayList<DeviceInfoWrapper> possibleDevicesList = new ArrayList<>();

    //-------------VARIABLES RELATED TO MULTITHREADING AND DATA TRANSFER----------------------------

    /**
     * This is the <code>GPS</code> singleton. Singletons are useful if we only want one instance of
     * a class, like in this case.
     */
    private static GPS gpsSingleton;

    /**
     * This is the <code>GPSDataReceiver</code> which receives updates if the location has changed.
     *
     * TODO can be replaced with a List of GPSDataReceivers in the future
     */
    private static GPSDataReceiver gpsDataReceiver;

    /**
     * Variable where the polling interval is stored (in milliseconds).
     */
    private long pollingInterval = -1;

    /**
     * Variable for access to the <code>PollingClock</code> <code>Runnable</code>.
     */
    private PollingClock pollingClock;

    /**
     * This is used to store the last known location. In decimal format.
     */
    private LatLng lastKnownLatLng;

    /**
     * This is used to store the last parsed location. In "wrong" degree format.
     */
    public LatLng debugLastParsedLatLng;

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
     * Buffer length (it's important to read <code>BUFFLEN</code> bytes consecutively if the
     * freshest data has to be read).
     */
    private static final int BUFFLEN = 1024;

    /**
     * Technical stuff to receive the right characters.
     */
    private static final int baudRate = 4800;

    /**
     * Use this to get devices and establish a connection.
     */
    private UsbManager manager;

    /**
     * This this the GPS USB device in use.
     */
    private UsbDevice device;

    /**
     * This represents the <code>connection</code> to the <code>device</code>.
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
     * <code>true</code>, if connection is initialized.
     */
    private boolean isInit = false;

    /**
     * <code>true</code>, if we have an <code>endpoint</code>.
     * Important to know before trying to read data.
     */
    private boolean hasEndpoint = false;

    /**
     *  Shows if we currently receive updates from the <code>Poller</code>.
     */
    private boolean isPollerSet = false;

    /**
     * This indicates if there is a permission to access the USB <code>device</code>.
     */
    private boolean hasPermission = false;

    /**
     * Flag shows if this <code>Object</code> has to die.
     */
    private boolean almostDead = false;

    //-------------READING VARIABLES----------------------------------------------------------------

    /**
     * This represents the index in the buffer where <code>readFromGPS()</code> is currently reading data.
     */
    private int readIndex = 0;

    /**
     * Here, the number of bytes read by <code>connection.bulkTransfer(...)</code> in
     * <code>readFromGPS()</code> is stored.
     */
    private int readCount = 0;

    /**
     * This is used for debugging purposes and sets a flag, if a read was possible.
     */
    private boolean hasRead = false;

    /**
     * This is used for debugging purposes and stores the number of bytes read successfully from the buffer.
     */
    private int hasReadNumber = 0;

    //-------------APPLICATION STUFF----------------------------------------------------------------

    /**
     * This is not an <code>Activity</code>, so a <code>Context</code> has to be stored somewhere.
     */
    private static Context context;

    /**
     * This <code>BroadcastReceiver</code> is used to detect if a USB <code>device</code> is attached.
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
                    onCreate();
                    loggah("ATTACHED - not null", false);
                } else {
                    loggah("ATTACHED - null", false);
                }
            }
        }
    };

    /**
     * This <code>BroadcastReceiver</code> is used to detect if a USB <code>device</code> is detached.
     */
    private final BroadcastReceiver detachReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();

            loggah("detachReceiver.onReceive()",false);
            loggah("USB device detached.", true);

            if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if(device != null) {
                    closit();
                    loggah("DETACHED - not null", false);
                } else {
                    loggah("DETACHED - null", false);
                }
            }
        }
    };

    /**
     * This <code>BroadcastReceiver</code> is used to establish a <code>connection</code> if the
     * permission to access the USB <code>device</code> was granted.
     */
    private final BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            loggah("permissionReceiver.onReceive()",false);

            if(ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        loggah("USB permission granted.", true);
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
     * This <code>handler</code> is used to receive <code>Message</code>s from the
     * <code>Poller</code> <code>Runnable</code>.
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
     *      -getting the <code>UsbManager</code>
     *      -register the <code>BroadcastReceiver</code>s
     *
     * @param context - stores the <code>context</code> we need to access for certain methods
     */
    private GPS(Context context) {
        this.context = context;

        //Set supported devices list.
        //Device: Navilock NL-650US; GPS: MediaTek MT3337; RS-232 to USB: Prolific PL-2303 HXD
        possibleDevicesList.add(new DeviceInfoWrapper(0x067B, 0x2303));

        manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        this.registerBroadcastReceivers();

        this.onCreate();
    }

    /**
     * This method return the instance of GPS. If an instance already exists, just set the context in it.
     *
     * @param context - context used by GPS
     *
     * @return - reference to instance
     */
    public static GPS getInstance(Context context) {
        if(GPS.gpsSingleton == null) {
            //create new GPS Object
            GPS.gpsSingleton = new GPS(context);
        } else {
            //or just change the context
            GPS.context = context;
            //don't forget to re-register the BroadcastReceivers in the new context
            GPS.gpsSingleton.registerBroadcastReceivers();
        }
        if(GPS.gpsSingleton.device == null) {
            //get device
            GPS.gpsSingleton.onCreate();
            GPS.gpsSingleton.loggah("getInstance() -> onCreate()",false);
            if(GPS.gpsSingleton.device != null) {
                //get connection
                GPS.gpsSingleton.init();
                GPS.gpsSingleton.loggah("getInstance() -> init()",false);
            }
        }
        return GPS.gpsSingleton;
    }

    /**
     * This method is used to register the BroadcastReceivers. That's necessary every time a new
     * Context is set.
     */
    private void registerBroadcastReceivers() {
        IntentFilter intentFilter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(permissionReceiver, intentFilter);

        IntentFilter intentFilter2 = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        context.registerReceiver(attachReceiver, intentFilter2);

        IntentFilter intentFilter3 = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(detachReceiver, intentFilter3);

        loggah("Receivers registered.", false);
    }

    //-------------READING METHODS------------------------------------------------------------------

    /**
     * This returns the last known location. If no PollingClock is set, this method calls setLastKnownLatLng().
     * Please be aware that that method would be executed in the UI Thread so it is not recommended
     * to use it directly. Instead, consider using a PollingClock.
     *
     * @return - last known location in LatLng
     */
    public LatLng getLastKnownLatLng() {

        //this is only called there is no Poller
        if(!isPollerSet) setLastKnownLatLng();

        return this.lastKnownLatLng;
    }

    /**
     * This calculates the current location.
     * Please be aware that this method needs a lot of time, so consider using multithreading.
     * Multithreading is currently implemented by the PollingClock
     *
     * TODO removing the synchronized keyword should not have been done, but somehow it fixed the slowdown problem (yay)
     */
    private void setLastKnownLatLng() {
        //get a GGA NMEA sentence
        String gga = getFirstGGAString();
        loggah("GGA String - " + gga, false);
        //parse the sentence into coordinates
        this.lastKnownLatLng = ggaToLatLng(gga);
        if(this.lastKnownLatLng != null) {
            loggah("Coordinates: " + lastKnownLatLng.getLatitude() + "," + lastKnownLatLng.getLongitude(), false);
        }
    }

    /**
     * This returns a LatLng object with the coordinates specified by the parameter, which has to be
     * a String containing an NMEA GGA sentence.
     *
     * @param gga - GGA dataset
     * @return - LatLng with location from GGA dataset
     */
    public LatLng ggaToLatLng(String gga) {
        //check for valid sentences
        if(gga.equals(FAILED_RESULT)) {
            loggah("Got invalid information from sensor.", true);
            Log.e(LCDT, "Got invalid information from sensor.");
            return null;
        }
        //get single parameters
        String[] parts = gga.split(",");

        //prepare debug message
        String toasterMessage = "";
        boolean toasted = false;

        double lat = 0;
        double debugLat = 0;
        try {
            //check if data is coming as expected
            if(parts[2].length() != 9) throw new NumberFormatException("No Latitude.");

            //get the information for degree, minutes, seconds
            String latDeg = parts[2].substring(0, 2);
            String latMin = parts[2].substring(2, 4);
            String latSec = parts[2].substring(5, 7) + "." + parts[2].substring(7);

            double lad = Double.parseDouble(latDeg);
            double lam = Double.parseDouble(latMin);
            double las = Double.parseDouble(latSec);

            //convert it into a valid double value
            las /= 60;
            lam += las;
            lam /= 60;
            lat = lad + lam;

            //just parse the "wrong" double value from the raw data
            debugLat = Double.parseDouble(parts[2]) / 100;
        } catch (NumberFormatException e) {
            toasterMessage += "No Latitude";
            toasted = true;
        }
        //make hemisphere adjustments
        if(parts[3].equals("S")) {
            lat *= -1;
        }

        double lng = 0;
        double debugLng = 0;
        try {
            //check if data is coming as expected
            if(parts[4].length() != 10) throw new NumberFormatException("No Longitude.");

            //get the information for degree, minutes, seconds
            String longDeg = parts[4].substring(0, 3);
            String longMin = parts[4].substring(3, 5);
            String longSec = parts[4].substring(6, 8) + "." + parts[4].substring(8);

            double lod = Double.parseDouble(longDeg);
            double lom = Double.parseDouble(longMin);
            double los = Double.parseDouble(longSec);

            //convert it into a valid double value
            los /= 60;
            lom += los;
            lom /= 60;
            lng = lod + lom;

            //just parse the "wrong" double value from the raw data
            debugLng = Double.parseDouble(parts[4]) / 100;
        } catch (NumberFormatException e) {
            if(toasted) {
                toasterMessage += " nor Longitude";
            } else {
                toasterMessage += "No Longitude";
            }
            toasted = true;
        }
        //make hemisphere adjustments
        if(parts[5].equals("W")) {
            lat *= -1;
        }

        if(toasted) {
            //set complete toaster message and return null (null means: no gps reception)
            toasterMessage += " found.";
            //the Toast isn't actually displayed anymore, but the message is still logged
            loggah(toasterMessage, false);
            return null;
        }

        //prepare return value("s")
        LatLng retVal = new LatLng(lat, lng);
        this.debugLastParsedLatLng = new LatLng(debugLat, debugLng);
        try {
            //get the time when data was received from the device
            double alt = Double.parseDouble(parts[1]);
            //TODO don't set wrong altitude, time was just for debugging
            retVal.setAltitude(alt);
        } catch (NumberFormatException e) {
            loggah("Time Fail.", false);
        }
        return retVal;
    }

    /**
     * Get the next GGA dataset from the USB GPS device.
     *
     * @return - GGA dataset
     */
    private String getFirstGGAString() {
        //get raw data (already as String)
        String data = getSomeData();

        StringBuilder builder = new StringBuilder();
        //get single NMEA sentences separated by $ (they are actually beginning with $)
        String[] sentences = data.split("\\$");
        loggah("Data length: " + data.length() + "; Array Length: " + sentences.length, false);
        String result = "No Result";
        for(String sen: sentences) {
            if(sen.equals("")) continue;
            //look for the right protocol
            if(sen.startsWith("GPGGA")) {
                //set the right sentence as result
                result = sen;
                break;
            }
        }
        builder.append('$');
        builder.append(result);

        return builder.toString();
    }

    /**
     * Read <code>BUFFLEN</code> bytes in a buffer and turn the buffer into a String.
     *
     * @return - raw data as a String
     */
    private String getSomeData() {
        //allocate new memory for the buffer (maybe this has not to be done every time)
        bytes = new byte[BUFFLEN];

        //measure time for log
        long then = System.currentTimeMillis();

        StringBuilder raw = new StringBuilder();
        //TODO make this more efficient
        for(int i=0; i<BUFFLEN; i++) {
            char c = 0;
            try {
                c = (char) readFromGPS();
            } catch (GPSException e) {
                //this was intended to work if device is disconnected during the reading process
                return NO_DEVICE;
            }
            //get char and put it into a StringBuilder
            raw.append(c);
        }

        long now = System.currentTimeMillis() - then;
        loggah("Reading Time: " + now, false);

        return raw.toString();
    }

    /**
     * This methods reads a byte from the device. The buffer is loaded lazy if needed. Therefore
     * it's recommended to call this method consecutively BUFFLEN times for always receiving the
     * most recent data.
     *
     * @return - a byte read from the buffer
     * @throws GPSException - is thrown if a device is missing when trying to read. TODO This still doesn't work as expected.
     */
    private byte readFromGPS() throws GPSException {
        byte b = 0;

        //if all data in the buffer is read, get new data
        if(readIndex >= readCount) {
            if(device == null) {
                //throw Exception if read is impossible
                throw new GPSException(NO_DEVICE);
            }
            readCount = connection.bulkTransfer(endpoint, bytes, BUFFLEN, TIMEOUT);
            //set reading index to the first element in the buffer
            readIndex = 0;
        }
        //call this if there is still unread data in the buffer
        if(readIndex < readCount) {
            b = bytes[readIndex];
            hasRead = true;
            hasReadNumber++;
            //move index forward
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
            //get one device (usually the only device)
            device = iterator.next();
        }

        //check for errors
        boolean nullHoldsAtOnCreate = false;
        boolean emptyDeviceListHoldsAtOnCreate = false;

        if(device == null) nullHoldsAtOnCreate = true;
        if(manager.getDeviceList().isEmpty()) emptyDeviceListHoldsAtOnCreate = true;
        if(nullHoldsAtOnCreate || emptyDeviceListHoldsAtOnCreate) {
            loggah("Maybe no device at startup: " + nullHoldsAtOnCreate + ", " + emptyDeviceListHoldsAtOnCreate, false);
            device = null;
            return;
        }

        //check for correct device
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

        //request permission for device
        if(!hasPermission) {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
            manager.requestPermission(device, permissionIntent);
        }
    }

    /**
     * Used to close connection, remove device and reset state.
     */
    public void closit() {
        loggah("Entered closit().", false);
        //stop polling (still a bit buggy when called from here)
        this.stopPolling();
        //close connection
        if(connection != null) {
            if(hasEndpoint && connection.releaseInterface(intf)) {
                loggah("Released interface.", false);
                connection.close();
            }
        } else {
            loggah("Connection is null on closit().",  false);
        }
        //remove device
        if(device != null) {
            device = null;
        } else {
            loggah("Device is null on closit().",  false);
        }
        //reset status
        isInit = false;
        hasEndpoint = false;
        hasPermission = false;
    }

    /**
     * This method should be called in an Activity's onDestroy() method. It unregisters all
     * BroadcastReceivers and should disconnect everything "safely" if the Activity is closed.
     * Only to be called in the LAST onDestroy() in the lifecycle of the WHOLE Application.
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
    }

    /**
     * This method should establish a connection.
     */
    private void init() {
        if(!isInit) {
            isInit = true;

            //get device (if not already done)
            this.onCreate();

            //init buffer for transfer control
            bytes = new byte[BUFFLEN];

            //for further explanations see:
            //https://developer.android.com/guide/topics/connectivity/usb/host.html#working-d
            //get connection
            connection = manager.openDevice(device);
            if(connection != null) {
                loggah("Got connection. Interface count: " + device.getInterfaceCount(), false);
                //get interface
                intf = device.getInterface(0);
                loggah("Endpoint count: " + intf.getEndpointCount(), false);

                for(int i=0; i<intf.getEndpointCount(); i++) {
                    UsbEndpoint temp = intf.getEndpoint(i);
                    if(temp.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK && temp.getDirection() == UsbConstants.USB_DIR_IN) {
                        //finally get endpoint
                        endpoint = temp;
                    }
                }

                if(endpoint != null) {
                    loggah("Hazn Endpoint.", false);
                    connection.claimInterface(intf, true);
                    byte[] buffer = new byte[1];
                    //got this transfer control parameters from:
                    //https://github.com/sintrb/Android-PL2303HXA/blob/master/Android-PL2303HXA/src/com/sin/android/usb/pl2303hxa/PL2303Driver.java
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
        //just pass these parameters to connection.controlTransfer() and use the return value
        //to check for errors
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

    /**
     * This method is used to register a GPSDataReceiver in this GPSDataSender.
     * Currently only usable for only one GPSDataReceiver at a time.
     *
     * @param gpsDataReceiver - GPSDataReceiver to be registered
     */
    @Override
    public void registerReceiver(GPSDataReceiver gpsDataReceiver) {
        //we could do this with an ArrayList, but we can only use it once anyways
        this.gpsDataReceiver = gpsDataReceiver;
    }

    /**
     * This method is used to unregister a GPSDataReceiver in this GPSDataSender.
     * Currently only usable for only one GPSDataReceiver at a time.
     *
     * @param gpsDataReceiver - GPSDataReceiver to be unregistered.
     */
    @Override
    public void unRegisterReceiver(GPSDataReceiver gpsDataReceiver) {
        this.gpsDataReceiver = null;
    }

    /**
     * Set the interval between two attempts to read data from the USB GPS device.
     * Please note, that this interval is currently the biggest problem.
     * Therefore, it's not guaranteed to receive data in your specified interval.
     *
     * @param millis - interval time in milliseconds
     */
    @Override
    public void setPollingInterval(long millis) {
        if(millis > 9999) {
            this.pollingInterval = millis;
        } else {
            //this looooong time comes from errors with a fixed and too short interval
            //and can be changed in the future if a polling interval is reconsidered
            this.pollingInterval = 10000;
            Log.i(LCDT, "Polling interval set to 10 seconds.");
        }
    }

    @Override
    public void updateReceivers() {
        //TODO synchronized should not have been removed, too. But I think, for now it's the best to
        //TODO reduce the synchronized keyword overall because it's rather error-prone
        //we could do this with an ArrayList, but we can only use it once anyways
        this.gpsDataReceiver.onUSBGPSLocationChanged(getLastKnownLatLng());
        /*
        if(debugLastParsedLatLng != null) {
            Toast.makeText(context, "" + debugLastParsedLatLng.getLatitude() + ", " + debugLastParsedLatLng.getLongitude(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "null", Toast.LENGTH_SHORT).show();
        }
        */
    }

    @Override
    public boolean startPolling() {
        //TODO human-generated method stub
        isPollerSet = true;
        if(hasEndpoint) {
            if(this.pollingInterval > 9999) {
                this.pollingClock = new PollingClock(this.pollingInterval);
            } else {
                //this looooong time comes from errors with a fixed and too short interval
                //and can be changed in the future if a polling interval is reconsidered
                this.pollingClock = new PollingClock(10000);
                Log.i(LCDT, "Polling interval set to 10 seconds.");
            }
            //start thread
            Thread pollingClockThread = new Thread(this.pollingClock);
            pollingClockThread.start();

            //this doesn't work:
            //this.directPoller = new DirectPoller();
            //Thread t = new Thread(this.directPoller);
            //t.start();

            loggah("Polling started", true);
            return true;
        } else {
            //reset values if start failed
            loggah("Polling start failed.", true);
            this.pollingClock = null;
            this.directPoller = null;
            isPollerSet = false;
            return false;
        }
    }

    @Override
    public void stopPolling() {
        //TODO human-generated method stub
        //stop the Threads
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
                    //this is buggy, but this should prevent to kill the process when the
                    //Poller Thread is still running
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        almostDead = false;
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
         *
         * Even if it's not in use, you should keep this in case we want to implement a restriction
         * for the polling frequency.
         */
        long wait;

        /**
         * This flag indicates if the loop has to stop.
         */
        boolean stopFlag;

        /**
         * This constructor is setting the polling interval and the flag.
         *
         * @param time - polling interval
         */
        public PollingClock(long time) {
            this.wait = time;
            stopFlag = false;
        }

        @Override
        public void run() {
            while(!stopFlag) {
                //Start a separate Thread. One thread could be sufficient (e.g. DirectPoller and not
                //PollingClock creating Poller-Threads), but when a fixed polling interval has to be
                //used, we need these nested Threads
                Thread thread = new Thread(new Poller(this));
                thread.start();
                try {
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
         * Call notify on this Thread. Called from outside.
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
            //get the data from the device
            GPS.this.setLastKnownLatLng();
            //handle message (inter-Thread-communication
            Message mess = pollHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putString("mess", "gotMess");
            mess.setData(b);
            pollHandler.sendMessage(mess);
            //wake up the thread for a new Poller to be executed
            pc.t_unlock();
            if(GPS.this.almostDead) {
                //if the process in the UI Thread ends, it should wait for the notify, because
                //otherwise, it's not guaranteed that this Thread can still access all necessary
                //methods and data
                GPS.this.notify();
            }
        }
    }

    /**
     * The direct Poller is stored here.
     */
    private DirectPoller directPoller;

    /**
     * This would be the best way to poll data without a fixed interval, but somehow it doesn't work...
     *
     * TODO : this is the more elegant way, but it doesn't really work...
     *
     * Maybe it has something to do with the wait() and notify() process between PollingClock
     * and Poller above?
     */
    private class DirectPoller implements Runnable {
        /**
         * This flag indicates if the loop has to stop.
         */
        private boolean stopFlag = false;

        @Override
        public void run() {
            while(!stopFlag) {
                //get the data from the device
                GPS.this.setLastKnownLatLng();
                //handle message (inter-Thread-communication
                Message mess = pollHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("mess", "gotMess");
                mess.setData(b);
                pollHandler.sendMessage(mess);
                if(GPS.this.almostDead) {
                    //if the process in the UI Thread ends, it should wait for the notify, because
                    //otherwise, it's not guaranteed that this Thread can still access all necessary
                    //methods and data
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
