package com.ubergeek42.WeechatAndroid.preferences

import com.ubergeek42.WeechatAndroid.preferences.BooleanPreference as B

object BufferListPreferences {
    @JvmField val sortByHot = B("sort_buffers", false)
    @JvmField val hideHidden = B("hide_hidden_buffers", true)
    @JvmField val filterNonHuman = B("filter_nonhuman_buffers", true)
    @JvmField val showFilter = B("show_buffer_filter", true)
    @JvmField val useGestureExclusionZone = B("use_gesture_exclusion_zone", true)
}