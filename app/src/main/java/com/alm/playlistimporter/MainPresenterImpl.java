package com.alm.playlistimporter;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.text.DateFormat;
import java.util.Date;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by A. Labay on 25/01/16.
 * As part of the project Playlist Importer.
 */
public class MainPresenterImpl extends MainPresenter {

    private Observable<Track> mObservable = null;

    private ContentResolver mResolver;

    private long playlistId = -1;

    public MainPresenterImpl(MainView view) {
        super(view);
        mResolver = view.getContext().getContentResolver();
    }

    @Override
    public void loadFile(Uri uri) {
        if (uri == null) {
            mView.showError("Invalid file");
            return;
        }

        if (mObservable != null) {
            return;
        }

        playlistId = createPlaylist(getDefaultName(uri));
        if (playlistId < 0) {
            mView.showError("Cannot create playlist, try again.");
            return;
        }

        createObservable(uri);
    }

    private Subscription createObservable(final Uri uri) {
        mObservable = PlaylistReader.execute(mView.getContext().getContentResolver(), uri);

        return mObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this);
    }

    @Override
    public void onStart() {
        mView.showMessage("Loading...");
    }

    @Override
    public void onCompleted() {
        mView.showMessage("Finish!");
        mObservable = null;
    }

    @Override
    public void onError(Throwable e) {
        mView.showMessage(e.getLocalizedMessage());
        deletePlaylist();
        mObservable = null;
    }

    @Override
    public void onNext(Track s) {
        if (s.id != -1) {
            s.isAdded = addToPlaylist(s.id);
        }

        mView.appendList(s);
    }

    private String getDefaultName(Uri uri) {
        String s = uri.getLastPathSegment();
        if (!TextUtils.isEmpty(s)) {
            return s.substring(0, s.lastIndexOf("."));
        }

        return "Playlist_" + DateFormat.getDateInstance(DateFormat.SHORT)
                .format(new Date());
    }

    private long createPlaylist(String name) {
        Cursor pCursor = null;
        try {
            pCursor = mResolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Playlists._ID},
                    MediaStore.Audio.Playlists.NAME + "=?", new String[]{name},
                    null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        long pId = -1;
        if (pCursor != null && pCursor.moveToFirst()) {
            pId = pCursor.getLong(0);
        }

        if (pCursor != null)
            pCursor.close();

        if (pId != -1)
            return pId;

        ContentValues values = new ContentValues(1);
        values.put(MediaStore.Audio.Playlists.NAME, name);

        Uri uri = mResolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            return ContentUris.parseId(uri);
        }

        return -1;
    }

    public boolean addToPlaylist(long id) {
        ContentValues v = new ContentValues(3);
        v.put(MediaStore.Audio.Playlists.Members.PLAYLIST_ID, playlistId);
        v.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, id);

        return mResolver.insert(
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                v) != null;
    }

    public long getPlaylistId() {
        return playlistId;
    }

    private void deletePlaylist() {
        mResolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Playlists._ID + "=?",
                new String[]{""+playlistId});
        playlistId = 0;
    }
}
