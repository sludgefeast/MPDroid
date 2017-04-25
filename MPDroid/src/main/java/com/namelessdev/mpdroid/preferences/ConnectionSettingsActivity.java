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

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

import android.app.AlertDialog;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.StyleRes;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextThemeWrapper;

/**
 * This Activity is used to modify connection preferences.
 */
public class ConnectionSettingsActivity extends AppCompatActivity {

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

    /**
     * This method overrides {@link ContextThemeWrapper#setTheme(int)} to use
     * {@link #getThemeResId()}.
     *
     * @param resid The resource ID for the current theme.
     */
    @Override
    public void setTheme(final int resid) {
        super.setTheme(getThemeResId());
    }

    /**
     * This method returns the current theme resource ID.
     *
     * @return The current theme resource ID.
     */
    @StyleRes
    private int getThemeResId() {
        return isLightThemeSelected() ? R.style.AppTheme_Light : R.style.AppTheme;
    }

    private boolean isLightThemeSelected() {
        return Tools.isLightThemeSelected(this);
    }
}
