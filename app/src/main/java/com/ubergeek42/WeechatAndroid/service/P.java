// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.

package com.ubergeek42.WeechatAndroid.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ThemeManager;

import com.ubergeek42.WeechatAndroid.R;
import com.ubergeek42.WeechatAndroid.Weechat;
import com.ubergeek42.WeechatAndroid.media.Config;
import com.ubergeek42.WeechatAndroid.preferences.ConnectionPreferences;
import com.ubergeek42.WeechatAndroid.preferences.LookAndFeelPreferences;
import com.ubergeek42.WeechatAndroid.preferences.Pref;
import com.ubergeek42.WeechatAndroid.preferences.SshPreferences;
import com.ubergeek42.WeechatAndroid.relay.Buffer;
import com.ubergeek42.WeechatAndroid.relay.BufferList;
import com.ubergeek42.WeechatAndroid.upload.UploadingConfigKt;
import com.ubergeek42.WeechatAndroid.utils.MigratePreferences;
import com.ubergeek42.WeechatAndroid.utils.ThemeFix;
import com.ubergeek42.WeechatAndroid.utils.Utils;
import com.ubergeek42.cats.Cat;
import com.ubergeek42.cats.CatD;
import com.ubergeek42.cats.Kitty;
import com.ubergeek42.cats.Root;
import com.ubergeek42.weechat.Color;
import com.ubergeek42.weechat.ColorScheme;
import com.ubergeek42.weechat.relay.connection.SSHConnection;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import javax.net.ssl.SSLSocketFactory;

import static com.ubergeek42.WeechatAndroid.preferences.ThemePreferences.applyThemePreference;
import static com.ubergeek42.WeechatAndroid.utils.Constants.*;


@SuppressWarnings("AccessStaticViaInstance")
public class P implements SharedPreferences.OnSharedPreferenceChangeListener{
    final private static @Root Kitty kitty = Kitty.make();

    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static SharedPreferences p;

    // we need to keep a reference, huh
    @SuppressLint("StaticFieldLeak")
    private static P instance;

    @MainThread public static void init(@NonNull Context context) {
        if (instance != null) return;
        instance = new P();
        P.context = context;
        Pref.register();
        p = PreferenceManager.getDefaultSharedPreferences(context);
        new MigratePreferences(context).migrate();
        loadUIPreferences();
        p.registerOnSharedPreferenceChangeListener(instance);
        calculateWeaselWidth();
        Config.initPreferences();
        UploadingConfigKt.initPreferences();
    }

    // sets the width of weasel (effectively the recycler view) for LineView. this is a workaround
    // necessary in order to circumvent a bug (?) in ViewPager: sometimes, when measuring views, the
    // RecyclerView will have a width of 0 (esp. when paging through buffers fast) and hence
    // LineView will receive a suggested maximum width of 0 in its onMeasure().
    //      note: other views in RecyclerView don't seem to have this problem. they either receive
    //      correct values or somehow recover from width 0. the difference seems to lie in the fact
    //      that they are inflated, and not created programmatically.
    // this method is called from onStart() instead of onCreate() as onCreate() is called when the
    // activities get recreated due to theme/battery state change. for some reason, the activities
    // get recreated even though the user is using another app; if it happens in the wrong screen
    // orientation, the value is wrong.
    // todo: switch to ViewPager2 and get rid of this nonsense
    public static @Cat void calculateWeaselWidth() {
        int windowWidth = context.getResources().getDisplayMetrics().widthPixels;
        boolean slidy = context.getResources().getBoolean(R.bool.slidy);
        P.weaselWidth = slidy ? windowWidth :
                windowWidth - context.getResources().getDimensionPixelSize(R.dimen.drawer_width);
    }

