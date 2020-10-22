package com.ubergeek42.WeechatAndroid.preferences

import androidx.preference.PreferenceManager
import com.ubergeek42.WeechatAndroid.upload.applicationContext
import com.ubergeek42.cats.Kitty
import com.ubergeek42.cats.Root

val context = applicationContext
val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)!!


fun interface Validator<P> {
    @Throws(Exception::class) fun validate(v: P)
}


abstract class Preference<A, P>(
    val key: String,
    val default: P
) {
    @Root private val kitty = Kitty.make()

    init {
        kitty.setPrefix(key)
        @Suppress("LeakingThis")
        preferences[key] = this
    }

    @Throws(Exception::class) protected abstract fun retrieve(): P
    @Throws(Exception::class) protected abstract fun convert(value: P): A
    @Throws(Exception::class) protected abstract fun validatePersistedType(value: Any?): P

    private val validators = mutableListOf<Validator<A>>()

    @Throws(Exception::class) fun validate(o: Any?): A {
        val value = convert(validatePersistedType(o))
        validators.forEach { it.validate(value) }
        return value
    }

    fun addValidator(validator: Validator<A>): Preference<A, P> {
        validators.add(validator)
        return this
    }

    open fun invalidate() {
        isValueSet = false
    }

    private var isValueSet = false

    @Suppress("UNCHECKED_CAST")
    var value: A = Unit as A
        private set
        get() {
            if (!isValueSet) {
                field = try {
                                validate(retrieve())
                            } catch (e: Exception) {
                                kitty.wtf("error while validating or converting value", e)
                                convert(default)
                            }
                isValueSet = true
            }
            return field
        }

    var hideUnlessCheck: Check = { true }

    fun hideUnless(check: Check): Preference<A, P> {
        hideUnlessCheck = check
        return this
    }

    val notHidden get() = hideUnlessCheck()


    var disableUnlessCheck: Check = { true }

    fun disableUnless(check: Check): Preference<A, P> {
        disableUnlessCheck = check
        return this
    }

    val notDisabled get() = disableUnlessCheck()


    companion object {
        val preferences = mutableMapOf<String, AnyPreference>()

        val triggers = mutableMapOf<AnyPreference, MutableList<() -> Unit>>()

        fun whenChanged(vararg preferences: AnyPreference, trigger: (() -> Unit)) {
            preferences.forEach { triggers.getOrPut(it, ::mutableListOf).add(trigger) }
        }
    }
}


private typealias AnyPreference = Preference<*, *>
private typealias Check = () -> Boolean


fun Iterable<AnyPreference>.disableUnless(check: Check) {
    forEach { it.disableUnless(check) }
}

fun Iterable<AnyPreference>.hideUnless(check: Check) {
    forEach { it.hideUnless(check) }
}