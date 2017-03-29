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

import com.namelessdev.mpdroid.R;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;

import java.util.List;

/**
 * This class lists all connections for choosing to modify a connection settings.
 */
public class ConnectionChooserFragment extends PreferenceFragment
        implements MenuItem.OnMenuItemClickListener {

    private static final int CURRENT = 0;

    private static final int DELETE = 1;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.servers, container, false);

        final AbsListView listView = (AbsListView) view.findViewById(android.R.id.list);
        registerForContextMenu(listView);

        final ImageButton addButton = (ImageButton) view.findViewById(R.id.serversAddButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                ((ConnectionSettingsActivity) getActivity()).onNewServer();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setPreferenceScreen(buildServerPreferencesScreen());
    }

    private PreferenceScreen buildServerPreferencesScreen() {
        final PreferenceScreen screen =
                getPreferenceManager().createPreferenceScreen(getActivity());

        final Preference category = new PreferenceCategory(screen.getContext());
        category.setTitle(R.string.servers);
        screen.addPreference(category);

        final List<ServerSetting> serverSettings = ServerSetting.read();
        for (final ServerSetting serverSetting : serverSettings) {
            addServerPreference(screen, serverSetting);
        }

        return screen;
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenu.ContextMenuInfo menuInfo) {
        final ServerSetting serverSetting = geServerSettingForMenuInfo(menuInfo);

        if (!serverSetting.isCurrent()) {
            menu.add(Menu.NONE, CURRENT, CURRENT, R.string.setCurrent)
                    .setOnMenuItemClickListener(this);
        }
        menu.add(Menu.NONE, DELETE, DELETE, R.string.delete).setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        final ServerSetting serverSetting = geServerSettingForMenuInfo(item.getMenuInfo());

        switch (item.getItemId()) {
            case CURRENT:
                serverSetting.setAsCurrent();
                return true;
            case DELETE:
                serverSetting.delete();
                setPreferenceScreen(buildServerPreferencesScreen());
                return true;
            default:
                return false;
        }
    }

    private ServerSetting geServerSettingForMenuInfo(final ContextMenu.ContextMenuInfo menuInfo) {
        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;

        final int serverIndex =
                info.position - 1; // preference category also counts, but doesn't matter

        return ServerSetting.read().get(serverIndex);
    }

    private void addServerPreference(final PreferenceScreen screen,
            final ServerSetting serverSetting) {
        final Preference serverItem = new Preference(screen.getContext());

        serverItem.setPersistent(false);
        serverItem.setKey(serverSetting.getKeyPrefix());
        serverItem.setTitle(serverSetting.getName());
        serverItem.setSummary(serverSetting.getHost() + ":" + serverSetting.getPort());
        serverItem.getExtras()
                .putParcelable(ConnectionModifierFragment.EXTRA_SERVER_SETTING, serverSetting);
        serverItem.setFragment(ConnectionSettingsActivity.FRAGMENT_MODIFIER_NAME);

        screen.addPreference(serverItem);
    }

}
