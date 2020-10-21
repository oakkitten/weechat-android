package com.ubergeek42.WeechatAndroid.preferences

import com.ubergeek42.WeechatAndroid.preferences.Preference.Companion.whenChanged
import com.ubergeek42.WeechatAndroid.upload.*
import com.ubergeek42.cats.Kitty
import com.ubergeek42.cats.Root
import okhttp3.HttpUrl.Companion.toHttpUrl


private typealias T = TextPreference
private typealias F = FloatPreference
private typealias E<T> = EnumPreference<T>
private typealias S<T> = StringRetrievingPreference<T>

private typealias EV = EnumPreference.Values


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
        override fun convert(v: String) = if (v.isEmpty()) {
                                              HttpUriGetter.simple
                                          } else {
                                              HttpUriGetter.fromRegex(v)
                                          }
    }

    private val help = NotAPreference("upload_help")

    private val advancedGroup = NotAPreference("upload_advanced_group")

    private val additionalHeaders = object : S<RequestModifier?>("upload_additional_headers", "") {
        override fun convert(v: String) = RequestModifier.additionalHeaders(v)
    }

    enum class Authentication(override val value: String) : EV {
        None("none"),
        Basic("basic"),
    }

    private val authentication = E("upload_authentication", Authentication.None, Authentication.values())

    private val basicAuthenticationUser = T("upload_authentication_basic_user", "")

    private val basicAuthenticationPassword = T("upload_authentication_basic_password", "")

    val rememberUploadsFor = object : S<Int>("upload_remember_uploads_for", "24") {
        override fun convert(v: String) = v.hours_to_ms
    }

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