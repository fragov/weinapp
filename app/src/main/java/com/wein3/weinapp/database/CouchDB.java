package com.wein3.weinapp.database;

import android.app.Application;
import android.util.Log;

import com.couchbase.lite.*;
import com.couchbase.lite.android.*;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CouchDB implements DatabaseSubject {

    private final String TAG = "CouchDB";
    private final String databaseName = "wein-couchdb";
    private final String couchDbUrl = "https://couchdb-ce198d.smileupps.com/";
    private final String userName = "admin";
    private final String userPw = "8da9af572a0c";
    private List<DatabaseObserver> myObservers;
    private List<Document> documents;
    private Manager manager = null;
    private Database database = null;

    private static CouchDB INSTANCE = null;

    private CouchDB(Application application) {
        myObservers = new ArrayList<>();
        documents = new ArrayList<>();

        try {
            manager = new Manager(new AndroidContext(application.getApplicationContext()),
                    Manager.DEFAULT_OPTIONS);
            DatabaseOptions options = new DatabaseOptions();
            options.setCreate(true);
            database = manager.openDatabase(databaseName, options);
        } catch (IOException e) {
            Log.d(TAG, "Cannot create database file on device", e);
        } catch (CouchbaseLiteException e) {
            Log.d(TAG, "Couchbase error", e);
        }

        if(database != null) {
            Query allDocumentsQuery = database.createAllDocumentsQuery();
            QueryEnumerator queryResult = null;
            try {
                queryResult = allDocumentsQuery.run();
            } catch (CouchbaseLiteException e) {
                Log.d(TAG, "Couchbase error", e);
            }
            for (Iterator<QueryRow> it = queryResult; it.hasNext(); ) {
                Document document = it.next().getDocument();
                documents.add(document);
                this.notifyObserversDocumentsAdded(document);
            }
            database.addChangeListener(new com.couchbase.lite.Database.ChangeListener() {
                public void changed(com.couchbase.lite.Database.ChangeEvent event) {
                    for (int i = 0; i < event.getChanges().size(); i++) {
                        Document retrievedDocument = database.getDocument(event.getChanges().get(i).getDocumentId());
                        if (retrievedDocument.isDeleted()) {
                            documents.remove(retrievedDocument);
                            notifyObserversDocumentsRemoved(retrievedDocument);
                        } else {
                            documents.add(retrievedDocument);
                            notifyObserversDocumentsAdded(retrievedDocument);
                        }
                    }
                }
            });
            URL syncGatewayURL = null;
            try {
                syncGatewayURL = new URL(couchDbUrl + databaseName);
            } catch (MalformedURLException e) {
                Log.d(TAG, "Malformed URL", e);
            }

            Replication mPush = database.createPushReplication(syncGatewayURL);
            mPush.setContinuous(true);
            mPush.setAuthenticator(AuthenticatorFactory.createBasicAuthenticator(userName, userPw));
            mPush.start();

            Replication mPull = database.createPullReplication(syncGatewayURL);
            mPull.setAuthenticator(AuthenticatorFactory.createBasicAuthenticator(userName, userPw));
            mPull.setContinuous(true);
            mPull.start();
        }
    }

    public static CouchDB getInstance(Application application) {
        if(INSTANCE == null) {
            INSTANCE = new CouchDB(application);
        }
        return INSTANCE;
    }

    public void insert(Map<String, Object> documentContent) {
        Document document = database.createDocument();
        try {
            document.putProperties(documentContent);
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot write document to database", e);
        }
    }

    public void update(Document document, Map<String, Object> documentContent) {
        try {
            document.putProperties(documentContent);
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot write document to database", e);
        }
    }

    public void remove(Document document) {
        try {
            document.delete();
        } catch (Exception e) {
            Log.e(TAG, "Cannot remove document from database", e);
        }
    }

    @Override
    public void registerObserver(DatabaseObserver databaseObserver) {
        if(!myObservers.contains(databaseObserver)) {
            myObservers.add(databaseObserver);
            databaseObserver.onRegister(documents);
        }
    }

    @Override
    public void removeObserver(DatabaseObserver databaseObserver) {
        if(myObservers.contains(databaseObserver)) {
            myObservers.remove(databaseObserver);
        }
    }

    @Override
    public void notifyObserversDocumentsAdded(Document document) {
        for (DatabaseObserver observer: myObservers) {
            observer.onDocumentAdded(document);
        }
    }

    @Override
    public void notifyObserversDocumentsRemoved(Document document) {
        for (DatabaseObserver observer: myObservers) {
            observer.onDocumentRemoved(document);
        }
    }
}
