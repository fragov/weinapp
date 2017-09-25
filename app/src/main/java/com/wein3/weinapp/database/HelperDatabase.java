package com.wein3.weinapp.database;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;

/**
 * Database handler to store the current path in case of accidental destruction of the app.
 */
public class HelperDatabase {

    private SQLiteDatabase sqLiteDatabase;
    private final String DATABASE_FILE = "helper_database.db";
    private final String TABLE = "current_path";

    /**
     * Initialize the temporary database.
     *
     * @param activity current Activity instance
     */
    public void init(Activity activity) {
        String path = activity.getFilesDir().getParent();
        sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(path + "/" + DATABASE_FILE, null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + "(latitude REAL, longitude REAL);");
    }

    /**
     * Replace the recently saved path with a new one.
     *
     * @param options PolylineOptions instance which should be stored
     */
    public void updateCurrentPath(PolylineOptions options) {
        if (options != null) {
            sqLiteDatabase.execSQL("DELETE FROM " + TABLE);
            for (LatLng position : options.getPoints()) {
                ContentValues values = new ContentValues();
                values.put("latitude", position.getLatitude());
                values.put("longitude", position.getLongitude());
                sqLiteDatabase.insert(TABLE, null, values);
            }
        }
    }

    /**
     * Get the recently saved path.
     *
     * @return PolylineOptions instance representing the path
     */
    public PolylineOptions getCurrentPath() {
        PolylineOptions options = new PolylineOptions();
        Cursor cursor = sqLiteDatabase.query(TABLE, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                double latitude = cursor.getDouble(0);
                double longitude = cursor.getDouble(1);
                LatLng position = new LatLng(latitude, longitude);
                options.add(position);
            } while (cursor.moveToNext());
        }
        return options;
    }

    /**
     * Safely close the database.
     */
    public void close() {
        sqLiteDatabase.close();
    }

}
