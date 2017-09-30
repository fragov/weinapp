package com.wein3.weinapp;

import android.content.Context;
import android.view.LayoutInflater;
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

    /**
     * constructor
     *
     * @param context context
     * @param mainDatabase CouchDatabase
     */
    public DBAdapter(Context context, CouchDB mainDatabase) {
        this.context = context;
        this.mainDatabase = mainDatabase;
        mainDatabase.registerObserver(this);
    }

    /**
     * number of documents in database
     *
     * @return count
     */
    @Override
    public int getCount() {
        return this.documents.size();
    }

    /**
     * retrieve item indicated by i
     *
     * @param i item
     * @return Object saved at this position
     */
    @Override
    public Object getItem(int i) {
        return this.documents.get(i);
    }

    /**
     * get id of item i
     *
     * @param i item
     * @return id of item i
     */
    @Override
    public long getItemId(int i) {
        return 0;
    }

    /**
     * get index position of object with specific id
     *
     * @param id Object's id
     * @return index
     */
    public int getIndexOf(String id) {
        for(int i = 0; i < this.documents.size(); i++) {
            if(this.documents.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * set text of textviews in listview
     *
     * @param i position of item
     * @param view View
     * @param viewGroup ViewGroup
     * @return view (listView)
     */
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_item, viewGroup, false);
        TextView textView = (TextView) rowView.findViewById(R.id.title);
        Document currentDocument;
        try {
            if(view == null) {
                textView.setPadding(10, 25, 10, 25);
            }
            currentDocument = this.documents.get(i);
            textView.setText(String.valueOf(currentDocument.getProperty("description")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        TextView desc = (TextView) rowView.findViewById(R.id.desc);

        try {
            if(view == null) {
                desc.setPadding(10, 25, 10, 25);
            }
            currentDocument = this.documents.get(i);
            String s = String.valueOf((currentDocument.getProperty("gewann")));
            String r = String.valueOf((currentDocument.getProperty("size")));
            String w = "Gewann: " + s + " | " + "Größe: " + r;
            desc.setText(w);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rowView;
    }

    /**
     * Remove document from database
     * @param document Document that should be removed
     */
    public void removeDocument(Document document) {
        mainDatabase.remove(document);
    }

    /**
     * register document in Database
     *
     * @param documents Document
     */
    @Override
    public void onRegister(List<Document> documents) {
        this.documents = documents;
        this.notifyDataSetChanged();
    }

    /**
     * add document to Database
     *
     * @param document Document
     */
    @Override
    public void onDocumentAdded(Document document) {
        this.documents.add(document);
        this.notifyDataSetChanged();
    }

    /**
     * remove Document from Database
     *
     * @param document Document
     */
    @Override
    public void onDocumentRemoved(Document document) {
        this.documents.remove(document);
        this.notifyDataSetChanged();
    }
}
