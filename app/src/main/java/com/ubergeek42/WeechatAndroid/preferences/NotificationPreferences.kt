@file:Suppress("SimplifyBooleanWithConstants")

package com.ubergeek42.WeechatAndroid.preferences

import com.ubergeek42.WeechatAndroid.preferences.BooleanPreference as B
import com.ubergeek42.WeechatAndroid.preferences.TextPreference as T

object NotificationPreferences {
    @JvmField val enable = B("notification_enable", true)
    @JvmField val sound = T("notification_sound", "content://settings/system/notification_sound")
    @JvmField val vibrate = B("notification_vibrate", true)
    @JvmField val light = B("notification_light", true)
    @JvmField val ticker = B("notification_ticker", true)

    init {
        listOf(sound, vibrate, light, ticker).disableUnless { enable.value == true }
    }
}