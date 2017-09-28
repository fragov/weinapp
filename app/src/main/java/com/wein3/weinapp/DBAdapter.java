package com.wein3.weinapp;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.couchbase.lite.Document;
import com.wein3.weinapp.database.CouchDB;
import com.wein3.weinapp.database.DatabaseObserver;

import java.util.List;

public class DBAdapter extends BaseAdapter implements DatabaseObserver {
    private List<Document> documents;
    private Context context;
    private CouchDB mainDatabase;

    public DBAdapter(Context context, CouchDB mainDatabase) {
        this.context = context;
        this.mainDatabase = mainDatabase;
        mainDatabase.registerObserver(this);
    }

    @Override
    public int getCount() {
        return this.documents.size();
    }

    @Override
    public Object getItem(int i) {
        return this.documents.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    public int getIndexOf(String id) {
        for(int i = 0; i < this.documents.size(); i++) {
            if(this.documents.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TextView textView = null;
        Document currentDocument;
        try {
            if(view == null) {
                textView = new TextView(this.context);
                textView.setPadding(10, 25, 10, 25);
            } else {
                textView = (TextView) view;
            }
            currentDocument = this.documents.get(i);
            textView.setText(String.valueOf(currentDocument.getProperty("description")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textView;
    }

    @Override
    public void onRegister(List<Document> documents) {
        this.documents = documents;
        this.notifyDataSetChanged();
    }

    @Override
    public void onDocumentAdded(Document document) {
        this.documents.add(document);
        this.notifyDataSetChanged();
    }

    @Override
    public void onDocumentRemoved(Document document) {
        this.documents.remove(document);
        this.notifyDataSetChanged();
    }
}