    // set colorPrimary and colorPrimaryDark according to color scheme or app theme
    // must be called after theme change (activity.onCreate(), for ThemeFix.fixIconAndColor()) and
    // after color scheme change (onStart(), as in this case the activity is not recreated, before applyColorSchemeToViews())
    // this method could be called from elsewhere but it needs *activity* context
    public static void storeThemeOrColorSchemeColors(Context context) {
        ColorScheme scheme = ColorScheme.get();
        TypedArray colors = context.obtainStyledAttributes(
                new int[] {R.attr.colorPrimary, R.attr.colorPrimaryDark, R.attr.toolbarIconColor});
        colorPrimary = scheme.colorPrimary != ColorScheme.NO_COLOR ?
                scheme.colorPrimary : colors.getColor(0, ColorScheme.NO_COLOR);
        colorPrimaryDark = scheme.colorPrimaryDark != ColorScheme.NO_COLOR ?
                scheme.colorPrimaryDark : colors.getColor(1, ColorScheme.NO_COLOR);
        toolbarIconColor = colors.getColor(2, ColorScheme.NO_COLOR);
        colors.recycle();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////// ui

    final public static float _1dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
            Weechat.applicationContext.getResources().getDisplayMetrics());
    final public static float _1_33dp = Math.round(1.33f * _1dp);
    final public static float _4dp = 4 * _1dp;
    final public static float _200dp = 200 * _1dp;

    public static boolean sortBuffers;
    public static boolean filterBuffers;
    public static boolean hideHiddenBuffers;
    public static boolean optimizeTraffic;
    public static boolean filterLines, autoHideActionbar;
    public static int maxWidth;
    public static boolean encloseNick, dimDownNonHumanLines;
    public static @Nullable DateTimeFormatter dateFormat;
    public static int align;

    public static int weaselWidth = 200;
    public static float textSize, letterWidth;
    public static TextPaint textPaint;

    static boolean notificationEnable;
    static boolean notificationTicker;
    static boolean notificationLight;
    static boolean notificationVibrate;
    static String notificationSound;

    public static boolean showSend, showTab, hotlistSync, volumeBtnSize;

    public static boolean showBufferFilter;

    public static boolean themeSwitchEnabled;
    public static boolean darkThemeActive = false;

    public static int colorPrimary = ColorScheme.NO_COLOR;
    public static int colorPrimaryDark = ColorScheme.NO_COLOR;
    public static int toolbarIconColor = ColorScheme.NO_COLOR;

    @MainThread private static void loadUIPreferences() {
        // buffer list preferences
        sortBuffers = Pref.bufferList.sortByHot.getValue();
        filterBuffers = Pref.bufferList.filterNonHuman.getValue();
        hideHiddenBuffers = Pref.bufferList.hideHidden.getValue();
        optimizeTraffic = Pref.connection.onlySyncOpenBuffers.getValue();  // okay this is out of sync with onChanged stuff—used for the bell icon

        // buffer-wide preferences
        filterLines = Pref.lookNFeel.filterLines.getValue();
        autoHideActionbar = Pref.lookNFeel.autoHideToolbar.getValue();
        maxWidth = Pref.lookNFeel.maxPrefixWidth.getValue();
        encloseNick = Pref.lookNFeel.encloseNick.getValue();
        dimDownNonHumanLines = Pref.theme.dimDownNonHumanLines.getValue();
        setTimestampFormat();
        setAlignment();

        // theme
        applyThemePreference();
        themeSwitchEnabled = Pref.theme.themeSwitch.getValue();

        // notifications
        notificationEnable = Pref.notifications.enable.getValue();
        notificationSound = Pref.notifications.sound.getValue();
        notificationTicker = Pref.notifications.ticker.getValue();
        notificationLight = Pref.notifications.light.getValue();
        notificationVibrate = Pref.notifications.vibrate.getValue();

        // buffer fragment
        showSend = Pref.buttons.showSend.getValue();
        showTab = Pref.buttons.showTab.getValue();
        hotlistSync = Pref.connection.syncBufferReadStatus.getValue();
        volumeBtnSize = Pref.buttons.volumeButtonsChangeTextSize.getValue();

        // buffer list filter
        showBufferFilter = Pref.bufferList.showFilter.getValue();
    }



    public static void applyThemeAfterActivityCreation(AppCompatActivity activity) {
        darkThemeActive = ThemeFix.isNightModeEnabledForActivity(activity);
        changeColorScheme();
    }

    // todo optimize this method—might be too expensive
    private static @CatD void changeColorScheme() {
        ThemeManager.loadColorSchemeFromPreferences(context);
        setTextSizeColorAndLetterWidth();
        BufferList.onGlobalPreferencesChanged(false);
    }

