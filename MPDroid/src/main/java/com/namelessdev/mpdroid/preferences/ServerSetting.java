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

package com.namelessdev.mpdroid.preferences;

import com.anpmech.mpd.MPDCommand;
import com.namelessdev.mpdroid.ConnectionInfo;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ServerSetting implements Comparable<ServerSetting>, Parcelable {

    public interface CurrentConnectionChangeListener {

        void onCurrentConnectionChanged();
    }

    /**
     * This field is used to instantiate this class from a {@link Parcel}.
     */
    public static final Creator<ServerSetting> CREATOR = new ServerSettingCreator();

    private static final String TAG = "ServerSetting";

    private static final String PREFERENCE_KEY_SERVER_IDS = "serverIDs";

    private static final String PREFERENCE_KEY_CURRENT_SERVER_ID = "currentServerID";

    /**
     * This is the default streaming port for the default MPD implementation.
     */
    private static final CharSequence DEFAULT_STREAMING_PORT = "8000";

    /**
     * This is the settings key used to store the MPD server name.
     */
    private static final String KEY_SERVERNAME = "servername";

    /**
     * This is the settings key used to store the MPD hostname or IP address.
     */
    private static final String KEY_SERVERHOST = "serverhost";

    /**
     * This is the settings key used to store the MPD host password.
     */
    private static final String KEY_PASSWORD = "password";

    /**
     * This is the settings key used to store the MPD music path.
     */
    private static final String KEY_MUSIC_PATH = "musicPath";

    /**
     * This is the settings key used to store the MPD local cover filename.
     */
    private static final String KEY_COVER_FILENAME = "coverFileName";

    /**
     * This is the settings key used to store whether a persistent notification is required for
     * this connection.
     */
    private static final String KEY_PERSISTENT_NOTIFICATION = "persistentNotification";

    /**
     * This is the settings key used to store the MPD host port.
     */
    private static final String KEY_PORT = "port";

    /**
     * This settings key stores the stream URL.
     */
    private static final String KEY_STREAM_URL = "streamUrl";

    private static final List<CurrentConnectionChangeListener> CURRENT_CONNECTION_CHANGE_LISTENERS =
            new ArrayList<>();

    private final Integer mID;

    private ServerSetting(final Integer id) {
        mID = id;
    }

    static {
        settings().registerOnSharedPreferenceChangeListener(
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                            final String key) {
                        if (PREFERENCE_KEY_CURRENT_SERVER_ID.equals(key)) {
                            notifyCurrentConnectionChangeListener();
                            return;
                        }

                        final ServerSetting current = current();
                        if (current == null) {
                            return;
                        }
                        final String keyPrefix = getKeyPrefix(current.getID());
                        if (!key.startsWith(keyPrefix)) {
                            return;
                        }

                        switch (key.substring(keyPrefix.length())) {
                            case ServerSetting.KEY_SERVERHOST:
                            case ServerSetting.KEY_PASSWORD:
                            case ServerSetting.KEY_PERSISTENT_NOTIFICATION:
                            case ServerSetting.KEY_PORT:
                            case ServerSetting.KEY_STREAM_URL:
                                notifyCurrentConnectionChangeListener();
                                break;
                        }
                    }
                });
    }

    public static void addCurrentConnectionChangeListener(
            final CurrentConnectionChangeListener listener) {
        CURRENT_CONNECTION_CHANGE_LISTENERS.add(listener);
    }

    private static void notifyCurrentConnectionChangeListener() {
        for (final CurrentConnectionChangeListener listener : CURRENT_CONNECTION_CHANGE_LISTENERS) {
            listener.onCurrentConnectionChanged();
        }
    }

    private static SharedPreferences settings() {
        return PreferenceManager.getDefaultSharedPreferences(MPDApplication.getInstance());
    }

    static List<ServerSetting> read() {
        final SharedPreferences settings = settings();

        final List<ServerSetting> serverSettings = new ArrayList<>();
        for (final Integer id : getServerIDs(settings)) {
            serverSettings.add(new ServerSetting(id));
        }
        Collections.sort(serverSettings);
        return serverSettings;
    }

    private static List<Integer> getServerIDs(final SharedPreferences settings) {
        final String concatenatedIDs = settings.getString(PREFERENCE_KEY_SERVER_IDS, "");

        final List<Integer> serverIDs = new ArrayList<>();

        if (!concatenatedIDs.isEmpty()) {
            for (final String id : concatenatedIDs.split("\\|")) {
                serverIDs.add(Integer.valueOf(id));
            }
        }

        return serverIDs;
    }

    private static void setServerIDs(final SharedPreferences settings,
            final List<Integer> serverIDs) {
        final StringBuilder sb = new StringBuilder();
        for (final Integer id : serverIDs) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(id);
        }
        settings.edit().putString(PREFERENCE_KEY_SERVER_IDS, sb.toString()).apply();
    }

    public static ServerSetting current() {
        final Integer serverID = getCurrentID(settings());
        return serverID != null ? new ServerSetting(serverID) : null;
    }

    private static Integer getCurrentID(final SharedPreferences settings) {
        final int serverID = settings.getInt(PREFERENCE_KEY_CURRENT_SERVER_ID, 0);
        return serverID > 0 ? serverID : null;
    }

    private static void setCurrentID(final SharedPreferences settings, final Integer id) {
        settings.edit().putInt(PREFERENCE_KEY_CURRENT_SERVER_ID, id).apply();
    }

    void delete() {
        final SharedPreferences settings = settings();

        if (isCurrent()) {
            setCurrentID(settings, 0);
        }

        final List<Integer> serverIDs = getServerIDs(settings);
        serverIDs.remove(mID);
        setServerIDs(settings, serverIDs);

        settings.edit()
                .remove(getKeyPrefix() + KEY_SERVERNAME)
                .remove(getKeyPrefix() + KEY_SERVERHOST)
                .remove(getKeyPrefix() + KEY_PASSWORD)
                .remove(getKeyPrefix() + KEY_MUSIC_PATH)
                .remove(getKeyPrefix() + KEY_COVER_FILENAME)
                .remove(getKeyPrefix() + KEY_PERSISTENT_NOTIFICATION)
                .remove(getKeyPrefix() + KEY_PORT)
                .remove(getKeyPrefix() + KEY_STREAM_URL)
                .apply();
    }

    static ServerSetting create() {
        final SharedPreferences settings = settings();

        final List<Integer> serverIDs = getServerIDs(settings);
        final Integer id = serverIDs.isEmpty() ? 1 : serverIDs.get(serverIDs.size() - 1) + 1;
        serverIDs.add(id);

        setServerIDs(settings, serverIDs);

        return new ServerSetting(id);
    }

    public void setAsCurrent() {
        if (!isCurrent()) {
            setCurrentID(settings(), mID);
        }
    }

    boolean isCurrent() {
        return mID.equals(getCurrentID(settings()));
    }

    /**
     * This method is the Preference for modifying the MPD server name.
     *
     * @param context The current context.
     * @return The host name Preference.
     */
    Preference getNamePreference(final Context context) {
        final EditTextPreference prefName = new EditTextPreference(context);
        prefName.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);
        prefName.setDialogTitle(R.string.servername);
        prefName.setTitle(R.string.servername);
        prefName.setSummary(R.string.servernameDescription);
        prefName.setDefaultValue(Tools.getString(R.string.servernameDefault));
        prefName.setKey(getKeyPrefix() + KEY_SERVERNAME);

        return prefName;
    }

    /**
     * This method is the Preference for modifying the MPD host.
     *
     * @param context The current context.
     * @return The host name Preference.
     */
    Preference getHostPreference(final Context context) {
        final EditTextPreference prefHost = new EditTextPreference(context);
        prefHost.getEditText().setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        prefHost.setDialogTitle(R.string.host);
        prefHost.setTitle(R.string.host);
        prefHost.setSummary(R.string.hostDescription);
        prefHost.setDefaultValue("127.0.0.1");
        prefHost.setKey(getKeyPrefix() + KEY_SERVERHOST);

        return prefHost;
    }

    /**
     * This method is the Preference for modifying the MPD host password.
     *
     * @param context The current context.
     * @return The password Preference.
     */
    Preference getPasswordPreference(final Context context) {
        final EditTextPreference prefPassword = new EditTextPreference(context);
        prefPassword.getEditText().setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        prefPassword.setDialogTitle(R.string.password);
        prefPassword.setTitle(R.string.password);
        prefPassword.setSummary(R.string.passwordDescription);
        prefPassword.setDefaultValue("");
        prefPassword.setKey(getKeyPrefix() + KEY_PASSWORD);

        return prefPassword;
    }

    /**
     * This method is the Preference for modifying the MPD music path.
     *
     * @param context The current context.
     * @return The music path Preference.
     */
    Preference getMusicPathPreference(final Context context) {
        final EditTextPreference prefMusicPath = new EditTextPreference(context);
        prefMusicPath.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);
        prefMusicPath.setDialogTitle(R.string.musicPath);
        prefMusicPath.setTitle(R.string.musicPath);
        prefMusicPath.setSummary(R.string.musicPathDescriptionNew);
        prefMusicPath.setDefaultValue("music/");
        prefMusicPath.setKey(getKeyPrefix() + KEY_MUSIC_PATH);

        return prefMusicPath;
    }

    /**
     * This method is the Preference for modifying the MPD local cover filename.
     *
     * @param context The current context.
     * @return The cover filename Preference.
     */
    Preference getCoverFilenamePreference(final Context context) {
        final EditTextPreference prefCoverFilename = new EditTextPreference(context);
        prefCoverFilename.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);
        prefCoverFilename.setDialogTitle(R.string.coverFileName);
        prefCoverFilename.setTitle(R.string.coverFileName);
        prefCoverFilename.setSummary(R.string.coverFileNameDescription);
        prefCoverFilename.setDefaultValue("folder.jpg");
        prefCoverFilename.setKey(getKeyPrefix() + KEY_COVER_FILENAME);

        return prefCoverFilename;
    }

    /**
     * This method is the Preference for modifying notification persistence for this connection.
     *
     * @param context The current context.
     * @return The notification persistence Preference.
     */
    Preference getPersistentNotificationPreference(final Context context) {
        final Preference preference = new CheckBoxPreference(context);
        preference.setDefaultValue(Boolean.FALSE);
        preference.setTitle(R.string.persistentNotification);
        preference.setSummary(R.string.persistentNotificationDescription);
        preference.setKey(getKeyPrefix() + KEY_PERSISTENT_NOTIFICATION);

        return preference;
    }

    /**
     * This method creates a Preference to create a MPD host port preference input.
     *
     * @param context The current context.
     * @return The EditTextPreference for this preference.
     */
    Preference getPortPreference(final Context context) {
        final EditTextPreference prefPort = new EditTextPreference(context);
        final EditText editText = prefPort.getEditText();

        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.addTextChangedListener(new ValidatePort(prefPort));

        prefPort.setDialogTitle(R.string.port);
        prefPort.setTitle(R.string.port);
        prefPort.setSummary(R.string.portDescription);
        prefPort.setDefaultValue("6600");
        prefPort.setKey(getKeyPrefix() + KEY_PORT);

        return prefPort;
    }

    /**
     * This method creates a Preference to create a Stream URL preference input.
     *
     * @param context The current context.
     * @return The EditTextPreference for this preference.
     */
    Preference getStreamURLPreference(final Context context) {
        final EditTextPreference result = new EditTextPreference(context);
        final EditText editText = result.getEditText();

        editText.addTextChangedListener(new ValidateStreamURL(result));
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editText.setHint(R.string.streamingUrlHint);

        result.setDialogTitle(R.string.streamingUrlTitle);
        result.setDialogMessage(R.string.streamingUrlDialogMessage);
        result.setTitle(R.string.streamingUrlTitle);
        result.setSummary(R.string.streamingUrlDescription);
        result.setKey(getKeyPrefix() + KEY_STREAM_URL);

        return result;
    }

    public Integer getID() {
        return mID;
    }

    public String getName() {
        return getString(KEY_SERVERNAME);
    }

    public String getHost() {
        return getString(KEY_SERVERHOST);
    }

    public int getPort() {
        return getInteger(KEY_PORT, MPDCommand.DEFAULT_MPD_PORT);
    }

    private String getStreamingServer() {
        String streamServer = getString(KEY_STREAM_URL);

        /**
         * If the stream server is not available, try to choose a sane default.
         */
        if (streamServer == null || streamServer.isEmpty()) {
            streamServer = "http://" + getHost() + ':' + getPort();
        }

        return streamServer;
    }

    /**
     * @return the prefix of preference key to identify the server.
     */
    String getKeyPrefix() {
        return getKeyPrefix(mID);
    }

    private static String getKeyPrefix(final Integer id) {
        return "Server" + id;
    }

    public ConnectionInfo getConnectionInfo(final ConnectionInfo previousInfo) {

        final String server = getHost();
        final int port = getPort();
        final String password = getString(KEY_PASSWORD, null);
        final ConnectionInfo.Builder connectionInfo =
                new ConnectionInfo.Builder(server, port, password);

        connectionInfo.setStreamingServer(getStreamingServer());

        final boolean persistentNotification = getBoolean(KEY_PERSISTENT_NOTIFICATION);
        if (persistentNotification) {
            connectionInfo.setNotificationPersistent();
        } else {
            connectionInfo.setNotificationNotPersistent();
        }

        connectionInfo.setMusicPath(getString(KEY_MUSIC_PATH));
        connectionInfo.setCoverFilename(getString(KEY_COVER_FILENAME));

        connectionInfo.setPreviousConnectionInfo(previousInfo);

        return connectionInfo.build();
    }

    private String getString(final String key) {
        return getString(key, "");
    }

    private String getString(final String key, final String defaultValue) {
        return settings().getString(getKeyPrefix() + key, defaultValue);
    }

    private boolean getBoolean(final String key) {
        return settings().getBoolean(getKeyPrefix() + key, false);
    }

    private int getInteger(final String key, final int defaultValue) {
        final String settingString = getString(key, Integer.toString(defaultValue).trim());
        if (!settingString.isEmpty()) {
            try {
                return Integer.parseInt(settingString);
            } catch (final NumberFormatException e) {
                Log.e(TAG, "Received a bad integer during processing", e);
            }
        }

        return defaultValue;
    }

    @Override
    public int compareTo(final ServerSetting another) {
        return getName().compareTo(another.getName());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(mID);
    }

    /**
     * This class is used to instantiate a ServerSetting Object from a {@code Parcel}.
     */
    private static final class ServerSettingCreator implements Creator<ServerSetting> {

        @Override
        public ServerSetting createFromParcel(final Parcel source) {
            final int id = source.readInt();
            return new ServerSetting(id);
        }

        @Override
        public ServerSetting[] newArray(final int size) {
            return new ServerSetting[size];
        }
    }

    /**
     * This class includes common code for validating user input for preferences.
     */
    private abstract static class CommonValidator implements TextWatcher {

        /**
         * The maximum host port for IPv4 / IPv6
         */
        private static final int MAX_PORT = 65535;

        /**
         * The Stream URL EditTextPreference to validate.
         */
        final EditTextPreference mPreferenceTextEdit;

        /**
         * Sole constructor.
         *
         * @param editTextPreference The stream URL EditTextPreference to validate.
         */
        CommonValidator(final EditTextPreference editTextPreference) {
            mPreferenceTextEdit = editTextPreference;
        }

        /**
         * This method enables or disables the positive button.
         *
         * @param editText   The edit text of the button to enable.
         * @param setEnabled If true, this will enable the positive button, false disables the
         *                   positive button.
         */
        private static void enablePositiveButton(final EditTextPreference editText,
                final boolean setEnabled) {
            final Dialog dialog = editText.getDialog();

            if (dialog instanceof AlertDialog) {
                final AlertDialog alertDlg = (AlertDialog) dialog;
                final Button button = alertDlg.getButton(DialogInterface.BUTTON_POSITIVE);

                button.setEnabled(setEnabled);
            }
        }

        /**
         * This method validate the user input port.
         *
         * @param hostPort The hostPort from the user input.
         * @return An error message upon error, null otherwise.
         */
        @StringRes
        static int validatePort(final String hostPort) {
            int error = Integer.MIN_VALUE;

            try {
                final int port = Integer.parseInt(hostPort);

                if (port > MAX_PORT) {
                    error = R.string.portIntegerAboveRange;
                } else if (port < 0) {
                    error = R.string.portMustBePositive;
                }
            } catch (final NumberFormatException ignored) {
                error = R.string.portIntegerUndefined;
            }

            return error;
        }

        /**
         * This method handles errors.
         *
         * @param error The error message.
         */
        protected void setError(final int error) {
            if (error == Integer.MIN_VALUE) {
                setError(null);
            } else {
                setError(mPreferenceTextEdit.getContext().getResources().getString(error));
            }
        }

        /**
         * This method handles errors.
         *
         * @param error The error message.
         */
        protected void setError(final CharSequence error) {
            enablePositiveButton(mPreferenceTextEdit, error == null);
            mPreferenceTextEdit.getEditText().setError(error);
        }

        protected abstract int validate(final Editable s);

        /**
         * This method is called to notify you that, somewhere within {@code s}, the text has been
         * changed.
         * <p>
         * <p>It is legitimate to make further changes to {@code s} from this callback, but be
         * careful not to get yourself into an infinite loop, because any changes you make will
         * cause this method to be called again recursively.</p>
         */
        @Override
        public void afterTextChanged(final Editable s) {
            setError(validate(s));
        }

        /**
         * This method is called to notify you that, within {@code s}, the {@code count} characters
         * beginning at {@code start} are about to be replaced by new text with length {@code
         * after}.
         * <p>
         * <p>It is an error to attempt to make changes to {@code s} from this callback.</p>
         */
        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count,
                final int after) {
        }

        /**
         * This method is called to notify you that, within {@code s}, the {@code count} characters
         * beginning at {@code start} have just replaced old text that had length {@code before}.
         * <p>
         * <p>It is an error to attempt to make changes to {@code s} from this callback.</p>
         */
        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before,
                final int count) {
        }

    }

    /**
     * This class implements a {@link TextWatcher} to validate the MPD host port user input.
     */
    private static final class ValidatePort extends CommonValidator {

        /**
         * Sole constructor.
         *
         * @param editTextPreference The stream URL EditTextPreference to validate.
         */
        private ValidatePort(final EditTextPreference editTextPreference) {
            super(editTextPreference);
        }

        @Override
        protected int validate(final Editable s) {
            return s.length() > 0 ? validatePort(s.toString()) : Integer.MIN_VALUE;
        }

    }

    /**
     * This class implements a {@link TextWatcher} to validate the media stream URL user input.
     */
    private static final class ValidateStreamURL extends CommonValidator {

        /**
         * These are valid URL schemes for Android {@link MediaPlayer}.
         */
        private static final String[] VALID_SCHEMES = {"http", "rtsp"};

        /**
         * This field flags whether the port has been previously applied.
         * <p>
         * If the user removes it from here, they will be warned, but it won't be reinserted.
         */
        private boolean mPortInserted;

        private static final Pattern URL_PATTERN;

        static {
            final String iri = "[" + Patterns.GOOD_IRI_CHAR + "]([" + Patterns.GOOD_IRI_CHAR +
                    "\\-]{0,61}[" + Patterns.GOOD_IRI_CHAR + "]){0,1}";
            final String goodGtldChar =
                    "a-zA-Z\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";
            final String gtld = "[" + goodGtldChar + "]{2,63}";
            final String urlHostname = iri + "(\\." + iri + ")*" + "[\\." + gtld + "]";
            URL_PATTERN = Pattern.compile("(http|Http|rtsp|Rtsp):\\/\\/" + urlHostname +
                    "(?:\\:\\d{1,5})?(\\/)?");
        }

        /**
         * Sole constructor.
         *
         * @param editTextPreference The stream URL EditTextPreference to validate.
         */
        private ValidateStreamURL(final EditTextPreference editTextPreference) {
            super(editTextPreference);
        }

        /**
         * This method validates the port.
         *
         * @param text                The text to extract the port from.
         * @param authorityColonIndex The index of the URI authority.
         * @param portColonIndex      The index of the URI port colon.
         * @return The error output while trying to validate the port.
         */
        private static int getPort(final String text, final int authorityColonIndex,
                final int portColonIndex) {
            final int authorityEndIndex = text.indexOf('/', authorityColonIndex + 3);
            final String intString;

            if (authorityEndIndex == -1) {
                intString = text.substring(portColonIndex + 1);
            } else {
                intString = text.substring(portColonIndex + 1, authorityEndIndex);
            }

            return intString.isEmpty() ? Integer.MIN_VALUE : validatePort(intString);
        }

        /**
         * This method inserts the default port into the editable during typing.
         *
         * @param s                   The editable to insert the default port into.
         * @param text                The text from the editable.
         * @param authorityColonIndex The authority colon index.
         * @throws ParseException If the URL fails validation after default port insertion.
         */
        private static void insertDefaultPort(final Editable s, final String text,
                final int authorityColonIndex) throws ParseException {
            final int authorityEndIndex = text.indexOf('/', authorityColonIndex + 3);

            if (authorityEndIndex == -1) {
                s.append(':');
                s.append(DEFAULT_STREAMING_PORT);
            } else {
                s.insert(authorityEndIndex, ":" + DEFAULT_STREAMING_PORT);
            }

            /**
             * This should not be invalid, this is a double-check.
             */
            if (!URL_PATTERN.matcher(s).matches()) {
                throw new ParseException("Failed to parse after insertion.", -1);
            }
        }

        /**
         * This checks for validity of the URL scheme.
         *
         * @param text The URL as text.
         * @return True if the scheme is one of the schemes listed in {@link #VALID_SCHEMES}, false
         * otherwise.
         */
        private static boolean isValidScheme(final String text) {
            final int colonIndex = text.indexOf(':');
            final boolean isValidScheme;

            /**
             * Don't bother the user until they've completed the scheme.
             */
            if (colonIndex == -1) {
                isValidScheme = true;
            } else {
                final String scheme = text.substring(0, colonIndex);
                isValidScheme = Arrays.binarySearch(VALID_SCHEMES, scheme) >= 0;
            }

            return isValidScheme;
        }

        @Override
        protected int validate(final Editable s) {
            if (s.length() == 0) {
                return Integer.MIN_VALUE;
            }

            final String text = s.toString();
            if (!isValidScheme(text)) {
                return R.string.invalidStreamScheme;
            }

            if (!URL_PATTERN.matcher(s).matches()) {
                return R.string.invalidUrl;
            }

            final int authorityColonIndex = text.indexOf(':');
            final int httpAuthIndex = text.indexOf('@');
            final int portColonIndex;

            if (httpAuthIndex == -1) {
                portColonIndex = text.indexOf(':', authorityColonIndex + 1);
            } else {
                portColonIndex = text.indexOf(':', httpAuthIndex + 1);
            }

            if (portColonIndex != -1 || mPortInserted) {
                return getPort(text, authorityColonIndex, portColonIndex);
            }

            try {
                mPortInserted = true;
                insertDefaultPort(s, text, authorityColonIndex);
            } catch (final ParseException ignored) {
                return R.string.errorParsingURL;
            }

            return Integer.MIN_VALUE;
        }
    }

}

