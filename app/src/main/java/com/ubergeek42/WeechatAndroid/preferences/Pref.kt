package com.ubergeek42.WeechatAndroid.preferences

import com.ubergeek42.WeechatAndroid.preferences.NotQuiteAPreference as Q

object Pref {
    val connection = ConnectionPreferences
    val bufferList = BufferListPreferences
    val lookNFeel = LookAndFeelPreferences
    val theme = ThemePreferences
    val buttons = ButtonPreferences

    val notificationsGroup = Q("notif_group")
    val notifications = NotificationPreferences

    val mediaPreview = MediaPreviewPreferences
    val upload = UploadPreferences

    @Throws(Exception::class)
    fun validate(key: String, value: Any) {
        val preference = Preference.preferences[key]
                ?: throw Exception("Preference not found: $key")
        preference.validate(value)
    }
}

