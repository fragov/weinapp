Map
===

Map ist die Haupt-Activity der App. Sie enthält die Mapbox-Karte und stellt den Großteil des User-Interfaces zur Verfügung. Hauptsächlich besteht dieses User Interface aus zwei FloatingActionButtons. Mit dem einen startet / stoppt man den TrackingService, der losgelöst von der Activity den aktuellen Standort des internen oder externen GPS-Providers ermittelt und per Broadcast an die Activity sendet. Mit dem anderen zentriert man den Kartenausschnitt auf den aktuellen Standort. Die Klasse implementiert mehrere Interfaces, nämlich:

- View.OnClickListener: reagiert auf Touch-Events der FloatingActionButtons.

- NavigationView.OnNavigationItemSelectedListener: behandelt Touch Events innerhalb der NavigationView.

- MapboxMap.OnCameraMoveListener: wird aufgerufen, wenn die Kameraperspektive bewegt wird. Im Moment wird in dieser Methode nur der aktuelle Zoom-Faktor der Kamera ausgelesen und als globale Variable gesetzt. Damit ist es möglich, beim Activity-Wechsel wieder auf den zuletzt eingestellten Zoom zuzugreifen und die Kamera entsprechend auszurichten.

- DatabaseObserver: Schnittstelle zwischen der Map Activity und der Couchbase-Datenbank.

Sollte bei Nutzung des internen GPS der entsprechende Provider ausgeschaltet sein, wird beim Start der Map Activity entsprechend nachgefragt. Über onActivityResult() wird das Ergebnis dieses Aufrufs registriert und die Kamerasicht entsprechend aktualisiert. Sollten die notwendigen GPS-Permissions fehlen sein, wird mit einem ähnlichen Dialog nachgefragt.

Die boolesche Variable pathTrackingEnabled gibt an, ob der aktuelle Pfad mitgeschnitten werden soll oder nicht. Diese wird durch Druck auf den Record-Button entsprechend gesetzt. Dabei wird entweder startNewRoute() oder stopCurrentRoute() aufgerufen. In startNewRoute() werden Initialisierungen durchgeführt und der TrackingService gestartet. In stopCurrentRoute() wird entsprechend die Aufnahme gestoppt und das Ergebnis als Polygon in die Couchbase-Datenbank geschrieben. Während der Aufnahme wird eine entsprechende Notification angezeigt und verhindert das die lokale SQLite-Datenbank (HelperDatabase) geschlossen wird.

Die Methoden saveStatus() und loadStatus() schreiben bzw. lesen die pathTrackingEnabled-Variable genauso wie den zuletzt gespeicherten Zoom-Faktor in bzw. aus den SharedPreferences. Der Hintergrund ist, dass es sein kann, dass die App während des Trackings vom System zerstört wird. Beim erneuten Erstellen muss dem System irgendwie mitgeteilt werden, ob der TrackingService zur Zeit läuft oder nicht. Das passiert mithilfe der SharedPreferences.

Mit der Flag useExternalGpsDevice kann spezifiziert werden, ob ein externes GPS-Gerät verwendet werden soll oder nicht. Je nachdem, wie diese Boolean-Flag gesetzt ist, beeinflusst vor allem die onCreate()-Methode in der Map Activity. Wird das interne GPS verwendet, funktioniert das Darstellen des aktuellen Standorts mit den Mapbox-spezifischen Funktionen (hier: setMyLocationEnabled(true)). Wird ein externer GPS-Provider benutzt, kann man diese Funktion nicht benutzen, sondern muss die Darstellung des eigenen Standorts selbst implementieren. Ist das GPS-Tracking aktiviert, können die entsprechenden Koordinaten unabhängig vom GPS-Provider bekommen werden, denn sie werden einfach als Broadcast vom TrackingService geschickt. Die TrackingService-Klasse implementiert dazu eigene LocationListener für jeden GPS-Provider und sendet immer dann einen Broadcast, wenn der aktuell gewählte Listener ein Update erhält. Dementsprechend wird die useExternalGpsDevice-Flag dem Intent zum Starten des TrackingService mitgegeben. Momentan muss diese Flag explizit im Code gesetzt werden. Es wäre aber leicht denkbar, sie auch dynamisch in den Einstellungen zu setzen.


TrackingBroadcastReceiver
=========================

