package com.ubergeek42.WeechatAndroid.preferences

import android.os.Build
import com.ubergeek42.WeechatAndroid.R
import com.ubergeek42.WeechatAndroid.media.Config
import com.ubergeek42.WeechatAndroid.media.Config.parseConfig
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference as E
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference.Values as EV
import com.ubergeek42.WeechatAndroid.preferences.EnumSetPreference as ES
import com.ubergeek42.WeechatAndroid.preferences.IntPreference as I
import com.ubergeek42.WeechatAndroid.preferences.NotQuiteAPreference as Q
import com.ubergeek42.WeechatAndroid.preferences.TextPreference as T

object MediaPreviewPreferences {
    enum class When(override val value: String) : EV {
        Never("never"),
        OnWifiOnly("wifi_only"),
        OnUnmeteredNetworksOnly("unmetered_only"),
        Always("always"),
    }

    enum class Context(override val value: String) : EV {
        Chat("chat"),
        Paste("paste"),
        Notifications("notifications");

        companion object {
            val default = if (Build.VERSION.SDK_INT >= 24) {
                setOf(Chat, Paste, Notifications)
            } else {
                setOf(Chat, Paste)
            }
        }
    }

    enum class InsecureRequests(override val value: String) : EV {
        Allow("optional"),
        RewriteAsHttps("rewrite"),
        Disallow("required"),
    }

    val enabledWhen = E("media_preview_enabled_for_network", When.Never, When.values())

    val enabledForContext = ES("media_preview_enabled_for_location", Context.default, Context.values())
            .disableUnless { enabledWhen.value != When.Never }

    val insecureRequests = E("media_preview_secure_request", InsecureRequests.RewriteAsHttps,
                                                             InsecureRequests.values())

    val help = Q("media_preview_help")

    val strategies = T("media_preview_strategies", context.resources.getString(
            R.string.pref__media_preview__strategies_default))
            .addValidator(Config::parseConfig)

    val advancedGroup = Q("media_preview_advanced_group")

    val maximumBodySize = MegabytesPreference("media_preview_maximum_body_size", "10")
    val diskCacheSize = MegabytesPreference("image_disk_cache_size", "250")

    val successCooldown = HoursPreference("media_preview_success_cooldown", "24")

    val thumbnailWidth = I("media_preview_thumbnail_width", "80")
    val thumbnailMinHeight = I("media_preview_thumbnail_min_height", "40")
    val thumbnailMaxHeight = I("media_preview_thumbnail_max_height", "160")

    init {
        setOf(insecureRequests, help, strategies, advancedGroup).disableUnless {
            enabledWhen.value != When.Never && enabledForContext.value.isNotEmpty()
        }
    }
}