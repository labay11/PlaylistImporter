package com.alm.playlistimporter;

/**
 * Created by A. Labay on 05/01/17.
 * As part of the project PlaylistImporter.
 */

public class Track {

    public int id;
    public String title, artist;
    public String uri;

    public boolean isAdded;

    public Track(int id, String title, String artist) {
        this.id = id;
        this.title = title;
        this.artist = artist;
    }

    public Track(int id, String title, String artist, String uri, boolean isAdded) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.uri = uri;
        this.isAdded = isAdded;
    }
}