    ///////////////////////////////////////////////////////////////////////////////////// connection

    public static String host;
    static String wsPath;
    static String pass;
    static ConnectionPreferences.Type connectionType;
    static String sshHost;
    static String sshUser;
    static SSHConnection.AuthenticationMethod sshAuthenticationMethod;
    static String sshPassword;
    static byte[] sshSerializedKey;
    static public int port;
    static int sshPort;
    static SSLSocketFactory sslSocketFactory;
    static boolean reconnect;
    static public boolean pinRequired;

    static boolean pingEnabled;
    static long pingIdleTime, pingTimeout;
    public static int lineIncrement;

    static String printableHost;
    static boolean connectionSurelyPossibleWithCurrentPreferences;

    @MainThread public static void loadConnectionPreferences() {
        host = Pref.connection.relay.host.getValue();
        pass = Pref.connection.relay.password.getValue();
        port = Pref.connection.relay.port.getValue();
        wsPath = Pref.connection.webSocket.path.getValue();;
        pinRequired = Pref.connection.ssl.pinRequired.getValue();

        connectionType = Pref.connection.type.getValue();
        sshHost = Pref.connection.ssh.host.getValue();
        sshPort = Pref.connection.ssh.port.getValue();
        sshUser = Pref.connection.ssh.user.getValue();
        sshAuthenticationMethod =
                Pref.connection.ssh.authenticationMethod.getValue() == SshPreferences.AuthenticationMethod.Key ?
                SSHConnection.AuthenticationMethod.KEY : SSHConnection.AuthenticationMethod.PASSWORD;
        sshPassword = Pref.connection.ssh.password.getValue();
        sshSerializedKey = Pref.connection.ssh.serializedKey.getValue();

        lineIncrement = Pref.connection.numberOfLinesToLoad.getValue();
        reconnect = Pref.connection.reconnectOnConnectionLoss.getValue();
        optimizeTraffic = Pref.connection.onlySyncOpenBuffers.getValue();

        pingEnabled = Pref.connection.ping.enabled.getValue();
        pingIdleTime = Pref.connection.ping.idleTime.getMilliseconds();
        pingTimeout = Pref.connection.ping.timeout.getMilliseconds();

        if (connectionType.usesSsl) {
            sslSocketFactory = SSLHandler.getInstance(context).getSSLSocketFactory();
        } else {
            sslSocketFactory = null;
        }

        printableHost = connectionType == ConnectionPreferences.Type.Ssh ? sshHost + "/" + host : host;
        connectionSurelyPossibleWithCurrentPreferences = false;     // and don't call me Shirley
    }

