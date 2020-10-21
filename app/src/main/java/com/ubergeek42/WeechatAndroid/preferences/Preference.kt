package com.ubergeek42.WeechatAndroid.preferences

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.ubergeek42.WeechatAndroid.upload.applicationContext
import com.ubergeek42.cats.Kitty
import com.ubergeek42.cats.Root

val context = applicationContext
val p = PreferenceManager.getDefaultSharedPreferences(context)!!


fun interface Validator<R> {
    @Throws(Exception::class) fun validate(v: R)
}


abstract class Preference<A, R>(
    val key: String,
    val default: R,
    private val storageClass: Class<R>
) {
    @Root private val kitty = Kitty.make()

    init {
        kitty.setPrefix(key)
        preferences[key] = this
    }

    @Throws(Exception::class)
    protected abstract fun retrieve(): R

    @Throws(Exception::class)
    protected abstract fun convert(v: R): A

    private val validators = mutableListOf<Validator<R>>()

    @Throws(Exception::class)
    fun validate(o: Any?): A {
        val v = storageClass.cast(o) as R
        if (v != default) validators.forEach { it.validate(v) }
        return convert(v)
    }

    fun addValidator(validator: Validator<R>): Preference<A, R> {
        validators.add(validator)
        return this
    }

    fun invalidate() {
        _isSet = false
    }

    private var _isSet = false
    private var _field: A? = null

    var value: A
        private set(v) {
            _field = v
        }

        get() {
            if (!_isSet) {
                _field = try {
                    validate(retrieve())
                } catch (e: Exception) {
                    kitty.wtf("error while validating or converting value", e)
                    convert(default)
                }
                _isSet = true
            }
            return _field as A
        }

    companion object {
        @Root private val kitty = Kitty.make()

        val preferences = mutableMapOf<String, Preference<*, *>>()

        @Throws(Exception::class)
        fun validate(key: String, value: Any) {
            val preference = preferences[key]!!
            preference.validate(value)
        }

        @JvmStatic fun register() {
            UploadPreferences

            p.registerOnSharedPreferenceChangeListener { _: SharedPreferences, key: String ->
                val preference = preferences[key]
                if (preference == null) {
                    kitty.warn("preference unknown: %s", key)
                } else {
                    preference.invalidate()
                    kitty.info("preference %s has been changed to %s", key, preference.value)
                    kitty.info("triggers %s %s", triggers, triggers.getValue(preference))
                    triggers.getValue(preference).forEach { it() }
                }
            }
        }

        private val triggers = mutableMapOf<Preference<*, *>,
                                            MutableList<() -> Unit>>()

        fun whenChanged(vararg preferences: Preference<*, *>, trigger: (() -> Unit)) {
            preferences.forEach { triggers.getOrPut(it, ::mutableListOf).add(trigger) }
        }
    }
}
