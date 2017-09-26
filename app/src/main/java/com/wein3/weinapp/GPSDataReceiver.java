package com.wein3.weinapp;

import com.mapbox.mapboxsdk.geometry.LatLng;

/**
 * Created by Matthias on 25.09.2017.
 */

public interface GPSDataReceiver {
    public void onUSBGPSLocationChanged(LatLng location);
}
