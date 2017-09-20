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
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
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
                mapboxMap = mbMap;
                mapboxMap.setMyLocationEnabled(true);
                Location myLocation = mapboxMap.getMyLocation();
                LatLng currentPosition = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                CameraPosition cameraPosition = new CameraPosition.Builder().target(currentPosition).zoom(16).build();
                mapboxMap.setCameraPosition(cameraPosition);
                mapboxMap.setOnMyLocationChangeListener(Map.this);
                options = new PolylineOptions();
                Location location = mapboxMap.getMyLocation();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                options.add(new LatLng(latitude, longitude));
                mapboxMap.addPolyline(options);

                // options = new PolylineOptions();
                /**
                 Location location;
                 // get the user's last known location as the first point of the polyline
                 int gpsPermission = getBaseContext().checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION");
                 int networkPermission = getBaseContext().checkCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION");
                 if (gpsPermission == PackageManager.PERMISSION_GRANTED) {
                 location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                 } else if (networkPermission == PackageManager.PERMISSION_GRANTED) {
                 location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                 } else {
                 location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                 }
                 LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
                 options.add(currentPosition);
                 **/
                // mapboxMap.addPolyline(options);
                // move the user's viewpoint to this position
                /**

                 **/
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
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            options.add(new LatLng(latitude, longitude));
        }
        Toast.makeText(getApplicationContext(), "Hallo", Toast.LENGTH_LONG).show();
    }
}
