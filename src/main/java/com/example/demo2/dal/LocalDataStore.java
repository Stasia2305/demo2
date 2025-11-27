package com.example.demo2.dal;

import com.example.demo2.entities.Playlist;
import com.example.demo2.entities.Song;

import java.sql.*;
import java.util.*;

public class LocalDataStore {
    private static final LocalDataStore INSTANCE = new LocalDataStore();
    private static final String DB_PATH = System.getProperty("user.home") + "/.mytunes/local_data.db";
    private Connection dbConnection;

    private LocalDataStore() {
        initializeDatabase();
    }

    public static LocalDataStore getInstance() {
        return INSTANCE;
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + DB_PATH;
            dbConnection = DriverManager.getConnection(url);
            createTables();
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Failed to initialize local data store: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement st = dbConnection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS songs (" +
                    "id INTEGER PRIMARY KEY, " +
                    "title TEXT NOT NULL, " +
                    "artist TEXT NOT NULL, " +
                    "duration_seconds INTEGER NOT NULL, " +
                    "file_path TEXT NOT NULL)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS playlists (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name TEXT NOT NULL UNIQUE)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS playlist_songs (" +
                    "playlist_id INTEGER NOT NULL, " +
                    "position INTEGER NOT NULL, " +
                    "song_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (playlist_id, position), " +
                    "FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE)");
        }
    }

    public List<Song> loadSongs() {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT id, title, artist, duration_seconds, file_path FROM songs";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                songs.add(new Song(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getInt("duration_seconds"),
                        rs.getString("file_path")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading songs: " + e.getMessage());
        }
        return songs;
    }

    public void saveSong(Song song) {
        String sql = "INSERT OR REPLACE INTO songs(id, title, artist, duration_seconds, file_path) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setInt(1, song.getId());
            ps.setString(2, song.getTitle());
            ps.setString(3, song.getArtist());
            ps.setInt(4, song.getDurationSeconds());
            ps.setString(5, song.getFilePath());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving song: " + e.getMessage());
        }
    }

    public void deleteSong(int id) {
        String sql = "DELETE FROM songs WHERE id = ?";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting song: " + e.getMessage());
        }
    }

    public List<Playlist> loadPlaylists() {
        List<Playlist> playlists = new ArrayList<>();
        String sql = "SELECT id, name FROM playlists";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                playlists.add(new Playlist(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            System.err.println("Error loading playlists: " + e.getMessage());
        }
        return playlists;
    }

    public void savePlaylist(Playlist playlist) {
        String sql = "INSERT OR REPLACE INTO playlists(id, name) VALUES(?, ?)";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setInt(1, playlist.getId());
            ps.setString(2, playlist.getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving playlist: " + e.getMessage());
        }
    }

    public void deletePlaylist(int id) {
        String sql = "DELETE FROM playlists WHERE id = ?";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting playlist: " + e.getMessage());
        }
    }

    public Map<Integer, List<Integer>> loadPlaylistSongs() {
        Map<Integer, List<Integer>> playlistSongs = new HashMap<>();
        String sql = "SELECT playlist_id, song_id FROM playlist_songs ORDER BY position";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int playlistId = rs.getInt("playlist_id");
                int songId = rs.getInt("song_id");
                playlistSongs.computeIfAbsent(playlistId, k -> new ArrayList<>()).add(songId);
            }
        } catch (SQLException e) {
            System.err.println("Error loading playlist songs: " + e.getMessage());
        }
        return playlistSongs;
    }

    public void savePlaylistSong(int playlistId, int position, int songId) {
        String sql = "INSERT OR REPLACE INTO playlist_songs(playlist_id, position, song_id) VALUES(?, ?, ?)";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, position);
            ps.setInt(3, songId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving playlist song: " + e.getMessage());
        }
    }

    public void deletePlaylistSong(int playlistId, int position) {
        String sql = "DELETE FROM playlist_songs WHERE playlist_id = ? AND position = ?";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, position);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting playlist song: " + e.getMessage());
        }
    }

    public void clearAllPlaylistSongs(int playlistId) {
        String sql = "DELETE FROM playlist_songs WHERE playlist_id = ?";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error clearing playlist songs: " + e.getMessage());
        }
    }
}
