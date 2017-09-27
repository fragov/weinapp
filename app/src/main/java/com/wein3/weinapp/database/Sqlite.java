package com.wein3.weinapp.database;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.wein3.weinapp.Area;

import java.util.HashMap;
import java.util.UUID;

/**
 * Database handler to store polygons.
 */
public class Sqlite implements Database {

    private SQLiteDatabase sqLiteDatabase;
    private final String DATABASE_FILE = "sqlite.db";
    private final String AREA_TABLE = "area";

    @Override
    public void init(final Application application) {
        String path = application.getFilesDir().getParent();
        //SQLiteDatabase.deleteDatabase(new File(path + "/" + DATABASE_FILE));
        sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(path + "/" + DATABASE_FILE, null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + AREA_TABLE
                + " (id TEXT PRIMARY KEY, rev TEXT, description TEXT, gewann TEXT, size REAL,"
                + "featureCollection TEXT, growingRegion TEXT, currentUsage TEXT, currentLandUsage TEXT,"
                + "category TEXT, channelWidth INTEGER, wineRowsCount INTEGER,"
                + "wineRowsLength INTEGER, vinesCount INTEGER, usefullLife INTEGER, business TEXT);");
    }

    @Override
    public HashMap<String, String> getListOfAreas() {
        HashMap<String, String> listOfAreas = new HashMap<>();
        Cursor rows = sqLiteDatabase.rawQuery("SELECT id, description FROM " + AREA_TABLE, null);
        if (rows.moveToFirst()) {
            do {
                listOfAreas.put(rows.getString(0), rows.getString(1));
            } while (rows.moveToNext());
        }
        rows.close();
        return listOfAreas;
    }

    @Override
    public Area getAreaById(String id) {
        Area area = null;
        Cursor row = sqLiteDatabase.rawQuery("SELECT * FROM " + AREA_TABLE + " WHERE id = '" + id + "'", null);
        if (row.moveToFirst()) {
            area = new Area();
            area.setId(row.getString(0) != null ? row.getString(0) : "");
            area.setRev(row.getString(1) != null ? row.getString(1) : "");
            area.setDescription(row.getString(2) != null ? row.getString(2) : "");
            area.setGewann(row.getString(3) != null ? row.getString(3) : "");
            area.setSize(row.getString(4) != null ? Double.parseDouble(row.getString(4)) : 0.0);
            area.setFeatureCollection(row.getString(5) != null ? row.getString(5) : "");
            area.setGrowingRegion(row.getString(6) != null ? row.getString(6) : "");
            area.setCurrentUsage(row.getString(7) != null ? row.getString(7) : "");
            area.setCurrentLandUsage(row.getString(8) != null ? row.getString(8) : "");
            area.setCategory(row.getString(9) != null ? row.getString(9) : "");
            area.setChannelWidth(row.getString(10) != null ? Integer.parseInt(row.getString(10)) : 0);
            area.setWineRowsCount(row.getString(11) != null ? Integer.parseInt(row.getString(11)) : 0);
            area.setWineRowsLength(row.getString(12) != null ? Integer.parseInt(row.getString(12)) : 0);
            area.setVinesCount(row.getString(13) != null ? Integer.parseInt(row.getString(13)) : 0);
            area.setUsefullLife(row.getString(14) != null ? Integer.parseInt(row.getString(14)) : 0);
            area.setBusiness(row.getString(15) != null ? row.getString(15) : "");

            row.close();
        }
        row.close();
        return area;
    }

    @Override
    public String insertArea(Area area) {
        ContentValues values = new ContentValues();
        String uuid = UUID.randomUUID().toString();
        values.put("id", uuid);
        values.put("rev", area.getRev());
        values.put("description", area.getDescription());
        values.put("gewann", area.getGewann());
        values.put("size", area.getSize());
        values.put("featureCollection", area.getFeatureCollection());
        values.put("growingRegion", area.getGrowingRegion());
        values.put("currentUsage", area.getCurrentUsage());
        values.put("currentLandUsage", area.getCurrentLandUsage());
        values.put("category", area.getCategory());
        values.put("channelWidth", area.getChannelWidth());
        values.put("wineRowsCount", area.getWineRowsCount());
        values.put("wineRowsLength", area.getWineRowsLength());
        values.put("vinesCount", area.getVinesCount());
        values.put("usefullLife", area.getUsefullLife());
        values.put("business", area.getBusiness());
        sqLiteDatabase.insert(AREA_TABLE, null, values);
        return uuid;
    }

    @Override
    public void updateArea(Area area) {
        ContentValues values = new ContentValues();
        values.put("rev", area.getRev());
        values.put("description", area.getDescription());
        values.put("gewann", area.getGewann());
        values.put("size", area.getSize());
        values.put("featureCollection", area.getFeatureCollection());
        values.put("growingRegion", area.getGrowingRegion());
        values.put("currentUsage", area.getCurrentUsage());
        values.put("currentLandUsage", area.getCurrentLandUsage());
        values.put("category", area.getCategory());
        values.put("channelWidth", area.getChannelWidth());
        values.put("wineRowsCount", area.getWineRowsCount());
        values.put("wineRowsLength", area.getWineRowsLength());
        values.put("vinesCount", area.getVinesCount());
        values.put("usefullLife", area.getUsefullLife());
        values.put("business", area.getBusiness());
        sqLiteDatabase.update(AREA_TABLE, values, "id = " + area.getId(), null);
    }

    @Override
    public void removeAreaById(String id) {
        sqLiteDatabase.delete(AREA_TABLE, "id = " + id, null);
    }

    @Override
    public void close() {
        sqLiteDatabase.close();
    }
}
