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

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.SearchRecentProvider;
import com.namelessdev.mpdroid.cover.CoverManager;
import com.namelessdev.mpdroid.cover.retriever.CachedCover;
import com.namelessdev.mpdroid.helpers.CachedMPD;
import com.namelessdev.mpdroid.tools.Tools;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.text.format.Formatter;

public class SettingsFragment extends PreferenceFragment {

    private final MPDApplication mApp = MPDApplication.getInstance();

    private Preference mCacheUsage2;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        if (!getResources().getBoolean(R.bool.isTablet)) {
            final PreferenceScreen interfaceCategory = (PreferenceScreen) findPreference(
                    "nowPlayingScreen");
            interfaceCategory.removePreference(findPreference("tabletUI"));
        }

        // Small seekbars don't work on lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findPreference("smallSeekbars").setEnabled(false);
        }

        mCacheUsage2 = findPreference("cacheUsage2");

        final CheckBoxPreference lightTheme = (CheckBoxPreference) findPreference("lightTheme");
        lightTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                Tools.resetActivity(getActivity());
                return true;
            }
        });

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

        if ("clearLocalCoverCache".equals(preference.getKey())) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.clearLocalCoverCache)
                    .setMessage(R.string.clearLocalCoverCachePrompt)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            CoverManager.getInstance().clear();
                            mCacheUsage2.setSummary("0.00B");
                        }
                    })
                    .setNegativeButton(R.string.cancel, Tools.NOOP_CLICK_LISTENER)
                    .show();
            return true;
        }

        if ("clearLocalAlbumCache".equals(preference.getKey())) {
            try {
                CachedMPD cMPD = (CachedMPD) mApp.getMPD();
                cMPD.clearCache();
            } catch (ClassCastException e) {
                // not album-cached
            }
            return true;
        }

        if ("clearSearchHistory".equals(preference.getKey())) {
            final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
                    SearchRecentProvider.AUTHORITY, SearchRecentProvider.MODE);
            suggestions.clearHistory();
            preference.setEnabled(false);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void refreshDynamicFields() {
        if (getActivity() == null) {
            return;
        }
        final long size = new CachedCover().getCacheUsage();
        final String usage = Formatter.formatFileSize(mApp, size);
        mCacheUsage2.setSummary(usage);
    }

}
