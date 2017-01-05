package com.alm.playlistimporter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements MainView {

    public static final int FILE_SELECT_CODE = 1;

    private TextView mInfo;
    private ArrayAdapter<String> mAdapter;
    private MainPresenterImpl mPresenter;

    private long mPlaylistID = -1;

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

        ListView lv = (ListView) findViewById(R.id.list);
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lv.setAdapter(mAdapter);
        lv.setOnItemClickListener(mOnItemClickListener);
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
    public void appendList(String item) {
        if (item.startsWith("#P#")) {
            mPlaylistID = Long.parseLong(item.substring(3));
            return;
        }

        mAdapter.add(item);
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

    private final AdapterView.OnItemClickListener mOnItemClickListener =
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String text = ((TextView) view).getText().toString();

                    Intent intent = new Intent(MainActivity.this, SongFinderActivity.class);
                    intent.putExtra(SongFinderActivity.EXTRA_SONG, text);
                    intent.putExtra(SongFinderActivity.EXTRA_PLAYLIST_ID, mPlaylistID);
                    startActivity(intent);
                }
            };
}
