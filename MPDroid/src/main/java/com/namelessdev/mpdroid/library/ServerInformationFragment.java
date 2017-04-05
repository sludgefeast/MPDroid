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

package com.namelessdev.mpdroid.library;

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.subsystem.status.MPDStatistics;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.cover.retriever.CachedCover;
import com.namelessdev.mpdroid.preferences.ServerSetting;

import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.format.Formatter;
import android.util.Log;

import java.io.IOException;

public class ServerInformationFragment extends PreferenceFragment {

    private static final String TAG = "ServerInfoFragment";

    private static final String KEY_REFRESH_MPD_DATABASE = "refreshMPDDatabase";

    private final MPDApplication mApp = MPDApplication.getInstance();

    private Preference mServer;

    private Preference mAlbums;

    private Preference mArtists;

    private Preference mSongs;

    private Preference mVersion;

    private Preference mCacheUsage;

    private Handler mHandler;

    public void onConnectionStateChanged() {
        final MPD mpd = mApp.getMPD();
        final boolean isConnected = mpd.isConnected();

        if (isConnected) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mpd.getStatistics().waitForValidity();
                    } catch (final InterruptedException ignored) {
                    }

                    final String versionText = mpd.getMpdVersion();
                    final MPDStatistics mpdStatistics = mpd.getStatistics();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mVersion.setSummary(versionText);
                            mArtists.setSummary(String.valueOf(mpdStatistics.getArtists()));
                            mAlbums.setSummary(String.valueOf(mpdStatistics.getAlbums()));
                            mSongs.setSummary(String.valueOf(mpdStatistics.getSongs()));
                            //TODO: display all statistics
                        }
                    });
                }
            }).start();
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        setPreferenceScreen(buildPreferencesScreen());
    }

    private PreferenceScreen buildPreferencesScreen() {
        final PreferenceScreen screen =
                getPreferenceManager().createPreferenceScreen(getActivity());

        mServer = new Preference(screen.getContext());
        screen.addPreference(mServer);

        final PreferenceScreen resetScreen = getPreferenceManager()
                .createPreferenceScreen(screen.getContext());
        resetScreen.setKey(KEY_REFRESH_MPD_DATABASE);
        resetScreen.setTitle(R.string.updateDBDetails);
        screen.addPreference(resetScreen);

        final Preference category = new PreferenceCategory(screen.getContext());
        category.setTitle(R.string.statistics);
        screen.addPreference(category);

        mVersion = addPreference(screen, "version", R.string.version);
        mArtists = addPreference(screen, "artists", R.string.artists);
        mAlbums = addPreference(screen, "albums", R.string.albums);
        mSongs = addPreference(screen, "songs", R.string.songs);

        mCacheUsage = addPreference(screen, "cacheUsage", R.string.cacheUsage);

        return screen;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDynamicFields();
    }

    private Preference addPreference(final PreferenceScreen screen, final String key,
            @StringRes int titleResId) {
        final Preference preference = new Preference(screen.getContext());

        preference.setPersistent(false);
        preference.setKey(key);
        preference.setTitle(titleResId);

        screen.addPreference(preference);

        return preference;
    }

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            @NonNull final Preference preference) {
        if (KEY_REFRESH_MPD_DATABASE.equals(preference.getKey())) {
            try {
                mApp.getMPD().refreshDatabase();
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to refresh the database.", e);
            }
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void refreshDynamicFields() {
        if (getActivity() == null) {
            return;
        }

        final ServerSetting current = ServerSetting.current();
        mServer.setTitle(current != null ? current.getName() : "");
        mServer.setSummary(current != null ? current.getHost() + ":" + current.getPort() : "");

        final long size = new CachedCover().getCacheUsage();
        final String usage = Formatter.formatFileSize(mApp, size);
        mCacheUsage.setSummary(usage);
        onConnectionStateChanged();
    }

}
