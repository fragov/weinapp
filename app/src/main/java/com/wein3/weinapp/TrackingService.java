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
import android.util.Log;
import android.widget.Toast;

import com.wein3.weinapp.database.HelperDatabase;

public class TrackingService extends Service {

    private HelperDatabase helperDatabase;
    private LocationManager locationManager;
    private TrackingLocationListener locationListener;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // enable GPS tracking
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
        // initialize helper database
        helperDatabase = new HelperDatabase();
        helperDatabase.init(getApplication());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
        locationManager = null;
    }

    private void sendLocation(final double latitude, final double longitude) {
        Intent intent = new Intent(Variables.TRACKING_BROADCAST_RECEIVER);
        intent.putExtra(Variables.LATITUDE, latitude);
        intent.putExtra(Variables.LONGITUDE, longitude);
        boolean resultCode = LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        // check if broadcast was successful and save the coordinates in the database if not
        if (!resultCode) {
            helperDatabase.addToCurrentPath(latitude, longitude);
        }
    }

    private class TrackingLocationListener implements LocationListener {

        public void onLocationChanged(Location location) {
            if (location != null) {
                sendLocation(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(TrackingService.this, R.string.gps_not_available, Toast.LENGTH_SHORT).show();
            }
        }

        public void onProviderDisabled(String provider) {

        }


        public void onProviderEnabled(String provider) {

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    }

}
