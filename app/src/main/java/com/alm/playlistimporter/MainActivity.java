package com.alm.playlistimporter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements MainView {

    public static final int FILE_SELECT_CODE = 1;

    private TextView mInfo;
    private TrackAdapter mAdapter;
    private MainPresenterImpl mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        mPresenter = new MainPresenterImpl(this);
    }

    private void init() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseFile();
            }
        });

        mInfo = (TextView) findViewById(R.id.tv_info);

        RecyclerView lv = (RecyclerView) findViewById(R.id.list);
        mAdapter = new TrackAdapter(this, mOnItemClickListener);
        lv.setLayoutManager(new LinearLayoutManager(this));
        lv.setHasFixedSize(true);
        lv.setAdapter(mAdapter);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void showError(String text) {
        mInfo.setTextColor(Color.RED);
        mInfo.setText(text);
    }

    @Override
    public void showMessage(String text) {
        mInfo.setTextColor(Color.BLACK);
        mInfo.setText(text);
    }

    @Override
    public void appendList(Track item) {
        mAdapter.addTrack(item);
    }

    public void clear() {
        mAdapter.clear();
        mInfo.setText(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE) {
            clear();
            if (resultCode == RESULT_OK) {
                mPresenter.loadFile(data.getData());
            } else {
                Snackbar.make(findViewById(R.id.container),
                        "No file selected",
                        Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private final BaseAdapter.OnItemClickListener<Track> mOnItemClickListener =
            new BaseAdapter.OnItemClickListener<Track>() {
                @Override
                public void onItemClick(View itemView, int pos, Track item) {
                    Intent intent = new Intent(MainActivity.this, SongFinderActivity.class);
                    intent.putExtra(SongFinderActivity.EXTRA_SONG, item.title + " - " + item.artist);
                    intent.putExtra(SongFinderActivity.EXTRA_PLAYLIST_ID, mPresenter.getPlaylistId());
                    startActivity(intent);
                }
            };

    private static class TrackAdapter extends BaseAdapter<Track, TrackHolder> {


        public TrackAdapter(Context context, OnItemClickListener<Track> itemClickListener) {
            super(context, itemClickListener);
        }

        @Override
        public TrackHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TrackHolder(mInflater.inflate(R.layout.list_item_song, parent, false));
        }

        public void addTrack(@NonNull Track track) {
            if (track.isAdded) {
                add(track);
            } else {
                add(0, track);
            }
        }
    }

    private static class TrackHolder extends BaseAdapter.BaseHolder<Track> {

        private TextView text1, text2;
        private View backgroud;

        public TrackHolder(View itemView) {
            super(itemView);
            text1 = (TextView) itemView.findViewById(R.id.text1);
            text2 = (TextView) itemView.findViewById(R.id.text2);
            backgroud = itemView.findViewById(R.id.list_item_container);
        }

        @Override
        public void bind(final Track item,
                         final BaseAdapter.OnItemClickListener<Track> itemClickListener) {
            text1.setText(item.title);
            text2.setText(item.artist);
            if (item.isAdded) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    backgroud.setBackground(null);
                } else {
                    //noinspection deprecation
                    backgroud.setBackgroundDrawable(null);
                }
            } else {
                backgroud.setBackgroundResource(R.color.track_not_added);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    itemClickListener.onItemClick(view, getAdapterPosition(), item);
                }
            });
        }
    }
}
