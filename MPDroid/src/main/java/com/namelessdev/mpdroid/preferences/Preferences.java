/*
 * Copyright (C) 2010-2017 The MPDroid Project
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

import com.anpmech.mpd.Log;
import com.namelessdev.mpdroid.BuildConfig;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.preferences.upgrade.PreferenceUpgrader;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class Preferences {

    private static final String PREFERENCE_KEY_ENABLE_ALBUM_ART_LIBRARY = "enableAlbumArtLibrary";

    /**
     * Preference key of the personalization key.
     */
    private static final String PREFERENCE_KEY_RATING_KEY = "ratingKey";

    /**
     * Preference key of the activation of favorites.
     */
    private static final String PREFERENCE_KEY_USE_FAVORITE = "useFavorites";

    /**
     * Preference key of the personalization key.
     */
    private static final String PREFERENCE_KEY_FAVORITE_KEY = "favoriteKey";

    private static final String PREFERENCE_KEY_PREFERENCES_VERSION = "preferencesVersion";

    private static final String TAG = "Preferences";

    private Preferences() {
    }

    public static boolean isAlbumArtLibraryEnabled() {
        return readBoolean(PREFERENCE_KEY_ENABLE_ALBUM_ART_LIBRARY, true);
    }

    public static String ratingsPersonalizationKey() {
        return preferences().getString(PREFERENCE_KEY_RATING_KEY, "").trim();
    }

    /**
     * Are favorites activated in preferences?
     *
     * @return true, if activated.
     */
    public static boolean areFavoritesActivated() {
        return preferences().getBoolean(PREFERENCE_KEY_USE_FAVORITE, false);
    }

    public static String favoritesPersonalizationKey() {
        return preferences().getString(PREFERENCE_KEY_FAVORITE_KEY, "").trim();
    }

    public static boolean readBoolean(final String key, final boolean defaultValue) {
        return preferences().getBoolean(key, defaultValue);
    }

    private static SharedPreferences preferences() {
        return PreferenceManager.getDefaultSharedPreferences(MPDApplication.getInstance());
    }

    public static void upgrade() {
        final SharedPreferences preferences = preferences();
        final int preferencesVersion = preferences.getInt(PREFERENCE_KEY_PREFERENCES_VERSION, 0);

        final List<PreferenceUpgrader> upgraders = readPreferenceUpgraders(preferencesVersion);
        if (upgraders.isEmpty()) {
            return;
        }

        final SharedPreferences.Editor editor = preferences.edit();

        for (final PreferenceUpgrader upgrader : upgraders) {
            upgrader.upgrade(preferences, editor);
        }
        editor.putInt(PREFERENCE_KEY_PREFERENCES_VERSION, BuildConfig.VERSION_CODE);

        editor.apply();
    }

    private static List<PreferenceUpgrader> readPreferenceUpgraders(final int preferencesVersion) {
        final List<PreferenceUpgrader> availableUpgraders = findAvailableUpgraders();

        final List<PreferenceUpgrader> upgraders = new ArrayList<>();

        for (final PreferenceUpgrader upgrader : availableUpgraders) {
            if (upgrader.getBasedAppVersionCode() >= preferencesVersion) {
                upgraders.add(upgrader);
            }
        }

        Collections.sort(upgraders, new Comparator<PreferenceUpgrader>() {
            @Override
            public int compare(final PreferenceUpgrader lhs, final PreferenceUpgrader rhs) {
                return lhs.getBasedAppVersionCode() - rhs.getBasedAppVersionCode();
            }
        });

        return upgraders;
    }

    private static List<PreferenceUpgrader> findAvailableUpgraders() {
        final List<PreferenceUpgrader> upgraders = new ArrayList<>();

        for (final Class<? extends PreferenceUpgrader> upgraderClass :
                PreferenceUpgrader.UPGRADER_CLASSES) {
            try {
                upgraders.add(upgraderClass.newInstance());
            } catch (final InstantiationException e) {
                Log.error(TAG, "Unable to create an instance of " + upgraderClass.getName(), e);
            } catch (final IllegalAccessException e) {
                Log.error(TAG, "Unable to create an instance of " + upgraderClass.getName(), e);
            }
        }

        return upgraders;
    }

}
