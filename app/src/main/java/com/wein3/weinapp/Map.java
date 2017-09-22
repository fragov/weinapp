package com.wein3.weinapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
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
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.Polygon;
import com.mapbox.services.commons.models.Position;
import com.wein3.weinapp.database.Database;
import com.wein3.weinapp.database.Sqlite;

import java.util.ArrayList;
import java.util.List;

public class Map extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener, MapboxMap.OnMyLocationChangeListener {

    private MapView mapView;
    private FloatingActionButton fabLocation;
    private FloatingActionButton fabPath;

    private MapboxMap mapboxMap;
    private PolylineOptions options;

    private LocationManager locationManager;

    /**
     * Default zoom factor.
     */
    private double defaultZoom = 16;

    /**
     * Boolean flag indicating whether or not GPS tracking of one's current path should be enabled.
     */
    private boolean pathTrackingEnabled;

    /**
     * Boolean flag indicating if the GPS receiver gets a valid signal.
     */
    private boolean receiveGpsSignal;

    private float displayHeight;
    private String description;
    private Database database;

    /**
     * Request code for intent to location source settings activity in order to enable GPS signal.
     */
    private final int REQUEST_CODE_LOCATION_SOURCE_SETTINGS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);

        // initialize database
        database = new Sqlite();
        database.init(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // set status variables
        pathTrackingEnabled = false;
        receiveGpsSignal = false;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        fabPath = (FloatingActionButton) findViewById(R.id.fabPath);
        fabPath.setImageResource(R.drawable.ic_record);
        fabPath.setOnClickListener(this);

        fabLocation = (FloatingActionButton) findViewById(R.id.fabLocation);
        fabLocation.setOnClickListener(this);
        fabLocation.setImageResource(R.drawable.ic_gps);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // request necessary location permissions
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

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = getResources().getDisplayMetrics().density;
        displayHeight = outMetrics.heightPixels / density;

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mbMap) {
                // get Mapbox map instance
                mapboxMap = mbMap;
                // move compass on Mapbox map
                int compassShift = 80;
                mapboxMap.getUiSettings().setCompassMargins(mapboxMap.getUiSettings().getCompassMarginLeft(),
                        compassShift, mapboxMap.getUiSettings().getCompassMarginRight(), (int) displayHeight - compassShift);
                // get the current location only if GPS is enabled
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    initializeMapboxMap();
                } else {
                    showGPSDisabledDialog();
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
        database.close();
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
     * Run necessary initialization for MapboxMap instance.
     */
    private void initializeMapboxMap() {
        // initialize Mapbox map
        mapboxMap.setMyLocationEnabled(true);
        // set listener for future location changes
        mapboxMap.setOnMyLocationChangeListener(Map.this);
        // set camera to current location
        Location location = mapboxMap.getMyLocation();
        if (location != null) {
            // move camera to current position
            mapboxMap.setCameraPosition(CameraPosition.DEFAULT);
            CameraPosition cameraPosition = new CameraPosition.Builder().target(getLatLng(location)).zoom(defaultZoom).build();
            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 10000);
        } else {
            // move camera to default position
            mapboxMap.setCameraPosition(CameraPosition.DEFAULT);
        }
    }

    /**
     * Show dialog to enable GPS.
     */
    private void showGPSDisabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.gps_disabled_dialog_title);
        builder.setMessage(R.string.gps_disabled_dialog_message);
        builder.setPositiveButton(R.string.gps_disabled_dialog_button_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_CODE_LOCATION_SOURCE_SETTINGS);
            }
        });
        builder.setNegativeButton(R.string.gps_disabled_dialog_button_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.create();
        builder.show();
    }

    /**
     * Get current position on the map.
     *
     * @return current location as LatLng instance
     */
    private LatLng getCurrentPosition() {
        return getLatLng(mapboxMap.getMyLocation());
    }

    /**
     * Convert Location to LatLng.
     *
     * @param location current location as Location instance
     * @return current location as LatLng instance
     */
    private LatLng getLatLng(final Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    /**
     * Move camera view to current location (with animation).
     */
    private void moveCamera(final Location location) {
        LatLng currentPosition = getLatLng(location);
        moveCamera(currentPosition);
    }

    /**
     * Move camera view to current location (with animation).
     */
    private void moveCamera(final LatLng position) {
        double currentZoom = mapboxMap.getCameraPosition().zoom;
        CameraPosition cameraPosition = new CameraPosition.Builder().target(position).zoom(currentZoom).build();
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    /**
     * Starts printing the GPS coordinates onto the map.
     */
    private void startNewRoute() {
        // initialize a new polyline
        options = new PolylineOptions();
        // add current location as first point to the polyline
        LatLng currentPosition = getCurrentPosition();
        options.add(currentPosition);
        // add polyline to the map
        Polyline polyline = mapboxMap.addPolyline(options);
        polyline.setColor(Color.RED);
        polyline.setWidth(3);
        // enable further GPS tracking
        pathTrackingEnabled = true;
        // set another icon while recording
        fabPath.setImageResource(R.drawable.ic_stop);
    }

    /**
     * Stops tracking of the GPS coordinates.
     */
    private void stopCurrentRoute() {
        // disable further GPS tracking
        pathTrackingEnabled = false;
        // reset the original icon
        fabPath.setImageResource(R.drawable.ic_record);

        Area area = new Area();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter description");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                description = input.getText().toString();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
        area.setDescription(description);

        List<List<Position>> positions = new ArrayList<>();
        positions.add(new ArrayList<Position>());
        PolygonOptions polygonOptions = new PolygonOptions();

        for (LatLng point : options.getPoints()) {
            positions.get(0).add(Position.fromCoordinates(point.getLongitude(), point.getLatitude(),
                    point.getAltitude()));
            polygonOptions.add(point);
        }


        List<Feature> features = new ArrayList<>();

        features.add(Feature.fromGeometry(Polygon.fromCoordinates(positions)));
        FeatureCollection featureCollection = FeatureCollection.fromFeatures(features);

        area.setFeatureCollection(featureCollection.toJson());

        database.insertArea(area);

        mapboxMap.addPolygon(polygonOptions);
        mapboxMap.removePolyline(options.getPolyline());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_CANCELED) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_LOCATION_SOURCE_SETTINGS:
                initializeMapboxMap();
                break;
        }
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fabPath:
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Location location = mapboxMap.getMyLocation();
                    if (location != null) {
                        if (pathTrackingEnabled) {
                            stopCurrentRoute();
                        } else {
                            startNewRoute();
                        }
                    } else {
                        Toast.makeText(Map.this, R.string.gps_not_available, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Map.this, R.string.gps_turned_off, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.fabLocation:
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Location location = mapboxMap.getMyLocation();
                    if (location != null) {
                        moveCamera(location);
                    } else {
                        Toast.makeText(Map.this, R.string.gps_not_available, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Map.this, R.string.gps_turned_off, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    /**
     * Called when an item in the navigation menu is selected.
     *
     * @param item The selected item
     * @return true to display the item as the selected item
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.

        switch (item.getItemId()) {
            case R.id.Action1:
                startActivity(new Intent(Map.this, GPS.class));
                break;
            case R.id.Action2:
                startActivity(new Intent(Map.this, DB.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
            // GPS signal is valid
            if (!receiveGpsSignal) {
                // GPS signal was invalid before, therefore
                // move the camera to the current location
                moveCamera(location);
                // set the flag correspondingly
                receiveGpsSignal = true;
            }
            // track this new location if path tracking is enabled
            if (pathTrackingEnabled) {
                LatLng currentPosition = getLatLng(location);
                options.add(currentPosition);
            }
        } else {
            // GPS signal is invalid
            if (receiveGpsSignal) {
                // GPS signal was valid before,
                // therefore set the flag accordingly
                receiveGpsSignal = false;
            }
        }
    }
}