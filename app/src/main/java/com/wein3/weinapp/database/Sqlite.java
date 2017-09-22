package com.wein3.weinapp.database;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.*;
import android.util.Log;

import com.wein3.weinapp.Area;

import java.io.File;
import java.util.HashMap;

public class Sqlite implements Database {

    private Activity activity;
    private SQLiteDatabase sqLiteDatabase;
    private static String databaseFile = "sqlite.db";
    private static String table = "areas";

    @Override
    public void init(Activity activity) {
        String path = activity.getFilesDir().getParent();
        //SQLiteDatabase.deleteDatabase(new File(path + "/" + databaseFile));
        sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(path + "/" + databaseFile, null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + table
                + " (id TEXT PRIMARY KEY, rev TEXT, description TEXT, gewann TEXT, size REAL,"
                + "featureCollection TEXT, growingRegion TEXT, currentUsage TEXT, currentLandUsage TEXT,"
                + "category TEXT, channelWidth INTEGER, wineRowsCount INTEGER,"
                + "wineRowsLength INTEGER, vinesCount INTEGER, usefullLife INTEGER, business TEXT);");
    }

    @Override
    public HashMap<String, String> getListOfAreas() {
        HashMap<String, String> listOfAreas = new HashMap<>();
        Cursor rows = sqLiteDatabase.rawQuery("SELECT id, description FROM " + table, null);
        if (rows.moveToFirst()) {
            do {
                listOfAreas.put(rows.getString(0), rows.getString(1));
            } while(rows.moveToNext());
        }
        rows.close();
        return listOfAreas;
    }

    @Override
    public Area getAreaById(String id) {
        Area area = null;
        Cursor row = sqLiteDatabase.rawQuery("SELECT * FROM " + table, null);
        if (row.moveToFirst()) {
            area = new Area();
            area.setId(row.getString(0));
            area.setRev(row.getString(1));
            area.setDescription(row.getString(2));
            area.setGewann(row.getString(3));
            area.setSize(Double.parseDouble(row.getString(4)));
            area.setFeatureCollection(row.getString(5));
            area.setGrowingRegion(row.getString(6));
            area.setCurrentUsage(row.getString(7));
            area.setCurrentLandUsage(row.getString(8));
            area.setCategory(row.getString(9));
            area.setChannelWidth(Integer.parseInt(row.getString(10)));
            area.setWineRowsCount(Integer.parseInt(row.getString(11)));
            area.setWineRowsLength(Integer.parseInt(row.getString(12)));
            area.setVinesCount(Integer.parseInt(row.getString(13)));
            area.setUsefullLife(Integer.parseInt(row.getString(14)));
            area.setBusiness(row.getString(15));

            row.close();
        }
        row.close();
        return area;
    }

    @Override
    public String insertArea(Area area) {
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
        return Long.toString(sqLiteDatabase.insert(table, null, values));
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
        sqLiteDatabase.update(table, values, "id = " + area.getId(), null);
    }

    @Override
    public void removeAreaById(String id) {
        sqLiteDatabase.delete(table, "id = " + id, null);
    }

    @Override
    public void close() {
        sqLiteDatabase.close();
    }
}