Ein selbst implementierter BroadcastReceiver in der Map-Klasse, der die Standortdaten, die vom TrackingService gesendet werden, empfängt. Diese Standortinformationen werden dann auf die Mapbox-Karte gezeichnet (mithilfe einer Polyline und den zugehörigen PolylineOptions) und gleichzeitig in die HelperDatabase geschrieben. Während das Tracking aktiviert ist, wird der aktuelle Standort in eine globale Variable innerhalb der Map Activity geschrieben und nach dem Beenden der Aufzeichnung wieder auf null gesetzt. Das hat den Vorteil, dass der aktuelle Standort leichter verfügbar ist. Hauptsächlich dient dieses Vorgehen aber dazu, über eine "== null"-Abfrage ermitteln zu können, ob während des laufenden Trackings bereits ein Standort empfangen wurde oder nicht. Auf diese Weise kann man die erste Koordinate des aufgezeichneten Weges ermitteln. Insbesondere bei einer Polyline ist es nämlich notwendig, diese mit einer ersten Koordinate zu initialisieren, da sonst das Zeichnen der Linie (aus welchem Grund auch immer) nicht funktioniert.


TrackingService
===============

Der TrackingService wird gestartet, wenn der Benutzer auf den Record-Button klickt. Dieser heißt intern "fabPath". Beim erneuten Klicken auf diesen Button wird der TrackingService beendet, Man kann dann entweder die Strecke als ein Polygon in die Datenbank speichern oder den gelaufenen Weg stornieren.

Der Service läuft die ganze Zeit im Hintergrund und hat mehrere innere Klassen, die die ganze Zeit auf GPS-Daten hören. Jedes Mal, wenn eine neue Koordinate zur Verfügung steht, wird diese über einen Broadcast an die Map-Klasse geschickt. Dort wird die Koordinate empfangen und zur polylineOptions hinzugefügt. Aus diesen polylineOptions wird dann die gelaufene Strecke gezeichnet.

Durch den TrackingService wird garantiert, dass die App, auch wenn sie minimiert ist (z.B. wenn ein Anruf kommt oder eine andere App gestartet wird), weiter den Weg mitverfolgt. Wenn der Service keine Koordinaten an die Map Activity senden kann, weil diese z.B. aufgrund des mangelnden Arbeitsspeichers zerstört wurde, schreibt er die Koordinaten in die interne Datenbank. Wenn die Map Activity gestartet wird, liest sie alle Koordinaten aus dieser lokalen Datenbank aus.

Im TrackingService sind als innere Klasse ein TrackingLocationListener und ein TrackingGpsDataReceiver implementiert. Der TrackingLocationListener ist ein LocationListener, der auf Signale des eingebauten GPS-Providers hört. Der TrackingGpsDataReceiver ist eine selbst implementierte Klasse, die Standortdaten vom externen GPS-Provider (in diesem Fall das Gerät von Navilock) verarbeitet. Beim Starten des Services wird dem Intent eine Boolean-Flag mitgegeben, ob der externe GPS-Provider verwendet werden soll oder nicht.


Offline-Maps
============

Das Management der Offline-Karten, inklusive das Herunterladen und eventuelles Löschen, ist ebenfalls in der Map Activity implementiert. Beim Klicken auf den Menüpunkt "Offline" des "wischbaren" Menüs auf der linken Seite der Map Activity bekommt der Benutzer eine Liste mit seinen gespeicherten Offline-Regionen angezeigt. Er kann eine davon auswählen und die Kamera zu diesem Standort verschieben. Er kann die gewählte Region löschen oder eine weitere Region erstellen. Wenn es noch keine gespeicherten Regionen gibt, wird dem Benutzer vorgeschlagen, eine neue Region zu erstellen. Dazu werden einfach die Eckpunkte des Bildschirms ausgelesen und die Region wird mit dem eingegebenen Namen und diesen Eckpunkten in der downloadRegion()-Methode heruntergeladen. Die von Mapbox gelieferten Methoden erlauben es uns, den Downloadprozess asynchron zu gestalten. Man ist also weiterhin in der Lage, die Karte zu benutzen und den Weg zu tracken. Die Offline-Karten werden automatisch in die interne Datenbank gespeichert. Das Löschen ist ebenfalls mit den mitgelieferten Methoden von Mapbox realisiert. 


HelperDatabase
==============

