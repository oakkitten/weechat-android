package com.ubergeek42.WeechatAndroid.preferences

import android.annotation.SuppressLint
import org.joda.time.format.DateTimeFormat
import java.text.SimpleDateFormat
import com.ubergeek42.WeechatAndroid.preferences.BooleanPreference as B
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference as E
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference.Values as EV
import com.ubergeek42.WeechatAndroid.preferences.FloatPreference as F
import com.ubergeek42.WeechatAndroid.preferences.IntPreference as I
import com.ubergeek42.WeechatAndroid.preferences.TextPreference as T

@SuppressLint("SimpleDateFormat")
object LookAndFeelPreferences {
    enum class Align(override val value: String) : EV {
        Left("left"),
        Right("right"),
        Timestamp("timestamp"),
        None("none"),
    }

    @JvmField val textSize = F("text_size", "16")
    @JvmField val autoHideToolbar = B("auto_hide_actionbar", true)
    @JvmField val filterLines = B("text_size", true)
    @JvmField val alignment = E("prefix_align", Align.Right, Align.values())
    @JvmField val maxPrefixWidth = I("prefix_max_width", "7")
    @JvmField val encloseNick = B("enclose_nick", false)
    @JvmField val timestampFormat = T("timestamp_format", "HH:mm:ss").addValidator {
        DateTimeFormat.forPattern(it)
    }
    @JvmField val bufferFont = T("buffer_font", "")
}