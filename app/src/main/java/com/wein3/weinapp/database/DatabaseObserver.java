package com.wein3.weinapp.database;

import com.couchbase.lite.Document;

import java.util.List;

public interface DatabaseObserver {
    void onRegister(List<Document> documents);
    void onDocumentAdded(Document document);
    void onDocumentRemoved(Document document);
}