Wir nutzen eine lokale SQLite-Datenbank, die HelperDatabase, um den aktuell verfolgten Weg zwischenzuspeichern. Diese Datenbank ist unabhängig von der Couchbase DB, die zum Abspeichern der fertigen Polygone benutzt wird. Der Grund dafür ist, das unsere Couchbase-Datenbank über das Internet mit einem Server gekoppelt ist und bei entsprechender Internetverbindung mit dem Server synchronisiert wird. Die SQLite-Datenbank hingegen liegt lokal auf dem Handy und muss keine Daten mit einem Server austauschen, zumal die zwischengespeicherten Einträge in der SQLite-Datenbank nicht mehr benötigt werden, sobald das Polygon mit Couchbase abgespeichert ist. Dementsprechend werden beim Beenden der Aufnahme über die clearTable()-Funktion der HelperDatabase alle Einträge der lokalen Datenbank gelöscht. Aus diesem Grund gibt es auch zwei verschiedene Datenbank-Handler-Klassen, eine für Couchbase und eine für SQLite. Da Mapbox relativ viele Ressourcen verbraucht, kommt es gerade bei schwachen Handys häufig vor, dass beim Activity-Wechsel innerhalb der App oder beim Wechsel von Portrait- in den Landscape-Modus die alte Activity nicht pausiert oder gestoppt (onPause() oder onStop()), sondern komplett zerstört (onDestroy()) wird. Das würde natürlich auch den aktuell getrackten Weg zerstören. Daher speichern wir die einzelnen Koordinaten des Trackings in eine einfache SQLite-Tabelle mit zwei Spalten (Längengrad und Breitengrad). Die HelperDatabase-Klasse stellt dafür die notwendigen Methoden bereit. Das erwähnte Abspeichern der Koordinaten passiert an zwei Stellen im Code. Einmal im TrackingBroadcastReceiver, einer inneren Klasse der Map Activity, und ein anderes Mal direkt im TrackingService selbst. Wenn nämlich der TrackingService feststellt, dass er einen Standort nicht als Broadcast verschicken kann, speichert er ihn in die lokale Datenbank, um die Daten nicht zu verlieren. In der onCreate()-Methode der Map Activity werden die Koordinaten dann wieder ausgelesen und in einer Polyline gemalt.


Couchbase Database
==================

Wir nutzen eine Couchbase Datenbank, um die aufgenommenen Polygone zu speichern. Die Datenbank ist nach dem Observer Pattern implementiert, sodass bei einer Änderung in 
der Datenbank alle entsprechenden Anwendungen (Liste der Polygone, etc) aktualisiert werden.


DBContent
=========

Hier sieht der Benutzer alle gespeicherten Polygone mit den Hauptcharakteristika. Wählt der Nutzer eines der Polygone durch ein kurzes Antippen aus,
so wird er zur Hauptklasse mit der Karte weitergeleitet und sieht das ausgewählte Polygon eingezeichnet in die Karte. Tippt der Nutzer dahingegen 
eines der Polygone in der Liste länger an, so kann er das gespeicherte Polygon entweder löschen oder editieren.


Layout
======

Das Layout orientiert sich am Layout der Kartenanwedung GoogleMaps, sodass ein Benutzer der App eine vertraute Umgebung vorfindet und sich schnell zurechtfindet.
Dafür wird in der Hauptklasse ein DrawerLayout verwendet.

GPS
===

Benutzung der Klasse:
Um die Klasse in Map.java zu benutzen, muss man, wie oben beschrieben, die Flag useExternalGpsDevice auf true setzen und neu kompilieren. Leider geht das noch nicht dynamisch, weil die Klasse noch unfertig (ist ein Prototyp ja sowieso) und provisorisch eingebunden wurde. Mit der Flag auf false kann höchtens die Test-Activity unter dem Menüpunkt "GPS" benutzt werden.
Die USB-Events werden eigentlich automatisch behandelt (falls die Klasse richtig in eine Activity eingebunden wurde). Dann wird eine Verbindung zum Gerät hergestellt und man kann dann später den Polling-Prozess starten um Location-Updates zu bekommen. Aktuell gibt es noch Probleme mit dem Multithreading, also sollte man den Polling-Prozess in der Activity manuell beenden und dann erst die Activity beenden oder das Gerät entfernen.
Genaueres sollte in der Klasse GPSTester.java stehen (bitte auch Kommentare beachten).

Bevor ich auf die Funktionalität der Klasse eingehe, möchte ich auf meinen Einstieg in das Thema eingehen:
Mangels Erfahrung mit dem Thema wurde sehr viel Zeit mit der Recherche und mit Testen (auch mit viel Trial-and-Error) verbracht. Deswegen wird aktuell nur ein Sensor (Navilock NL-650US mit GPS-Chipsatz MediaTek MT3337 und der RS-232 to USB-Schnittstelle Prolific PL-2303HXD) unterstützt. Aber eine Implementierung für andere (hoffentlich genauere) Sensoren mit der PL-2303HXD-Schnittstelle scheint keine riesigen Anpassungen zu erfordern. Bei den technischen Details zum Ansprechen der Schnittstelle habe ich von diesem Treiber:
	https://github.com/sintrb/Android-PL2303HXA/blob/master/Android-PL2303HXA/src/com/sin/android/usb/pl2303hxa/PL2303Driver.java .
