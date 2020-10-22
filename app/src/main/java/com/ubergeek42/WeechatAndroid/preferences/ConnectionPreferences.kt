@file:Suppress("SimplifyBooleanWithConstants")

package com.ubergeek42.WeechatAndroid.preferences

import com.ubergeek42.WeechatAndroid.preferences.TextPreference as T
import com.ubergeek42.WeechatAndroid.preferences.BooleanPreference as B
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference as E
import com.ubergeek42.WeechatAndroid.preferences.NotQuiteAPreference as Q
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference.Values as EV
import com.ubergeek42.WeechatAndroid.preferences.IntPreference as I


object WebSocketPreferences {
    val path = T("ws_path", "weechat")
}


object SslPreferences {
    val pinRequired = B("ssl_pin_required", false)
    val clearCerts = Q("ssl_clear_certs")
    val clientCertificate = Q("ssl_client_certificate")
}


object RelayPreferences {
    val host = T("host", "").addValidator(::ensureNoSpaces)
    val port = I("port", "9001").addValidator(::validPort)
    val password = T("password", "")
}


object PingSettings {
    val enabled = B("ping_enabled", true)
    val idleTime = SecondsPreference("ping_idle", "300").disableUnless { enabled.value == true }
    val timeout = SecondsPreference("ping_timeout", "30").disableUnless { enabled.value == true }
}


object ConnectionPreferences {
    enum class Type(override val value: String) : EV {
        Plain("plain"),
        Secure("ssl"),
        Ssh("ssh"),
        Websocket("websocket"),
        WebsocketSecure("websocket-ssl"),
    }

    val type = E("connection_type", Type.Plain, Type.values())

    val webSocket = WebSocketPreferences

    val sslGroup = Q("ssl_group")
    val ssl = SslPreferences

    val sshGroup = Q("ssh_group")
    val ssh = SshPreferences

    val relay = RelayPreferences

    val numberOfLinesToLoad = I("line_increment", "300")
    val reconnectOnConnectionLoss = B("reconnect", true)
    val connectOnBoot = B("boot_connect", false)
    val onlySyncOpenBuffers = B("optimize_traffic", false)
    val syncBufferReadStatus = B("hotlist_sync", false)

    val ping = PingSettings

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