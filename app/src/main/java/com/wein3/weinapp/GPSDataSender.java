package com.wein3.weinapp;

/**
 * Created by Matthias on 25.09.2017.
 */

public interface GPSDataSender {
    public void registerReceiver(GPSDataReceiver gpsDataReceiver);
    public void unRegisterReceiver(GPSDataReceiver gpsDataReceiver);
    public void setPollingInterval(long millis);
    public void startPolling();
    public void updateReceivers();
    public void stopPolling();
}