    @MainThread public static @StringRes int validateConnectionPreferences() {
        if (TextUtils.isEmpty(host)) return R.string.error__pref_validation__relay_host_not_set;
        if (TextUtils.isEmpty(pass)) return R.string.error__pref_validation__relay_password_not_set;
        if (connectionType == ConnectionPreferences.Type.Ssh) {
            if (TextUtils.isEmpty(sshHost)) return R.string.error__pref_validation__ssh_host_not_set;
            if (sshAuthenticationMethod == SSHConnection.AuthenticationMethod.KEY) {
                if (Utils.isEmpty(sshSerializedKey)) return R.string.error__pref_validation__ssh_key_not_set;
            } else {
                if (TextUtils.isEmpty(sshPassword)) return R.string.error__pref_validation__ssh_password_not_set;
            }
        }
        return 0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @MainThread @Override @CatD public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            // buffer list preferences
            case PREF_SORT_BUFFERS: sortBuffers = Pref.bufferList.sortByHot.getValue(); break;
            case PREF_FILTER_NONHUMAN_BUFFERS: filterBuffers = Pref.bufferList.filterNonHuman.getValue(); break;
            case PREF_HIDE_HIDDEN_BUFFERS: hideHiddenBuffers = Pref.bufferList.hideHidden.getValue(); break;
            case PREF_AUTO_HIDE_ACTIONBAR: autoHideActionbar = Pref.lookNFeel.autoHideToolbar.getValue(); break;

            // buffer-wide preferences
            case PREF_FILTER_LINES:
                filterLines = Pref.lookNFeel.filterLines.getValue();
                BufferList.onGlobalPreferencesChanged(true);
                break;
            case PREF_MAX_WIDTH:
                maxWidth = Pref.lookNFeel.maxPrefixWidth.getValue();
                BufferList.onGlobalPreferencesChanged(false);
                break;
            case PREF_ENCLOSE_NICK:
                encloseNick = Pref.lookNFeel.encloseNick.getValue();
                BufferList.onGlobalPreferencesChanged(false);
                break;
            case PREF_DIM_DOWN:
                dimDownNonHumanLines = Pref.theme.dimDownNonHumanLines.getValue();
                BufferList.onGlobalPreferencesChanged(false);
                break;
            case PREF_THEME_SWITCH:
                themeSwitchEnabled = Pref.theme.themeSwitch.getValue();
                break;
            case PREF_TIMESTAMP_FORMAT:
                setTimestampFormat();
                BufferList.onGlobalPreferencesChanged(false);
                break;
            case PREF_PREFIX_ALIGN:
                setAlignment();
                BufferList.onGlobalPreferencesChanged(false);
                break;
            case PREF_TEXT_SIZE:
            case PREF_BUFFER_FONT:
                setTextSizeColorAndLetterWidth();
                BufferList.onGlobalPreferencesChanged(false);
                break;
            case PREF_THEME:
                applyThemePreference();
                break;
            case PREF_COLOR_SCHEME_DAY:
            case PREF_COLOR_SCHEME_NIGHT:
                changeColorScheme();
                break;

            // notifications
            case PREF_NOTIFICATION_ENABLE: notificationEnable = Pref.notifications.enable.getValue(); break;
            case PREF_NOTIFICATION_SOUND: notificationSound = Pref.notifications.sound.getValue(); break;
            case PREF_NOTIFICATION_TICKER: notificationTicker = Pref.notifications.ticker.getValue(); break;
            case PREF_NOTIFICATION_LIGHT: notificationLight = Pref.notifications.light.getValue(); break;
            case PREF_NOTIFICATION_VIBRATE: notificationVibrate = Pref.notifications.vibrate.getValue(); break;

            // buffer fragment
            case PREF_SHOW_SEND: showSend = Pref.buttons.showSend.getValue(); break;
            case PREF_SHOW_TAB: showTab = Pref.buttons.showTab.getValue(); break;
            case PREF_HOTLIST_SYNC: hotlistSync = Pref.connection.syncBufferReadStatus.getValue(); break;
            case PREF_VOLUME_BTN_SIZE: volumeBtnSize = Pref.buttons.volumeButtonsChangeTextSize.getValue(); break;

            // buffer list fragment
            case PREF_SHOW_BUFFER_FILTER: showBufferFilter = Pref.bufferList.showFilter.getValue(); break;

            default:
                Config.onSharedPreferenceChanged(p, key);
                UploadingConfigKt.onSharedPreferenceChanged(p, key);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @MainThread private static void setTimestampFormat() {
        String t = Pref.lookNFeel.timestampFormat.getValue();
        dateFormat = (TextUtils.isEmpty(t)) ? null : DateTimeFormat.forPattern(t);
    }

    @MainThread private static void setAlignment() {
        LookAndFeelPreferences.Align alignment = Pref.lookNFeel.alignment.getValue();
        switch (alignment) {
            case Right:     align = Color.ALIGN_RIGHT; break;
            case Left:      align = Color.ALIGN_LEFT; break;
            case Timestamp: align = Color.ALIGN_TIMESTAMP; break;
            default:        align = Color.ALIGN_NONE; break;
        }
    }

    @MainThread private static void setTextSizeColorAndLetterWidth() {
        textSize = Pref.lookNFeel.textSize.getValue();
        String bufferFont = Pref.lookNFeel.bufferFont.getValue();

        Typeface typeface = Typeface.MONOSPACE;
        try {typeface = Typeface.createFromFile(bufferFont);} catch (Exception ignored) {}

        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(typeface);
        textPaint.setColor(0xFF000000 | ColorScheme.get().default_color[0]);
        textPaint.setTextSize(textSize * context.getResources().getDisplayMetrics().scaledDensity);

        letterWidth = (textPaint.measureText("m"));
    }

    @MainThread public static void setTextSizeColorAndLetterWidth(float size) {
        p.edit().putString(PREF_TEXT_SIZE, Float.toString(size)).apply();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    final private static String ALIVE = "alive";

    public static boolean isServiceAlive() {
        return p.getBoolean(ALIVE, false);
    }

    static void setServiceAlive(boolean alive) {
        p.edit().putBoolean(ALIVE, alive).apply();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////// save/restore
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private final static String PREF_DATA = "sb";
    private final static String PREF_PROTOCOL_ID = "pid";

    // protocol must be changed each time anything that uses the following function changes
    // needed to make sure nothing crashes if we cannot restore the data
    private static final int PROTOCOL_ID = 18;

    @AnyThread @Cat public static void saveStuff() {
        for (Buffer buffer : BufferList.buffers) saveLastReadLine(buffer);
        String data = Utils.serialize(new Object[]{openBuffers, bufferToLastReadLine, sentMessages});
        p.edit().putString(PREF_DATA, data).putInt(PREF_PROTOCOL_ID, PROTOCOL_ID).apply();
    }

    @SuppressWarnings("unchecked")
    @MainThread @Cat public static void restoreStuff() {
        if (p.getInt(PREF_PROTOCOL_ID, -1) != PROTOCOL_ID) return;
        Object o = Utils.deserialize(p.getString(PREF_DATA, null));
        if (!(o instanceof Object[])) return;
        Object[] array = (Object[]) o;
        if (array[0] instanceof LinkedHashSet) openBuffers = (LinkedHashSet<Long>) array[0];
        if (array[1] instanceof LinkedHashMap) bufferToLastReadLine = (LinkedHashMap<Long, BufferHotData>) array[1];
        if (array[2] instanceof LinkedList) sentMessages = (LinkedList<String>) array[2];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // contains names of open buffers. needs more synchronization?
    static public @NonNull LinkedHashSet<Long> openBuffers = new LinkedHashSet<>();

    // this stores information about last read line (in `desktop` weechat) and according number of
    // read lines/highlights. this is subtracted from highlight counts client receives from the server
    static private @NonNull LinkedHashMap<Long, BufferHotData> bufferToLastReadLine = new LinkedHashMap<>();

    synchronized public static boolean isBufferOpen(long pointer) {
        return openBuffers.contains(pointer);
    }

    synchronized public static void setBufferOpen(long pointer, boolean open) {
        if (open) openBuffers.add(pointer);
        else openBuffers.remove(pointer);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private static class BufferHotData implements Serializable {
        long lastSeenLine = -1;
        long lastReadLineServer = -1;
        int readUnreads = 0;
        int readHighlights = 0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // restore buffer's stuff. this is called for every buffer upon buffer creation
    @WorkerThread synchronized public static void restoreLastReadLine(Buffer buffer) {
        BufferHotData data = bufferToLastReadLine.get(buffer.pointer);
        if (data != null) {
            buffer.setLastSeenLine(data.lastSeenLine);
            buffer.lastReadLineServer = data.lastReadLineServer;
            buffer.readUnreads = data.readUnreads;
            buffer.readHighlights = data.readHighlights;
        }
    }

    // save buffer's stuff. this is called when information is about to be written to disk
    private synchronized static void saveLastReadLine(Buffer buffer) {
        BufferHotData data = bufferToLastReadLine.get(buffer.pointer);
        if (data == null) {
            data = new BufferHotData();
            bufferToLastReadLine.put(buffer.pointer, data);
        }
        data.lastSeenLine = buffer.getLastSeenLine();
        data.lastReadLineServer = buffer.lastReadLineServer;
        data.readUnreads = buffer.readUnreads;
        data.readHighlights = buffer.readHighlights;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////// saving messages
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static LinkedList<String> sentMessages = new LinkedList<>();

    static void addSentMessage(String line) {
        for (Iterator<String> it = sentMessages.iterator(); it.hasNext();) {
            String s = it.next();
            if (line.equals(s)) it.remove();
        }
        sentMessages.add(Utils.cut(line, 2000));
        if (sentMessages.size() > 40)
            sentMessages.pop();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private static String getString(String key, String defValue) {
        return p.getString(key, defValue);
    }
}
