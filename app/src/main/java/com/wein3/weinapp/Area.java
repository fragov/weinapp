package com.wein3.weinapp;

import com.mapbox.services.commons.geojson.GeometryCollection;

public class Area {
    private String id;
    private String rev;
    private String description;
    private String gewann;
    private Double size;
    private GeometryCollection geometry;
    private String growingRegion;
    private String currentUsage;
    private String currentLandUsage;
    private String category;
    private Integer channelWidht;
    private Integer wineRowsCount;
    private Integer wineRowsLength;
    private Integer vinesCount;
    private Integer usefullLife;
    private String business;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRev() {
        return rev;
    }

    public void setRev(String rev) {
        this.rev = rev;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGewann() {
        return gewann;
    }

    public void setGewann(String gewann) {
        this.gewann = gewann;
    }

    public Double getSize() {
        return size;
    }

    public void setSize(Double size) {
        this.size = size;
    }

    public GeometryCollection getGeometry() {
        return geometry;
    }

    public void setGeometry(GeometryCollection geometry) {
        this.geometry = geometry;
    }

    public String getGrowingRegion() {
        return growingRegion;
    }

    public void setGrowingRegion(String growingRegion) {
        this.growingRegion = growingRegion;
    }

    public String getCurrentUsage() {
        return currentUsage;
    }

    public void setCurrentUsage(String currentUsage) {
        this.currentUsage = currentUsage;
    }

    public String getCurrentLandUsage() {
        return currentLandUsage;
    }

    public void setCurrentLandUsage(String currentLandUsage) {
        this.currentLandUsage = currentLandUsage;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getChannelWidht() {
        return channelWidht;
    }

    public void setChannelWidht(Integer channelWidht) {
        this.channelWidht = channelWidht;
    }

    public Integer getWineRowsCount() {
        return wineRowsCount;
    }

    public void setWineRowsCount(Integer wineRowsCount) {
        this.wineRowsCount = wineRowsCount;
    }

    public Integer getWineRowsLength() {
        return wineRowsLength;
    }

    public void setWineRowsLength(Integer wineRowsLength) {
        this.wineRowsLength = wineRowsLength;
    }

    public Integer getVinesCount() {
        return vinesCount;
    }

    public void setVinesCount(Integer vinesCount) {
        this.vinesCount = vinesCount;
    }

    public Integer getUsefullLife() {
        return usefullLife;
    }

    public void setUsefullLife(Integer usefullLife) {
        this.usefullLife = usefullLife;
    }

    public String getBusiness() {
        return business;
    }

    public void setBusiness(String business) {
        this.business = business;
    }
}