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

package com.namelessdev.mpdroid.preferences.upgrade;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class ConnectionPreferenceUpgrader implements PreferenceUpgrader {

    private static final String KEY_HOSTNAME = "hostname";

    private static final String KEY_SERVERNAME = "servername";

    private static final String KEY_SERVERHOST = "serverhost";

    private static final String KEY_PASSWORD = "password";

    private static final String KEY_PERSISTENT_NOTIFICATION = "persistentNotification";

    private static final String KEY_PORT = "port";

    private static final String KEY_STREAM_URL = "streamUrl";

    private static final String KEY_MUSIC_PATH = "musicPath";

    private static final String KEY_COVER_FILENAME = "coverFileName";

    private static final String KEY_SERVER_IDS = "serverIDs";

    private static final String KEY_CURRENT_SERVER_ID = "currentServerID";

    @Override
    public int getBasedAppVersionCode() {
        return 58;
    }

    @Override
    public void upgrade(final SharedPreferences preferences,
            final SharedPreferences.Editor editor) {

        final List<Integer> serverIDs = new ArrayList<>();
        for (final String key : preferences.getAll().keySet()) {
            if (key.endsWith(KEY_HOSTNAME)) {
                final String ssid = key.substring(0, key.indexOf(KEY_HOSTNAME));
                final Integer id = serverIDs.size() + 1;
                serverIDs.add(id);
                migrateServerDefinition(preferences, editor, ssid, id);
            }
        }

        editor.remove(KEY_MUSIC_PATH);
        editor.remove(KEY_COVER_FILENAME);

        setServerIDs(editor, serverIDs);
    }

    private void migrateServerDefinition(final SharedPreferences preferences,
            final SharedPreferences.Editor editor, final String ssid, final Integer serverID) {

        editor.putString(getKeyPrefix(serverID) + KEY_SERVERNAME,
                ssid.isEmpty() ? "Default" : ssid);

        editor.putString(getKeyPrefix(serverID) + KEY_SERVERHOST,
                preferences.getString(ssid + KEY_HOSTNAME, ""));
        editor.remove(ssid + KEY_HOSTNAME);

        editor.putString(getKeyPrefix(serverID) + KEY_PASSWORD,
                preferences.getString(ssid + KEY_PASSWORD, ""));
        editor.remove(ssid + KEY_PASSWORD);

        editor.putBoolean(getKeyPrefix(serverID) + KEY_PERSISTENT_NOTIFICATION,
                preferences.getBoolean(ssid + KEY_PERSISTENT_NOTIFICATION, false));
        editor.remove(ssid + KEY_PERSISTENT_NOTIFICATION);

        editor.putInt(getKeyPrefix(serverID) + KEY_PORT,
                preferences.getInt(ssid + KEY_PORT, 0));
        editor.remove(ssid + KEY_PORT);

        editor.putString(getKeyPrefix(serverID) + KEY_STREAM_URL,
                preferences.getString(ssid + KEY_STREAM_URL, ""));
        editor.remove(ssid + KEY_STREAM_URL);

        editor.putString(getKeyPrefix(serverID) + KEY_MUSIC_PATH,
                preferences.getString(KEY_MUSIC_PATH, ""));

        editor.putString(getKeyPrefix(serverID) + KEY_COVER_FILENAME,
                preferences.getString(KEY_COVER_FILENAME, ""));

        if (ssid.isEmpty()) {
            editor.putInt(KEY_CURRENT_SERVER_ID, serverID);
        }
    }

    private static String getKeyPrefix(final Integer id) {
        return "Server" + id;
    }

    private void setServerIDs(final SharedPreferences.Editor editor,
            final List<Integer> serverIDs) {
        final StringBuilder sb = new StringBuilder();
        for (final Integer id : serverIDs) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(id);
        }
        editor.putString(KEY_SERVER_IDS, sb.toString());
    }
}
