package com.wein3.weinapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.wein3.weinapp.database.HelperDatabase;

/**
 * Service responsible for GPS tracking which broadcasts the current location
 * to other activities or writes them to the database if no broadcast is possible.
 */
public class TrackingService extends Service {

    private LocationManager locationManager;
    private TrackingLocationListener locationListener;
    private GPS gps;
    private boolean useExternalGpsDevice;

    /**
     * Return the communication channel to the service.
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to bindService().
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called by the system every time a client explicitly starts the service by calling
     * startService(), providing the arguments it supplied and a
     * unique integer token representing the start request.
     *
     * @param intent  The Intent supplied to startService() as given.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request to
     *                start.
     * @return The return value indicates what semantics the system should
     * use for the service's current started state.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // check if intent contains the external GPS device flag
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                if (bundle.containsKey(Variables.KEY_USE_EXTERNAL_GPS_DEVICE)) {
                    useExternalGpsDevice = bundle.getBoolean(Variables.KEY_USE_EXTERNAL_GPS_DEVICE);
                } else {
                    useExternalGpsDevice = false;
                }
            } else {
                useExternalGpsDevice = false;
            }
        } else {
            useExternalGpsDevice = false;
        }
        if (useExternalGpsDevice) {
            // enable GPS tracking from external GPS provider
            TrackingGpsDataReceiver trackingGpsDataReceiver = new TrackingGpsDataReceiver();
            gps = GPS.getInstance(getApplicationContext());
            gps.registerReceiver(trackingGpsDataReceiver);
            gps.startPolling();
        } else {
            // enable GPS tracking from built-in GPS provider
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationListener = new TrackingLocationListener();
            int gpsPermission = getBaseContext().checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION");
            int networkPermission = getBaseContext().checkCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION");
            if (gpsPermission == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
            } else if (networkPermission == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0, locationListener);
            } else {
                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 2000, 0, locationListener);
            }
        }
        // initialize helper database
        HelperDatabase.openDatabase(getApplication());
        return START_STICKY;
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (useExternalGpsDevice) {
            // destroy LocationManager for external GPS provider
            gps.stopPolling();
            gps = null;
        } else {
            // destroy LocationManager for built-in GPS provider
            locationManager.removeUpdates(locationListener);
            locationManager = null;
        }
    }

    /**
     * Send current location via broadcast to other activities or
     * save it to the database if broadcasting is not possible.
     *
     * @param latitude latitude of the current location as double value.
     * @param longitude longitude of the current location as double value.
     */
    private void sendLocation(final double latitude, final double longitude) {
        Intent intent = new Intent(Variables.TRACKING_BROADCAST_RECEIVER);
        intent.putExtra(Variables.LATITUDE, latitude);
        intent.putExtra(Variables.LONGITUDE, longitude);
        boolean resultCode = LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        // check if broadcast was successful and save the coordinates in the database if not
        if (!resultCode) {
            HelperDatabase.addToCurrentPath(latitude, longitude);
        }
    }

    /**
     * Custom LocationListener to handle location data from built-in GPS provider.
     */
    private class TrackingLocationListener implements LocationListener {

        /**
         * Called when the location has changed.
         *
         * @param location The new location, as a Location object.
         */
        public void onLocationChanged(Location location) {
            if (location != null) {
                sendLocation(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(TrackingService.this, R.string.gps_not_available, Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Called when the provider status changes. This method is called when
         * a provider is unable to fetch a location or if the provider has recently
         * become available after a period of unavailability.
         *
         * @param provider the name of the location provider associated with this
         *                 update.
         * @param status   LocationProvider.OUT_OF_SERVICE if the
         *                 provider is out of service, and this is not expected to change in the
         *                 near future; LocationProvider.TEMPORARILY_UNAVAILABLE if
         *                 the provider is temporarily unavailable but is expected to be available
         *                 shortly; and LocationProvider.AVAILABLE if the
         *                 provider is currently available.
         * @param extras   an optional Bundle which will contain provider specific
         *                 status variables.
         */
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        /**
         * Called when the provider is enabled by the user.
         *
         * @param provider the name of the location provider associated with this
         *                 update.
         */
        @Override
        public void onProviderEnabled(String provider) {

        }

        /**
         * Called when the provider is disabled by the user.
         *
         * @param provider the name of the location provider associated with this
         *                 update.
         */
        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    /**
     * Custom LocationListener to handle location data from external GPS provider.
     */
    private class TrackingGpsDataReceiver implements GPSDataReceiver {

        /**
         * This method is called from GPSDataSender if location has changed.
         *
         * @param location new location as LatLng instance.
         */
        @Override
        public void onUSBGPSLocationChanged(LatLng location) {
            if (location!= null) {
                sendLocation(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(TrackingService.this, R.string.gps_not_available, Toast.LENGTH_SHORT).show();
            }
        }
    }

}
