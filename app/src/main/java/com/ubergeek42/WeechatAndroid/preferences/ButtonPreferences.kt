package com.ubergeek42.WeechatAndroid.preferences

import com.ubergeek42.WeechatAndroid.preferences.BooleanPreference as B

object ButtonPreferences {
    @JvmField val showTab = B("tabbtn_show", true)
    @JvmField val showSend = B("sendbtn_show", true)
    @JvmField val volumeButtonsChangeTextSize = B("volumebtn_size", true)
}