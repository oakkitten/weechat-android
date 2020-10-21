package com.ubergeek42.WeechatAndroid.preferences

import java.lang.IllegalArgumentException

class BooleanPreference(key: String, default: Boolean)
        : Preference<Boolean, Boolean>(key, default, Boolean::class.java) {
    override fun retrieve() = p.getBoolean(key, default)

    override fun convert(v: Boolean) = v
}

abstract class StringRetrievingPreference<A>(key: String, default: String)
        : Preference<A, String>(key, default, String::class.java) {
    override fun retrieve() = p.getString(key, default) ?: ""
}

abstract class NullableStringRetrievingPreference<A>(key: String)
        : Preference<A, String?>(key, null, String::class.java as Class<String?>) {
    override fun retrieve() = p.getString(key, null)
}

open class TextPreference(key: String, default: String) : StringRetrievingPreference<String>(key, default) {
    override fun convert(v: String) = v
}

open class IntPreference(key: String, default: String) : StringRetrievingPreference<Int>(key, default) {
    override fun convert(v: String) = v.toInt()
}

open class FloatPreference(key: String, default: String) : StringRetrievingPreference<Float>(key, default) {
    override fun convert(v: String) = v.toFloat()
}

class EnumPreference<E> (key: String, default: E, private val enumValues: Array<E>)
        : StringRetrievingPreference<E>(key, default.value) where E: Enum<E>, E : EnumPreference.Values {
    override fun convert(v: String): E {
        enumValues.forEach {
            if (it.value == v) return it
        }
        throw IllegalArgumentException("could not convert value $v")
    }

    interface Values {
        val value: String
    }
}

class NotQuiteAPreference(key: String) : Preference<String, String>(key, "", String::class.java) {
    override fun retrieve() = ""
    override fun convert(v: String) = ""
}

////////////////////////////////////////////////////////////////////////////////////////////////////

fun String.ensureNoSpaces() {
    if (this.contains("\\S".toRegex())) throw Exception("Setting can't contain spaces!")
}