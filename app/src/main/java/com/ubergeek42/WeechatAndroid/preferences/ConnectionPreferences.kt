@file:Suppress("SimplifyBooleanWithConstants")

package com.ubergeek42.WeechatAndroid.preferences

import com.ubergeek42.WeechatAndroid.service.SSLHandler
import com.ubergeek42.WeechatAndroid.preferences.TextPreference as T
import com.ubergeek42.WeechatAndroid.preferences.BooleanPreference as B
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference as E
import com.ubergeek42.WeechatAndroid.preferences.NotQuiteAPreference as Q
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference.Values as EV
import com.ubergeek42.WeechatAndroid.preferences.IntPreference as I


object WebSocketPreferences {
    @JvmField val path = T("ws_path", "weechat")
}


object SslPreferences {
    @JvmField val pinRequired = B("ssl_pin_required", false)
    @JvmField val clearCerts = Q("ssl_clear_certs").disableUnless {
        SSLHandler.getInstance(context).userCertificateCount > 0
    }
    @JvmField val clientCertificate = Q("ssl_client_certificate")
}


object RelayPreferences {
    @JvmField val host = T("host", "").addValidator(::ensureNoSpaces)
    @JvmField val port = I("port", "9001").addValidator(::validPort)
    @JvmField val password = T("password", "")
}


object PingSettings {
    @JvmField val enabled = B("ping_enabled", true)
    @JvmField val idleTime: SecondsPreference = SecondsPreference("ping_idle", "300").disableUnless { enabled.value == true }
    @JvmField val timeout = SecondsPreference("ping_timeout", "30").disableUnless { enabled.value == true }
}


object ConnectionPreferences {
    enum class Type(
        override val value: String,
        @JvmField val usesSsl: Boolean = false
    ) : EV {
        Plain("plain"),
        Secure("ssl", usesSsl = true),
        Ssh("ssh"),
        Websocket("websocket"),
        WebsocketSecure("websocket-ssl", usesSsl = true),
    }

    @JvmField val type = E("connection_type", Type.Plain, Type.values())

    @JvmField val webSocket = WebSocketPreferences

    val sslGroup = Q("ssl_group")
    @JvmField val ssl = SslPreferences

    val sshGroup = Q("ssh_group")
    @JvmField val ssh = SshPreferences

    @JvmField val relay = RelayPreferences

    @JvmField val numberOfLinesToLoad = I("line_increment", "300")
    @JvmField val reconnectOnConnectionLoss = B("reconnect", true)
    @JvmField val connectOnBoot = B("boot_connect", false)
    @JvmField val onlySyncOpenBuffers = B("optimize_traffic", false)
    @JvmField val syncBufferReadStatus = B("hotlist_sync", false)

    @JvmField val ping = PingSettings

    init {
        webSocket.path.hideUnless {
            type.value == Type.Websocket || type.value == Type.WebsocketSecure
        }

        sslGroup.hideUnless {
            type.value == Type.Secure || type.value == Type.Secure
        }

        sshGroup.hideUnless { type.value == Type.Ssh }
    }
}