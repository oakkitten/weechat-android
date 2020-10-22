package com.ubergeek42.WeechatAndroid.preferences

import android.content.SharedPreferences
import com.ubergeek42.cats.Kitty
import com.ubergeek42.cats.Root
import com.ubergeek42.WeechatAndroid.preferences.NotQuiteAPreference as Q

object Pref {
    @Root private val kitty = Kitty.make()


    val connection = ConnectionPreferences
    val bufferList = BufferListPreferences
    val lookNFeel = LookAndFeelPreferences
    val theme = ThemePreferences
    val buttons = ButtonPreferences

    val notificationsGroup = Q("notif_group")
    val notifications = NotificationPreferences

    val mediaPreview = MediaPreviewPreferences
    val upload = UploadPreferences


    fun getByKey(key: String?) = Preference.preferences[key]

    @Throws(Exception::class)
    fun validate(key: String, value: Any?) {
        val preference = Preference.preferences[key]
                ?: throw Exception("Preference not found: $key")
        preference.validate(value)
    }

    @JvmStatic fun register() {
        sharedPreferences.registerOnSharedPreferenceChangeListener {
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

