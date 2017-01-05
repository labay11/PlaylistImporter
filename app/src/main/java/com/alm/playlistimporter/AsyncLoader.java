package com.alm.playlistimporter;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * Created by adria on 25/08/14.
 * As part of the project MyPlayer.
 */
public abstract class AsyncLoader<D> extends AsyncTaskLoader<D> {

    private D mData;

    public static final String CASE_ACCENT_INSENSITIVE = " COLLATE UNICODE";

    /**
     * Constructor of <code>WrappedAsyncTaskLoader</code>
     *
     * @param context The {@link Context} to use.
     */
    public AsyncLoader(Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deliverResult(D data) {
        if (!isReset()) {
            this.mData = data;
            super.deliverResult(data);
        } else {
            // An asynchronous query came in while the loader is stopped
        }
    }

    @Override
    protected void onStartLoading() {
        if (this.mData != null) {
            deliverResult(this.mData);
        } else if (takeContentChanged() || this.mData == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        // Ensure the loader is stopped
        onStopLoading();
        this.mData = null;
    }
}
