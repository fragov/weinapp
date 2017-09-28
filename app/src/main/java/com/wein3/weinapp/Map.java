package com.wein3.weinapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.couchbase.lite.Document;
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
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.Polygon;
import com.mapbox.services.commons.models.Position;
import com.wein3.weinapp.database.CouchDB;
import com.wein3.weinapp.database.DatabaseObserver;
import com.wein3.weinapp.database.HelperDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Main activity containing the map.
 */
public class Map extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener, MapboxMap.OnCameraIdleListener, DatabaseObserver {

    /**
     * Debug tag for logging.
     */
    private final String DEBUG_TAG = this.getClass().getSimpleName();

    /**
     * Default zoom factor.
     */
    private final double DEFAULT_ZOOM_FACTOR = 16;

    /**
     * Default camera animation length (long).
     */
    private final int CAMERA_ANIMATION_LONG = 5000;

    /**
     * Default camera animation length (short).
     */
    private final int CAMERA_ANIMATION_SHORT = 1000;

    /**
     * Request code for intent to location source settings activity in order to enable GPS signal.
     */
    private final int REQUEST_CODE_LOCATION_SOURCE_SETTINGS = 0;

    /**
     * Key for NotificationManager.
     */
    private final int KEY_NOTIFICATION_MANAGER = R.string.tracking_service_running;

    /**
     * Key to store path tracking flag.
     */
    private final String KEY_PATH_TRACKING_ENABLED = "path_tracking_enabled";

    /**
     * Key to store zoom factor.
     */
    private final String KEY_ZOOM_FACTOR = "zoom_factor";

    /**
     * Key for Map activity's SharedPreferences.
     */
    private final String KEY_SHARED_PREFERENCES_MAP = "shared_preferences_map";

    /**
     * Boolean flag indicating whether or not GPS tracking of one's current path is enabled.
     */
    private boolean pathTrackingEnabled;

    /**
     * Boolean flag indicating whether or not an external GPS provider should be used.
     */
    private boolean useExternalGpsDevice = false;

    /**
     * Current zoom factor of camera.
     * Negative values indicate that the zoom factor is not set.
     */
    private double currentZoom;

    /**
     * Database handler to store polygons.
     */
    private CouchDB mainDatabase;

    /**
     * Database handler to temporarily store the current path.
     */
    private HelperDatabase helperDatabase;

    /**
     * MapView containing the map.
     */
    private MapView mapView;

    /**
     * MapboxMap instance managing the map.
     */
    private MapboxMap mapboxMap;

    /**
     * PolylineOptions instance representing the coordinates of the path on the map.
     */
    private PolylineOptions polylineOptions;

    /**
     * Polyline instance representing the actual path on the map.
     */
    private Polyline currentPolyline;

    /**
     * LatLng instance containing the latest position received from the TrackingService.
     */
    private LatLng currentPosition;

    /**
     * Global LocationManager instance.
     */
    private LocationManager locationManager;

    /**
     * Global NotificationManager instance.
     */
    private NotificationManager notificationManager;

    /**
     * Global instance of custom BroadcastReceiver to receive location updates
     * from GPS tracking service.
     */
    private TrackingBroadcastReceiver trackingBroadcastReceiver;

    /**
     * FloatingActionButton to move the camera to the current location.
     */
    private FloatingActionButton fabLocation;

    /**
     * FloatingActionButton to enable or disable path tracking.
     */
    private FloatingActionButton fabPath;

    /**
     * Intent to start GPS tracking service.
     */
    private Intent trackingServiceIntent;

    /**
     * Couchbase data.
     */
    private List<Document> documents;

    /**
     * Overridden methods of Activity class.
     * =====================================
     */

