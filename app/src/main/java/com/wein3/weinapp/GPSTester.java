package com.wein3.weinapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class GPSTester extends AppCompatActivity {

    private Button getLatLong;
    private TextView textViewLat;
    private TextView textViewLong;
    private TextView satTime;

    private GPS gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        getLatLong = (Button) findViewById(R.id.getLatLong);
        textViewLat = (TextView) findViewById(R.id.textViewLat);
        textViewLong = (TextView) findViewById(R.id.textViewLong);
        satTime = (TextView) findViewById(R.id.satTime);

        gps = new GPS(this);

        getLatLong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                Intent intent = getIntent();
                device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                */
                if(!gps.hasDevice()) {
                    loggah("No device found upon button usage.", true);
                    return;
                }

                LatLng res = gps.getLastKnownLatLng();

                textViewLat.setText(Double.toString(res.getLatitude()));
                textViewLong.setText(Double.toString(res.getLongitude()));
                satTime.setText(Double.toString(res.getAltitude()));
            }
        });
    }

    public void loggah(String mess, boolean toast) {
        if(gps != null) gps.loggah(mess, toast);
    }

    @Override
    protected void onDestroy() {
        if(gps != null) gps.onDestroy();
        super.onDestroy();
    }
}
