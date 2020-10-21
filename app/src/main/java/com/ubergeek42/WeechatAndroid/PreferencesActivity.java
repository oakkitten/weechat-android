package com.ubergeek42.WeechatAndroid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.DialogFragmentGetter;
import androidx.preference.DialogPreference;
import androidx.preference.FilePreference;
import androidx.preference.FontPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.RingtonePreferenceFix;
import androidx.preference.ThemePreference;

import com.ubergeek42.WeechatAndroid.media.Config;
import com.ubergeek42.WeechatAndroid.upload.HttpUriGetter;
import com.ubergeek42.WeechatAndroid.upload.RequestModifier;
import com.ubergeek42.WeechatAndroid.utils.Utils;

import java.util.Set;

import okhttp3.HttpUrl;

import static com.ubergeek42.WeechatAndroid.utils.Constants.*;
import static com.ubergeek42.WeechatAndroid.utils.Toaster.ErrorToast;

public class PreferencesActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    final static private String KEY = "key";

    final static private int PREF_RINGTONE_ID = 0;
    final static private int PREF_SSH_KEY_ID = 1;
    final static private int PREF_TLS_CLIENT_FILE_ID = 3;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preferences);

        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        // this is exactly the place for the following statement. why? no idea.
        if (savedInstanceState != null)
            return;

        Fragment p = getSupportFragmentManager().findFragmentByTag(null);
        if (p == null) p = new PreferencesFragment();

        String key = getIntent().getStringExtra(KEY);
        if (key != null) {
            Bundle args = new Bundle();
            args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key);
            p.setArguments(args);
        }

        getSupportFragmentManager().beginTransaction()
                .add(R.id.preferences, p, null)
                .commit();
    }

    @Override public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        if (PREF_NOTIFICATION_GROUP.equals(preferenceScreen.getKey()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
            return true;
        }
        Intent intent = new Intent(PreferencesActivity.this, PreferencesActivity.class);
        intent.putExtra(KEY, preferenceScreen.getKey());
        startActivity(intent);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static class PreferencesFragment extends PreferenceFragmentCompat implements DialogPreference.TargetFragment, Preference.OnPreferenceChangeListener {

        private static final String FRAGMENT_DIALOG_TAG = "android.support.v7.preference.PreferenceFragment.DIALOG";
        private String key;
        private Preference sslGroup = null;
        private Preference sshGroup = null;
        private Preference wsPath = null;

        private Preference resumePreference;

        // don't check permissions if preference is null, instead use resumePreference
        @Override public void onDisplayPreferenceDialog(Preference preference) {
            final DialogFragment f;

            if (preference == null) {
                preference = resumePreference;
            } else if (preference instanceof FontPreference || preference instanceof ThemePreference) {
                boolean granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if (!granted) {
                    requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                    resumePreference = preference;
                    return;
                }
            }

            int code = -1;
            if (preference instanceof FilePreference) {
                switch (preference.getKey()) {
                    case PREF_SSH_KEY_FILE: code = PREF_SSH_KEY_ID; break;
                    case PREF_SSL_CLIENT_CERTIFICATE: code = PREF_TLS_CLIENT_FILE_ID; break;
                }
            }

            if (preference instanceof DialogFragmentGetter) {
                f = makeFragment((DialogFragmentGetter) preference, code);
            } else if (preference instanceof RingtonePreferenceFix) {
                Intent intent = ((RingtonePreferenceFix) preference).makeRingtoneRequestIntent();
                startActivityForResult(intent, PREF_RINGTONE_ID);
                return;
            } else {
                super.onDisplayPreferenceDialog(preference);
                return;
            }

            f.setTargetFragment(this, 0);
            f.show(requireFragmentManager(), FRAGMENT_DIALOG_TAG);
        }

        @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            onDisplayPreferenceDialog(null);
        }

        // this makes fragment display preferences. key is the key of the preference screen
        // that this fragment is supposed to display. the key is set in activity's onCreate
        @Override public void onCreatePreferences(Bundle bundle, String key) {
            setPreferencesFromResource(R.xml.preferences, this.key = key);

            fixMultiLineTitles(getPreferenceScreen());

            String[] listenTo = {};
            if (PREF_CONNECTION_GROUP.equals(key)) {
                sslGroup = findPreference(PREF_SSL_GROUP);
                sshGroup = findPreference(PREF_SSH_GROUP);
                wsPath = findPreference(PREF_WS_PATH);
                showHideStuff(getPreferenceScreen().getSharedPreferences().getString(PREF_CONNECTION_TYPE, PREF_CONNECTION_TYPE_D));
                listenTo = new String[] {PREF_CONNECTION_TYPE, PREF_HOST, PREF_PORT};
            } else if (PREF_SSH_GROUP.equals(key)) {
                listenTo = new String[]{PREF_SSH_AUTHENTICATION_METHOD, PREF_SSH_HOST, PREF_SSH_PORT};
                switchSshAuthenticationMethodPreferences(null);
            } else if (PREF_PING_GROUP.equals(key))
                listenTo = new String[] {PREF_PING_IDLE, PREF_PING_TIMEOUT};
            else if (PREF_LOOKFEEL_GROUP.equals(key))
                listenTo = new String[] {PREF_TEXT_SIZE, PREF_MAX_WIDTH, PREF_TIMESTAMP_FORMAT};
            else if (PREF_THEME_GROUP.equals(key)) {
                enableDisableThemeSwitch(getPreferenceScreen().getSharedPreferences().getString(PREF_THEME, PREF_THEME_D));
                listenTo = new String[]{PREF_THEME};
            } else if (PREF_BUFFERLIST_GROUP.equals(key)) {
                enableDisableGestureExclusionZoneSwitch();
            } else if (PREF_MEDIA_PREVIEW_GROUP.equals(key)) {
                enableDisableMediaPreviewPreferences(null, null);
                listenTo = new String[]{PREF_MEDIA_PREVIEW_ENABLED_FOR_NETWORK,
                        PREF_MEDIA_PREVIEW_ENABLED_FOR_LOCATION,
                        PREF_MEDIA_PREVIEW_STRATEGIES};
            } else if (PREF_UPLOAD_GROUP.equals(key)) {
                enableDisableUploadingPreferences(null);
                showHideBasicAuthentication(null);
                listenTo = new String[]{PREF_UPLOAD_ACCEPT,
                        PREF_UPLOAD_AUTHENTICATION,
                        PREF_UPLOAD_URI,
                        PREF_UPLOAD_REGEX,
                        PREF_UPLOAD_ADDITIONAL_HEADERS};
            }

            for (String p : listenTo)
                findPreference(p).setOnPreferenceChangeListener(this);
        }

        // this only sets the title of the action bar
        @Override public void onActivityCreated(Bundle savedInstanceState) {
            ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
            if (actionBar != null)
                actionBar.setTitle((key == null) ? getString(R.string.menu__preferences) : findPreference(key).getTitle());
            super.onActivityCreated(savedInstanceState);
        }

        // this is required for RingtonePreferenceFix, which requires an activity to operate
        @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case PREF_RINGTONE_ID: ((RingtonePreferenceFix) findPreference(PREF_NOTIFICATION_SOUND)).onActivityResult(data); break;
                    case PREF_SSH_KEY_ID: ((FilePreference) findPreference(PREF_SSH_KEY_FILE)).onActivityResult(data); break;
                    case PREF_TLS_CLIENT_FILE_ID: ((FilePreference) findPreference(PREF_SSL_CLIENT_CERTIFICATE)).onActivityResult(data); break;
                }
            }
        }

        @Override public boolean onPreferenceChange(Preference preference, Object o) {

            Weechat.runOnMainThread(() -> {
                int count = getPreferenceScreen().getPreferenceCount();
                for (int i = 0; i < count; i++) {
                    Preference p = getPreferenceScreen().getPreference(i);
                    String key = p.getKey();
                    try {
                        boolean enabled = com.ubergeek42.WeechatAndroid.preferences.Preference.getByKey(key).getNotDisabled();
                        boolean visible = com.ubergeek42.WeechatAndroid.preferences.Preference.getByKey(key).getNotHidden();

                        p.setEnabled(enabled);
                        p.setVisible(visible);
                    } catch (NullPointerException e) {
                        System.out.println("not found " + key);
                    }
                }
            });

            String key = preference.getKey();
            boolean valid = true;
            int errorResource = -1;

            try {
                if (Utils.isAnyOf(key, PREF_HOST, PREF_SSH_HOST)) {
                    valid = !((String) o).contains(" ");
                    errorResource = R.string.error__pref__no_spaces_allowed_in_hostnames;
                } else if (PREF_UPLOAD_URI.equals(key)) {
                    if (!TextUtils.isEmpty((String) o)) HttpUrl.get((String) o);
                } else if (PREF_SSH_AUTHENTICATION_METHOD.equals(key)) {
                    switchSshAuthenticationMethodPreferences((String) o);
                } else if (Utils.isAnyOf(key, PREF_TEXT_SIZE, PREF_MAX_WIDTH, PREF_PORT, PREF_SSH_PORT, PREF_PING_IDLE, PREF_PING_TIMEOUT)) {
                    valid = Utils.isAllDigits((String) o);
                    errorResource = R.string.error__pref__invalid_number;
                } else if (PREF_TIMESTAMP_FORMAT.equals(key)) {
                    valid = Utils.isValidTimestampFormat((String) o);
                    errorResource = R.string.error__pref__invalid_timestamp_format;
                } else if (PREF_CONNECTION_TYPE.equals(key)) {
                    showHideStuff((String) o);
                } else if (PREF_THEME.equals(key)) {
                    enableDisableThemeSwitch((String) o);
                } else if (PREF_MEDIA_PREVIEW_ENABLED_FOR_NETWORK.equals(key)) {
                    enableDisableMediaPreviewPreferences((String) o, null);
                } else if (PREF_MEDIA_PREVIEW_ENABLED_FOR_LOCATION.equals(key)) {
                    enableDisableMediaPreviewPreferences(null, (Set) o);
                } else if (PREF_MEDIA_PREVIEW_STRATEGIES.equals(key)) {
                    // this method will show a toast on error
                    valid = Config.parseConfigSafe((String) o) != null;
                } else if (PREF_UPLOAD_ACCEPT.equals(key)) {
                    enableDisableUploadingPreferences((String) o);
                } else if (PREF_UPLOAD_AUTHENTICATION.equals(key)) {
                    showHideBasicAuthentication((String) o);
                } else if (PREF_UPLOAD_REGEX.equals(key)) {
                    if (((String) o).length() > 0) {
                        HttpUriGetter.fromRegex((String) o);
                    }
                } else if (PREF_UPLOAD_ADDITIONAL_HEADERS.equals(key)) {
                    RequestModifier.additionalHeaders((String) o);
                }
            } catch (Exception e) {
                valid = false;
                ErrorToast.show(R.string.error__etc__prefix, e.getMessage());
            }

            if (!valid && errorResource != -1)
                ErrorToast.show(errorResource);
            return valid;
         }

        // this hides and shows ssl / websocket / ssh preference screens
        // must not be called when the settings do not exist in the tree
        private void showHideStuff(String type) {
            sslGroup.setVisible(Utils.isAnyOf(type, PREF_TYPE_SSL, PREF_TYPE_WEBSOCKET_SSL));
            sshGroup.setVisible(PREF_TYPE_SSH.equals(type));
            wsPath.setVisible(Utils.isAnyOf(type, PREF_TYPE_WEBSOCKET, PREF_TYPE_WEBSOCKET_SSL));
        }

        private void switchSshAuthenticationMethodPreferences(@Nullable String method) {
            if (method == null) method = getPreferenceScreen().getSharedPreferences().getString(
                    PREF_SSH_AUTHENTICATION_METHOD, PREF_SSH_AUTHENTICATION_METHOD_D);
            boolean key = PREF_SSH_AUTHENTICATION_METHOD_KEY.equals(method);
            findPreference(PREF_SSH_KEY_FILE).setVisible(key);
            findPreference(PREF_SSH_PASSWORD).setVisible(!key);
        }

        // visually disable and uncheck the theme switch preference if the theme is chosen by system
        private void enableDisableThemeSwitch(String theme) {
            CheckBoxPreference themeSwitchPreference = getPreferenceScreen().findPreference(PREF_THEME_SWITCH);
            if (themeSwitchPreference == null) return;

            boolean system = PREF_THEME_SYSTEM.equals(theme);
            themeSwitchPreference.setEnabled(!system);
            if (system) themeSwitchPreference.setChecked(false);
        }

        private void enableDisableGestureExclusionZoneSwitch() {
            Preference p = getPreferenceScreen().findPreference(PREF_USE_GESTURE_EXCLUSION_ZONE);
            if (p != null) p.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        }

        private void enableDisableMediaPreviewPreferences(String network, Set location) {
            if (network == null) network = getPreferenceScreen().getSharedPreferences().getString(
                    PREF_MEDIA_PREVIEW_ENABLED_FOR_NETWORK, PREF_MEDIA_PREVIEW_ENABLED_FOR_NETWORK_D);
            if (location == null) location = getPreferenceScreen().getSharedPreferences().getStringSet(
                    PREF_MEDIA_PREVIEW_ENABLED_FOR_LOCATION, PREF_MEDIA_PREVIEW_ENABLED_FOR_LOCATION_D);
            boolean networkEnabled = !PREF_MEDIA_PREVIEW_ENABLED_FOR_NETWORK_NEVER.equals(network);
            boolean locationEnabled = !location.isEmpty();

            for (String key : new String[] {
                    PREF_MEDIA_PREVIEW_SECURE_REQUEST,
                    PREF_MEDIA_PREVIEW_HELP,
                    PREF_MEDIA_PREVIEW_STRATEGIES,
                    PREF_MEDIA_PREVIEW_ADVANCED_GROUP
            }) {
                Preference p = findPreference(key);
                if (p != null) p.setEnabled(networkEnabled && locationEnabled);
            }

            Preference p = findPreference(PREF_MEDIA_PREVIEW_ENABLED_FOR_LOCATION);
            if (p != null) p.setEnabled(networkEnabled);
        }


        private void enableDisableUploadingPreferences(@Nullable String enabled) {
            if (enabled == null) enabled = getPreferenceScreen().getSharedPreferences().getString(
                    PREF_UPLOAD_ACCEPT, PREF_UPLOAD_ACCEPT_D);
            boolean showSettings = !PREF_UPLOAD_ACCEPT_TEXT_ONLY.equals(enabled);
            for (String key : new String[]{
                    PREF_UPLOAD_URI,
                    PREF_UPLOAD_FORM_FIELD_NAME,
                    PREF_UPLOAD_REGEX,
                    PREF_UPLOAD_HELP,
                    PREF_UPLOAD_ADVANCED_GROUP,
            }) {
                Preference p = findPreference(key);
                if (p != null) p.setEnabled(showSettings);
            }
        }

        private void showHideBasicAuthentication(@Nullable String authentication) {
            if (authentication == null)
                authentication = getPreferenceScreen().getSharedPreferences().getString(
                        PREF_UPLOAD_AUTHENTICATION, PREF_UPLOAD_AUTHENTICATION_D);
            boolean basic = "basic".equals(authentication);
            Preference p = findPreference(PREF_UPLOAD_AUTHENTICATION_BASIC_USER);
            if (p != null) p.setVisible(basic);
            p = findPreference(PREF_UPLOAD_AUTHENTICATION_BASIC_PASSWORD);
            if (p != null) p.setVisible(basic);
        }

        // recursively make all currently visible preference titles multiline
        private static void fixMultiLineTitles(PreferenceGroup screen) {
            for (int i = 0; i < screen.getPreferenceCount(); i++) {
                Preference p = screen.getPreference(i);
                p.setSingleLineTitle(false);
                if (p instanceof PreferenceGroup && !(p instanceof PreferenceScreen)) fixMultiLineTitles((PreferenceGroup) p);
            }
        }

        DialogFragment makeFragment(DialogFragmentGetter preference, int code) {
            DialogFragment fragment = preference.getDialogFragment();
            Bundle bundle = new Bundle(1);
            bundle.putString("key", ((Preference) preference).getKey());
            if (code != -1) bundle.putInt("code", code);
            fragment.setArguments(bundle);
            return fragment;
        }
    }
}