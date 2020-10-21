package com.ubergeek42.WeechatAndroid.preferences

import androidx.preference.PrivateKeyPickerPreference
import com.ubergeek42.weechat.relay.connection.SSHServerKeyVerifier
import com.ubergeek42.WeechatAndroid.preferences.TextPreference as T
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference as E
import com.ubergeek42.WeechatAndroid.preferences.NotQuiteAPreference as Q
import com.ubergeek42.WeechatAndroid.preferences.StringRetrievingPreference as S
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference.Values as EV
import com.ubergeek42.WeechatAndroid.preferences.IntPreference as I
import com.ubergeek42.WeechatAndroid.preferences.NullableStringRetrievingPreference as N

object SshPreferences {

    val host = T("ssh_host", "").addValidator { it.ensureNoSpaces() }

    val port = I("ssh_port", "22")

    val user = T("ssh_user", "")

    enum class AuthenticationMethod(override val value: String) : EV {
        Password("password"),
        Key("key"),
    }

    val authenticationMethod = E("ssh_authentication_method",
                                 AuthenticationMethod.Password, AuthenticationMethod.values())

    val password = T("ssh_password", "")

    val serializedKey = object : N<ByteArray?>("ssh_key_file") {
        override fun convert(v: String?) = PrivateKeyPickerPreference.getData(v)
    }

    // not present in preferences
    val serverKeyVerifier = object : S<SSHServerKeyVerifier>("ssh_server_key_verifier", "") {
        override fun convert(v: String): SSHServerKeyVerifier {
            return if (v.isNotEmpty()) {
                SSHServerKeyVerifier.decodeFromString(v)
            } else {
                SSHServerKeyVerifier()
            }.also {
                it.listener = SSHServerKeyVerifier.Listener {
                    p.edit().putString(key, it.encodeToString()).apply()
                }
            }
        }

        override fun invalidate() {
            // never invalidate, use a single instance of SSHServerKeyVerifier
        }
    }

    val clearKnownHosts = Q("ssh_clear_known_host")
}
