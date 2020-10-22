@file:Suppress("SimplifyBooleanWithConstants")

package com.ubergeek42.WeechatAndroid.preferences

import com.ubergeek42.WeechatAndroid.preferences.BooleanPreference as B
import com.ubergeek42.WeechatAndroid.preferences.TextPreference as T

object NotificationPreferences {
    val enable = B("notification_enable", true)
    val sound = T("notification_sound", "content://settings/system/notification_sound")
    val vibrate = B("notification_vibrate", true)
    val lights = B("notification_light", true)
    val ticker = B("notification_ticker", true)

    init {
        listOf(sound, vibrate, lights, ticker).disableUnless { enable.value == true }
    }
}