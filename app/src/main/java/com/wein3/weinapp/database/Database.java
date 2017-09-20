package com.wein3.weinapp.database;

import com.wein3.weinapp.Area;

import java.util.HashMap;

public interface Database {
    /**
     * Initialise database
     * @return status of initialisation
     */
    boolean init();

    /**
     * Get list of available areas as HashMap of String ids and String descriptions
     * @return HashMap of polygons
     */
    HashMap<String, String> getListOfAreas();

    /**
     * Get area by id
     * @param id ID of Area in database
     * @return Polygon data
     */
    Area getAreaById(String id);

    /**
     * Add new area to database
     * @param area Area Object
     * @return id of new Polygon
     */
    String insertArea(Area area);

    /**
     * Update existing area in database
     * @param id ID of Area in database
     * @param area Area Object
     */
    void updateArea(String id, Area area);

    /**
     * Removes area from database
     * @param id ID of Area in database
     */
    void removeAreaById(String id);

    /**
     * Close database
     */
    void close();
}
