package com.wein3.weinapp.database;

import android.app.Application;

import com.wein3.weinapp.Area;

import java.util.HashMap;

public interface Database {

    /**
     * Initialize database.
     *
     * @param application current application.
     */
    void init(Application application);

    /**
     * Get list of available areas as HashMap of IDs and descriptions.
     *
     * @return HashMap of polygons.
     */
    HashMap<String, String> getListOfAreas();

    /**
     * Get area by ID.
     *
     * @param id ID of area.
     * @return polygon data.
     */
    Area getAreaById(String id);

    /**
     * Add new area to database.
     *
     * @param area area instance.
     * @return ID of new polygon.
     */
    String insertArea(Area area);

    /**
     * Update existing area in database.
     *
     * @param area area instance.
     */
    void updateArea(Area area);

    /**
     * Remove area from database.
     *
     * @param id ID of area which should be removed.
     */
    void removeAreaById(String id);

    /**
     * Close database.
     */
    void close();
}
