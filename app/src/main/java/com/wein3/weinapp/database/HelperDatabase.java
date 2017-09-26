package com.wein3.weinapp.database;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.wein3.weinapp.StatusVariables;

import java.util.ArrayList;
import java.util.List;

/**
 * Database handler to store the current path in case of accidental destruction of the app.
 */
public class HelperDatabase {

    private SQLiteDatabase sqLiteDatabase;
    private final String DATABASE_FILE = "helper_database.db";
    private final String CURRENT_PATH_TABLE = "current_path";
    private final String STATUS_TABLE = "status";

    /**
     * Open or create helper database.
     * Create a table with two rows, one for latitude and the other for longitude.
     *
     * @param activity current Activity instance
     */
    public void init(final Activity activity) {
        String path = activity.getFilesDir().getParent();
        sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(path + "/" + DATABASE_FILE, null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + CURRENT_PATH_TABLE + "(latitude REAL NOT NULL, longitude REAL NOT NULL);");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + STATUS_TABLE + "(path_tracking INTEGER NOT NULL, zoom_factor REAL NOT NULL);");
    }

    /**
     * Replace the recently saved path with the specified one.
     *
     * @param options PolylineOptions instance which should be stored
     */
    public void updateCurrentPath(final PolylineOptions options) {
        if (options != null) {
            deleteTable(CURRENT_PATH_TABLE);
            for (LatLng position : options.getPoints()) {
                ContentValues values = new ContentValues();
                values.put("latitude", position.getLatitude());
                values.put("longitude", position.getLongitude());
                sqLiteDatabase.insert(CURRENT_PATH_TABLE, null, values);
            }
        }
    }

    /**
     * Get the recently saved path.
     *
     * @return List of LatLng instances representing the path
     */
    public List<LatLng> getCurrentPath() {
        List<LatLng> coordinates = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.query(CURRENT_PATH_TABLE, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                double latitude = cursor.getDouble(0);
                double longitude = cursor.getDouble(1);
                LatLng position = new LatLng(latitude, longitude);
                coordinates.add(position);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return coordinates;
    }

    /**
     * Update status variables.
     *
     * @param statusVariables custom class encapsulating necessary attributes
     */
    public void updateStatus(final StatusVariables statusVariables) {
        if (statusVariables != null) {
            deleteTable(STATUS_TABLE);
            ContentValues values = new ContentValues();
            values.put("path_tracking", statusVariables.getPathTrackingAsInteger());
            values.put("zoom_factor", statusVariables.getZoomFactor());
            sqLiteDatabase.insert(STATUS_TABLE, null, values);
        }
    }

    /**
     * Get status variables.
     *
     * @return StatusVariable instance with all attribute values or null if no variables are stored in the database
     */
    public StatusVariables getStatus() {
        StatusVariables statusVariables = null;
        Cursor cursor = sqLiteDatabase.query(STATUS_TABLE, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            int pathTracking = cursor.getInt(0);
            double zoomFactor = cursor.getDouble(1);
            statusVariables = new StatusVariables(pathTracking, zoomFactor);
        }
        cursor.close();
        return statusVariables;
    }

    /**
     * Delete the recently saved path.
     */
    public void deleteTable(final String table) {
        sqLiteDatabase.execSQL("DELETE FROM " + table);
    }

    /**
     * Safely close the database.
     */
    public void close() {
        sqLiteDatabase.close();
    }

}
