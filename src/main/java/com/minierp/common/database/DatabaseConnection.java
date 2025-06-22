package com.minierp.common.database;

// Importiert JDBC-Klassen für SQL-Verbindung
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Diese Klasse stellt einen Zugang nach Singleton-Prinzip zur SQLite Datenbankverbindung her.
 * Die Verbindung wird wiederverwendet, um unnötigen Overhead zu vermeiden.
 */
public class DatabaseConnection {

    // JDBC-URL zur SQLite-Datenbankdatei im Projektverzeichnis.
    // Beim ersten Zugriff wird die Datei 'miniERP.db' automatisch erstellt.
    private static final String URL = "jdbc:sqlite:miniERP.db";

    // Die einzige Datenbankverbindung (Singleton-Prinzip)
    private static Connection connection;

    // Privater Konstruktor verhindert, dass Objekte von außen erstellt werden können
    private DatabaseConnection() {}

    /**
     * Liefert eine aktive Verbindung zur SQLite-Datenbank.
     * Wenn noch keine Verbindung besteht oder die alte geschlossen wurde,
     * wird eine neue aufgebaut.
     *
     * Die Methode ist synchronized, um sie thread-sicher zu machen.
     * Dadurch wird bei parallelem Zugriff sichergestellt, dass nur eine Verbindung erzeugt wird.
     *
     * @return Eine gültige Connection-Instanz
     * @throws SQLException Wenn beim Verbindungsaufbau etwas schiefgeht
     */
    public static synchronized Connection getConnection() throws SQLException {
        // Verbindung nur herstellen, wenn keine existiert oder sie geschlossen ist
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
        }
        return connection;
    }

    //Methode zum Schließen der DB-Verbindung
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
                //nur für erstes Debugging bis alles läuft
                e.printStackTrace();
            }
        }
    }


}
