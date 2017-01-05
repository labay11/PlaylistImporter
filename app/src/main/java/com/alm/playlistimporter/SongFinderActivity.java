package com.alm.playlistimporter;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by A. Labay on 06/08/16.
 * As part of the project Playlist Importer.
 */
public class SongFinderActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<List<Track>> {

    public static final String EXTRA_PLAYLIST_ID = "extra_playlist_id";
    public static final String EXTRA_SONG = "extra_song_key";

    private SongAdapter mAdapter;

    private long mPlaylistId;
    private String mSongText;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_find);

        mPlaylistId = getIntent().getLongExtra(EXTRA_PLAYLIST_ID, -1);
        mSongText = getIntent().getStringExtra(EXTRA_SONG);

        if (mPlaylistId == -1) {
            Toast.makeText(this, "Error getting playlist id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView mTextView = (TextView) findViewById(R.id.text);
        RecyclerView mListView = (RecyclerView) findViewById(R.id.list);
        EditText mEditText = (EditText) findViewById(R.id.edit_text);

        mAdapter = new SongAdapter(this, new BaseAdapter.OnItemClickListener<Track>() {
            @Override
            public void onItemClick(View itemView, int pos, Track item) {
                confirmAddToPlaylist(item);

            }
        });
        mListView.setLayoutManager(new LinearLayoutManager(this));
        mListView.setHasFixedSize(true);
        mListView.setAdapter(mAdapter);

        mTextView.setText(mSongText);

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() > 2) {
                    query(charSequence.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Bundle bundle = new Bundle(1);
        bundle.putString("query", mSongText);
        getSupportLoaderManager().initLoader(0, bundle, this);
    }

    private void confirmAddToPlaylist(final Track song) {
        new AlertDialog.Builder(this)
                .setMessage(String.format("Do you want to add %s to the playlist instead of %s", song.title, mSongText))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        addToPlaylist(song.id);
                    }
                }).setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void addToPlaylist(long songId) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songId);
        values.put(MediaStore.Audio.Playlists.Members.PLAYLIST_ID, mPlaylistId);

        Uri uri = getContentResolver()
                .insert(MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistId),
                        values);

        if (uri != null) {
            Toast.makeText(SongFinderActivity.this, "Inserted OK", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(SongFinderActivity.this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void query(String text) {
        Bundle bundle = new Bundle(1);
        bundle.putString("query", text);
        getSupportLoaderManager().restartLoader(0, bundle, this);
    }

    @Override
    public Loader<List<Track>> onCreateLoader(int id, Bundle args) {
        return new SearchLoader(this, args.getString("query"));
    }

    @Override
    public void onLoadFinished(Loader<List<Track>> loader, List<Track> data) {
        mAdapter.set(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Track>> loader) {
    }

    private static class SongAdapter extends BaseAdapter<Track, TrackHolder> {

        public SongAdapter(Context context, OnItemClickListener<Track> itemClickListener) {
            super(context, itemClickListener);
        }

        @Override
        public TrackHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TrackHolder(mInflater.inflate(R.layout.list_item_song, parent, false));
        }
    }

    private static class TrackHolder extends BaseAdapter.BaseHolder<Track> {

        public TextView mTitleView, mArtistView;

        public TrackHolder(View itemView) {
            super(itemView);
            mTitleView = (TextView) itemView.findViewById(R.id.text1);
            mArtistView = (TextView) itemView.findViewById(R.id.text2);
        }

        @Override
        public void bind(final Track item, final BaseAdapter.OnItemClickListener<Track> itemClickListener) {
            mTitleView.setText(item.title);
            mArtistView.setText(item.artist);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    itemClickListener.onItemClick(itemView, getAdapterPosition(), item);
                }
            });
        }
    }

    public static class SearchLoader extends AsyncLoader<List<Track>> {

        public static final String[] PROJECTION = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST
        };

        private String query;

        public SearchLoader(Context context, String query) {
            super(context);
            this.query = "%" + MediaStore.Audio.keyFor(query) + "%";
        }

        @Override
        public List<Track> loadInBackground() {
            Cursor cursor = getContext()
                    .getContentResolver()
                    .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            PROJECTION,
                            MediaStore.Audio.Media.TITLE_KEY + " LIKE ? OR " + MediaStore.Audio.Media.ARTIST_KEY + " LIKE ?",
                            new String[]{query, query},
                            MediaStore.Audio.Media.ARTIST + "," + MediaStore.Audio.Media.TITLE);

            if (cursor == null)
                return null;

            if (!cursor.moveToFirst()) {
                cursor.close();
                return null;
            }

            List<Track> songs = new ArrayList<>(cursor.getCount());

            do {
                songs.add(new Track(cursor.getInt(0), cursor.getString(1), cursor.getString(2)));
            } while (cursor.moveToNext());

            cursor.close();

            return songs;
        }
    }
}
