package com.wein3.weinapp;

/**
 * Interface for a class that provides GPS data.
 * Works like an Observable in the Observer-Pattern.
 */
public interface GPSDataSender {
    /**
     * This method is used to register a GPSDataReceiver in this GPSDataSender.
     * Currently only usable for only one GPSDataReceiver at a time.
     *
     * @param gpsDataReceiver - GPSDataReceiver to be registered
     */
    public void registerReceiver(GPSDataReceiver gpsDataReceiver);

    /**
     * This method is used to unregister a GPSDataReceiver in this GPSDataSender.
     * Currently only usable for only one GPSDataReceiver at a time.
     *
     * @param gpsDataReceiver - GPSDataReceiver to be unregistered.
     */
    public void unRegisterReceiver(GPSDataReceiver gpsDataReceiver);

    /**
     * Set the interval between two attempts to read data from the USB GPS device.
     * Please note, that this interval is currently the biggest problem.
     * Therefore, it's not guaranteed to receive data in your specified interval.
     *
     * @param millis - interval time in milliseconds
     */
    public void setPollingInterval(long millis);

    /**
     * Try to start the sensor data polling process.
     *
     * @return - true if start is successful, false otherwise
     */
    public boolean startPolling();

    /**
     * Update all registered GPSDataReceivers.
     */
    public void updateReceivers();

    /**
     * Stop the polling process.
     */
    public void stopPolling();
}
