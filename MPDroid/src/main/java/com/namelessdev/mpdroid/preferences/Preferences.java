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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.namelessdev.mpdroid.MPDApplication;

public final class Preferences {

    /**
     * Preference key of the activation of favorites.
     */
    private static final String PREFERENCE_USE_FAVORITE = "useFavorites";

    /**
     * Preference key of the personalization key.
     */
    private static final String PREFERENCE_FAVORITE_KEY = "favoriteKey";

    private Preferences() {
    }

    /**
     * Are favorites activated in preferences?
     *
     * @return true, if activated.
     */
    public static boolean areFavoritesActivated() {
        final SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(MPDApplication.getInstance());
        return settings.getBoolean(PREFERENCE_USE_FAVORITE, false);
    }

    public static String favoritesPersonalizationKey() {
        final SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(MPDApplication.getInstance());
        return settings.getString(PREFERENCE_FAVORITE_KEY, "").trim();
    }

}
