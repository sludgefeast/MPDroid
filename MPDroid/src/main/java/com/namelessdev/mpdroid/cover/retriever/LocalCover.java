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

package com.namelessdev.mpdroid.cover.retriever;

import com.namelessdev.mpdroid.LocalWebServer;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.helpers.AlbumInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.text.TextUtils.isEmpty;

public class LocalCover implements ICoverRetriever {

    private static final String[] EXT = {
            "jpg", "png", "jpeg",
    };

    private static final String PLACEHOLDER_FILENAME = "%placeholder_filename";

    // Note that having two PLACEHOLDER_FILENAME is on purpose
    private static final String[] FILENAMES = {
            "%placeholder_custom", PLACEHOLDER_FILENAME, "AlbumArt", "cover", "folder", "front",
    };

    private static final String[] SUB_FOLDERS = {
            "", "artwork", "Covers"
    };

    private static final String TAG = "LocalCover";

    private final MPDApplication mApp = MPDApplication.getInstance();

    @Override
    public List<String> getCoverUrls(final AlbumInfo albumInfo) throws Exception {
        if (isEmpty(albumInfo.getParentDirectory())) {
            return Collections.emptyList();
        }

        FILENAMES[0] = mApp.getConnectionSettings().getCoverFilename();

        final LocalWebServer localWebServer =
                mApp.getConnectionSettings().getLocalWebServer();

        String lfilename, url;
        final List<String> coverUrls = new ArrayList<>();
        for (final String subfolder : SUB_FOLDERS) {
            for (String baseFilename : FILENAMES) {
                for (final String ext : EXT) {

                    if (baseFilename == null
                            || (baseFilename.startsWith("%") && !baseFilename
                            .equals(PLACEHOLDER_FILENAME))) {
                        continue;
                    }
                    if (baseFilename.equals(PLACEHOLDER_FILENAME)
                            && albumInfo.getFilename() != null) {
                        final int dotIndex = albumInfo.getFilename().lastIndexOf('.');
                        if (dotIndex == -1) {
                            continue;
                        }
                        baseFilename = albumInfo.getFilename().substring(0, dotIndex);
                    }

                    // Add file extension except for the filename coming
                    // from settings
                    if (baseFilename.equals(FILENAMES[0])) {
                        lfilename = baseFilename;
                    } else {
                        lfilename = subfolder + '/' + baseFilename + '.' + ext;
                    }

                    url = localWebServer.buildUrl(albumInfo.getParentDirectory(), lfilename);

                    if (!coverUrls.contains(url)) {
                        coverUrls.add(url);
                    }
                }
            }
        }

        return coverUrls;
    }


    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public boolean isCoverLocal() {
        return false;
    }

}
