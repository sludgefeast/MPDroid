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

package com.namelessdev.mpdroid.fragments;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.LibraryTabsUtil;
import com.namelessdev.mpdroid.ui.ToolbarHelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

abstract class LibraryFragmentBase extends Fragment {

    /**
     * The {@link PagerAdapter} that will provide fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every loaded fragment in memory.
     *
     * If this becomes too memory intensive, it may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        mSectionsPagerAdapter = new SectionsPagerAdapter(context, getChildFragmentManager());
        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.library_tabs_fragment, container, false);
        mViewPager = (ViewPager) view.findViewById(R.id.pager);
        if (mSectionsPagerAdapter != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
        }
        /*mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(final int position) {
                if (mActivity != null) {
                    mActivity.pageChanged(position);
                }
            }
        });*/

        final Resources resources = getResources();
        final TabLayout tabs = (TabLayout) view.findViewById(R.id.tabs);
        tabs.setTabTextColors(resources.getColor(R.color.library_tab_text_color),
                resources.getColor(R.color.library_tab_text_color_selected));
        tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabs.setupWithViewPager(mViewPager);

        final Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.inflateMenu(R.menu.mpd_main_menu);
        ToolbarHelper.addStandardMenuItemClickListener(this, toolbar, null);
        ToolbarHelper.addSearchView(getActivity(), toolbar);
        ToolbarHelper.addRefresh(toolbar);

        return view;
    }

    /*
    public void setCurrentItem(final int item, final boolean smoothScroll) {
        if (mViewPager != null) {
            mViewPager.setCurrentItem(item, smoothScroll);
        }
    }
    */

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    private static final class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private final Context mContext;

        private final Map<Class<?>, String> mFragmentTabs = new HashMap<>();

        /**
         * Sole constructor.
         *
         * @param context The current context for context.
         * @param fm      The fragment manager as required by the {@link FragmentPagerAdapter}.
         */
        private SectionsPagerAdapter(final Context context, final FragmentManager fm) {
            super(fm);

            mContext = context;

            LibraryTabsUtil.addTabConfigurationListener(new LibraryTabsUtil.TabConfigurationListener() {
                @Override
                public void onTabsChanged() {
                    SectionsPagerAdapter.this.notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getCount() {
            return LibraryTabsUtil.getCurrentLibraryTabs().size();
        }

        /**
         * This gets the fragment name, instantiates it and returns the instance.
         *
         * @param tClass The class to instantiate.
         * @param <T>    The class type, always BrowseFragment.
         * @return A fragment instantiation.
         */
        private <T extends BrowseFragment<?>> Fragment createFragment(final Class<T> tClass) {
            final BrowseFragment<?> fragment =
                    (BrowseFragment<?>) Fragment.instantiate(mContext, tClass.getName());
            fragment.setEmbedded(true);
            return fragment;
        }

        @Override
        public Fragment getItem(final int position) {
            final Fragment fragment;
            final String tab = LibraryTabsUtil.getCurrentLibraryTabs().get(position);

            switch (tab) {
                case LibraryTabsUtil.TAB_ALBUMS:
                    final SharedPreferences settings =
                            PreferenceManager.getDefaultSharedPreferences(mContext);

                    if (settings.getBoolean(ArtistsFragment.PREFERENCE_ALBUM_LIBRARY, true)) {
                        fragment = createFragment(AlbumsGridFragment.class);
                    } else {
                        fragment = createFragment(AlbumsFragment.class);
                    }
                    break;
                case LibraryTabsUtil.TAB_ARTISTS:
                    fragment = createFragment(ArtistsFragment.class);
                    break;
                case LibraryTabsUtil.TAB_FILES:
                    fragment = createFragment(FSFragment.class);
                    break;
                case LibraryTabsUtil.TAB_GENRES:
                    fragment = createFragment(GenresFragment.class);
                    break;
                case LibraryTabsUtil.TAB_PLAYLISTS:
                    fragment = createFragment(PlaylistsFragment.class);
                    break;
                case LibraryTabsUtil.TAB_STREAMS:
                    fragment = createFragment(StreamsFragment.class);
                    break;
                default:
                    throw new IllegalStateException("getItem() called with invalid Item.");
            }

            mFragmentTabs.put(fragment.getClass(), tab);

            return fragment;
        }

        @Override
        public int getItemPosition(final Object object) {
            final int index = LibraryTabsUtil.getCurrentLibraryTabs().indexOf(
                    mFragmentTabs.get(object.getClass()));
            return index >= 0 ? index : POSITION_NONE;
        }

        @Override
        public CharSequence getPageTitle(final int position) {
            final String tab = LibraryTabsUtil.getCurrentLibraryTabs().get(position);
            return mContext.getString(LibraryTabsUtil.getTabTitleResId(tab));
        }
    }
}
