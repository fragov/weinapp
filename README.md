TrackingService:
Der Trackingservice wird dann gestartet, wenn der Benutzer auf den "Record" button klickt. Dieser heißt intern "fabPath" genannt und wird in der OnClick methode in der Map Klasse aufgerufen. Der Service läuft die ganze Zeit im Hintergrund und er hat eine innere Klasse, die die ganze Zeit auf GPS daten hört. Jedes mal, wenn eine neue Koordinate reinkommt, wird diese an die Map Klasse über einen Broadcast geschickt. In der Map wird jedes mal eine Koordinate abgefangen und zur Polylineoptions hinzugefügt. Aus diesen Options wird dann die gelaufene Strecke gezeichnet.
Durch den Trackingservice wird garantiert, dass die App, auch wenn sie minimiert wird (wenn z.B. ein Anruf kommmt oder man eine andere Anwendung startet), weiter den Weg mitverfolgt. Wenn der Service keine Koordinaten an die Map Activity senden kann, weil diese z.B. auf Grund des mangelnden Arbeitsspeichers zerstört wurde, schreibt er die Koordinaten in die interne Datenbank. Wenn die Map Activity gestartet wird, liest sie alle Koordinaten aus der Selben Datenbank aus und man hat trotzdem den gesamten Weg aufgezeichnet.
Beim erneuten Klicken auf den selben Button wird der Trackingservice beendet und man kann dann entweder die Strecke als ein Polygon in die Datenbank speichern oder den gelaufenen Weg stornieren.


OfflineMaps:
