package com.ubergeek42.WeechatAndroid.preferences

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.ubergeek42.WeechatAndroid.preferences.Preference.Companion.whenChanged
import com.ubergeek42.WeechatAndroid.preferences.BooleanPreference as B
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference as E
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference.Values as EV
import com.ubergeek42.WeechatAndroid.preferences.TextPreference as T

object ThemePreferences {
    enum class Theme(override val value: String) : EV {
        System("system"),
        Dark("dark"),
        Light("light"),
    }

    @JvmField val theme = E("theme", Theme.System, Theme.values())
    @JvmField val themeSwitch = B("theme_switch", false).disableUnless { theme.value != Theme.System }
    @JvmField val colorSchemeDay = T("color_scheme_day", "squirrely-light-theme.properties")
    @JvmField val colorSchemeNight = T("color_scheme_night", "squirrely-dark-theme.properties")
    @JvmField val dimDownNonHumanLines = B("dim_down", true)

    // a brief recap on how themes work here
    // * first, we set night mode here for the whole application. applyThemePreference() does't know
    //   about activities. at this point we can't tell the effective theme, as activities can have
    //   their own local settings. this call will recreate activities, if necessary.
    // * after an activity is created, applyThemeAfterActivityCreation() is called. that's when we
    //   know the actual theme that is going to be used. this theme will be used during the whole
    //   lifecycle of the activity; if changed—by the user or the system—the activity is recreated.
    // * color scheme can be changed without changing the theme. so we call it on activity creation
    //   an on preference change.
    @JvmStatic fun applyThemePreference() {
        AppCompatDelegate.setDefaultNightMode(when (theme.value) {
            Theme.System -> systemThemeFlag
            Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
        })
    }

    init {
        whenChanged(theme) { applyThemePreference() }
    }
}

private val systemThemeFlag = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY else
    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
