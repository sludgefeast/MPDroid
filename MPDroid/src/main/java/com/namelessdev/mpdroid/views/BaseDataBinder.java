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

package com.namelessdev.mpdroid.views;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayDataBinder;
import com.namelessdev.mpdroid.cover.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.cover.CoverAsyncHelper;
import com.namelessdev.mpdroid.cover.CoverDownloadListener;
import com.namelessdev.mpdroid.cover.CoverManager;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.views.holders.AlbumCoverHolder;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.view.View;

abstract class BaseDataBinder<T> implements ArrayDataBinder<T> {

    static final CharSequence SEPARATOR = " - ";

    final boolean mEnableCache;

    BaseDataBinder() {
        final MPDApplication app = MPDApplication.getInstance();
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);

        mEnableCache = settings.getBoolean(CoverManager.PREFERENCE_CACHE, true);
    }

    static CoverAsyncHelper getCoverHelper(final AlbumCoverHolder holder,
            final int defaultSize) {
        final CoverAsyncHelper coverHelper = new CoverAsyncHelper();
        final int height = holder.mAlbumCover.getHeight();

        // If the list is not displayed yet, the height is 0. This is a
        // problem, so set a fallback one.
        if (height == 0) {
            coverHelper.setCoverMaxSize(defaultSize);
        } else {
            coverHelper.setCoverMaxSize(height);
        }

        coverHelper.resetCover();

        return coverHelper;
    }

    static void loadArtwork(final CoverAsyncHelper coverHelper,
            final AlbumInfo albumInfo) {
        coverHelper.downloadCover(albumInfo);
    }

    static CoverDownloadListener setCoverListener(final AlbumCoverHolder holder,
            final CoverAsyncHelper coverHelper) {
        // listen for new artwork to be loaded
        final CoverDownloadListener acd =
                new AlbumCoverDownloadListener(holder.mAlbumCover, holder.mCoverArtProgress, false);
        final AlbumCoverDownloadListener oldAcd
                = (AlbumCoverDownloadListener) holder.mAlbumCover
                .getTag(R.id.AlbumCoverDownloadListener);

        if (oldAcd != null) {
            oldAcd.detach();
        }

        holder.mAlbumCover.setTag(R.id.AlbumCoverDownloadListener, acd);
        holder.mAlbumCover.setTag(R.id.CoverAsyncHelper, coverHelper);
        coverHelper.addCoverDownloadListener(acd);

        return acd;
    }

    /**
     * This is a helper function for onLayoutInflation.
     *
     * @param targetView The view given by onLayoutInflation, from which the view will be found by
     *                   the {@code resource} given.
     * @param resource   The resource id view to find.
     * @param isVisible  If true, the visibility of the resource view will be set to {@code
     *                   View.VISIBLE}, otherwise the visibility of the resource view will be set
     *                   to {@code View.GONE}.
     * @return The unmodified targetView.
     */
    static View setViewVisible(final View targetView, @IdRes final int resource,
            final boolean isVisible) {
        final View view = targetView.findViewById(resource);
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        return targetView;
    }

}
