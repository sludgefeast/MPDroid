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

package com.namelessdev.mpdroid.helpers;

import com.namelessdev.mpdroid.ConnectionInfo;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.cover.retriever.GracenoteCover;
import com.namelessdev.mpdroid.preferences.ServerSetting;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;

/**
 * Asynchronous worker thread-class for long during operations on JMPDComm.
 */
public class MPDAsyncWorker implements Handler.Callback,
        SharedPreferences.OnSharedPreferenceChangeListener,
        ServerSetting.CurrentConnectionChangeListener {

    private static final int LOCAL_UID = 500;

    static final int EVENT_CONNECTION_CONFIG = LOCAL_UID + 1;

    static final int EVENT_EXEC_ASYNC = LOCAL_UID + 2;

    static final int EVENT_EXEC_ASYNC_FINISHED = LOCAL_UID + 3;

    private static final String TAG = "MPDAsyncWorker";

    /**
     * A handler for the MPDAsyncHelper object.
     */
    private final Handler mHelperHandler;

    private ConnectionInfo mConnectionInfo = ConnectionInfo.EMPTY;

    MPDAsyncWorker(final Handler helperHandler) {
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(MPDApplication.getInstance());
        settings.registerOnSharedPreferenceChangeListener(this);

        ServerSetting.addCurrentConnectionChangeListener(this);

        mHelperHandler = helperHandler;
    }

    /**
     * Handles messages to the off UI thread {@code Handler}/{@code Looper}.
     *
     * @param msg The incoming message to handle.
     * @return True if message was handled, false otherwise.
     */
    @Override
    public final boolean handleMessage(final Message msg) {
        boolean result = true;

        switch (msg.what) {
            case EVENT_EXEC_ASYNC:
                ((Runnable) msg.obj).run();
                break;
            default:
                result = false;
                break;
        }

        return result;
    }

    @Override
    public void onCurrentConnectionChanged() {
        updateConnectionSettings();
    }

    /**
     * Called when a shared preference is changed, added, or removed.
     * <p>
     * <p>This may be called even if a preference is set to its existing value. This callback will
     * be run the program's main thread.</p>
     *
     * @param sharedPreferences The {@link SharedPreferences} that received the change.
     * @param key               The key of the preference that was changed, added, or
     */
    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
            final String key) {

        switch (key) {
            case MPDApplication.USE_LOCAL_ALBUM_CACHE_KEY:
                final boolean useAlbumCache = sharedPreferences.getBoolean(key, false);

                mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_SET_USE_CACHE, useAlbumCache);
                break;
            case GracenoteCover.PREFERENCE_CUSTOM_CLIENT_ID_KEY:
                final SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(GracenoteCover.USER_ID);
                editor.apply();
                break;
        }
    }

    /**
     * Initiates the worker thread {@code Handler} in an off UI thread {@code Looper}.
     *
     * @return A {@code Handler} for this object.
     */
    final Handler startThread() {
        final HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        return new Handler(handlerThread.getLooper(), this);
    }

    ConnectionInfo updateConnectionSettings() {

        final ServerSetting currentServer = ServerSetting.current();
        final ConnectionInfo connectionInfo = currentServer != null ?
                currentServer.getConnectionInfo(mConnectionInfo) : ConnectionInfo.EMPTY;

        if (connectionInfo.hasServerChanged() || connectionInfo.hasStreamInfoChanged()
                || connectionInfo.wasNotificationPersistent() !=
                connectionInfo.isNotificationPersistent()) {
            mConnectionInfo = connectionInfo;
            mHelperHandler.obtainMessage(EVENT_CONNECTION_CONFIG, connectionInfo).sendToTarget();
        }

        return connectionInfo;
    }
}
