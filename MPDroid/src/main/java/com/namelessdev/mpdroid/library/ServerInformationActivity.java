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

package com.namelessdev.mpdroid.library;

import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.subsystem.status.StatusChangeListener;
import com.namelessdev.mpdroid.MPDActivity;
import com.namelessdev.mpdroid.R;

import android.os.Bundle;

public class ServerInformationActivity extends MPDActivity implements StatusChangeListener {

    private ServerInformationFragment mInformationFragment;

    /**
     * Called upon connection.
     *
     * @param commandErrorCode If this number is non-zero, the number will correspond to a
     *                         {@link MPDException} error code. If this number is zero, the
     *                         connection MPD protocol commands were successful.
     */
    @Override
    public void connectionConnected(final int commandErrorCode) {
        super.connectionConnected(commandErrorCode);
        mInformationFragment.onConnectionStateChanged();
    }

    /**
     * Called upon disconnection.
     *
     * @param reason The reason given for disconnection.
     */
    @Override
    public void connectionDisconnected(final String reason) {
        super.connectionDisconnected(reason);
        mInformationFragment.onConnectionStateChanged();
    }

    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        mInformationFragment = new ServerInformationFragment();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, mInformationFragment).commit();
    }

    @Override
    public void onPause() {
        mApp.getMPD().getConnectionStatus().removeListener(this);
        mApp.removeStatusChangeListener(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mApp.addStatusChangeListener(this);
        mApp.getMPD().getConnectionStatus().addListener(this);
    }

    @Override
    public void outputsChanged() {
    }

    @Override
    public void playlistChanged(final int oldPlaylistVersion) {
    }

    @Override
    public void randomChanged() {
    }

    @Override
    public void repeatChanged() {
    }

    @Override
    public void stateChanged(final int oldState) {
    }

    @Override
    public void stickerChanged() {
    }

    @Override
    public void storedPlaylistChanged() {
    }

    @Override
    public void trackChanged(final int oldTrack) {
    }

    @Override
    public void volumeChanged(final int oldVolume) {
    }

}
