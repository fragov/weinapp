package com.wein3.weinapp.database;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Database handler to store the current path so that it can
 * be re-established in case of accidental destruction of the app.
 */
public class HelperDatabase {

    private static SQLiteDatabase sqLiteDatabase;
    private static final String DATABASE_FILE = "helper_database.db";
    private static final String CURRENT_PATH_TABLE = "current_path";

    /**
     * Open or create helper database.
     * Create a table with two rows, one for latitude and the other for longitude.
     *
     * @param application current Application instance.
     */
    public static void openDatabase(final Application application) {
        if (sqLiteDatabase == null) {
            String path = application.getFilesDir().getParent();
            sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(path + "/" + DATABASE_FILE, null);
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + CURRENT_PATH_TABLE + "(latitude REAL NOT NULL, longitude REAL NOT NULL);");
        }
    }

    /**
     * Add a single coordinate to current path.
     *
     * @param latitude  double value representing the latitude.
     * @param longitude double value representing the longitude.
     */
    public static void addToCurrentPath(final double latitude, final double longitude) {
        ContentValues values = new ContentValues();
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        sqLiteDatabase.insert(CURRENT_PATH_TABLE, null, values);
    }

    /**
     * Get the recently saved path.
     *
     * @return List of LatLng instances representing the path.
     */
    public static List<LatLng> getCurrentPath() {
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
     * Delete the recently saved path.
     */
    public static void clearTable() {
        sqLiteDatabase.execSQL("DELETE FROM " + CURRENT_PATH_TABLE);
    }

    /**
     * Safely close the database.
     */
    public static void close() {
        sqLiteDatabase.close();
        sqLiteDatabase = null;
    }

}