    /**
     * Create activity and instantiate views and global variables.
     *
     * @param savedInstanceState if the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in the onSaveInstanceState() method
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // initialize activity
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);

        // reset instance state
        loadStatus();

        // create or open main database
        mainDatabase = CouchDB.getInstance(getApplication());
        mainDatabase.registerObserver(this);

        // create or open helper database
        helperDatabase = new HelperDatabase();
        helperDatabase.init(getApplication());

        // initialize LocationManager and NotificationManager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (useExternalGpsDevice) {
            GPS.getInstance(getApplicationContext());
        }

        // set Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // set FloatingActionButton according to current instance state
        fabPath = (FloatingActionButton) findViewById(R.id.fabPath);
        fabPath.setOnClickListener(this);
        if (pathTrackingEnabled) {
            fabPath.setImageResource(R.drawable.ic_stop);
        } else {
            fabPath.setImageResource(R.drawable.ic_record);
        }
        fabLocation = (FloatingActionButton) findViewById(R.id.fabLocation);
        fabLocation.setOnClickListener(this);
        fabLocation.setImageResource(R.drawable.ic_gps);

        // set layout
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

        // create MapView instance
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        // run custom initialization steps as soon as the MapboxMap instance is ready
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mbMap) {
                // get Mapbox map instance
                mapboxMap = mbMap;
                // add listener for end of camera movement
                mapboxMap.setOnCameraIdleListener(Map.this);
                // add the recently saved polyline from the database
                polylineOptions = new PolylineOptions();
                List<LatLng> coordinates = helperDatabase.getCurrentPath();
                for (LatLng position : coordinates) {
                    polylineOptions.add(position);
                }
                updatePolyline(polylineOptions);
                // move compass on the map according to current screen size
                Display display = getWindowManager().getDefaultDisplay();
                DisplayMetrics outMetrics = new DisplayMetrics();
                display.getMetrics(outMetrics);
                float density = getResources().getDisplayMetrics().density;
                int displayHeight = (int) (outMetrics.heightPixels / density);
                int compassShift = 80;
                mapboxMap.getUiSettings().setCompassMargins(
                        mapboxMap.getUiSettings().getCompassMarginLeft(),
                        compassShift,
                        mapboxMap.getUiSettings().getCompassMarginRight(),
                        displayHeight - compassShift
                );
                // enable plotting of current (or last known) location
                mapboxMap.setMyLocationEnabled(true);
                // set camera view to its default position
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    // GPS is enabled, therefore directly move camera to current location
                    Location location = mapboxMap.getMyLocation();
                    if (currentZoom >= 0) {
                        CameraPosition cameraPosition = new CameraPosition.Builder().target(getLatLng(location)).zoom(currentZoom).build();
                        mapboxMap.setCameraPosition(cameraPosition);
                    } else {
                        moveCamera(location, DEFAULT_ZOOM_FACTOR, CAMERA_ANIMATION_LONG);
                    }
                } else {
                    // GPS is not enabled, therefore show corresponding
                    // dialog asking to enable GPS signal
                    showGPSDisabledDialog();
                }

                if (documents != null) {
                    for (Document document : documents) {
                        try {
                            GeoJsonSource geoJsonSource = new GeoJsonSource(document.getId(),
                                    document.getProperty("featureCollection").toString());
                            mapboxMap.addSource(geoJsonSource);
                            LineLayer lineLayer = new LineLayer(document.getId(), document.getId());
                            mapboxMap.addLayer(lineLayer);
                        } catch (Exception e) {
                            Log.d(DEBUG_TAG, "Cannot add polygons from Database to Map", e);
                        }
                    }
                }
            }
        });

        // create and register custom BroadcastReceiver to receive
        // location updates from GPS tracking service
        trackingBroadcastReceiver = new TrackingBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                trackingBroadcastReceiver,
                new IntentFilter(Variables.TRACKING_BROADCAST_RECEIVER)
        );
    }

    /**
     * Start activity.
     */
    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    /**
     * Make activity accessible.
     */
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * Push activity to the background.
     */
    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * Stop activity.
     */
    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    /**
     * Manage destruction of the activity. The MapboxMap instance consumes
     * a lot of ressources, therefore it may be that the Map activity is
     * destroyed and recreated when changing from portrait to landscape
     * mode or when switching activities within the app. In this case,
     * some status variables have to be saved as SharedPreferences to
     * be able to restore the instance state on recreation.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        helperDatabase.close();
        saveStatus();
    }

    /**
     * This is called when the overall system is running low on memory, and
     * actively running processes should trim their memory usage.
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    /**
     * Called to retrieve per-instance state from an activity before being killed
     * so that the state can be restored in onCreate() or
     * onRestoreInstanceState().
     *
     * @param savedInstanceState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        mapView.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            if (requestCode == REQUEST_CODE_LOCATION_SOURCE_SETTINGS) {
                Location location = mapboxMap.getMyLocation();
                moveCamera(location, DEFAULT_ZOOM_FACTOR, CAMERA_ANIMATION_LONG);
            }
            return;
        }
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed;
     * if you return false it will not be shown.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Methods to move the camera.
     * ===========================
     */

