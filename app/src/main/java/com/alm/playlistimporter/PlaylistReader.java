package com.alm.playlistimporter;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by A. Labay on 15/01/16.
 * As part of the project Playlist Importer.
 */
public class PlaylistReader implements Observable.OnSubscribe<Track> {

    public static final String TAG = PlaylistReader.class.getSimpleName();

    public static final int TYPE_ITUNES = 0, TYPE_M3U = 1;

    private Uri mFile;
    private ContentResolver mResolver;
    private int mType;

    /**
     * Constructs the iTunes playlist parser
     * @param file where the playlist is.
     *             Must be a *.xml exported from iTunes or *.m3u file.
     */
    public PlaylistReader(ContentResolver cr, @NonNull Uri file) {
        mResolver = cr;
        mFile = file;
        mType = getType(file.getLastPathSegment());

        if (mType == -1) {
            Log.e(TAG, "Cannot parse file: " + file);
            throw new UnsupportedOperationException("Not a iTunes playlist file");
        }
    }

    public static Observable<Track> execute(ContentResolver cr, @NonNull Uri file) {
        return Observable.create(new PlaylistReader(cr, file));
    }

    private int getType(String name) {
        if (name.endsWith("xml")) {
            return TYPE_ITUNES;
        } else if (name.endsWith("m3u")) {
            return TYPE_M3U;
        }

        return -1;
    }

    private PlaylistParser getParser() {
        if (mType == TYPE_ITUNES) {
            return null; //new XMLPlaylistParser(mIdRetriever);
        } else if (mType == TYPE_M3U) {
            return new M3UPlaylistParser(mIdRetriever);
        }

        return null;
    }

    private final IdRetriever mIdRetriever = new IdRetriever() {
        String[] PROJECTION = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA};

        @Override
        public Track getMediaId(String title_key, String artist_key, String uri) {
            Track track = try_uri(uri);
            if (track != null)
                return track;

            return try_title(title_key, artist_key);
        }

        private Track try_uri(String uri) {
            Cursor c = mResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION,
                    MediaStore.Audio.Media.DATA + " LIKE ",
                    new String[]{"%" + Uri.parse(uri).getLastPathSegment()},
                    null);

            if (c == null)
                return null;

            if (!c.moveToFirst()) {
                c.close();
                return null;
            }

