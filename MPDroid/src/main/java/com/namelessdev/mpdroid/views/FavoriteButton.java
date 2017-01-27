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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.Music;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.preferences.Preferences;
import com.namelessdev.mpdroid.tools.Tools;

import java.io.IOException;
import java.util.List;

public class FavoriteButton extends ToggleButton implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "FavoriteButton";

    private final MPDApplication mApp = MPDApplication.getInstance();

    private Album mAlbum;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FavoriteButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public FavoriteButton(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FavoriteButton(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FavoriteButton(final Context context) {
        this(context, null);
    }

    private void init() {
        setText("");
        setTextOn("");
        setTextOff("");
        setButtonDrawable(R.drawable.favorite);
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void setAlbum(final Album album) {
        mAlbum = album;

        setOnCheckedChangeListener(null);
        try {
            setChecked(isFavoriteAlbum());
        } catch (final IOException | MPDException e) {
            setChecked(false);
            Log.e(TAG, "Unable to determine favorite state of album.", e);
        }
        setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        try {
            if (isChecked) {
                addAlbum();
                Tools.notifyUser(R.string.addToFavorites, mAlbum.getName());
            } else {
                removeAlbum();
                Tools.notifyUser(R.string.removeFromFavorites, mAlbum.getName());
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Unable to change favorite state of album.", e);
        }
    }

    /**
     * Marks album as a favorite.
     *
     * @throws IOException
     * @throws MPDException
     */
    private void addAlbum()
            throws IOException, MPDException {
        final String personalizationKey = Preferences.favoritesPersonalizationKey();
        mApp.getMPD().getStickerManager().addFavorites(
                mApp.getMPD().getSongs(mAlbum), personalizationKey);
    }

    /**
     * Removes album from favorites.
     *
     * @throws IOException
     * @throws MPDException
     */
    private void removeAlbum()
            throws IOException, MPDException {
        final String personalizationKey = Preferences.favoritesPersonalizationKey();
        mApp.getMPD().getStickerManager().removeFavorites(
                mApp.getMPD().getSongs(mAlbum), personalizationKey);
    }

    /**
     * Determines if album is favored.
     *
     * @return true, if album is favored
     * @throws IOException
     * @throws MPDException
     */
    private boolean isFavoriteAlbum()
            throws IOException, MPDException {
        final String personalizationKey = Preferences.favoritesPersonalizationKey();
        final List<Music> songs = mApp.getMPD().getSongs(mAlbum);
        return !songs.isEmpty() && mApp.getMPD().getStickerManager().
                isFavorite(songs.get(0), personalizationKey);
    }

}
