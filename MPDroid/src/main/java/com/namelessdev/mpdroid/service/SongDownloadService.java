/*
 * Copyright (C) 2010-2016 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.anpmech.mpd.item.Item;
import com.anpmech.mpd.item.Music;
import com.namelessdev.mpdroid.LocalWebServer;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SongDownloadService extends IntentService {

    public interface SongProvider<T extends Item<T>> {
        Collection<Music> provideSongs(T item);
    }

    private static final String TAG = "SongDownloadService";

    private static final String MUSIC_DIRECTORY =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/";

    private static final String TITLE = "TITLE";

    private static final String WEB_SERVER = "WEB_SERVER";

    private static final String SONG_PATHS = "SONG_PATHS";

    private static final String CANCEL =
            "com.namelessdev.mpdroid.service.SongDownloadService.CANCEL";

    private static final String DOWNLOAD_ID = "DOWNLOAD_ID";

    private static SongDownloadService INSTANCE;

    private NotificationManager mNotificationManager;

    private int mDownloadCount = 0;

    private Set<Integer> mCancelledDownloads;

    public SongDownloadService() {
        super("SongDownloaderService");
    }

    public static <T extends Item<T>> void download(final Context context,
                                                    final LocalWebServer localWebServer,
                                                    final T item,
                                                    final SongProvider<T> songProvider) {
        Tools.notifyUser(R.string.downloadItem, item.getName());
        new MusicCollectionTask<>(context, localWebServer, songProvider).execute(item);
    }

    static void start(final Context context, final String title,
                      final LocalWebServer localWebServer,
                      final Collection<Music> songs) {
        final Intent intent = new Intent(context, SongDownloadService.class);

        intent.putExtra(TITLE, title);
        intent.putExtra(WEB_SERVER, localWebServer);

        final ArrayList<String> songsPaths = new ArrayList<>();
        for (final Music song : songs) {
            songsPaths.add(song.getFullPath());
        }
        intent.putExtra(SONG_PATHS, songsPaths);

        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mCancelledDownloads = new HashSet<>();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        INSTANCE = this;
    }

    @Override
    public void onDestroy() {
        INSTANCE = null;

        mDownloadCount = 0;
        mNotificationManager = null;
        mCancelledDownloads = null;

        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final int downloadID = ++mDownloadCount;

        final String title = intent.getStringExtra(TITLE);

        final LocalWebServer localWebServer =
                (LocalWebServer) intent.getSerializableExtra(WEB_SERVER);
        final List<String> songsPaths = (List<String>) intent.getSerializableExtra(SONG_PATHS);

        final Intent cancelIntent = new Intent(this, CancelActionReceiver.class);
        cancelIntent.setAction(CANCEL).putExtra(DOWNLOAD_ID, downloadID);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(this).
                        setOngoing(true).
                        setSmallIcon(R.drawable.icon_notification).
                        setContentTitle(Tools.getString(R.string.downloadItem, title)).
                        setContentText(Tools.getString(R.string.downloadInProgress)).
                        addAction(android.R.drawable.ic_menu_close_clear_cancel,
                                Tools.getString(R.string.cancel), pendingIntent);

        try {
            int i = 0;
            for (final String songPath : songsPaths) {
                notificationCompatBuilder.setProgress(songsPaths.size(), i++, false);
                mNotificationManager.notify(downloadID, notificationCompatBuilder.build());
                if (isCancelled(downloadID)) {
                    notificationCompatBuilder.setContentText(
                            Tools.getString(R.string.downloadCancelled));
                    return;
                }
                final String errorMessage = downloadSong(downloadID, localWebServer, songPath);
                if (errorMessage != null) {
                    notificationCompatBuilder.setContentText(errorMessage);
                    return;
                }
            }

            notificationCompatBuilder.setContentText(Tools.getString(R.string.downloadCompleted));

        } finally {
            mCancelledDownloads.remove(downloadID);

            final NotificationCompat.Builder finishedNotificationCompatBuilder =
                    new NotificationCompat.Builder(this).
                            setSmallIcon(R.drawable.icon_notification).
                            setContentTitle(notificationCompatBuilder.mContentTitle).
                            setContentText(notificationCompatBuilder.mContentText);
            mNotificationManager.notify(downloadID, finishedNotificationCompatBuilder.build());
        }
    }

    private String downloadSong(final int downloadID,
                                final LocalWebServer localWebServer,
                                final String songPath) {
        final File downloadDestination = new File(MUSIC_DIRECTORY + songPath);
        downloadDestination.getParentFile().mkdirs();

        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {

            final String url = localWebServer.buildUrl(songPath);
            Log.i(TAG, "Downloading ... " + url);

            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                final String error = "HTTP Error " + connection.getResponseCode() + " " +
                        connection.getResponseMessage();
                Log.e(TAG, error);
                return error;
            }

            input = connection.getInputStream();
            output = new FileOutputStream(downloadDestination);

            final byte data[] = new byte[4096];
            int count;
            while ((count = input.read(data)) != -1) {
                if (isCancelled(downloadID)) {
                    downloadDestination.delete();
                    return Tools.getString(R.string.downloadCancelled);
                }
                output.write(data, 0, count);
            }
            Log.i(TAG, "Downloaded " + url);
            return null;

        } catch (final Exception e) {
            Log.e(TAG, "Exception while download songs", e);
            return e.getLocalizedMessage();
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
            } catch (final IOException ignored) {
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean isCancelled(final int downloadID) {
        return mCancelledDownloads.contains(downloadID);
    }

    private static class MusicCollectionTask<ItemT extends Item<ItemT>>
            extends AsyncTask<ItemT, Integer, Void> {

        private final Context mContext;

        private final LocalWebServer mLocalWebServer;

        private final SongProvider<ItemT> mSongProvider;

        MusicCollectionTask(final Context context,
                            final LocalWebServer localWebServer,
                            final SongProvider<ItemT> songProvider) {
            mContext = context;
            mLocalWebServer = localWebServer;
            mSongProvider = songProvider;
        }

        @Override
        protected Void doInBackground(final ItemT... items) {
            final ItemT item = items[0];
            final Collection<Music> songs = mSongProvider.provideSongs(item);
            if (songs != null && !songs.isEmpty()) {
                SongDownloadService.start(mContext, item.getName(), mLocalWebServer, songs);
            }
            return null;
        }
    }

    public static class CancelActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            try {
                INSTANCE.mCancelledDownloads.add(intent.getExtras().getInt(DOWNLOAD_ID));
            } catch (final NullPointerException ignore) {
            }
        }
    }

}
