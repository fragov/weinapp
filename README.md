TrackingService:
Der Trackingservice wird dann gestartet, wenn der Benutzer auf den "Record" button klickt. Dieser heißt intern "fabPath" genannt und wird in der OnClick methode in der Map Klasse aufgerufen. Der Service läuft die ganze Zeit im Hintergrund und er hat eine innere Klasse, die die ganze Zeit auf GPS daten hört. Jedes mal, wenn eine neue Koordinate reinkommt, wird diese an die Map Klasse über einen Broadcast geschickt. In der Map wird jedes mal eine Koordinate abgefangen und zur Polylineoptions hinzugefügt. Aus diesen Options wird dann die gelaufene Strecke gezeichnet.
Durch den Trackingservice wird garantiert, dass die App, auch wenn sie minimiert wird (wenn z.B. ein Anruf kommmt oder man eine andere Anwendung startet), weiter den Weg mitverfolgt. Wenn der Service keine Koordinaten an die Map Activity senden kann, weil diese z.B. auf Grund des mangelnden Arbeitsspeichers zerstört wurde, schreibt er die Koordinaten in die interne Datenbank. Wenn die Map Activity gestartet wird, liest sie alle Koordinaten aus der Selben Datenbank aus und man hat trotzdem den gesamten Weg aufgezeichnet.
Beim erneuten Klicken auf den selben Button wird der Trackingservice beendet und man kann dann entweder die Strecke als ein Polygon in die Datenbank speichern oder den gelaufenen Weg stornieren.

Im TrackingService sind als innere Klasse ein TrackingLocationListener und
ein TrackingGpsDataReceiver implementiert. Der TrackingLocationListener
ist ein LocationListener, der auf Signale des eingebauten
GPS-Providers hört. Der TrackingGpsDataReceiver ist eine selbst implementierte
Klasse, die Standortdaten vom externen GPS-Provider (in diesem Fall das
Gerät von Navilock) verarbeitet. Beim Starten des Services wird dem Intent
eine Boolean-Flag mitgegeben, ob der externe GPS-Provider verwendet werden
soll oder nicht.

OfflineMaps:


HelperDatabase

Wir nutzen eine lokale SQLite-Datenbank, die HelperDatabase, um den aktuell
verfolgten Weg zwischenzuspeichern. Diese Datenbank ist unabhängig von
der Couchbase DB, die zum Abspeichern der fertigen Polygone benutzt wird.
Dementsprechend gibt es auch zwei verschiedene Datenbank-Handler-Klassen
für Couchbase und SQLite. Da Mapbox relativ viele Ressourcen verbraucht,
kommt es gerade bei schwachen Handys häufig vor, dass beim
Activity-Wechsel innerhalb der App oder beim Wechsel von Portrait-
in den Landscape-Modus die alte Activity nicht pausiert oder gestoppt
(onPause() oder onStop()), sondern komplett zerstört (onDestroy()) wird.
Das würde natürlich den aktuell getrackten Weg auch zerstören.
Daher speichern wir die einzelnen Koordinaten des Trackings in eine
einfache SQLite-Tabelle mit zwei Spalten (Längengrad und Breitengrad). Die
HelperDatabase-Klasse stellt dafür die notwendigen Methoden bereit. Das
erwähnte Abspeichern der Koordinaten passiert an zwei Stellen im Code. Einmal
im TrackingBroadcastReceiver, einer inneren Klasse der Map Activity, 
direkt nachdem sie den Standort vom TrackingService empfangen hat, und
ein anderes Mal direkt im TrackingService selbst. Wenn nämlich der
TrackingService feststellt, dass er einen Standort nicht als Broadcast
verschicken kann, speichert er ihn in die lokale Datenbank, um die
Daten nicht zu verlieren. In der onCreate()-Methode der Map Activity
werden die Koordinaten dann wieder ausgelesen und in einer Polyline
gemalt.

TrackingBroadcastReceiver

Ein selbst implementierter BroadcastReceiver, der die Standortdaten,
die vom TrackingService gesendet werden, empfängt. Diese Standortinformationen
werden dann auf die Mapbox-Karte gezeichnet (mithilfe einer Polyline und den
zugehörigen PolylineOptions) und gleichzeitig in die
HelperDatabase geschrieben. Des Weiteren wird der aktuelle Standort zur
leichteren Handhabung innerhalb der Map Activity in einer globalen
Variablen gespeichert. Das hat außerdem den Vorteil, dass man über eine
"== null"-Abfrage ganz leicht ermitteln kann, ob bereits ein Standort
eingegangen ist oder nicht. Insbesondere bei der Initialisierung der
Polyline ist es nämlich notwendig, diese mit einer ersten Koordinate zu
initialisieren, da sonst das Zeichnen der Linie (aus welchem Grund auch immer)
nicht funktioniert.
