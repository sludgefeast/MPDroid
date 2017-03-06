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

package com.namelessdev.mpdroid.preferences;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.text.format.Formatter;
import android.util.Log;

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.subsystem.status.MPDStatistics;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.SearchRecentProvider;
import com.namelessdev.mpdroid.cover.CoverManager;
import com.namelessdev.mpdroid.cover.retriever.CachedCover;
import com.namelessdev.mpdroid.tools.Tools;

import java.io.IOException;

public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = "SettingsFragment";

    private final MPDApplication mApp = MPDApplication.getInstance();

    private Preference mAlbums;

    private Preference mArtists;

    private Preference mSongs;

    private Preference mVersion;

    private Preference mCacheUsage1;

    private Preference mCacheUsage2;

    private Handler mHandler;

    private PreferenceScreen mInformationScreen;

    public void onConnectionStateChanged() {
        final MPD mpd = mApp.getMPD();
        final boolean isConnected = mpd.isConnected();

        mInformationScreen.setEnabled(isConnected);

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
        addPreferencesFromResource(R.xml.settings);

        mHandler = new Handler();

        mInformationScreen = (PreferenceScreen) findPreference("informationScreen");

        if (!getResources().getBoolean(R.bool.isTablet)) {
            final PreferenceScreen interfaceCategory = (PreferenceScreen) findPreference(
                    "nowPlayingScreen");
            interfaceCategory.removePreference(findPreference("tabletUI"));
        }

        mVersion = findPreference("version");
        mArtists = findPreference("artists");
        mAlbums = findPreference("albums");
        mSongs = findPreference("songs");

        // Small seekbars don't work on lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findPreference("smallSeekbars").setEnabled(false);
        }

        mCacheUsage1 = findPreference("cacheUsage1");
        mCacheUsage2 = findPreference("cacheUsage2");

        final CheckBoxPreference lightTheme = (CheckBoxPreference) findPreference("lightTheme");
        lightTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                Tools.resetActivity(getActivity());
                return true;
            }
        });

        /** Allow these to be changed individually, pauseOnPhoneStateChange might be overridden. */
        final CheckBoxPreference phonePause = (CheckBoxPreference) findPreference(
                "pauseOnPhoneStateChange");
        final CheckBoxPreference phoneStateChange = (CheckBoxPreference) findPreference(
                "playOnPhoneStateChange");

        refreshDynamicFields();

        findPreference("ratings_favorites").setEnabled(
                mApp.getMPD().getStickerManager().isAvailable());
    }

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
                                         @NonNull final Preference preference) {
        // Is it the connection screen which is called?
        if (preference.getKey() == null) {
            return false;
        }

        if ("refreshMPDDatabase".equals(preference.getKey())) {
            try {
                mApp.getMPD().refreshDatabase();
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to refresh the database.", e);
            }
            return true;
        }

        if ("clearLocalCoverCache".equals(preference.getKey())) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.clearLocalCoverCache)
                    .setMessage(R.string.clearLocalCoverCachePrompt)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            // Todo : The covermanager must already have been
                            // initialized, get rid of the getInstance arguments
                            CoverManager.getInstance().clear();
                            mCacheUsage1.setSummary("0.00B");
                            mCacheUsage2.setSummary("0.00B");
                        }
                    })
                    .setNegativeButton(R.string.cancel, Tools.NOOP_CLICK_LISTENER)
                    .show();
            return true;
        }

        if ("pauseOnPhoneStateChange".equals(preference.getKey())) {
            /**
             * Allow these to be changed individually,
             * pauseOnPhoneStateChange might be overridden.
             */
            final CheckBoxPreference phonePause = (CheckBoxPreference) findPreference(
                    "pauseOnPhoneStateChange");
            final CheckBoxPreference phoneStateChange = (CheckBoxPreference) findPreference(
                    "playOnPhoneStateChange");
        } else if ("clearSearchHistory".equals(preference.getKey())) {
            final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
                    SearchRecentProvider.AUTHORITY, SearchRecentProvider.MODE);
            suggestions.clearHistory();
            preference.setEnabled(false);
        }

        return false;
    }

    private void refreshDynamicFields() {
        if (getActivity() == null) {
            return;
        }
        final long size = new CachedCover().getCacheUsage();
        final String usage = Formatter.formatFileSize(mApp, size);
        mCacheUsage1.setSummary(usage);
        mCacheUsage2.setSummary(usage);
        onConnectionStateChanged();
    }
}
