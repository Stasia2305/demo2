package com.example.demo2.dal;

import java.sql.*;

/**
 * Provides JDBC connection to EASV-DB4 (SQL Server) and initializes schema on first use.
 * Falls back gracefully when database is unavailable.
 */
public final class DBManager {
    private static final String SERVER = "10.176.111.34";
    private static final int PORT = 1433;
    private static final String DATABASE = "Natbur001_MyTunes";
    private static final String USERNAME = "CS2025b_e_4";
    private static final String PASSWORD = "CS2025bE4#23";
    private static final String DB_URL = "jdbc:sqlserver://" + SERVER + ":" + PORT + ";databaseName=" + DATABASE + ";encrypt=true;trustServerCertificate=true";
    private static volatile boolean initialized = false;
    private static volatile boolean available = true;

    private DBManager() {}

    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ignored) {
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static Connection getConnection() throws SQLException {
        if (!available) {
            throw new SQLException("Database is not available. Running in offline mode.");
        }
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            ensureInitialized(conn);
            return conn;
        } catch (SQLException e) {
            available = false;
            throw e;
        }
    }

    private static synchronized void ensureInitialized(Connection conn) throws SQLException {
        if (initialized) return;
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'songs') " +
                    "CREATE TABLE songs (" +
                    "id INT PRIMARY KEY IDENTITY(1,1), " +
                    "title NVARCHAR(255) NOT NULL, " +
                    "artist NVARCHAR(255) NOT NULL, " +
                    "duration_seconds INT NOT NULL DEFAULT 0, " +
                    "file_path NVARCHAR(MAX) NOT NULL)");

            st.executeUpdate("IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'playlists') " +
                    "CREATE TABLE playlists (" +
                    "id INT PRIMARY KEY IDENTITY(1,1), " +
                    "name NVARCHAR(255) NOT NULL UNIQUE)");

            st.executeUpdate("IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'playlist_songs') " +
                    "CREATE TABLE playlist_songs (" +
                    "playlist_id INT NOT NULL, " +
                    "position INT NOT NULL, " +
                    "song_id INT NOT NULL, " +
                    "PRIMARY KEY (playlist_id, position), " +
                    "FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE)");
        }
        initialized = true;
    }
}
