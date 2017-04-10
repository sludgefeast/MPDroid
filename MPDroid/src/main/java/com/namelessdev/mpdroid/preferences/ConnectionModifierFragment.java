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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

/**
 * This class is the Fragment used to configure a specific connection.
 */
public class ConnectionModifierFragment extends PreferenceFragment {

    /**
     * This is the Bundle extra used to start this Fragment.
     */
    public static final String EXTRA_SERVER_SETTING = "SERVER_SETTING";

    private static final String KEY_CONNECTION_CATEGORY = "connectionCategory";

    private ServerSetting mServerSetting;

    static void edit(final ServerSetting serverSetting, final Activity activity) {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(ConnectionModifierFragment.EXTRA_SERVER_SETTING, serverSetting);
        final Fragment fragment = Fragment.instantiate(activity,
                ConnectionModifierFragment.class.getName(), bundle);

        final FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(android.R.id.content, fragment)
                .addToBackStack(serverSetting.getName())
                .commit();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mServerSetting = getArguments().getParcelable(EXTRA_SERVER_SETTING);
        if (mServerSetting == null) {
            throw new IllegalStateException("Server Setting must not be null.");
        }

        buildScreen();
    }

    @Override
    public void onDestroy() {
        if (ServerSetting.current() == null) {
            mServerSetting.setAsCurrent();
        }

        super.onDestroy();
    }

    private void buildScreen() {
        final PreferenceScreen screen =
                getPreferenceManager().createPreferenceScreen(getActivity());
        final Context context = screen.getContext();

        screen.setKey(KEY_CONNECTION_CATEGORY);
        screen.addPreference(mServerSetting.getNamePreference(context));
        screen.addPreference(mServerSetting.getHostPreference(context));
        screen.addPreference(mServerSetting.getPortPreference(context));
        screen.addPreference(mServerSetting.getPasswordPreference(context));
        screen.addPreference(mServerSetting.getStreamURLPreference(context));
        screen.addPreference(mServerSetting.getPersistentNotificationPreference(context));
        screen.addPreference(mServerSetting.getMusicPathPreference(context));
        screen.addPreference(mServerSetting.getCoverFilenamePreference(context));

        setPreferenceScreen(screen);
    }
}