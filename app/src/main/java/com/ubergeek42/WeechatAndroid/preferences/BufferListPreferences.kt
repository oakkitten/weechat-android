package com.ubergeek42.WeechatAndroid.preferences

import com.ubergeek42.WeechatAndroid.preferences.BooleanPreference as B

object BufferListPreferences {
    val sortByHot = B("sort_buffers", false)
    val hideHidden = B("hide_hidden_buffers", true)
    val filterNonHuman = B("filter_nonhuman_buffers", true)
    val showFilter = B("show_buffer_filter", true)
    val useGestureExclusionZone = B("use_gesture_exclusion_zone", true)
}