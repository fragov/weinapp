package com.wein3.weinapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.couchbase.lite.Document;
import com.couchbase.lite.internal.database.util.TextUtils;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.wein3.weinapp.database.CouchDB;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class DBContent extends AppCompatActivity {
    private final String TAG = "DBContent";
    private ListView listView;
    private DBAdapter adapter;

    /**
     * Database handler to store polygons.
     */
    private CouchDB mainDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dbcontent);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar2);
        toolbar.setNavigationIcon(R.drawable.ic_arrowback);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Your Polygons");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // create or open main database
        mainDatabase = CouchDB.getInstance(getApplication());


        this.listView = (ListView) findViewById(R.id.listView);
        this.adapter = new DBAdapter(this, mainDatabase);
        this.listView.setAdapter(this.adapter);

        //click icon on list and show the polygon in map.java
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> a, android.view.View v, int position, long id) {
                final Document listItemDocument = (Document) listView.getItemAtPosition(position);
                Double lat = 0.0;
                Double lng = 0.0;
                try {
                    JSONArray features = new JSONObject(listItemDocument.getProperty("geometry")
                            .toString()).getJSONArray("features");
                    JSONObject feature = features.getJSONObject(0);
                    JSONObject geometry = feature.getJSONObject("geometry");
                    if (geometry != null) {
                        // Get the Coordinates
                        JSONArray coords = geometry.getJSONArray("coordinates");
                        JSONArray coordinates = coords.getJSONArray(0);
                        for (int lc = 0; lc < coordinates.length(); lc++) {
                            JSONArray coord = coordinates.getJSONArray(lc);
                            lat += coord.getDouble(1);
                            lng += coord.getDouble(0);
                        }
                        lat /= coordinates.length();
                        lng /= coordinates.length();
                    }
                } catch (Exception exception) {
                    Log.e(TAG, "Exception Loading GeoJSON: " + exception.toString());
                }

                Intent output = new Intent();
                output.putExtra("lat", lat);
                output.putExtra("lng", lng);
                setResult(RESULT_OK, output);
                finish();
            }
        });

        //clicklistener for long clicks (edit or delete document)
        this.listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> a, android.view.View v, int position, long id) {
                final Document listItemDocument = (Document) listView.getItemAtPosition(position);
                final int listItemIndex = position;
                final CharSequence[] items = {"Delete Item"};
                AlertDialog.Builder builder = new AlertDialog.Builder(DBContent.this);
                builder.setTitle("Perform Action");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if(item == 0) {
                            try {
                                listItemDocument.delete();
                            } catch (Exception e) {
                                Log.e(TAG, "An error happened", e);
                            }
                        }
                    }
                });
                AlertDialog alert = builder.show();
                return true;
            }
        });

    }
}
