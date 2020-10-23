package com.ubergeek42.WeechatAndroid.preferences

import android.content.SharedPreferences
import com.ubergeek42.cats.Kitty
import com.ubergeek42.cats.Root
import com.ubergeek42.WeechatAndroid.preferences.NotQuiteAPreference as Q

object Pref {
    @Root private val kitty = Kitty.make()


    @JvmField val connection = ConnectionPreferences
    @JvmField val bufferList = BufferListPreferences
    @JvmField val lookNFeel = LookAndFeelPreferences
    @JvmField val theme = ThemePreferences
    @JvmField val buttons = ButtonPreferences

    val notificationsGroup = Q("notif_group")
    @JvmField val notifications = NotificationPreferences

    @JvmField val mediaPreview = MediaPreviewPreferences
    @JvmField val upload = UploadPreferences


    fun getByKey(key: String?) = Preference.preferences[key]

    @Throws(Exception::class)
    fun validate(key: String, value: Any?) {
        val preference = Preference.preferences[key]
                ?: throw Exception("Preference not found: $key")
        preference.validate(value)
    }

    @JvmStatic fun register() {
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener {
                _: SharedPreferences, key: String ->
            val preference = Preference.preferences[key]
            if (preference == null) {
                kitty.wtf("preference unknown: %s", key)
            } else {
                preference.invalidate()
                kitty.debug("preference %s has been changed to %s", key, preference.value)
                Preference.triggers[preference]?.forEach { it() }
            }
        }
    }
}

