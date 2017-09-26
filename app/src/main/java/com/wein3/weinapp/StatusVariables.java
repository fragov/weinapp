package com.wein3.weinapp;

/**
 * Helper class encapsulating important status variables.
 */
public class StatusVariables {

    private boolean pathTracking;
    private double zoomFactor;

    public StatusVariables(final boolean pathTracking, final double zoomFactor) {
        this.setPathTrackingAsBoolean(pathTracking);
        this.setZoomFactor(zoomFactor);
    }

    public StatusVariables(final int pathTracking, final double zoomFactor) {
        this.setPathTrackingAsInteger(pathTracking);
        this.setZoomFactor(zoomFactor);
    }

    public int getPathTrackingAsInteger() {
        if (pathTracking) {
            return 1;
        } else {
            return 0;
        }
    }

    public boolean getPathTrackingAsBoolean() {
        return pathTracking;
    }

    public void setPathTrackingAsInteger(final int pathTrackingEnabled) {
        if (pathTrackingEnabled == 0) {
            this.pathTracking = false;
        } else {
            this.pathTracking = true;
        }
    }

    public void setPathTrackingAsBoolean(final boolean pathTrackingEnabled) {
        this.pathTracking = pathTrackingEnabled;
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    public void setZoomFactor(final double zoomFactor) {
        this.zoomFactor = zoomFactor;
    }
}
