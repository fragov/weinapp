package com.wein3.weinapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

public class Map extends AppCompatActivity implements View.OnClickListener, MapboxMap.OnMyLocationChangeListener, NavigationView.OnNavigationItemSelectedListener {

    private MapView mapView;
    private FloatingActionButton floatingActionButton;

    private MapboxMap mapboxMap;
    private PolylineOptions options;
    Location myLocation;

    private boolean gpsTrackingEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(Map.this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

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
                final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                // get current location
                mapboxMap.getUiSettings().setCompassMargins(mapboxMap.getUiSettings().getCompassMarginLeft(),
                        mapboxMap.getUiSettings().getCompassMarginTop()+80, mapboxMap.getUiSettings().getCompassMarginRight(),
                        mapboxMap.getUiSettings().getCompassMarginBottom()-80);
                mapboxMap.setMyLocationEnabled(true);
                myLocation = mapboxMap.getMyLocation();
                if (!manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                    showGPSDisabledDialog();
                }
                else {
                    // initialize map
                    mapboxMap.setOnMyLocationChangeListener(Map.this);
                    LatLng currentPosition = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                    // move camera to current location
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(currentPosition).zoom(16).build();
                    mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    // add current location as first point to the polyline
                    options = new PolylineOptions();
                    options.add(currentPosition);
                    //mapboxMap.addPolyline(options);

                }
            }
        });




    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.Action1) {
            Intent intent = new Intent(Map.this, GPS.class);
            startActivity(intent);
        } else if (id == R.id.Action2) {

        } else if (id == R.id.Action3) {

        } else if (id == R.id.Action4) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
     * Get current location.
     *
     * @return current location as LatLng instance
     */
    private LatLng getCurrentLocation() {
        Location myLocation = mapboxMap.getMyLocation();
        LatLng currentPosition = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        return currentPosition;
    }

    /**
     * Starts tracking of the GPS coordinates.
     */
    private void startNewRoute() {
        LatLng currentPosition = getCurrentLocation();
        // create a new polyline starting at the current location
        options = new PolylineOptions();
        options.add(currentPosition);
        Polyline line = mapboxMap.addPolyline(options);
        line.setColor(Color.RED);
        line.setWidth(3);
        // enable tracking
        gpsTrackingEnabled = true;
    }

    /**
     * Stops tracking of the GPS coordinates.
     */
    private void stopCurrentRoute() {
        gpsTrackingEnabled = false;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                if (gpsTrackingEnabled) {
                    stopCurrentRoute();
                } else {
                    startNewRoute();
                }
                break;
            default:
                break;
        }
    }

    /**
     * Called when the location of the My Location view has changed
     * (be it latitude/longitude, bearing or accuracy).
     *
     * @param location The current location of the My Location view The type of map change event.
     */
    @Override
    public void onMyLocationChange(@Nullable Location location) {
        if (gpsTrackingEnabled && location != null) {
            LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
            options.add(currentPosition);
            double currentZoom = mapboxMap.getCameraPosition().zoom;
            CameraPosition cameraPosition = new CameraPosition.Builder().target(currentPosition).zoom(currentZoom).build();
            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 25000, null);
        }
    }

    public void showGPSDisabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("GPS ist aus!");
        builder.setMessage("Gps ist ausgeschaltet. Es sind nicht alle Funktionen ohne GPS verfügbar. GPS anschalten?");
        builder.setPositiveButton("GPS anschalten", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

            }
        }).setNegativeButton("Weiter ohne GPS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.create();
        builder.show();
    }
}