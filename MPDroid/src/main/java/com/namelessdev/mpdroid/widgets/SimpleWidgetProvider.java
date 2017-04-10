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

package com.namelessdev.mpdroid.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.widget.RemoteViews;

import com.anpmech.mpd.subsystem.status.MPDStatusMap;
import com.anpmech.mpd.subsystem.status.StatusChangeListenerBase;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.tools.Tools;

public class SimpleWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "WidgetProvider";

    private static String WIDGET_ACTION = "com.namelessdev.mpdroid.widgets.ACTION";

    public SimpleWidgetProvider() {
        MPDApplication.getInstance().addStatusChangeListener(new StatusChangeListenerBase() {
            @Override
            public void stateChanged(final int oldState) {
                updateWidget(MPDApplication.getInstance());
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (intent.getCategories() != null &&
                intent.getCategories().contains(WIDGET_ACTION)) {
            Tools.runCommand(intent.getAction());
        }
    }

    /**
     * Link up various button actions using {@link PendingIntent}.
     *
     * @param context The current context.
     * @param views   The button views.
     */
    protected void linkButtons(final Context context, final RemoteViews views) {
        Intent intent;
        PendingIntent pendingIntent;

        // text button to start full app
        intent = new Intent(context, MainMenuActivity.class);
        pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_app, pendingIntent);

        // media buttons
        addButtonAction(context, views, R.id.control_prev, MPDControl.ACTION_PREVIOUS);
        addButtonAction(context, views, R.id.control_play, MPDControl.ACTION_TOGGLE_PLAYBACK);
        addButtonAction(context, views, R.id.control_next, MPDControl.ACTION_NEXT);
    }

    protected void addButtonAction(final Context context, final RemoteViews views,
                                 final int viewId, final String action) {
        final Intent intent = new Intent(context, getClass());
        intent.addCategory(WIDGET_ACTION);
        intent.setAction(action);
        final PendingIntent pendingIntent =
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(viewId, pendingIntent);
    }

    @LayoutRes
    protected int getLayoutResId() {
        return R.layout.widget_simple;
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager,
                         final int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.v(TAG, "Enter onUpdate");
        updateWidget(context);
    }

    private void updateWidget(final Context context) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), getLayoutResId());

        // Set correct drawable for pause state
        final boolean isPlaying =
                MPDApplication.getInstance().getMPD().getStatus().isState(MPDStatusMap.STATE_PLAYING);
        views.setImageViewResource(R.id.control_play, isPlaying ?
                R.drawable.ic_appwidget_music_pause : R.drawable.ic_appwidget_music_play);

        // Initialise given widgets to default state, where we launch MPDroid on
        // default click and hide actions if service not running.
        linkButtons(context, views);
        pushUpdate(context, views);
    }

    /**
     * Set the RemoteViews to use for all AppWidget instances
     *
     * @param context The current context.
     * @param views   The button views.
     */
    private void pushUpdate(final Context context, final RemoteViews views) {
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        gm.updateAppWidget(new ComponentName(context, getClass()), views);
    }

}
