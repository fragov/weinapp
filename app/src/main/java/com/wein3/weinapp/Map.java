package com.wein3.weinapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

public class Map extends AppCompatActivity implements MapboxMap.OnMyLocationChangeListener {

    private MapView mapView;
    private FloatingActionButton floatingActionButton;

    private MapboxMap mapboxMap;
    private PolylineOptions options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);

        // request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mbMap) {
                // initialize map
                mapboxMap = mbMap;
                mapboxMap.setMyLocationEnabled(true);
                mapboxMap.setOnMyLocationChangeListener(Map.this);
                // get current location
                Location myLocation = mapboxMap.getMyLocation();
                LatLng currentPosition = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                // move camera to current location
                CameraPosition cameraPosition = new CameraPosition.Builder().target(currentPosition).build();
                mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                // add current location as first point to the polyline
                options = new PolylineOptions();
                options.add(currentPosition);
                mapboxMap.addPolyline(options);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * Called when the location of the My Location view has changed
     * (be it latitude/longitude, bearing or accuracy).
     *
     * @param location The current location of the My Location view The type of map change event.
     */
    @Override
    public void onMyLocationChange(@Nullable Location location) {
        if (location != null) {
            LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
            options.add(currentPosition);
            double currentZoom = mapboxMap.getCameraPosition().zoom;
            CameraPosition cameraPosition =  new CameraPosition.Builder().target(currentPosition).zoom(currentZoom).build();
            mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }
}