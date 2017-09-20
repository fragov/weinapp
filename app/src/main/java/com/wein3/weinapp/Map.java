package com.wein3.weinapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

public class Map extends AppCompatActivity implements LocationListener {

    private LocationManager locationManager;

    private MapView mapView;
    private PolylineOptions options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

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
            public void onMapReady(MapboxMap mapboxMap) {
                options = new PolylineOptions();
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
                mapboxMap.addPolyline(options);
                // move the user's viewpoint to this position
                CameraPosition cameraPosition = new CameraPosition.Builder().target(currentPosition).zoom(16).build();
                mapboxMap.setCameraPosition(cameraPosition);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
        int gpsPermission = getBaseContext().checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION");
        int networkPermission = getBaseContext().checkCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION");
        if (gpsPermission == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
        } else if (networkPermission == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, this);
        } else {
            locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 5000, 0, this);
        }
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
        locationManager.removeUpdates(this);
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

    // ### methods provided by the LocationListener interface ###

    /**
     * Called when the location has changed.
     *
     * @param location The new location, as a Location object.
     */
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            LatLng newPosition = new LatLng(location.getLatitude(), location.getLongitude());
            options.add(newPosition);
        }
    }

    /**
     * Called when the provider status changes.
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
