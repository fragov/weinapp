package com.wein3.weinapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class GPS extends AppCompatActivity {

    private Button getLatLong;
    private TextView textViewLat;
    private TextView textViewLong;

    private LatLng lastKnownLatLng;
    private boolean hasPos = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        getLatLong = (Button) findViewById(R.id.getLatLong);
        textViewLat = (TextView) findViewById(R.id.textViewLat);
        textViewLong = (TextView) findViewById(R.id.textViewLong);

        getLatLong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LatLng loc = GPS.this.getLastKnownLatLng();
                String s = "Not yet implemented";
                getLatLong.setText(s);
                textViewLat.setText(s);
                textViewLong.setText(s);
                /*
                //Please add nullptr check
                textViewLat.setText(Double.toString(loc.getLatitude()));
                textViewLong.setText(Double.toString(loc.getLongitude()));
                */
            }
        });
    }

    public LatLng getLastKnownLatLng() {
        return this.lastKnownLatLng;
    }
}
