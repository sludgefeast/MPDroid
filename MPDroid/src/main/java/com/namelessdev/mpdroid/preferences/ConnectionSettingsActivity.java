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

import com.namelessdev.mpdroid.MPDActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

import android.app.AlertDialog;
import android.app.Fragment;
import android.os.Bundle;

/**
 * This Activity is used to modify connection preferences.
 */
public class ConnectionSettingsActivity extends MPDActivity {

    /**
     * This method checks to see if preferences have been setup for this application previously.
     *
     * @return True if preferences haven't been setup for this application, false otherwise.
     */
    private boolean hasEmptyPreferences() {
        return ServerSetting.read().isEmpty();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.servers);

        if (hasEmptyPreferences()) {
            final ServerSetting serverSetting = ServerSetting.create();
            ConnectionModifierFragment.edit(serverSetting, this);
            showWelcomeAlert();
        } else {
            final Fragment fragment =
                    Fragment.instantiate(this, ConnectionChooserFragment.class.getName());
            getFragmentManager().beginTransaction().replace(android.R.id.content, fragment)
                    .commit();
        }
    }

    private void showWelcomeAlert() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.warningText1);
        builder.setPositiveButton(R.string.ok, Tools.NOOP_CLICK_LISTENER);
        builder.show();
    }

}