    /**
     * Move camera to given location (with animation).
     * If the given Location instance is null, do nothing. If the given zoom factor is invalid,
     * use the default one. If the given animation length is invalid, do not use an animation.
     *
     * @param location        chosen position as Location instance
     * @param zoomFactor      chosen zoom factor
     * @param animationLength chosen length of the animation in milliseconds
     */
    private void moveCamera(final Location location, final double zoomFactor, final int animationLength) {
        LatLng currentPosition = getLatLng(location);
        moveCamera(currentPosition, zoomFactor, animationLength);
    }

    /**
     * Move camera to given location (with animation).
     * If the given LatLng instance is null, do nothing. If the given zoom factor is invalid,
     * use the default one. If the given animation length is invalid, do not use an animation.
     *
     * @param position        chosen position as LatLng instance
     * @param zoomFactor      chosen zoom factor
     * @param animationLength chosen length of the animation in milliseconds
     */
    private void moveCamera(final LatLng position, final double zoomFactor, final int animationLength) {
        if (position != null) {
            CameraPosition cameraPosition;
            if (zoomFactor >= 0) {
                cameraPosition = new CameraPosition.Builder().target(position).zoom(zoomFactor).build();
            } else {
                cameraPosition = new CameraPosition.Builder().target(position).zoom(DEFAULT_ZOOM_FACTOR).build();
            }
            if (animationLength >= 0) {
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), animationLength);
            } else {
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }

    /**
     * Move camera to given location (with animation) while preserving the current zoom factor.
     * If the given Location instance is null, do nothing. If the given animation length is
     * invalid, do not use an animation.
     *
     * @param location        chosen position as Location instance
     * @param animationLength chosen length of the animation in milliseconds
     */
    private void moveCamera(final Location location, final int animationLength) {
        LatLng currentPosition = getLatLng(location);
        moveCamera(currentPosition, animationLength);
    }

    /**
     * Move camera to given location (with animation) while preserving the current zoom factor.
     * If the given LatLng instance is null, do nothing. If the given animation length is
     * invalid, do not use an animation.
     *
     * @param position        chosen position as LatLng instance
     * @param animationLength chosen length of the animation in milliseconds
     */
    private void moveCamera(final LatLng position, final int animationLength) {
        if (position != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder().target(position).zoom(currentZoom).build();
            if (animationLength >= 0) {
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), animationLength);
            } else {
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }

    /**
     * Methods to update SharedPreferences.
     * ====================================
     */

