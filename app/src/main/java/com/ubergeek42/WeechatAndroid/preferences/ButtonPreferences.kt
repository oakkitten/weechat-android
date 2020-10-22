package com.ubergeek42.WeechatAndroid.preferences

import com.ubergeek42.WeechatAndroid.preferences.BooleanPreference as B

object ButtonPreferences {
    val showTab = B("tabbtn_show", true)
    val showSend = B("sendbtn_show", true)
    val volumeButtonsChangeTextSize = B("volumebtn_size", true)
}