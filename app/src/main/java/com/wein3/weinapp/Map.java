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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.couchbase.lite.Document;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.Polygon;
import com.mapbox.services.commons.models.Position;
import com.wein3.weinapp.database.CouchDB;
import com.wein3.weinapp.database.DatabaseObserver;
import com.wein3.weinapp.database.HelperDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Main activity containing map and basic user interface.
 */
public class Map extends AppCompatActivity implements View.OnClickListener,
        NavigationView.OnNavigationItemSelectedListener, MapboxMap.OnCameraMoveListener,
        DatabaseObserver {

    /**
     * Debug tag for logging.
     */
    private final String DEBUG_TAG = this.getClass().getSimpleName();

    /**
     * Specify default character set.
     */
    private final String DEFAULT_CHARSET = "UTF-8";

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
     * Request codes for intents.
     */
    private final int REQUEST_CODE_LOCATION_SOURCE_SETTINGS = 0;
    private final int DB_CONTENT_RESULT_CODE = 1;

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
     * Key to store region name.
     */
    private final String KEY_FIELD_REGION_NAME = "FIELD_REGION_NAME";

    /**
     * Key for Map activity's SharedPreferences.
     */
    private final String KEY_SHARED_PREFERENCES_MAP = "shared_preferences_map";

    /**
     * Manager to handle offline Mapbox maps.
     */
    private OfflineManager offlineManager;

    /**
     * Coordinates that limit the range in which an offline map is to be downloaded.
     */
    private LatLngBounds latLngBounds;

    /**
     * Boolean flag indicating whether or not GPS tracking of one's current path is enabled.
     */
    private boolean pathTrackingEnabled;

    /**
     * Boolean flag indicating whether or not an external GPS provider should be used.
     */
    private boolean useExternalGpsDevice = false;

    /**
     * Current zoom factor of camera view.
     * Negative values indicate that the zoom factor is not set.
     */
    private double currentZoom;

    /**
     * Couchbase database handler to store polygons.
     */
    private CouchDB mainDatabase;

    /**
     * MapView containing the Mapbox map.
     */
    private MapView mapView;

    /**
     * MapboxMap instance managing the map.
     */
    private MapboxMap mapboxMap;

    /**
     * PolylineOptions instance representing the current path's coordinates.
     */
    private PolylineOptions polylineOptions;

    /**
     * Polyline instance representing the actual path on the map.
     */
    private Polyline currentPolyline;

    /**
     * LatLng instance containing the latest position received from TrackingService.
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
     * Global instance of custom BroadcastReceiver
     * to receive location updates from GPS tracking service.
     */
    private TrackingBroadcastReceiver trackingBroadcastReceiver;

    /**
     * FloatingActionButton to select layers
     */
    private FloatingActionButton fabLayers;

    /**
     * FloatingActionButton to move the camera to the current location.
     */
    private FloatingActionButton fabLocation;

    /**
     * FloatingActionButton to enable or disable path tracking.
     */
    private FloatingActionButton fabPath;

    /**
     * Couchbase data.
     */
    private List<Document> documents;

    /**
     *
     */
    private int regionSelected;

    /**
     *
     */
    private ProgressBar progressBar;

    /**
     *
     */
    private HashMap<String, com.mapbox.mapboxsdk.annotations.Polygon> polygons;

    /**
     * Overridden methods of Activity class.
     * =====================================
     */

    /**
     * Create activity and instantiate views and global variables.
     *
     * @param savedInstanceState if the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data
     *                           it most recently supplied in the onSaveInstanceState() method.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // initialize activity
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);

        // create MapView instance
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        // reset instance state
        loadStatus();

        // create or open main database
        mainDatabase = CouchDB.getInstance(getApplication());
        mainDatabase.registerObserver(this);

        polygons = new HashMap<>();

        // create or open helper database
        HelperDatabase.openDatabase(getApplication());

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

        fabLayers = (FloatingActionButton) findViewById(R.id.fabLayers);
        fabLayers.setOnClickListener(this);
        fabLayers.setImageResource(R.drawable.ic_layers);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        // set layout
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // initialize NotificationManager in order to create an "is recording" notification
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (useExternalGpsDevice) {
            // initialize external GPS provider
            GPS.getInstance(getApplicationContext());
        } else {
            // initialize built-in GPS provider
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            // request necessary location permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                }
            }
        }

        // run custom initialization steps as soon as the MapboxMap instance is ready
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mbMap) {
                // get MapboxMap instance
                mapboxMap = mbMap;
                // add camera movement listener
                mapboxMap.setOnCameraMoveListener(Map.this);
                // add the recently saved polyline from the database
                polylineOptions = new PolylineOptions();
                List<LatLng> coordinates = HelperDatabase.getCurrentPath();
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
                int compassShift = 110;

                mapboxMap.getUiSettings().setCompassMargins(
                        mapboxMap.getUiSettings().getCompassMarginLeft(),
                        compassShift,
                        mapboxMap.getUiSettings().getCompassMarginRight(),
                        displayHeight - compassShift
                );

                if (useExternalGpsDevice) {
                    // disable default MapboxMap location listener
                    mapboxMap.setMyLocationEnabled(false);
                    // TODO: Plot current position according to GPS data from external device
                } else {
                    // enable default MapboxMap location listener
                    mapboxMap.setMyLocationEnabled(true);
                    // check if GPS signal is enabled
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
                }

                // read all saved polygons from the database
                if (documents != null) {
                    for (Document document : documents) {
                        addDocumentAsPolygonToMap(document);
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

        // create OfflineManager to handle offline Mapbox maps
        offlineManager = OfflineManager.getInstance(Map.this);
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
     * be able to restore the instance state on recreation. Correspondingly,
     * the database has to be kept open when path tracking is enabled.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (!pathTrackingEnabled) {
            HelperDatabase.close();
        }
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
        switch(requestCode) {
            case DB_CONTENT_RESULT_CODE:
                if (resultCode == DBContent.RESULT_OK && data != null) {
                    moveCamera(new LatLng(data.getDoubleExtra("lat", 0.0),
                            data.getDoubleExtra("lng", 0.0)), DEFAULT_ZOOM_FACTOR,
                            CAMERA_ANIMATION_SHORT);
                    //PolygonInfoListDialogFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
                }
                break;
            case REQUEST_CODE_LOCATION_SOURCE_SETTINGS:
                // Check for the request code regardless of the result code. In Android 4.3
                // the GPS settings menu can only be exited by cancelling it. Maybe in other
                // versions it is different.
                Location location = mapboxMap.getMyLocation();
                moveCamera(location, DEFAULT_ZOOM_FACTOR, CAMERA_ANIMATION_LONG);
                break;
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
        Intent intent = new Intent(this, TrackingService.class);
        intent.putExtra(Variables.KEY_USE_EXTERNAL_GPS_DEVICE, useExternalGpsDevice);
        startService(intent);
        // show corresponding notificationf
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
        Intent intent = new Intent(this, TrackingService.class);
        stopService(intent);
        // cancel notification
        notificationManager.cancel(KEY_NOTIFICATION_MANAGER);
        // cleanup of helper database
        HelperDatabase.clearTable();
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
                documentContent.put("geometry", featureCollection.toJson());
                mainDatabase.insert(documentContent);
                //mapboxMap.addPolygon(polygonOptions);
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
     * Methods responsible for downloading and showing offline Mapbox maps.
     * ====================================================================
     */

    private void downloadRegionDialog() {
        // Set up download interaction. Display a dialog
        // when the user clicks download button and require
        // a user-provided region name
        AlertDialog.Builder builder = new AlertDialog.Builder(Map.this);

        final EditText regionNameEdit = new EditText(Map.this);
        regionNameEdit.setHint(R.string.region_input);
        // Build the dialog box
        builder.setTitle(R.string.new_region_input)
                .setView(regionNameEdit)
                .setMessage(R.string.input_dialog_message)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String regionName = regionNameEdit.getText().toString();
                        // Require a region name to begin the download.
                        // If the user-provided string is empty, display
                        // a toast message and do not begin download.
                        if (regionName.length() == 0) {
                            Toast.makeText(Map.this, R.string.region_name_input, Toast.LENGTH_SHORT).show();
                            downloadRegionDialog();
                        } else {
                            // Begin download process
                            downloadRegion(regionName);
                        }
                    }
                })
                .setNegativeButton(R.string.choose_new_region, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        // Display the dialog
        builder.show();
    }

    /**
     * Downloads a region (with the given Name) on which the screen is currently placed
     * @param regionName
     */
    public void downloadRegion(final String regionName) {
        progressBar.setVisibility(View.VISIBLE);
        //latLngBounds = b.build();
        latLngBounds = mapboxMap.getProjection().getVisibleRegion().latLngBounds;
        double minZoom = mapboxMap.getCameraPosition().zoom;
        double maxZoom = mapboxMap.getMaxZoomLevel();
        float pixelRatio = this.getResources().getDisplayMetrics().density;


        OfflineTilePyramidRegionDefinition definition =
                new OfflineTilePyramidRegionDefinition(mapboxMap.getStyleUrl(),
                        latLngBounds, minZoom, maxZoom, pixelRatio);

        byte[] metadata;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(KEY_FIELD_REGION_NAME, regionName);
            String json = jsonObject.toString();
            metadata = json.getBytes(DEFAULT_CHARSET);
        } catch (Exception exception) {
            Log.e("TAG", "Failed to encode metadata: " + exception.getMessage());
            metadata = null;
        }


        offlineManager.createOfflineRegion(definition, metadata, new OfflineManager.CreateOfflineRegionCallback() {

            @Override
            public void onCreate(OfflineRegion offlineRegion) {

                offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);

                offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
                    @Override
                    public void onStatusChanged(OfflineRegionStatus status) {

                        double percentage = status.getRequiredResourceCount() >= 0 ?
                                (100.0 * status.getCompletedResourceCount() /
                                        status.getRequiredResourceCount()) : 0.0;

                        if (status.isComplete()) {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(Map.this, R.string.toast_region_download_success, Toast.LENGTH_SHORT).show();
                        } else if (status.isRequiredResourceCountPrecise()) {
                            Log.d("TAG", percentage + " ");
                        }
                    }

                    @Override
                    public void onError(OfflineRegionError error) {
                        Log.d("ERROR", "onError reason: " + error.getReason());
                        Log.e("ERROR", "onError message: " + error.getMessage());
                    }

                    @Override
                    public void mapboxTileCountLimitExceeded(long limit) {
                        Log.e("LIMIT", "mapbox tile count limit exceeded: " + limit);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("ERROR", "Error: " + error);
            }
        });
    }

    /**
     * Shows a dialog with all saved lists
     */
    private void listRegions() {
        // Build a region list when the user clicks the list button

        // Reset the region selected int to 0
        regionSelected = 0;

        // Query the DB asynchronously
        offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
            @Override
            public void onList(final OfflineRegion[] offlineRegions) {
                // Check result. If no regions have been
                // downloaded yet, notify user and return
                if (offlineRegions == null || offlineRegions.length == 0) {
                    downloadRegionDialog();
                    return;
                }

                // Add all of the region names to a list
                ArrayList<String> offlineRegionsNames = new ArrayList<>();
                for (OfflineRegion offlineRegion : offlineRegions) {
                    offlineRegionsNames.add(getRegionName(offlineRegion));
                }
                final CharSequence[] items = offlineRegionsNames.toArray(new CharSequence[offlineRegionsNames.size()]);

                // Build a dialog containing the list of regions
                AlertDialog dialog = new AlertDialog.Builder(Map.this)
                        .setTitle(R.string.offlineregions)
                        .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Track which region the user selects
                                regionSelected = which;
                            }
                        })
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // Get the region bounds and zoom
                                LatLngBounds bounds = ((OfflineTilePyramidRegionDefinition)
                                        offlineRegions[regionSelected].getDefinition()).getBounds();
                                double regionZoom = ((OfflineTilePyramidRegionDefinition)
                                        offlineRegions[regionSelected].getDefinition()).getMinZoom();

                                moveCamera(bounds.getCenter(), DEFAULT_ZOOM_FACTOR, CAMERA_ANIMATION_LONG);
                            }
                        })
                        .setNeutralButton(R.string.new_region, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                downloadRegionDialog();
                            }
                        })
                        .setNegativeButton(R.string.delete_region, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // Make progressBar indeterminate and
                                // set it to visible to signal that
                                // the deletion process has begun
                                progressBar.setIndeterminate(true);
                                progressBar.setVisibility(View.VISIBLE);

                                // Begin the deletion process
                                offlineRegions[regionSelected].delete(new OfflineRegion.OfflineRegionDeleteCallback() {
                                    @Override
                                    public void onDelete() {
                                        // Once the region is deleted, remove the
                                        // progressBar and display a toast
                                        progressBar.setVisibility(View.INVISIBLE);
                                        progressBar.setIndeterminate(false);
                                        Toast.makeText(getApplicationContext(), getString(R.string.toast_region_deleted),
                                                Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        progressBar.setVisibility(View.INVISIBLE);
                                        progressBar.setIndeterminate(false);
                                        Log.e("TAG", "Error: " + error);
                                    }
                                });
                            }
                        }).create();
                dialog.show();

            }

            @Override
            public void onError(String error) {
                Log.e("TAG", "Error: " + error);
            }
        });
    }

    /**
     * Gets a Name of Region from a given offlineRegion
     * @param offlineRegion
     * @return
     */
    private String getRegionName(OfflineRegion offlineRegion) {
        // Get the region name from the offline region metadata
        String regionName = "";
        try {
            byte[] metadata = offlineRegion.getMetadata();
            String json = new String(metadata, DEFAULT_CHARSET);
            JSONObject jsonObject = new JSONObject(json);
            regionName = jsonObject.getString(KEY_FIELD_REGION_NAME);
        } catch (Exception exception) {
            Log.e("TAG", "Failed to decode metadata: " + exception.getMessage());
        }
        return regionName;
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
            case R.id.fabLayers:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.select_layer_type);
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(Map.this,
                        android.R.layout.select_dialog_item);
                arrayAdapter.add("Streets");
                arrayAdapter.add("Outdoors");
                arrayAdapter.add("Light");
                arrayAdapter.add("Dark");
                arrayAdapter.add("Satellite");
                arrayAdapter.add("Satellite w/ Streets");

                builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                mapboxMap.setStyle(Style.MAPBOX_STREETS);
                                break;
                            case 1:
                                mapboxMap.setStyle(Style.OUTDOORS);
                                break;
                            case 2:
                                mapboxMap.setStyle(Style.LIGHT);
                                break;
                            case 3:
                                mapboxMap.setStyle(Style.DARK);
                                break;
                            case 4:
                                mapboxMap.setStyle(Style.SATELLITE);
                                break;
                            case 5:
                                mapboxMap.setStyle(Style.SATELLITE_STREETS);
                                break;
                        }

                    }
                });


                builder.show();
                break;
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
                if (useExternalGpsDevice) {
                    // TODO: Move the camera to the latest location from the external GPS provider
                } else {
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
                startActivityForResult(new Intent(Map.this, DBContent.class), DB_CONTENT_RESULT_CODE);
                break;
            case R.id.Action3:
                listRegions();
                break;
            default:
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Methods provided by MapboxMap.OnCameraMoveListener which
     * store camera view information when the camera is moving.
     * ========================================================
     */

    /**
     * Called repeatedly as the camera continues to move after an onCameraMoveStarted call.
     * This may be called as often as once every frame and should not perform expensive operations.
     */
    @Override
    public void onCameraMove() {
        currentZoom = mapboxMap.getCameraPosition().zoom;
    }

    /**
     * Methods provided by DatabaseObserver interface handling the Couchbase database.
     * ===============================================================================
     */

    /**
     * @param documents
     */
    @Override
    public void onRegister(List<Document> documents) {
        this.documents = documents;
    }

    /**
     * @param document
     */
    @Override
    public void onDocumentAdded(Document document) {
        this.documents.add(document);
        addDocumentAsPolygonToMap(document);
    }

    /**
     * @param document
     */
    @Override
    public void onDocumentRemoved(Document document) {
        this.documents.remove(document);
        mapboxMap.removePolygon(polygons.get(document.getId()));
        polygons.remove(document.getId());
    }

    private void addDocumentAsPolygonToMap(Document document) {
        Object geometry = document.getProperty("geometry");
        if (geometry != null) {
            try {
                JSONObject featureCollection = new JSONObject(geometry.toString());
                JSONArray features = featureCollection.getJSONArray("features");
                for(int f = 0; f < features.length(); f++) {
                    JSONObject featureObject = features.getJSONObject(f);

                    Feature feature = Feature.fromJson(featureObject.toString());
                    if (feature.getGeometry() instanceof Polygon) {

                        List<LatLng> list = new ArrayList<>();
                        for (int i = 0; i < ((Polygon) feature.getGeometry()).getCoordinates()
                                .size(); i++) {
                            for (int j = 0;
                                 j < ((Polygon) feature.getGeometry()).getCoordinates().get(i)
                                         .size(); j++) {
                                list.add(new LatLng(
                                        ((Polygon) feature.getGeometry()).getCoordinates().get(i)
                                                .get(j).getLatitude(),
                                        ((Polygon) feature.getGeometry()).getCoordinates().get(i)
                                                .get(j).getLongitude()
                                ));
                            }
                        }

                        polygons.put(document.getId(), mapboxMap.addPolygon(new PolygonOptions()
                                .addAll(list)
                                .fillColor(Color.parseColor("#8A8ACB"))
                                .alpha(0.3f)
                        ));
                    }
                }
            } catch (JSONException e) {
                Log.e(DEBUG_TAG, "Can not create Feature from JSON", e);
            }
        }
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
                    polylineOptions.add(position);
                    updatePolyline(polylineOptions);
                } else {
                    // recently saved position available, therefore just add it to the polyline
                    polylineOptions.add(position);
                    HelperDatabase.addToCurrentPath(latitude, longitude);
                }
            }
            currentPosition = position;
        }
    }

}