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

package com.namelessdev.mpdroid.tools;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LibraryTabsUtil {

    public interface TabConfigurationListener {
        void onTabsChanged();
    }

    public static final String TAB_ALBUMS = "albums";

    public static final String TAB_ARTISTS = "artists";

    public static final String TAB_FILES = "files";

    public static final String TAB_GENRES = "genres";

    public static final String TAB_PLAYLISTS = "playlists";

    public static final String TAB_STREAMS = "streams";

    public static final String TAB_RANDOM = "random";

    public static final String TAB_FAVORITES = "favorites";

    private static final MPDApplication APP = MPDApplication.getInstance();

    private static final String LIBRARY_TABS_DELIMITER = "|";

    private static final String DEFAULT_LIBRARY_TABS = TAB_ARTISTS
            + LIBRARY_TABS_DELIMITER + TAB_ALBUMS
            + LIBRARY_TABS_DELIMITER + TAB_PLAYLISTS
            + LIBRARY_TABS_DELIMITER + TAB_STREAMS
            + LIBRARY_TABS_DELIMITER + TAB_FILES
            + LIBRARY_TABS_DELIMITER + TAB_GENRES
            + LIBRARY_TABS_DELIMITER + TAB_RANDOM
            + LIBRARY_TABS_DELIMITER + TAB_FAVORITES;

    private static final String LIBRARY_TABS_SETTINGS_KEY = "currentLibraryTabs";

    private static List<String> CURRENT_TABS;

    private static final HashMap<String, Integer> TABS = new HashMap<>();

    static {
        TABS.put(TAB_ARTISTS, R.string.artists);
        TABS.put(TAB_ALBUMS, R.string.albums);
        TABS.put(TAB_PLAYLISTS, R.string.playlists);
        TABS.put(TAB_STREAMS, R.string.streams);
        TABS.put(TAB_FILES, R.string.files);
        TABS.put(TAB_GENRES, R.string.genres);
        TABS.put(TAB_RANDOM, R.string.random);
        TABS.put(TAB_FAVORITES, R.string.favorites);
    }

    private static final Set<TabConfigurationListener> TAB_CONFIGURATION_LISTENERS =
            new HashSet<>();

    private LibraryTabsUtil() {
    }

    public static Iterable<String> getAllLibraryTabs() {
        return getTabsListFromString(DEFAULT_LIBRARY_TABS);
    }

    public static void addTabConfigurationListener(final TabConfigurationListener listener) {
        TAB_CONFIGURATION_LISTENERS.add(listener);
    }

    public static List<String> getCurrentLibraryTabs() {
        if (CURRENT_TABS == null) {
            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(APP);
            String currentSettings =
                    settings.getString(LIBRARY_TABS_SETTINGS_KEY, DEFAULT_LIBRARY_TABS);

            if (currentSettings.isEmpty()) {
                currentSettings = DEFAULT_LIBRARY_TABS;
                saveLibraryTabsString(DEFAULT_LIBRARY_TABS);
            }

            CURRENT_TABS = getTabsListFromString(currentSettings);

            // remove not existing tabs (in case of an app version downgrade)
            for (int i = CURRENT_TABS.size() - 1; i>=0; i--) {
                if (!TABS.containsKey(CURRENT_TABS.get(i))) {
                    CURRENT_TABS.remove(i);
                }
            }
        }

        return CURRENT_TABS;
    }

    @StringRes
    public static int getTabTitleResId(final String tab) {
        return TABS.get(tab);
    }

    private static List<String> getTabsListFromString(final String tabs) {
        return new ArrayList<>(Arrays.asList(tabs.split('\\'
                + LIBRARY_TABS_DELIMITER)));
    }

    @NonNull
    private static String getTabsStringFromList(final List<String> tabs) {
        final StringBuilder resultTabs;

        if (tabs == null || tabs.isEmpty()) {
            resultTabs = new StringBuilder("");
        } else {
            resultTabs = new StringBuilder(tabs.size() * 10);
            resultTabs.append(tabs.get(0));
            for (int i = 1; i < tabs.size(); i++) {
                resultTabs.append(LIBRARY_TABS_DELIMITER);
                resultTabs.append(tabs.get(i));
            }
        }

        return resultTabs.toString();
    }

    public static void resetLibraryTabs() {
        saveLibraryTabsString(DEFAULT_LIBRARY_TABS);
        notifyTabsChanged();
    }

    public static void saveCurrentLibraryTabs(final List<String> tabs) {
        saveLibraryTabsString(getTabsStringFromList(tabs));
        notifyTabsChanged();
    }

    private static void saveLibraryTabsString(final String tabsString) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(APP);
        settings.edit().putString(LIBRARY_TABS_SETTINGS_KEY, tabsString).commit();
        CURRENT_TABS = null;
    }

    private static void notifyTabsChanged() {
        for (final TabConfigurationListener listener : TAB_CONFIGURATION_LISTENERS) {
            listener.onTabsChanged();
        }
    }
}