            if (c.getCount() == 1) {
                Track t = new Track(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), true);
                c.close();
                return t;
            }
            return null;
        }

        private Track try_title(String title, String artist) {
            Cursor c = mResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION,
                    MediaStore.Audio.Media.TITLE_KEY + " =? AND " + MediaStore.Audio.Media.ARTIST_KEY + "=?",
                    new String[]{MediaStore.Audio.keyFor(title), MediaStore.Audio.keyFor(artist)},
                    null);

            if (c == null)
                return null;

            if (!c.moveToFirst()) {
                c.close();
                return null;
            }

            if (c.getCount() == 1) {
                Track t = new Track(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), true);
                c.close();
                return t;
            }

            return null;
        }
    };

    @Override
    public void call(Subscriber<? super Track> subscriber) {
        Log.i(TAG, "Started reader");
        PlaylistParser parser = getParser();

        if (parser == null) {
            Log.e(TAG, "Unknown parser");
            subscriber.onError(new UnsupportedOperationException("Cannot parse this file: " + mFile));
            return;
        }

        InputStream stream;
        try {
            stream = mResolver.openInputStream(mFile);
        } catch (FileNotFoundException e) {
            subscriber.onError(e);
            Log.e(TAG, "File not found");
            return;
        }

        if (stream == null) {
            subscriber.onError(new UnsupportedOperationException("Cannot parse this file: " + mFile));
            Log.e(TAG, "Null input stream");
            return;
        }

        Log.i(TAG, "Start parsing...");
        try {
            parser.parse(stream, subscriber);
        } catch (IOException e) {
            Log.i(TAG, "Error parsing", e);
            subscriber.onError(e);
        }
        Log.i(TAG, "End parsing");


        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        subscriber.onCompleted();
    }

    /*private String getDefaultName(String string) {
        if (string != null) {
            string = string.trim();
            if (!TextUtils.isEmpty(string))
                return string;
        }

        String s = mFile.getLastPathSegment();
        if (!TextUtils.isEmpty(s)) {
            return s.substring(0, s.lastIndexOf("."));
        }

        return "Playlist_" + DateFormat.getDateInstance(DateFormat.SHORT)
                .format(new Date());
    }

    private long getPlaylistId(String name) {
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

    private ContentValues[] getContentValues(long pId, ArrayList<Track> ids) {
        if (ids.isEmpty())
            return null;

        ContentValues[] values = new ContentValues[ids.size()];
        int excluded = 0;
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i).id == 1) {
                ++excluded;
                continue;
            }
            ContentValues v = new ContentValues(3);
            v.put(MediaStore.Audio.Playlists.Members.PLAYLIST_ID, pId);
            v.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, ids.get(i).id);
            v.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i);
            values[i-excluded] = v;
        }

        return Arrays.copyOf(values, ids.size()-excluded);
    }*/

    /*private static class XMLPlaylistParser extends PlaylistParser {

        public XMLPlaylistParser(IdRetriever idRetriever) {
            super(idRetriever);
        }

        private String name;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public ArrayList<Track> parse(@NonNull InputStream file, Subscriber<? super Track> subscriber) {
            ArrayList<Long> strings = new ArrayList<>();
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(file, "UTF-8");

                parser.nextTag();
                parser.require(XmlPullParser.START_TAG, null, "plist");
                skipTo(parser, "dict"); // skip to the first key
                skipTo(parser, "dict"); // skip to the tracks dict
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    int type = parser.getEventType();
                    if (type == XmlPullParser.END_TAG) {
                        if (parser.getName().equals("dict"))
                            return null;
                    }

                    if (type != XmlPullParser.START_TAG)
                        continue;

                    String tag = parser.getName();
                    switch (tag) {
                        case "key":
                            String[] keys = parseTrack(parser);
                            if (keys != null) {
                                long id = mIdRetriever.getMediaId(keys[0], keys[1]);
                                if (id == -1) {
                                    subscriber.onNext(new Track(-1, keys[0], keys[1], uri, false));
                                } else {
                                    strings.add(id);
                                }
                            }
                            break;
                    }
                }
                skipTo(parser, "array");
                skipTo(parser, "key");
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    int type = parser.getEventType();
                    if (type == XmlPullParser.START_TAG) {
                        if (parser.getName().equals("key")) {
                            String key = parser.nextText();
                            if (key.equalsIgnoreCase("name")) {
                                skipTo(parser, "string");
                                name = parser.nextText();
                                break;
                            }
                        }
                    } else if (type == XmlPullParser.TEXT) {
                        if (parser.getText().equalsIgnoreCase("name")) {
                            skipTo(parser, "string");
                            name = parser.nextText();
                            break;
                        }
                    }
                }
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }

            return strings;
        }

        private String[] parseTrack(XmlPullParser parser) {
            String name = null, artist = null;
            try {
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.getEventType() == XmlPullParser.END_TAG) {
                        if (parser.getName().equals("dict"))
                            return null;
                    }

                    if (parser.getEventType() != XmlPullParser.START_TAG)
                        continue;

                    String tag = parser.getName();
                    if (tag.equals("key")) {
                        String key = parser.nextText();
                        if (key.equalsIgnoreCase("name")) {
                            skipTo(parser, "string");
                            name = parser.nextText();
                        } else if (key.equalsIgnoreCase("artist")) {
                            skipTo(parser, "string");
                            artist = parser.nextText();
                        }
                    }

                    if (name != null && artist != null)
                        break;
                }

                skipTo(parser, "dict");
                return new String[] {MediaStore.Audio.keyFor(name), MediaStore.Audio.keyFor(artist)};
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }

            return null; // something failed, don't add it to the playlist
        }

        private void skipTo(XmlPullParser parser, String tag) throws XmlPullParserException, IOException {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG
                        || parser.getEventType() == XmlPullParser.END_TAG)
                    if (parser.getName().equals(tag)) {
                        return;
                    }
            }
        }
    }*/

    private static class M3UPlaylistParser extends PlaylistParser {

        //private final Pattern mPattern = Pattern.compile("#EXTINF:\\d+,(.+)-(.+?)$");


        public M3UPlaylistParser(IdRetriever idRetriever) {
            super(idRetriever);
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void parse(@NonNull InputStream file, Subscriber<? super Track> subscriber) throws IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#EXTM3U"))
                    continue; // starting the file

                if (!line.startsWith("#EXTINF"))
                    continue;

                try {
                    String title_artist = line.substring(line.indexOf(",") + 1, line.length());
                    String[] keys = getTrackKey(title_artist);
                    String uri = br.readLine();

                    Track track = mIdRetriever.getMediaId(keys[0], keys[1], uri);
                    if (track == null) {
                        track = new Track(-1, keys[0], keys[1], uri, false);
                    }
                    subscriber.onNext(track);
                } catch (Exception ignored) {
                    // something failed in this item so skip it but continue parsing the playlist
                }
            }
        }

        private String[] getTrackKey(String line) {
            /*
                Rewrite!!!!
                Choose first index of '-' no matter how many of them
                if this fails @IdRetriever will try uri.
             */
            line = line.trim();
            int counter = 0;
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == '-') {
                    counter++;
                }
            }

            if (counter == 1) {
                String[] names = line.split("-");
                return new String[]{names[0].trim(), names[1].trim()};
            } else if (counter == 2) {
                int pos = line.lastIndexOf("-");
                return new String[]{
                        line.substring(0, pos).trim(),
                        line.substring(pos + 1, line.length()).trim()
                };
            } else { // last solution
                String[] parts = line.split("-");
                String name = "", artist = "";
                for (int i = 0; i < parts.length; i++) {
                    if (i <= (parts.length/2))
                        name += parts[i];
                    else
                        artist += parts[i];
                }

                return new String[]{name, artist};
            }
        }
    }

    private static abstract class PlaylistParser {

        protected IdRetriever mIdRetriever;

        public PlaylistParser(IdRetriever idRetriever) {
            mIdRetriever = idRetriever;
        }

        /**
         * Parses the playlist
         * @param file the playlist
         */
        public abstract void parse(@NonNull InputStream file, Subscriber<? super Track> subscriber) throws IOException;


        /**
         * @return the playlist name
         *
         * Call this after the {@link #parse(InputStream, Subscriber)}}} has finished
         */
        public abstract String getName();
    }

    private interface IdRetriever {

        Track getMediaId(String title_key, String artist_key, String uri);

    }
}
