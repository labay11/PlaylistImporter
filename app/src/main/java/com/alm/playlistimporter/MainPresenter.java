package com.alm.playlistimporter;

import android.net.Uri;

import rx.Subscriber;

/**
 * Created by A. Labay on 25/01/16.
 * As part of the project Playlist Importer.
 */
public abstract class MainPresenter extends Subscriber<String> {

    protected MainView mView;

    public MainPresenter(MainView view) {
        mView = view;
    }

    public abstract void loadFile(Uri uri);
}
