package com.alm.playlistimporter;

import android.content.Context;

/**
 * Created by A. Labay on 25/01/16.
 * As part of the project Playlist Importer.
 */
public interface MainView {

    Context getContext();

    void showError(String text);

    void showMessage(String text);

    void appendList(Track item);
}
