package com.ubergeek42.WeechatAndroid.preferences

import com.ubergeek42.WeechatAndroid.service.P
import com.ubergeek42.WeechatAndroid.upload.i


open class BooleanPreference(key: String, default: Boolean) : Preference<Boolean, Boolean>(key, default) {
    override fun retrieve() = sharedPreferences.getBoolean(key, default)
    override fun validatePersistedType(value: Any?) = value as Boolean
    override fun convert(value: Boolean) = value
}


abstract class PersistedStringPreference<A>(key: String, default: String) : Preference<A, String>(key, default) {
    override fun retrieve() = sharedPreferences.getString(key, default) ?: ""
    override fun validatePersistedType(value: Any?) = value as String
}


abstract class PersistedNullableStringPreference<A>(key: String) : Preference<A, String?>(key, null) {
    override fun retrieve() = sharedPreferences.getString(key, null)
    override fun validatePersistedType(value: Any?) = value as String?
}


open class TextPreference(key: String, default: String) : PersistedStringPreference<String>(key, default) {
    override fun convert(value: String) = value
}


open class IntPreference(key: String, default: String) : PersistedStringPreference<Int>(key, default) {
    override fun convert(value: String) = value.toInt()
}


open class FloatPreference(key: String, default: String) : PersistedStringPreference<Float>(key, default) {
    override fun convert(value: String) = value.toFloat()
}


open class EnumPreference<E> (key: String, default: E, enumValues: Array<E>)
        : PersistedStringPreference<E>(key, default.value)
        where E: Enum<E>, E : EnumPreference.Values {
    private val enumValues: Map<String, E> = enumValues.associateBy { it.value }

    override fun convert(value: String) = enumValues.getValue(value)

    interface Values {
        val value: String
    }
}


open class EnumSetPreference<E> (key: String, default: Set<E>, enumValues: Array<E>)
        : Preference<Set<E>, Set<String>>(key, default.map { it.value }.toSet())
        where E: Enum<E>, E : EnumPreference.Values {
    private val enumValues: Map<String, E> = enumValues.associateBy { it.value }

    override fun retrieve() = sharedPreferences.getStringSet(key, default)!! as Set<String>
    override fun validatePersistedType(value: Any?): Set<String> = cast(value)
    override fun convert(value: Set<String>) = value.map { enumValues.getValue(it) }.toSet()
}


open class NotQuiteAPreference(key: String) : Preference<Unit, Unit>(key, Unit) {
    override fun retrieve() {}
    override fun convert(value: Unit) {}
    override fun validatePersistedType(value: Any?) {}
}


////////////////////////////////////////////////////////////////////////////////////////////////////


open class MegabytesPreference(key: String, default: String) : FloatPreference(key, default) {
    val bytes get() = (value * 1000 * 1000).i
}

open class HoursPreference(key: String, default: String) : FloatPreference(key, default) {
    val milliseconds get() = (value * 60 * 60 * 1000).i
}

open class SecondsPreference(key: String, default: String) : FloatPreference(key, default) {
    val milliseconds get() = (value * 1000).i
}

open class DpPreference(key: String, default: String) : FloatPreference(key, default) {
    val dp get() = (value * P._1dp).i
}

////////////////////////////////////////////////////////////////////////////////////////////////////

private val reSpaces = "\\s".toRegex()
fun ensureNoSpaces(string: String) {
    !string.contains(reSpaces) || throw Exception("Preference can’t contain spaces")
}

fun validPort(port: Int) {
    port in 0..65535 || throw Exception("Port outside valid range")
}

@Suppress("USELESS_CAST")
inline fun <reified C: Iterable<E>, reified E> cast(o: Any?): C {
    (o as C).forEach { it as E }
    return o as C
}
