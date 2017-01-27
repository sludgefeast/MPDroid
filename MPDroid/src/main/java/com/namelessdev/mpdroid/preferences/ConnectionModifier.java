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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.StringRes;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;

import com.namelessdev.mpdroid.R;

import java.text.ParseException;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * This class is the Fragment used to configure a specific connection.
 */
public class ConnectionModifier extends PreferenceFragment {

    /**
     * This is the default streaming port for the default MPD implementation.
     */
    public static final CharSequence DEFAULT_STREAMING_PORT = "8000";

    /**
     * This is the Bundle extra used to start this Fragment.
     */
    public static final String EXTRA_SERVICE_SET_ID = "SSID";

    /**
     * This is the settings key used to store the MPD hostname or IP address.
     */
    public static final String KEY_HOSTNAME = "hostname";

    /**
     * This is the settings key used to store the MPD host password.
     */
    public static final String KEY_PASSWORD = "password";

    /**
     * This is the settings key used to store whether a persistent notification is required for
     * this connection.
     */
    public static final String KEY_PERSISTENT_NOTIFICATION = "persistentNotification";

    /**
     * This is the settings key used to store the MPD host port.
     */
    public static final String KEY_PORT = "port";

    /**
     * This settings key stores the stream URL.
     */
    public static final String KEY_STREAM_URL = "streamUrl";

    private static final String KEY_CONNECTION_CATEGORY = "connectionCategory";

    /**
     * This method is the Preference for modifying the MPD hostname.
     *
     * @param context   The current context.
     * @param keyPrefix The Wi-Fi Set Service ID.
     * @return The host name Preference.
     */
    private static Preference getHost(final Context context, final String keyPrefix) {
        final EditTextPreference prefHost = new EditTextPreference(context);
        prefHost.getEditText().setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        prefHost.setDialogTitle(R.string.host);
        prefHost.setTitle(R.string.host);
        prefHost.setSummary(R.string.hostDescription);
        prefHost.setDefaultValue("127.0.0.1");
        prefHost.setKey(keyPrefix + KEY_HOSTNAME);

        return prefHost;
    }

    /**
     * This method creates a Preference for the master category of this class.
     *
     * @param context   The current context.
     * @param keyPrefix The Wi-Fi Set Service ID.
     * @return The EditTextPreference for this preference.
     */
    private static Preference getMasterCategory(final Context context, final String keyPrefix) {
        final PreferenceCategory masterCategory = new PreferenceCategory(context);
        masterCategory.setTitle(keyPrefix.isEmpty() ?
                R.string.defaultSettings : R.string.wlanBasedSettings);
        return masterCategory;
    }

    /**
     * This method is the Preference for modifying the MPD host password.
     *
     * @param context   The current context.
     * @param keyPrefix The Wi-Fi Set Service ID.
     * @return The password Preference.
     */
    private static Preference getPassword(final Context context, final String keyPrefix) {
        final EditTextPreference prefPassword = new EditTextPreference(context);
        prefPassword.getEditText().setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        prefPassword.setDialogTitle(R.string.password);
        prefPassword.setTitle(R.string.password);
        prefPassword.setSummary(R.string.passwordDescription);
        prefPassword.setDefaultValue("");
        prefPassword.setKey(keyPrefix + KEY_PASSWORD);

        return prefPassword;
    }

    /**
     * This method is the Preference for modifying notification persistence for this connection.
     *
     * @param context   The current context.
     * @param keyPrefix The Wi-Fi Set Service ID.
     * @return The notification persistence Preference.
     */
    private static Preference getPersistentNotification(final Context context,
                                                        final String keyPrefix) {
        final Preference preference = new CheckBoxPreference(context);
        preference.setDefaultValue(Boolean.FALSE);
        preference.setTitle(R.string.persistentNotification);
        preference.setSummary(R.string.persistentNotificationDescription);
        preference.setKey(keyPrefix + KEY_PERSISTENT_NOTIFICATION);

        return preference;
    }

    /**
     * This method creates a Preference to create a MPD host port preference input.
     *
     * @param context   The current context.
     * @param keyPrefix The Wi-Fi Set Service ID.
     * @return The EditTextPreference for this preference.
     */
    private static Preference getPort(final Context context, final String keyPrefix) {
        final EditTextPreference prefPort = new EditTextPreference(context);
        final EditText editText = prefPort.getEditText();

        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.addTextChangedListener(new ValidatePort(prefPort));

        prefPort.setDialogTitle(R.string.port);
        prefPort.setTitle(R.string.port);
        prefPort.setSummary(R.string.portDescription);
        prefPort.setDefaultValue("6600");
        prefPort.setKey(keyPrefix + KEY_PORT);

        return prefPort;
    }

    /**
     * This method creates a Preference to create a Stream URL preference input.
     *
     * @param context   The current context.
     * @param keyPrefix The Wi-Fi Set Service ID.
     * @return The EditTextPreference for this preference.
     */
    private static Preference getStreamURL(final Context context, final String keyPrefix) {
        final EditTextPreference result = new EditTextPreference(context);
        final EditText editText = result.getEditText();

        editText.addTextChangedListener(new ValidateStreamURL(result));
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editText.setHint(R.string.streamingUrlHint);

        result.setDialogTitle(R.string.streamingUrlTitle);
        result.setDialogMessage(R.string.streamingUrlDialogMessage);
        result.setTitle(R.string.streamingUrlTitle);
        result.setSummary(R.string.streamingUrlDescription);
        result.setKey(keyPrefix + KEY_STREAM_URL);

        return result;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final PreferenceScreen screen =
                getPreferenceManager().createPreferenceScreen(getActivity());
        final Context context = screen.getContext();
        final String serviceSetId = getArguments().getString(EXTRA_SERVICE_SET_ID);

        if (serviceSetId == null) {
            throw new IllegalStateException("Set service ID must not be null.");
        }

        screen.setKey(KEY_CONNECTION_CATEGORY);
        screen.addPreference(getMasterCategory(context, serviceSetId));
        screen.addPreference(getHost(context, serviceSetId));
        screen.addPreference(getPort(context, serviceSetId));
        screen.addPreference(getPassword(context, serviceSetId));
        screen.addPreference(getStreamURL(context, serviceSetId));
        screen.addPreference(getPersistentNotification(context, serviceSetId));
        setPreferenceScreen(screen);
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
