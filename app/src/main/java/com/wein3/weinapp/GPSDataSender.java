package com.wein3.weinapp;

/**
 * Created by Matthias on 25.09.2017.
 */

public interface GPSDataSender {
    public GPSDataReceiver getReceiver();
    public void registerReceiver(GPSDataReceiver gpsDataReceiver);
    public void setPollingInterval(long millis);
    public void updateReceivers();
    public void startPolling();
    public void stopPolling();
}
