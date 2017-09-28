package com.wein3.weinapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import com.wein3.weinapp.database.CouchDB;

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

        // create or open main database
        mainDatabase = CouchDB.getInstance(getApplication());


        this.listView = (ListView) findViewById(R.id.list);
        this.adapter = new DBAdapter(this, mainDatabase);
        this.listView.setAdapter(this.adapter);



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
