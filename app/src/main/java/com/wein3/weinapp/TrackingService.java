package com.wein3.weinapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

public class TrackingService extends Service implements MapboxMap.OnMyLocationChangeListener {

    private MapView mapView;
    private MapboxMap mapboxMap;
    private StringBuilder sb;
    private NotificationManager nManager;
    private int NOTIFICATION = R.string.local_service_started;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //get the Manager for the notification
        nManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // Display a notification about us starting.  We put an icon in the status bar.
        showRecordingNotification();
        //gets an instance of Mapbox with our token
        Mapbox.getInstance(this, getString(R.string.access_token));
        //get the Mapview from the xml Layout file
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.content_map, null);
        mapView = (MapView) layout.findViewById(R.id.mapView);
        mapView.onCreate(null);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                TrackingService.this.mapboxMap = mapboxMap;
                initializeMapboxMap();
            }
        });
        sb = new StringBuilder();
        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //this service sends the whole String with all coordinates to the Map activity
        sendMessageToActivity(sb.toString());
        nManager.cancel(NOTIFICATION);
    }


    private void initializeMapboxMap() {
        // initialize Mapbox map
        mapboxMap.setMyLocationEnabled(true);
        // set listener for future location changes
        mapboxMap.setOnMyLocationChangeListener(TrackingService.this);
    }

    private void showRecordingNotification(){
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, Map.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_record)// the status icon
                .setTicker(getText(R.string.local_service_started))  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_started))  // the label of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .setOngoing(true)  //makes the swipe of notification impossible
                .build();

        // Send the notification.
        nManager.notify(NOTIFICATION, notification);
    }

    private void sendMessageToActivity(String message) {
        Intent intent = new Intent("GPSLocationUpdates");
        intent.putExtra("coords", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onMyLocationChange(@Nullable Location location) {
        if(location == null){
            Toast.makeText(this, "Komme nicht an GPS Daten ran", Toast.LENGTH_SHORT).show();
        }
        else {
            //when the String is empty then there is no "," needed at the beginning
            if (sb.toString() == "" || sb.toString() == null) {
                sb.append(location.getLatitude());
                sb.append(",");
                sb.append(location.getLongitude());
            } else {
                sb.append(",");
                sb.append(location.getLatitude());
                sb.append(",");
                sb.append(location.getLongitude());
            }
        }
    }
}
