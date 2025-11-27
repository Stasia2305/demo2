package com.example.demo2.dal;

import com.example.demo2.entities.Playlist;
import com.example.demo2.entities.Song;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for playlists and their song ordering.
 * Falls back to in-memory storage when database is unavailable.
 */
public class PlaylistDAO {
    private final InMemoryStore memoryStore = InMemoryStore.getInstance();

    public List<Playlist> findAll() throws SQLException {
        if (!DBManager.isAvailable()) {
            return memoryStore.getAllPlaylists();
        }
        String sql = "SELECT id, name FROM playlists ORDER BY name COLLATE NOCASE";
        try (Connection c = DBManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Playlist> list = new ArrayList<>();
            while (rs.next()) list.add(new Playlist(rs.getInt("id"), rs.getString("name")));
            return list;
        } catch (SQLException e) {
            return memoryStore.getAllPlaylists();
        }
    }

    public Playlist insert(Playlist p) throws SQLException {
        if (!DBManager.isAvailable()) {
            return memoryStore.insertPlaylist(p);
        }
        String sql = "INSERT INTO playlists(name) VALUES(?)";
        try (Connection c = DBManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
            return p;
        } catch (SQLException e) {
            return memoryStore.insertPlaylist(p);
        }
    }

    public void update(Playlist p) throws SQLException {
        if (!DBManager.isAvailable()) {
            memoryStore.updatePlaylist(p);
            return;
        }
        String sql = "UPDATE playlists SET name=? WHERE id=?";
        try (Connection c = DBManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            memoryStore.updatePlaylist(p);
        }
    }

    public boolean delete(int playlistId) throws SQLException {
        if (!DBManager.isAvailable()) {
            return memoryStore.deletePlaylist(playlistId);
        }
        String sql = "DELETE FROM playlists WHERE id=?";
        try (Connection c = DBManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return memoryStore.deletePlaylist(playlistId);
        }
    }

    public List<Song> getSongs(int playlistId) throws SQLException {
        if (!DBManager.isAvailable()) {
            return memoryStore.getPlaylistSongs(playlistId);
        }
        String sql = "SELECT s.id, s.title, s.artist, s.duration_seconds, s.file_path " +
                "FROM playlist_songs ps JOIN songs s ON ps.song_id = s.id " +
                "WHERE ps.playlist_id=? ORDER BY ps.position";
        try (Connection c = DBManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Song> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Song(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("artist"),
                            rs.getInt("duration_seconds"),
                            rs.getString("file_path")));
                }
                return list;
            }
        } catch (SQLException e) {
            return memoryStore.getPlaylistSongs(playlistId);
        }
    }

    public void addSongToEnd(int playlistId, int songId) throws SQLException {
        if (!DBManager.isAvailable()) {
            memoryStore.addSongToPlaylist(playlistId, songId);
            return;
        }
        String maxSql = "SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlist_id=?";
        try (Connection c = DBManager.getConnection();
             PreparedStatement maxPs = c.prepareStatement(maxSql)) {
            c.setAutoCommit(false);
            int nextPos = 0;
            try {
                maxPs.setInt(1, playlistId);
                try (ResultSet rs = maxPs.executeQuery()) {
                    if (rs.next()) nextPos = rs.getInt(1) + 1;
                }
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO playlist_songs(playlist_id, position, song_id) VALUES(?,?,?)")) {
                    ins.setInt(1, playlistId);
                    ins.setInt(2, nextPos);
                    ins.setInt(3, songId);
                    ins.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            memoryStore.addSongToPlaylist(playlistId, songId);
        }
    }

    public void removeAtPosition(int playlistId, int position) throws SQLException {
        if (!DBManager.isAvailable()) {
            memoryStore.removeSongFromPlaylist(playlistId, position);
            return;
        }
        try (Connection c = DBManager.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM playlist_songs WHERE playlist_id=? AND position=?");
                 PreparedStatement shift = c.prepareStatement(
                         "UPDATE playlist_songs SET position = position - 1 WHERE playlist_id=? AND position > ?")) {
                del.setInt(1, playlistId);
                del.setInt(2, position);
                del.executeUpdate();

                shift.setInt(1, playlistId);
                shift.setInt(2, position);
                shift.executeUpdate();
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            memoryStore.removeSongFromPlaylist(playlistId, position);
        }
    }

    public void move(int playlistId, int fromPos, int toPos) throws SQLException {
        if (fromPos == toPos) return;
        
        if (!DBManager.isAvailable()) {
            memoryStore.movePlaylistSong(playlistId, fromPos, toPos);
            return;
        }
        
        try (Connection c = DBManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement temp = c.prepareStatement(
                        "UPDATE playlist_songs SET position=-1 WHERE playlist_id=? AND position=?")) {
                    temp.setInt(1, playlistId);
                    temp.setInt(2, fromPos);
                    temp.executeUpdate();
                }
                if (fromPos < toPos) {
                    try (PreparedStatement shiftDown = c.prepareStatement(
                            "UPDATE playlist_songs SET position=position-1 WHERE playlist_id=? AND position>? AND position<=?")) {
                        shiftDown.setInt(1, playlistId);
                        shiftDown.setInt(2, fromPos);
                        shiftDown.setInt(3, toPos);
                        shiftDown.executeUpdate();
                    }
                } else {
                    try (PreparedStatement shiftUp = c.prepareStatement(
                            "UPDATE playlist_songs SET position=position+1 WHERE playlist_id=? AND position>=? AND position<?")) {
                        shiftUp.setInt(1, playlistId);
                        shiftUp.setInt(2, toPos);
                        shiftUp.setInt(3, fromPos);
                        shiftUp.executeUpdate();
                    }
                }
                try (PreparedStatement place = c.prepareStatement(
                        "UPDATE playlist_songs SET position=? WHERE playlist_id=? AND position=-1")) {
                    place.setInt(1, toPos);
                    place.setInt(2, playlistId);
                    place.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            memoryStore.movePlaylistSong(playlistId, fromPos, toPos);
        }
    }
}
