# MiniERP

Kleines ERP mit Java 17, SQLite und einem React-/TypeScript-Frontend. Das Backend behält seine Model-, DAO- und Service-Schichten; eine HTTP-Schicht verbindet die Services mit React.

## Lokal starten

Voraussetzungen: JDK 17 oder neuer, Maven, Node.js 22 und pnpm 11.25.0. Alternativ kann das Backend in IntelliJ über `com.minierp.api.ApiServer` gestartet werden.

Backend im Projektverzeichnis:

```sh
mvn compile exec:java
```

Frontend in einem zweiten Terminal:

```sh
cd frontend
pnpm install
pnpm dev
```

Die Oberfläche läuft standardmäßig auf `http://localhost:5173`, die API auf `http://127.0.0.1:8080/api`. Für eine andere API-Adresse `frontend/.env.example` nach `frontend/.env.local` kopieren und `VITE_API_BASE_URL` anpassen. Die Vite-Konsole zeigt den tatsächlich verwendeten Frontend-Port an.

Falls das vorhandene Windows-JDK `Unable to establish loopback connection` mit `Invalid argument: connect` meldet, kann ein verkürzter Temp-Pfad mit `~1` die Ursache sein. Für den betroffenen Terminal-Prozess vor dem Start oder Testlauf einen vollständigen Temp-Pfad setzen:

```powershell
New-Item -ItemType Directory -Force .tools/tmp | Out-Null
$env:TEMP = (Resolve-Path .tools/tmp).Path
$env:TMP = $env:TEMP
```

## Konfiguration und Daten

| Variable | Standard | Bedeutung |
| --- | --- | --- |
| `MINIERP_PORT` | `8080` | API-Port auf dem lokalen Rechner |
| `MINIERP_DB_URL` | `jdbc:sqlite:miniERP.db` | JDBC-Verbindungsadresse |
| `VITE_API_BASE_URL` | `http://127.0.0.1:8080/api` | API-Adresse für React |

`SchemaInitializer` legt fehlende Tabellen beim Start an. Er ersetzt keine bestehenden Tabellen und ist noch kein versioniertes Migrationssystem. Die API bearbeitet Anfragen nacheinander, weil die bestehenden DAOs eine JDBC-Verbindung teilen. Sie ist für lokale Entwicklung konfiguriert; Anmeldung und Mehrbenutzerbetrieb sind noch nicht implementiert.

Lokale Datenbanken, Zugangsdaten in `*.local`, Abhängigkeiten und Build-Ausgaben werden nicht versioniert. Die API erzeugt keine Beispieldaten.

## Nutzbare Funktionen

- Übersicht mit echten Kunden- und Produktzahlen.
- Kunden und Produkte durchsuchen, anlegen und bearbeiten; Formulare zeigen Fehler der API an.
- Laden, leere Listen, Verbindungsfehler und erfolgreiche Speicherungen sind in der Oberfläche abgebildet.
- Die Auftragserstellung im Service berechnet `Positionssumme × (1 − Rabatt) × (1 + Steuer)`. Rabatt und Steuer sind Anteile (`0.10` = 10 %, `0.19` = 19 %); der Gesamtbetrag wird abschließend kaufmännisch auf zwei Nachkommastellen gerundet. Auftrag und Positionen werden gemeinsam gespeichert oder zurückgerollt.

## HTTP-API

Antworten sind JSON. Kunden- und Produktlisten enthalten aktive Datensätze. Neue Datensätze erhalten serverseitig ihre ID und den Status aktiv.

| Methode | Pfad | Funktion |
| --- | --- | --- |
| GET | `/api/health` | Erreichbarkeit |
| GET / POST | `/api/customers` | Kunden auflisten / anlegen |
| GET / PUT | `/api/customers/{id}` | Kunden lesen / bearbeiten |
| GET / POST | `/api/products` | Produkte auflisten / anlegen |
| GET / PUT | `/api/products/{id}` | Produkte lesen / bearbeiten |

POST und PUT erwarten `Content-Type: application/json`. Kundenfelder: `name`, `address`, `birthdate` (ISO-Datum oder null), `email`, `phone`. Produktfelder: `name`, `description` (optional), `weight`, `price`, `minStockThreshold`. Dezimalwerte können als JSON-Zahl oder Dezimalstring übertragen werden. IDs und Aktivstatus sind keine schreibbaren Formularfelder.

Fehlerformat: `{ "error": "Beschreibung" }`. Statuscodes: 400 ungültige Eingabe, 404 unbekannter Datensatz/Endpunkt, 409 Duplikat, 405 unzulässige Methode, 413 zu große Anfrage, 415 falscher Inhaltstyp, 500 interner Fehler. POST liefert 201 und den gespeicherten Datensatz mit `Location`-Header.

## Qualität und Versionskontrolle

```sh
mvn test
cd frontend
pnpm run build
```

Die Integrationstests verwenden eigene Datenbanken im Arbeitsspeicher. Sie prüfen unter anderem HTTP-CRUD, Duplikate, Geburtsdaten, Produktvalidierung, Rabatt-/Steuerberechnung und Transaktions-Rollback. GitHub Actions führt Java-Tests und Frontend-Build bei Pushes und Pull Requests aus.

Repository: https://github.com/AndreasTatu/MiniERP. Aktueller Entwicklungsbranch: `codex/erp-frontend-api`; der lokale Git-Remote heißt `AndreasTatu`.

Als nächste Ausbauschritte stehen die Auftragsoberfläche, die Lageroberfläche, Archivierung/Reaktivierung und versionierte Datenbankmigrationen an.
