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

package com.namelessdev.mpdroid;


import android.net.Uri;

import java.io.Serializable;


public class LocalWebServer implements Serializable {

    private static final String URL_PREFIX = "http://";

    private final String mWebserverPath;

    private final String mMpdServerName;

    LocalWebServer(final String webserverPath, final String mpdServerName) {
        mWebserverPath = webserverPath;
        mMpdServerName = mpdServerName;
    }

    public String buildUrl(final String path, final String fileName) {
        return buildUrl(mMpdServerName, mWebserverPath, path, fileName);
    }

    public String buildUrl(final String pathFileName) {
        return buildUrl(mMpdServerName, mWebserverPath, pathFileName, null);
    }

    private static String buildUrl(final String mpdServerName, final String webserverPath,
            final String path, final String fileName) {
        final String serverName;
        final String musicPath;
        if (webserverPath.startsWith(URL_PREFIX)) {
            int hostPortEnd = webserverPath.indexOf(URL_PREFIX.length(), '/');
            if (hostPortEnd == -1) {
                hostPortEnd = webserverPath.length();
            }
            serverName = webserverPath.substring(URL_PREFIX.length(), hostPortEnd);
            musicPath = webserverPath.substring(hostPortEnd);
        } else {
            serverName = mpdServerName;
            musicPath = webserverPath;
        }
        final Uri.Builder uriBuilder = Uri.parse(URL_PREFIX + serverName).buildUpon();
        appendPathString(uriBuilder, musicPath);
        appendPathString(uriBuilder, path);
        appendPathString(uriBuilder, fileName);

        final Uri uri = uriBuilder.build();
        return uri.toString();
    }

    private static void appendPathString(final Uri.Builder builder, final String baseString) {
        if (baseString != null && !baseString.isEmpty()) {
            final String[] components = baseString.split("/");
            for (final String component : components) {
                builder.appendPath(component);
            }
        }
    }
}
