package com.wein3.weinapp.database;

import com.wein3.weinapp.PolygonData;

import java.util.HashMap;

public interface Database {
    /**
     * Initialise database
     * @return status of initialisation
     */
    boolean init();

    /**
     * Get list of available polygons as HashMap of String ids and String descriptions
     * @return HashMap of polygons
     */
    HashMap<String, String> getPolygonsList();

    /**
     * Get polygon data by id
     * @param id ID of Polygon in database
     * @return Polygon data
     */
    PolygonData getPolygonData(String id);

    /**
     * Add new polygon to database
     * @param polygonData Polygon Data
     * @return id of new Polygon
     */
    String insertPolygonData(PolygonData polygonData);

    /**
     * Update polygon in database
     * @param id ID of Polygon in database
     * @param polygonData Polygon Data
     */
    void updatePolygonData(String id, PolygonData polygonData);

    /**
     * Removes polygon from database
     * @param id ID of Polygon in database
     */
    void removePolygonData(String id);

    /**
     * Close database
     */
    void close();
}
