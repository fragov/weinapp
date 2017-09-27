package com.wein3.weinapp;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.wein3.weinapp.database.Database;
import com.wein3.weinapp.database.Sqlite;

import java.util.ArrayList;
import java.util.HashMap;

import static java.sql.Types.NULL;

public class DBContent extends AppCompatActivity {

    ListView list;
    ArrayList arrayList;
    ArrayAdapter arrayAdapter;
    HashMap <String, String> map;
    Database sql;

    private SQLiteDatabase sqLiteDatabase;
    private static String databaseFile = "sqlite.db";
    private static String table = "areas";

    /**
     * create Activity, read data from database and display it in listview
     *
     * @param savedInstanceState current state of app
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dbcontent);

        sql = new Sqlite();
        sql.init(this);

        list = (ListView) findViewById(R.id.list);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String selectedFromList = list.getItemAtPosition(position).toString();
                // get a reference to the already created main layout
                LinearLayout mainLayout = (LinearLayout) findViewById(R.id.layout_dbcontent);

                // inflate the layout of the popup window
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.popup_window, null);

                // create the popup window
                int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                boolean focusable = true; // lets taps outside the popup also dismiss it
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

                //set text of popup window
                ((TextView) popupWindow.getContentView().findViewById(R.id.popup)).setText(getAreaString(selectedFromList));
                // show the popup window
                popupWindow.showAtLocation(mainLayout, Gravity.CENTER, 0, 0);

                // dismiss the popup window when touched
                popupView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        popupWindow.dismiss();
                        return true;
                    }
                });
                Button showinMap = (Button)findViewById(R.id.showRectangle);
                Button cancel = (Button) findViewById(R.id.cancel);
                popupView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switch (v.getId()){
                            case R.id.showRectangle:
                                //Map.moveCamera();
                                //Intent intent = new Intent(getBaseContext(), Map.class);
                                //getBaseContext().startActivity(intent);
                                break;
                            case R.id.cancel:
                                //TODO
                                break;
                        }
                    }
                });
            }
        });
        arrayList = new ArrayList<String>();

        arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, arrayList){
            /**
             * overwrite view-Method in order to set textcolor (black)
             *
             * @param position of list item
             * @param convertView view
             * @param parent viewgroup
             *
             * @return updated textview
             */
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                //View view = super.getView(position, convertView, parent);
                TextView textView = (TextView)super.getView(position, convertView, parent);
                textView.setTextColor(Color.BLACK);
                return textView;
            }
        };


        for (String id : sql.getListOfAreas().keySet()) {
            arrayList.add(sql.getListOfAreas().get(id));
        }
        list.setAdapter(arrayAdapter);
    }

    public String getAreaString (String id){
        /*Area area = sql.getAreaById(id);
        String gewann = "Gewann: ";
        if (area.getGewann() != null){
            gewann += area.getGewann();
        } else{
            gewann += "";
        }*/
        //String ids = "ID: " + (area.getId() != null ? area.getId() : "");
        //String gewann = "Gewann: " + (area.getGewann() != null ? area.getGewann() : "");
        Area area = null;
        String ids = "ID: ";
        Cursor row = sqLiteDatabase.rawQuery("SELECT * FROM " + table + " WHERE id = '" + id + "'", null);
        if (row.moveToFirst()) {
            ids += row.getString(0) != null ? row.getString(0) : "";
            /*area.setRev(row.getString(1) != null ? row.getString(1) : "");
            area.setDescription(row.getString(2) != null ? row.getString(2) : "");
            area.setGewann(row.getString(3) != null ? row.getString(3) : "");
            area.setSize(row.getString(4) != null ? Double.parseDouble(row.getString(4)) : 0.0);
            area.setFeatureCollection(row.getString(5) != null ? row.getString(5) : "");
            area.setGrowingRegion(row.getString(6) != null ? row.getString(6) : "");
            area.setCurrentUsage(row.getString(7) != null ? row.getString(7) : "");
            area.setCurrentLandUsage(row.getString(8) != null ? row.getString(8) : "");
            area.setCategory(row.getString(9) != null ? row.getString(9) : "");
            area.setChannelWidth(row.getString(10) != null ? Integer.parseInt(row.getString(10)) : 0);
            area.setWineRowsCount(row.getString(11) != null ? Integer.parseInt(row.getString(11)) : 0);
            area.setWineRowsLength(row.getString(12) != null ? Integer.parseInt(row.getString(12)) : 0);
            area.setVinesCount(row.getString(13) != null ? Integer.parseInt(row.getString(13)) : 0);
            area.setUsefullLife(row.getString(14) != null ? Integer.parseInt(row.getString(14)) : 0);
            area.setBusiness(row.getString(15) != null ? row.getString(15) : "");*/

            row.close();
        }
        row.close();
        return ids;
    }
}
