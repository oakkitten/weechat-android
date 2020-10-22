package com.ubergeek42.WeechatAndroid.preferences

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

    val theme = E("theme", Theme.System, Theme.values())
    val themeSwitch = B("theme_switch", false) .disableUnless { theme.value != Theme.System }
    val colorSchemeDay = T("color_scheme_day", "squirrely-light-theme.properties")
    val colorSchemeNight = T("color_scheme_night", "squirrely-dark-theme.properties")
    val dimDownNonHumanLines = B("dim_down", true)
}