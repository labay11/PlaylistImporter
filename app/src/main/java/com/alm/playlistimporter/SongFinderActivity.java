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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by A. Labay on 06/08/16.
 * As part of the project Playlist Importer.
 */
public class SongFinderActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<List<SongFinderActivity.Song>> {

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
        ListView mListView = (ListView) findViewById(R.id.list);
        EditText mEditText = (EditText) findViewById(R.id.edit_text);

        mAdapter = new SongAdapter(this);
        mListView.setAdapter(mAdapter);

        mTextView.setText(mSongText);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                confirmAddToPlaylist(mAdapter.getItem(i));
            }
        });

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

    private void confirmAddToPlaylist(final Song song) {
        new AlertDialog.Builder(this)
                .setMessage(String.format("Do you want to add %s to the playlist instead of %s", song.mTitle, mSongText))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        addToPlaylist(song.mId);
                    }
                }).setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void addToPlaylist(long songId) {
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistId),
                null, null, null, null);

        int count = cursor.getCount();
        cursor.close();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songId);
        values.put(MediaStore.Audio.Playlists.Members.PLAYLIST_ID, mPlaylistId);
        values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, count);

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
    public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
        return new SearchLoader(this, args.getString("query"));
    }

    @Override
    public void onLoadFinished(Loader<List<Song>> loader, List<Song> data) {
        mAdapter.setItems(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Song>> loader) {
    }

    private static class SongAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        private final List<Song> mItems = new ArrayList<>();

        public SongAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Song getItem(int i) {
            return mItems.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View contentView, ViewGroup viewGroup) {
            SongHolder holder;
            if (contentView == null) {
                contentView = mInflater.inflate(R.layout.list_item_song, viewGroup, false);
                holder = new SongHolder(contentView);
                contentView.setTag(holder);
            } else {
                holder = (SongHolder) contentView.getTag();
            }

            holder.bind(getItem(i));

            return contentView;
        }

        public void setItems(Collection<Song> mList) {
            mItems.clear();
            if (mList != null) {
                mItems.addAll(mList);
            }

            notifyDataSetChanged();
        }
    }

    private static class SongHolder {

        public TextView mTitleView, mArtistView;

        public SongHolder(View content) {
            mTitleView = (TextView) content.findViewById(R.id.text1);
            mArtistView = (TextView) content.findViewById(R.id.text2);
        }

        public void bind(Song s) {
            mTitleView.setText(s.mTitle);
            mArtistView.setText(s.mArtist);
        }
    }

    public static class Song {

        public final String mTitle;
        public final String mArtist;

        public final long mId;

        public Song(long id, String title, String artist) {
            this.mId = id;
            this.mTitle = title;
            this.mArtist = artist;
        }
    }

    public static class SearchLoader extends AsyncLoader<List<Song>> {

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
        public List<Song> loadInBackground() {
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

            List<Song> songs = new ArrayList<>(cursor.getCount());

            do {
                songs.add(new Song(cursor.getInt(0), cursor.getString(1), cursor.getString(2)));
            } while (cursor.moveToNext());

            cursor.close();

            return songs;
        }
    }
}
