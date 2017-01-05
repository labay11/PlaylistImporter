package com.alm.playlistimporter;

import android.net.Uri;

import java.util.PriorityQueue;
import java.util.Queue;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by A. Labay on 25/01/16.
 * As part of the project Playlist Importer.
 */
public class MainPresenterImpl extends MainPresenter {

    private Observable<String> mObservable = null;

    private Queue<Uri> mQueue = new PriorityQueue<>();

    public MainPresenterImpl(MainView view) {
        super(view);
    }

    @Override
    public void loadFile(Uri uri) {
        if (uri == null) {
            mView.showError("Invalid file");
            return;
        }

        if (mObservable != null) {
            mQueue.add(uri);
            return;
        }

        createObservable(uri);
    }

    private Subscription createObservable(final Uri uri) {
        mObservable = PlaylistReader
                .execute(mView.getContext().getContentResolver(), uri);

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
        if (!mQueue.isEmpty()) {
            loadFile(mQueue.poll());
        }
    }

    @Override
    public void onError(Throwable e) {
        mView.showMessage(e.getLocalizedMessage());
        mObservable = null;
    }

    @Override
    public void onNext(String s) {
        mView.appendList(s);
    }
}
