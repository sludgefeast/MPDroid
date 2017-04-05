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

package com.namelessdev.mpdroid.ui;


import com.anpmech.mpd.subsystem.AudioOutput;
import com.namelessdev.mpdroid.AboutActivity;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ServerInformationActivity;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;
import com.namelessdev.mpdroid.preferences.SettingsActivity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;

public class Toolbar extends android.support.v7.widget.Toolbar {

    public Toolbar(final Context context) {
        super(context);
    }

    public Toolbar(final Context context,
            @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public Toolbar(final Context context, @Nullable final AttributeSet attrs,
            final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void addRefresh() {
        inflateMenu(R.menu.mpd_refreshmenu);
    }

    public void addSearchView(final Activity activity) {
        inflateMenu(R.menu.mpd_searchmenu);
        // Don't catch everything, we'd rather have a crash than an unusuable search field
        SearchView searchView = (SearchView) getMenu().findItem(R.id.menu_search)
                .getActionView();
        manuallySetupSearchView(activity, searchView);
    }

    public void addStandardMenuItemClickListener(final Fragment fragment,
            final android.support.v7.widget.Toolbar.OnMenuItemClickListener chainedListener) {
        setOnMenuItemClickListener(new android.support.v7.widget.Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem menuItem) {
                if (chainedListener != null) {
                    boolean chainedListenerResult = chainedListener.onMenuItemClick(menuItem);
                    if (chainedListenerResult) {
                        return true;
                    }
                }

                final Activity activity = fragment.getActivity();
                if (activity != null) {
                    standardOnMenuItemClick(activity, menuItem);
                }

                return false;
            }
        });
    }

    public void addStandardMenuItemClickListener(final Activity activity,
            final android.support.v7.widget.Toolbar.OnMenuItemClickListener chainedListener) {
        setOnMenuItemClickListener(new android.support.v7.widget.Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem menuItem) {
                if (chainedListener != null) {
                    boolean chainedListenerResult = chainedListener.onMenuItemClick(menuItem);
                    if (chainedListenerResult) {
                        return true;
                    }
                }

                if (activity != null) {
                    standardOnMenuItemClick(activity, menuItem);
                }

                return false;
            }
        });
    }

    public void hideBackButton() {
        setNavigationIcon(null);
        setNavigationOnClickListener(null);
    }

    public static void manuallySetupSearchView(final Activity activity,
            final SearchView searchView) {
        SearchManager searchManager = (SearchManager) activity
                .getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
    }

    /**
     * Make the toolbar show a "back" button. Use this when you have the toolbar inside a fragment.
     *
     * @param fragment The fragment to get the current {@link Activity} from.
     */
    public void showBackButton(final Fragment fragment) {
        showBackButton(fragment.getActivity());
    }

    /**
     * Make the toolbar show a "back" button. Use this when you have the toolbar inside an
     * activity.
     *
     * @param activity The activity used to set the back button action.
     */
    private void showBackButton(final Activity activity) {
        setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity != null) {
                    activity.onBackPressed();
                }
            }
        });
    }

    private boolean standardOnMenuItemClick(final Context context, final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_server_info:
                context.startActivity(new Intent(context, ServerInformationActivity.class));
                return true;
            case R.id.menu_outputs:
                final Intent outputIntent = new Intent(context,
                        SimpleLibraryActivity.class);
                outputIntent.putExtra(AudioOutput.EXTRA, "1");
                context.startActivity(outputIntent);
                return true;
            case R.id.menu_refresh:
                LocalBroadcastManager.getInstance(MPDApplication.getInstance()).sendBroadcast(
                        new Intent(MPDApplication.INTENT_ACTION_REFRESH));
                return true;
            case R.id.menu_settings:
                context.startActivity(new Intent(context, SettingsActivity.class));
                return true;
            case R.id.menu_about:
                context.startActivity(new Intent(context, AboutActivity.class));
                return true;
            default:
                return false;
        }
    }
}
