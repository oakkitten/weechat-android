package com.ubergeek42.WeechatAndroid.preferences

import com.ubergeek42.WeechatAndroid.preferences.Preference.Companion.whenChanged
import com.ubergeek42.WeechatAndroid.upload.*
import com.ubergeek42.cats.Kitty
import com.ubergeek42.cats.Root
import okhttp3.HttpUrl.Companion.toHttpUrl

import com.ubergeek42.WeechatAndroid.preferences.TextPreference as T
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference as E
import com.ubergeek42.WeechatAndroid.preferences.NotQuiteAPreference as Q
import com.ubergeek42.WeechatAndroid.preferences.PersistedStringPreference as S
import com.ubergeek42.WeechatAndroid.preferences.EnumPreference.Values as EV


object UploadPreferences {
    @Root private val kitty = Kitty.make()

    enum class Accept(override val value: String) : EV {
        TextOnly("text_only"),
        TextAndMedia("text_and_media"),
        Everything("everything"),
    }

    val accept = E("upload_accept", Accept.TextOnly, Accept.values())

    val uploadUri = T("upload_uri", "").addValidator { it.ensureNoSpaces() }
                                       .addValidator { it.toHttpUrl() }

    val formFieldName = T("upload_form_field_name", "file")

    val httpUriGetter = object : S<HttpUriGetter>("upload_regex", "^https://\\S+") {
        override fun convert(value: String) = if (value.isEmpty()) {
                                              HttpUriGetter.simple
                                          } else {
                                              HttpUriGetter.fromRegex(value)
                                          }
    }

    private val help = Q("upload_help")

    private val advancedGroup = Q("upload_advanced_group")

    private val additionalHeaders = object : S<RequestModifier?>("upload_additional_headers", "") {
        override fun convert(value: String) = RequestModifier.additionalHeaders(value)
    }

    enum class Authentication(override val value: String) : EV {
        None("none"),
        Basic("basic"),
    }

    private val authentication = E("upload_authentication", Authentication.None, Authentication.values())

    private val basicAuthenticationUser = T("upload_authentication_basic_user", "")

    private val basicAuthenticationPassword = T("upload_authentication_basic_password", "")

    val rememberUploadsFor = HoursPreference("upload_remember_uploads_for", "24")

    ////////////////////////////////////////////////////////////////////////////////////////////////

    var requestModifiers = emptyList<RequestModifier>()

    init {
        listOf(uploadUri, formFieldName, httpUriGetter, help, advancedGroup, additionalHeaders,
               authentication, basicAuthenticationUser, basicAuthenticationPassword,
               rememberUploadsFor).disableUnless {
            accept.value != Accept.TextOnly
        }

        listOf(basicAuthenticationUser, basicAuthenticationPassword).disableUnless {
            authentication.value == Authentication.Basic
        }

        whenChanged(additionalHeaders, authentication,
                    basicAuthenticationUser, basicAuthenticationPassword) {
            requestModifiers = sequence {
                additionalHeaders.value?.let {
                    yield(it)
                }

                if (authentication.value == Authentication.Basic) {
                    yield(RequestModifier.basicAuthentication(
                            basicAuthenticationUser.value,
                            basicAuthenticationPassword.value))
                }
            }.toList()
        }

        whenChanged(accept) {
            enableDisableComponent(Aliases.ShareActivityText.name, accept.value == Accept.TextOnly)
            enableDisableComponent(Aliases.ShareActivityMedia.name, accept.value == Accept.TextAndMedia)
            enableDisableComponent(Aliases.ShareActivityEverything.name, accept.value == Accept.Everything)
        }
    }
}

private enum class Aliases {
    ShareActivityText,
    ShareActivityMedia,
    ShareActivityEverything,
}