package com.wein3.weinapp.database;

import com.couchbase.lite.Document;

/**
 * Database Subject Interface
 */
public interface DatabaseSubject {
    /**
     * Register an observer
     * @param databaseObserver Database observer
     */
    void registerObserver(DatabaseObserver databaseObserver);

    /**
     * Remove observer
     * @param databaseObserver Database observer that should be removed
     */
    void removeObserver(DatabaseObserver databaseObserver);

    /**
     * Notify observers, that new document are added
     * @param document Document added to database
     */
    void notifyObserversDocumentsAdded(Document document);

    /**
     * Notify observers, that document was removed from database
     * @param document Document that was removed from database
     */
    void notifyObserversDocumentsRemoved(Document document);
}
