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

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.widget.ImageButton;

public class MenuButton {

    /**
     * Tint the overflow button if needed
     * @param menuButton
     * @param resources
     */
    public static void tint(final ImageButton menuButton, final Resources resources) {
        if (MPDApplication.getInstance().isLightThemeSelected()) {
            final Drawable drawable = DrawableCompat.wrap(menuButton.getDrawable());
            DrawableCompat.setTint(drawable, resources.getColor(android.R.color.darker_gray));
            menuButton.setImageDrawable(drawable);
        }
    }
}
