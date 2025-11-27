package com.example.demo2.dal;

import com.example.demo2.entities.Playlist;
import com.example.demo2.entities.Song;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class InMemoryStore {
    private static final InMemoryStore INSTANCE = new InMemoryStore();
    private final Map<Integer, Song> songs = new HashMap<>();
    private final Map<Integer, Playlist> playlists = new HashMap<>();
    private final Map<Integer, List<Integer>> playlistSongs = new HashMap<>();
    private final AtomicInteger songIdCounter = new AtomicInteger(1);
    private final AtomicInteger playlistIdCounter = new AtomicInteger(1);
    private final LocalDataStore localStore = LocalDataStore.getInstance();

    private InMemoryStore() {
        loadFromLocalStore();
    }

    public static InMemoryStore getInstance() {
        return INSTANCE;
    }

    private void loadFromLocalStore() {
        List<Song> loadedSongs = localStore.loadSongs();
        for (Song s : loadedSongs) {
            songs.put(s.getId(), s);
            if (s.getId() >= songIdCounter.get()) {
                songIdCounter.set(s.getId() + 1);
            }
        }

        List<Playlist> loadedPlaylists = localStore.loadPlaylists();
        for (Playlist p : loadedPlaylists) {
            playlists.put(p.getId(), p);
            if (p.getId() >= playlistIdCounter.get()) {
                playlistIdCounter.set(p.getId() + 1);
            }
        }

        playlistSongs.putAll(localStore.loadPlaylistSongs());
    }

    public List<Song> getAllSongs() {
        return new ArrayList<>(songs.values());
    }

    public Song insertSong(Song song) {
        int id = songIdCounter.getAndIncrement();
        song.setId(id);
        songs.put(id, song);
        localStore.saveSong(song);
        return song;
    }

    public void updateSong(Song song) {
        if (song.getId() != null) {
            songs.put(song.getId(), song);
            localStore.saveSong(song);
        }
    }

    public boolean deleteSong(int id) {
        boolean existed = songs.remove(id) != null;
        playlistSongs.values().forEach(list -> list.remove(Integer.valueOf(id)));
        localStore.deleteSong(id);
        return existed;
    }

    public Song searchSongById(int id) {
        return songs.get(id);
    }

    public List<Song> searchSongs(String query) {
        String lower = query.toLowerCase();
        return songs.values().stream()
                .filter(s -> s.getTitle().toLowerCase().contains(lower) ||
                           s.getArtist().toLowerCase().contains(lower))
                .sorted(Comparator.comparing(Song::getTitle))
                .collect(Collectors.toList());
    }

    public List<Playlist> getAllPlaylists() {
        return new ArrayList<>(playlists.values());
    }

    public Playlist insertPlaylist(Playlist playlist) {
        int id = playlistIdCounter.getAndIncrement();
        playlist.setId(id);
        playlists.put(id, playlist);
        playlistSongs.put(id, new ArrayList<>());
        localStore.savePlaylist(playlist);
        return playlist;
    }

    public void updatePlaylist(Playlist playlist) {
        if (playlist.getId() != null) {
            playlists.put(playlist.getId(), playlist);
            localStore.savePlaylist(playlist);
        }
    }

    public boolean deletePlaylist(int id) {
        playlistSongs.remove(id);
        localStore.clearAllPlaylistSongs(id);
        localStore.deletePlaylist(id);
        return playlists.remove(id) != null;
    }

    public List<Song> getPlaylistSongs(int playlistId) {
        List<Integer> songIds = playlistSongs.getOrDefault(playlistId, new ArrayList<>());
        return songIds.stream()
                .map(songs::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void addSongToPlaylist(int playlistId, int songId) {
        List<Integer> list = playlistSongs.computeIfAbsent(playlistId, k -> new ArrayList<>());
        int position = list.size();
        list.add(songId);
        localStore.savePlaylistSong(playlistId, position, songId);
    }

    public void removeSongFromPlaylist(int playlistId, int position) {
        List<Integer> list = playlistSongs.get(playlistId);
        if (list != null && position >= 0 && position < list.size()) {
            list.remove(position);
            localStore.deletePlaylistSong(playlistId, position);
            for (int i = position; i < list.size(); i++) {
                localStore.savePlaylistSong(playlistId, i, list.get(i));
            }
        }
    }

    public void movePlaylistSong(int playlistId, int fromPos, int toPos) {
        List<Integer> list = playlistSongs.get(playlistId);
        if (list != null && fromPos >= 0 && fromPos < list.size() && toPos >= 0 && toPos < list.size()) {
            int songId = list.remove(fromPos);
            list.add(toPos, songId);
            localStore.clearAllPlaylistSongs(playlistId);
            for (int i = 0; i < list.size(); i++) {
                localStore.savePlaylistSong(playlistId, i, list.get(i));
            }
        }
    }

    public void clear() {
        songs.clear();
        playlists.clear();
        playlistSongs.clear();
        songIdCounter.set(1);
        playlistIdCounter.set(1);
    }
}
