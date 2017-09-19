package com.wein3.weinapp;

import com.mapbox.services.commons.geojson.Polygon;

public class PolygonData {
    private String description;
    private Polygon polygon;

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public String getDescription() {
        return description;
    }
}
