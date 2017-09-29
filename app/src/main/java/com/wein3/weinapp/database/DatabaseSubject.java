package com.wein3.weinapp.database;

import com.couchbase.lite.Document;

/**
 * interface for database subject
 */
public interface DatabaseSubject {
    void registerObserver(DatabaseObserver databaseObserver);
    void removeObserver(DatabaseObserver databaseObserver);
    void notifyObserversDocumentsAdded(Document document);
    void notifyObserversDocumentsRemoved(Document document);
}
