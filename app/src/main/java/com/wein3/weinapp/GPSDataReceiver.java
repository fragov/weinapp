package com.wein3.weinapp;

import com.mapbox.mapboxsdk.geometry.LatLng;

/**
 * Interface for a class that receives GPS data.
 * Works like an Observer in the Observer-Pattern.
 */
public interface GPSDataReceiver {
    /**
     * This method is called from GPSDataSender if location has changed.
     * Implement this method to receive location updates.
     *
     * @param location - new location
     */
    public void onUSBGPSLocationChanged(LatLng location);
}
