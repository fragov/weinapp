package com.wein3.weinapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;


import static java.lang.Math.round;

public class TrackingService extends Service {

    private LocationManager locationManager;
    private MyLocationListener listener;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);
        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void sendMessageToActivity(String message) {
        Intent intent = new Intent("GPSLocationUpdates");
        intent.putExtra("coords", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    private void sendCoords(Double x, Double y){
        Intent intent = new Intent("GPSLocationUpdates");
        intent.putExtra("Latitude", x);
        intent.putExtra("Longitude", y);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    public class MyLocationListener implements LocationListener {

        public void onLocationChanged(Location location) {
            if (location == null) {
                Toast.makeText(TrackingService.this, "Komme nicht an GPS Daten ran", Toast.LENGTH_LONG).show();
            } else {
                sendCoords(location.getLatitude(), location.getLongitude());
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