Ansonsten habe ich zur Verwendung des USB-Gerätes die entsprechenden Android-Klassen benutzt, wie in diesem Tutorial beschrieben:
	https://developer.android.com/guide/topics/connectivity/usb/host.html#working-d .

Um die Funktionsweise der Klasse zu beschreiben, stelle ich die Probleme bei der Entwicklung und deren Lösungen gegenüber, und zwar in folgender Reihenfolge:
Ansprechen der USB-Schnittstelle,
Lesen der Daten,
Verarbeitung der Daten,
Datenübertragung (z.B. vom TrackingService zur Map)

- Ansprechen der USB-Schnittstelle:
Das größte Problem beim Ansprechen der USB-Schnittstelle war, dass niemand im Team Erfahrung damit hatte. Der Treiber von github hat dort weitergeholfen. Aufgrund weiter bestehender Unwissenheit kann an dieser Stelle nicht mehr erklärt werden.

- Lesen der Daten:
Problem: Pro Sekunde wird nur ein Paket von 6 NMEA-Sätzen vom Gerät bereitgestellt. Das heißt, der GGA-Datensatz, den man benötigt, wird nur einmal pro Sekunde gelesen. Bei GPS-Empfang ist das kein Problem, da alle Felder des Satzes besetzt sind. Ohne GPS-Empfang sind die Felder jedoch leer. D.h. es passen mehr Sätze in den Buffer, aber es dauert deswegen auch länger, bis dieser vollgeschrieben ist. Das führt zu einer längeren Lesezeit.
Bei einem festen Polling-Intervall kann das dazu führen, dass die Lesezeit das Intervall überschreitet. Dadurch entstehen mehre Threads gleichzeitig, was zu einem Slowdown oder sogar einem Absturz führt.
Lösung: Multithreading (damit die lange Lesedauer im Hintergrund ausgeführt wird) und Verzichten auf ein festes Polling-Intervall...

- Verarbeitung der Daten:
Aus dem vollen Buffer bekommt man dann einen String. Mit den String-Methoden in Java kann man diesen ganz einfach zerlegen und nach einem GGA-Datensatz suchen. mit split(",") kommt man dann in diesem Satz an die einzelnen Einträge. Praktischerweise sind dann dieselben Werte immer an derselben Stelle im Array.
Problem: Das umwandeln der Daten in double-Werte ist etwas problematisch. Man kann die Strings mit Breiten- und Längengrad nicht einfach parsen. Zwar bekäme man dann auch double-Werte, allerdings sind diese falsch. Ich habe keine Ahnung, warum der Gerätehersteller entschieden hat, die werte als Dezimalzahlen weiterzugeben, aber eigentlich liegen die Rohdaten im Grad-Minute-Sekunden-Format vor.
Lösung: Man zerlegt einfach die Rohdaten in einzelne Werte für Minuten und Sekunden und rechnet diese dann in Anteile eines Grades um, um eine valide Dezimalzahl zu erzeugen, die von Mapbox benötigt wird.

- Datenübertragung:
Zur Datenübertragung registriert man einfach einen GPSDataReceiver (das ist auch ein Interface, das die Klasse, die Location-Updates erhalten will, implementieren soll) in der GPS-Klasse. Dann ruft das GPS bei einem Update die implementierte Methode aus dem Interface auf.
Problematisch wird aber die Nutzung der Klasse in mehreren Klassen mit verschiedenen Contexts: Wir bekommen die Daten vom GPS in einem Service, wollen aber schon vorher in der Activity das externe GPS initialisieren und auch nachher Daten vom Service zur Activity bekommen. Man kann aber leider keine Referenz zur GPS-Klasse mit einem Intent an den TrackingService weitergeben.
Lösung:
Die GPS-Klasse als Singleton verwenden. Wir können das externe GPS sowieso nur einmal nutzen, weil wir das nur einmal haben. Durch den Singleton muss man aber einige Fälle abfangen, die durch ein einfaches Zerstören der Klasse nicht aufgetreten wären. Deswegen sieht der Code jetzt etwas unübersichtlich aus. Mit mehr Zeit hätte man da sicher noch mehr Struktur reinbekommen...
