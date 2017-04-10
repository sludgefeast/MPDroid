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

package com.namelessdev.mpdroid.fragments;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.preferences.Preferences;
import com.namelessdev.mpdroid.tools.LibraryTabsUtil;
import com.namelessdev.mpdroid.ui.Toolbar;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class LibraryFragmentBase extends Fragment {

    /**
     * The {@link PagerAdapter} that will provide fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every loaded fragment in memory.
     * <p>
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

        final Resources resources = getResources();
        final TabLayout tabs = (TabLayout) view.findViewById(R.id.tabs);
        tabs.setTabTextColors(resources.getColor(R.color.library_tab_text_color),
                resources.getColor(R.color.library_tab_text_color_selected));
        tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabs.setupWithViewPager(mViewPager);

        final Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.inflateMenu(R.menu.mpd_main_menu);
        toolbar.addStandardMenuItemClickListener(this, null);
        toolbar.addSearchView(getActivity());
        toolbar.addRefresh();

        return view;
    }

    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to one of the
     * primary sections of the app.
     */
    private static final class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private static final String TAG = "SectionsPagerAdapter";

        private final Context mContext;

        /**
         * Mapping from Fragment Class name to tab name to achieve reordering tabs.
         */
        private final Map<Class<?>, String> mFragmentTabs = new HashMap<>();

        /**
         * Access to private field {@link FragmentStatePagerAdapter#mFragments} to achieve
         * reordering tabs.
         */
        private final List<Fragment> mFragments;

        /**
         * Access to private field {@link FragmentStatePagerAdapter#mSavedState} to achieve
         * reordering tabs.
         */
        private final List<Fragment.SavedState> mSavedState;

        /**
         * Sole constructor.
         *
         * @param context The current context for context.
         * @param fm      The fragment manager as required by the {@link FragmentStatePagerAdapter}.
         */
        private SectionsPagerAdapter(final Context context, final FragmentManager fm) {
            super(fm);

            mFragments = acquireField("mFragments");
            mSavedState = acquireField("mSavedState");

            mContext = context;

            LibraryTabsUtil.addTabConfigurationListener(new LibraryTabsUtil.TabConfigurationListener() {
                @Override
                public void onTabsChanged() {
                    SectionsPagerAdapter.this.notifyDataSetChanged();
                }
            });
        }

        /**
         * Acquires a private field of super class {@link FragmentStatePagerAdapter}.
         * @param fieldName field name
         * @param <T> field type
         * @return field
         */
        private <T> T acquireField(final String fieldName) {
            try {
                final Field field = FragmentStatePagerAdapter.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                return (T) field.get(this);
            } catch (final NoSuchFieldException e) {
                Log.e(TAG, "Unable to acquire fragments list", e);
            } catch (final IllegalAccessException e) {
                Log.e(TAG, "Unable to acquire fragments list", e);
            }
            return null;
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
                    fragment = Preferences.isAlbumArtLibraryEnabled() ?
                            createFragment(AlbumsGridFragment.class) :
                            createFragment(AlbumsFragment.class);
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
                case LibraryTabsUtil.TAB_RANDOM:
                    fragment = createFragment(RandomBrowseFragment.class);
                    break;
                case LibraryTabsUtil.TAB_FAVORITES:
                    fragment = createFragment(FavoritesFragment.class);
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

        @Override
        public void notifyDataSetChanged() {
            if (mFragments == null || mSavedState == null) {
                super.notifyDataSetChanged();
                return;
            }

            // reorder tabs

            final List<Fragment> oldFragments = new ArrayList<>(mFragments);
            final List<Fragment.SavedState> oldSavedStates = new ArrayList<>(mSavedState);

            mFragments.clear();
            mSavedState.clear();

            for (int i = 0; i < oldFragments.size(); i++) {
                final Fragment fragment = oldFragments.get(i);
                if (fragment == null) {
                    continue;
                }
                int newPos = getItemPosition(fragment);
                if (newPos == POSITION_NONE) {
                    continue;
                }
                if (newPos == POSITION_UNCHANGED) {
                    newPos = i;
                }
                while (mFragments.size() <= newPos) {
                    mFragments.add(null);
                }
                mFragments.set(newPos, fragment);

                if (i < oldSavedStates.size()) {
                    while (mSavedState.size() <= newPos) {
                        mSavedState.add(null);
                    }
                    mSavedState.set(newPos, oldSavedStates.get(i));
                }
            }
            saveState();

            super.notifyDataSetChanged();
        }
    }
}
