package com.wein3.weinapp.database;

import com.couchbase.lite.Document;

import java.util.List;

/**
 * Database Observer Interface
 */
public interface DatabaseObserver {
    /**
     * Send all documents to new registered observer
     * @param documents List of documents
     */
    void onRegister(List<Document> documents);

    /**
     * Notify observer on new document
     * @param document New document
     */
    void onDocumentAdded(Document document);

    /**
     * Notify observer on deleted document
     * @param document Deleted document
     */
    void onDocumentRemoved(Document document);
}
