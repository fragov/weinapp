package com.wein3.weinapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

public class TrackingService extends Service implements MapboxMap.OnMyLocationChangeListener {

    private MapView mapView;
    private MapboxMap mapboxMap;
    Location myLocation;
    boolean lBound = false;
    private StringBuilder sb;
    private Intent i;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        sendMessageToActivity(sb.toString());
    }


    private void initializeMapboxMap() {
        // initialize Mapbox map
        mapboxMap.setMyLocationEnabled(true);
        // set listener for future location changes
        mapboxMap.setOnMyLocationChangeListener(TrackingService.this);
    }

    private void sendMessageToActivity(String msg) {
        Intent intent = new Intent("GPSLocationUpdates");
        intent.putExtra("coords", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onMyLocationChange(@Nullable Location location) {
        myLocation = location;
        LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
        if(sb.toString() == "" || sb.toString() == null){
            sb.append(location.getLatitude());
            sb.append(",");
            sb.append(location.getLongitude());
        }
        else{
            sb.append(",");
            sb.append(location.getLatitude());
            sb.append(",");
            sb.append(location.getLongitude());
        }
    }
}
