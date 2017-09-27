package com.wein3.weinapp;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.wein3.weinapp.database.Database;
import com.wein3.weinapp.database.Sqlite;

import java.util.ArrayList;
import java.util.HashMap;

public class DBContent extends AppCompatActivity {

    ListView list;
    ArrayList arrayList;
    ArrayAdapter arrayAdapter;
    HashMap<String, String> map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dbcontent);

        Database sql = new Sqlite();
        sql.init(getApplication());

        list = (ListView) findViewById(R.id.list);
        arrayList = new ArrayList<String>();
        //map = sql.getListOfAreas();

        //arrayList.add(map);
        arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, arrayList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                //View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(Color.BLACK);
                return textView;
            }
        };

        for (String id : sql.getListOfAreas().keySet()) {
            //Area area = sql.getAreaById(id);
            arrayList.add(sql.getListOfAreas().get(id));
        }
        list.setAdapter(arrayAdapter);
    }
}