    /**
     * Save current status variables to SharedPreferences.
     */
    private void saveStatus() {
        SharedPreferences data = getSharedPreferences(KEY_SHARED_PREFERENCES_MAP, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = data.edit();
        edit.putBoolean(KEY_PATH_TRACKING_ENABLED, pathTrackingEnabled);
        edit.putFloat(KEY_ZOOM_FACTOR, (float) currentZoom);
        edit.commit();
    }

    /**
     * Load current status variables from SharedPreferences.
     */
    private void loadStatus() {
        SharedPreferences data = getSharedPreferences(KEY_SHARED_PREFERENCES_MAP, Context.MODE_PRIVATE);
        pathTrackingEnabled = data.getBoolean(KEY_PATH_TRACKING_ENABLED, false);
        currentZoom = (double) data.getFloat(KEY_ZOOM_FACTOR, -1);
    }

    /**
     * Methods for starting and stopping GPS tracking.
     * ================================================
     */

    /**
     * Start printing the GPS coordinates onto the map.
     * Do nothing if the current position cannot be determined.
     */
    private void startNewRoute() {
        // start GPS tracking service
        trackingServiceIntent = new Intent(this, TrackingService.class);
        trackingServiceIntent.putExtra(Variables.KEY_USE_EXTERNAL_GPS_DEVICE, useExternalGpsDevice);
        startService(trackingServiceIntent);
        // show corresponding notification
        showRecordingNotification();
        // set another icon while recording
        fabPath.setImageResource(R.drawable.ic_stop);
        // set flag indicating that further GPS tracking is enabled
        pathTrackingEnabled = true;
    }

    /**
     * Stop tracking of the GPS coordinates.
     */
    private void stopCurrentRoute() {
        // stop GPS tracking service
        stopService(trackingServiceIntent);
        // cancel notification
        notificationManager.cancel(KEY_NOTIFICATION_MANAGER);
        // cleanup of helper database
        helperDatabase.clearTable();
        // save the polyline in main database
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.save_polygon_dialog_enter_description);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                HashMap<String, Object> documentContent = new HashMap<>();
                documentContent.put("description", input.getText().toString());
                List<List<Position>> positions = new ArrayList<>();
                positions.add(new ArrayList<Position>());
                PolygonOptions polygonOptions = new PolygonOptions();
                for (LatLng point : polylineOptions.getPoints()) {
                    positions.get(0).add(Position.fromCoordinates(point.getLongitude(), point.getLatitude(),
                            point.getAltitude()));
                    polygonOptions.add(point);
                }
                List<Feature> features = new ArrayList<>();
                features.add(Feature.fromGeometry(Polygon.fromCoordinates(positions)));
                FeatureCollection featureCollection = FeatureCollection.fromFeatures(features);
                documentContent.put("featureCollection", featureCollection.toJson());
                mainDatabase.insert(documentContent);
                mapboxMap.addPolygon(polygonOptions);
                mapboxMap.removePolyline(polylineOptions.getPolyline());
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mapboxMap.removePolyline(polylineOptions.getPolyline());
            }
        });
        builder.show();
        // reset the original button icon
        fabPath.setImageResource(R.drawable.ic_record);
        // set flag indicating that further GPS tracking is disabled
        pathTrackingEnabled = false;
        // remove current position
        currentPosition = null;
    }

    /**
     * Helper methods.
     * ===============
     */

    /**
     * Convert Location to LatLng.
     *
     * @param location current location as Location instance
     * @return current location as LatLng instance or null, if location instance is invalid
     */
    private LatLng getLatLng(final Location location) {
        if (location != null) {
            return new LatLng(location.getLatitude(), location.getLongitude());
        } else {
            return null;
        }
    }

    /**
     * Replace the current polyline with a new one.
     *
     * @param polylineOptions coordinates of the new polyline as PolylineOptions instance
     */
    private void updatePolyline(final PolylineOptions polylineOptions) {
        if (currentPolyline != null) {
            mapboxMap.removePolyline(currentPolyline);
        }
        currentPolyline = mapboxMap.addPolyline(polylineOptions);
        currentPolyline.setColor(Color.RED);
        currentPolyline.setWidth(3);
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
                startActivityForResult(
                        new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                        REQUEST_CODE_LOCATION_SOURCE_SETTINGS
                );
            }
        });
        builder.setNegativeButton(R.string.gps_disabled_dialog_button_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create();
        builder.show();
    }

    /**
     * Show notification while GPS tracking is running.
     */
    private void showRecordingNotification() {
        // PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, Map.class), 0);
        // set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_record) // the status icon
                .setTicker(getText(R.string.tracking_service_running)) // the status text
                .setWhen(System.currentTimeMillis()) // the time stamp
                .setContentTitle(getText(R.string.tracking_service_running)) // the label of the entry
                .setContentIntent(contentIntent) // the intent to send when the entry is clicked
                .setOngoing(true) // makes the swipe of notification impossible
                .build();
        // send the notification.
        notificationManager.notify(R.string.tracking_service_running, notification);
    }

    /**
     * Methods provided by View.OnClickListener interface.
     * Handling of touch events on the FloatingActionButtons.
     * ======================================================
     */

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fabPath:
                if (useExternalGpsDevice) {
                    if (pathTrackingEnabled) {
                        stopCurrentRoute();
                    } else {
                        startNewRoute();
                    }
                } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    if (pathTrackingEnabled) {
                        stopCurrentRoute();
                    } else {
                        startNewRoute();
                    }
                } else {
                    Toast.makeText(Map.this, R.string.gps_turned_off, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.fabLocation:
                Location location = mapboxMap.getMyLocation();
                if (location != null) {
                    if (currentZoom >= 0) {
                        moveCamera(location, CAMERA_ANIMATION_SHORT);
                    } else {
                        moveCamera(location, DEFAULT_ZOOM_FACTOR, CAMERA_ANIMATION_LONG);
                    }
                } else {
                    Toast.makeText(Map.this, R.string.gps_not_available, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    /**
     * Methods provided by NavigationView.OnNavigationItemSelectedListener interface.
     * Handling of touch events on an item within the navigation bar.
     * ==============================================================
     */

    /**
     * Called when an item in the navigation menu is selected.
     *
     * @param item The selected item
     * @return true to display the item as the selected item
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Action1:
                startActivity(new Intent(Map.this, GPSTester.class));
                break;
            case R.id.Action2:
                startActivity(new Intent(Map.this, DBContent.class));
                break;
            default:
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Methods provided by MapboxMap.OnCameraIdleListener interface.
     * Store information of the camera view when camera movement has ended.
     * ====================================================================
     */

    /**
     * Called when camera movement has ended.
     */
    @Override
    public void onCameraIdle() {
        currentZoom = mapboxMap.getCameraPosition().zoom;
    }

    /**
     * Methods provided by DatabaseObserver interface handling the Couchbase database.
     * ===============================================================================
     */

    /**
     *
     * @param documents
     */
    @Override
    public void onRegister(List<Document> documents) {
        this.documents = documents;
    }

    /**
     *
     * @param document
     */
    @Override
    public void onDocumentAdded(Document document) {
        this.documents.remove(document);
        // TODO: Remove object from map
    }

    /**
     *
     * @param document
     */
    @Override
    public void onDocumentRemoved(Document document) {
        this.documents.add(document);
        // TODO: Add object to map
    }

    /**
     * Custom BroadcastReceiver.
     * =========================
     */

    /**
     * Custom BroadcastReceiver to handle location updates from GPS tracking service.
     */
    private class TrackingBroadcastReceiver extends BroadcastReceiver {

        /**
         * This method is called when the BroadcastReceiver is receiving an Intent
         * broadcast.
         *
         * @param context The Context in which the receiver is running.
         * @param intent  The Intent being received.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            // get location data included in the Intent
            double latitude = intent.getDoubleExtra(Variables.LATITUDE, 0);
            double longitude = intent.getDoubleExtra(Variables.LONGITUDE, 0);
            LatLng position = new LatLng(latitude, longitude);
            // add the current location to the polyline only if tracking is enabled
            if (pathTrackingEnabled) {
                // check if the BroadcastReceiver already received a location update
                if (currentPosition == null) {
                    // no recently saved position available, therefore
                    // add current location as first point to the polyline
                    polylineOptions = new PolylineOptions();
                    polylineOptions.add(position);
                    updatePolyline(polylineOptions);
                } else {
                    // recently saved position available, therefore just add it to the polyline
                    polylineOptions.add(position);
                    helperDatabase.addToCurrentPath(latitude, longitude);
                }
            }
            currentPosition = position;
        }
    }

}