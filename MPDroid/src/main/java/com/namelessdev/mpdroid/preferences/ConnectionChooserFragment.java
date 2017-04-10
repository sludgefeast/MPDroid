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
import com.namelessdev.mpdroid.adapters.ArrayAdapter;
import com.namelessdev.mpdroid.adapters.ArrayDataBinder;
import com.namelessdev.mpdroid.ui.MenuButton;
import com.namelessdev.mpdroid.views.holders.ViewHolder;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.List;

/**
 * This class lists all connections for choosing to modify a connection settings.
 */
public class ConnectionChooserFragment extends ListFragment {

    private AbsListView mListView;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.servers, container, false);

        mListView = (AbsListView) view.findViewById(android.R.id.list);
        registerForContextMenu(mListView);

        final ImageButton addButton = (ImageButton) view.findViewById(R.id.serversAddButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final ServerSetting serverSetting = ServerSetting.create();
                ConnectionModifierFragment.edit(serverSetting, getActivity());
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshItems();
    }

    private void refreshItems() {
        mListView.setAdapter(
                new ArrayAdapter<>(this.getActivity(), new ServerSettingDataBinder(),
                        ServerSetting.read()));
    }

    private static class ServerItemTag {

        ServerSetting mServerSetting;

        boolean mIsMenu;

        ServerItemTag(final ServerSetting serverSetting, final boolean isMenu) {
            mServerSetting = serverSetting;
            mIsMenu = isMenu;
        }
    }

    private class ServerSettingDataBinder
            implements ArrayDataBinder<ServerSetting>, View.OnClickListener {

        @Override
        public ViewHolder findInnerViews(final View targetView) {
            final ServerSettingViewHolder viewHolder = new ServerSettingViewHolder();
            viewHolder.mServerIconCurrent = (ImageView) targetView
                    .findViewById(R.id.server_icon_current);
            viewHolder.mServerNameTextView = (TextView) targetView.findViewById(R.id.server_name);
            viewHolder.mServerURLTextView = (TextView) targetView.findViewById(R.id.server_url);
            viewHolder.mMenuButton = (ImageButton) targetView.findViewById(R.id.menu);
            return viewHolder;
        }

        @Override
        public int getLayoutId() {
            return R.layout.server_list_item;
        }

        @Override
        public boolean isEnabled(final int position, final List<ServerSetting> items,
                final Object item) {
            return true;
        }

        @Override
        public void onDataBind(final Context context, final View targetView,
                final ViewHolder viewHolder,
                final List<ServerSetting> items, final ServerSetting item, final int position) {
            final ServerSettingViewHolder holder = (ServerSettingViewHolder) viewHolder;
            holder.mServerIconCurrent
                    .setVisibility(item.isCurrent() ? View.VISIBLE : View.INVISIBLE);

            holder.mServerNameTextView.setText(item.getName());
            holder.mServerNameTextView.setOnClickListener(this);
            holder.mServerNameTextView.setTag(new ServerItemTag(item, false));
            holder.mServerURLTextView.setText(item.getHost() + ":" + item.getPort());
            holder.mServerURLTextView.setOnClickListener(this);
            holder.mServerURLTextView.setTag(new ServerItemTag(item, false));

            MenuButton.tint(holder.mMenuButton, ConnectionChooserFragment.this.getResources());
            holder.mMenuButton.setOnClickListener(this);
            holder.mMenuButton.setTag(new ServerItemTag(item, true));
        }

        @Override
        public View onLayoutInflation(final Context context, final View targetView,
                final List<ServerSetting> items) {
            return targetView;
        }

        @Override
        public void onClick(final View v) {
            final ServerItemTag serverItemTag = (ServerItemTag) v.getTag();

            if (serverItemTag.mIsMenu) {
                final PopupMenu popupMenu = new ServerPopupMenu(serverItemTag.mServerSetting, v);
                popupMenu.show();
            } else {
                ConnectionModifierFragment
                        .edit(serverItemTag.mServerSetting,
                                ConnectionChooserFragment.this.getActivity());
            }
        }
    }

    private static class ServerSettingViewHolder implements ViewHolder {

        ImageView mServerIconCurrent;

        TextView mServerNameTextView;

        TextView mServerURLTextView;

        ImageButton mMenuButton;

    }

    private class ServerPopupMenu extends PopupMenu implements PopupMenu.OnMenuItemClickListener {

        private final ServerSetting mServerSetting;

        ServerPopupMenu(final ServerSetting serverSetting, final View anchor) {
            super(ConnectionChooserFragment.this.getActivity(), anchor);

            mServerSetting = serverSetting;

            getMenuInflater().inflate(R.menu.mpd_servermenu, getMenu());
            if (mServerSetting.isCurrent()) {
                getMenu().findItem(R.id.server_current).setVisible(false);
            }
            setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(final MenuItem item) {
            switch (item.getItemId()) {
                case R.id.server_current:
                    mServerSetting.setAsCurrent();
                    refreshItems();
                    return true;
                case R.id.server_delete:
                    mServerSetting.delete();
                    refreshItems();
                    return true;
                default:
                    return false;
            }
        }
    }

}
