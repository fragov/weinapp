package com.wein3.weinapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mapbox.mapboxsdk.geometry.LatLng;

/**
 * This Activity has no explanation because we won't need this in our finished application.
 * For now, it's just a simple way to test the GPS class.
 */
public class GPSTester extends AppCompatActivity implements GPSDataReceiver {

    /**
     * Button to start and stop polling process.
     */
    private Button getLatLong;

    /**
     * TextView to show latitude.
     */
    private TextView textViewLat;

    /**
     * TextView to show longitude.
     */
    private TextView textViewLong;

    /**
     * TextView to show time provided by sensor.
     */
    private TextView satTime;

    /**
     * Button which is sending a Toast if pressed. Used to test if multithreading works and
     * responsiveness is guaranteed.
     */
    private Button multithreadToaster;

    private GPS gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        getLatLong = (Button) findViewById(R.id.getLatLong);
        textViewLat = (TextView) findViewById(R.id.textViewLat);
        textViewLong = (TextView) findViewById(R.id.textViewLong);
        satTime = (TextView) findViewById(R.id.satTime);
        multithreadToaster = (Button) findViewById(R.id.multithreadToaster);

        gps = GPS.getInstance(this);
        gps.registerReceiver(this);
        gps.setPollingInterval(10000);

        getLatLong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!gps.hasDevice()) {
                    loggah("No device found upon button usage.", true);
                    return;
                }
                if(!gps.isPollerSet()) {
                    gps.startPolling();
                    getLatLong.setText("stop polling");
                } else {
                    gps.stopPolling();
                    getLatLong.setText("start polling");
                }
            }
        });

        multithreadToaster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loggah("Multithread toasted.", true);
            }
        });
    }

    /**
     * Conveniently call the logging method from the gps.
     *
     * @param mess - look at gps.loggah(mess, toast)
     * @param toast - look at gps.loggah(mess, toast)
     */
    public void loggah(String mess, boolean toast) {
        if(gps != null) gps.loggah(mess, toast);
    }

    @Override
    protected void onDestroy() {
        if(gps != null) gps.closit();
        loggah("GPSTester.closit()", false);
        super.onDestroy();
    }

    @Override
    public void onUSBGPSLocationChanged(LatLng location) {
        if(location != null) {
            LatLng loc = gps.debugLastParsedLatLng;
            textViewLat.setText(Double.toString(loc.getLatitude()));
            textViewLong.setText(Double.toString(loc.getLongitude()));
            satTime.setText(Double.toString(location.getAltitude()));
        } else {
            //loggah("No GPS.", true);
            textViewLat.setText("FAIL");
            textViewLong.setText("FAIL");
            satTime.setText("FAIL");
        }
    }
}
