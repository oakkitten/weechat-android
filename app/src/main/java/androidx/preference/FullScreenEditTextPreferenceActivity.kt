package androidx.preference

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.ubergeek42.WeechatAndroid.R
import com.ubergeek42.WeechatAndroid.utils.afterTextChanged
import com.ubergeek42.WeechatAndroid.views.EditTextActivity
import androidx.core.content.edit


abstract class FullScreenEditTextPreferenceActivity : EditTextActivity(allowSoftKeyboard = true) {
    abstract val key: String
    abstract val title: String
    abstract val defaultValue: String

    private val preferences get() = PreferenceManager.getDefaultSharedPreferences(this)

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            AlertDialog.Builder(this@FullScreenEditTextPreferenceActivity)
                .setMessage(R.string.pref__FullScreenEditTextPreference__discard_changes_prompt)
                .setPositiveButton(R.string.pref__FullScreenEditTextPreference__discard_changes_discard) { _, _ -> finish() }
                .setNegativeButton(R.string.pref__FullScreenEditTextPreference__discard_changes_cancel, null)
                .show()
        }
    }.also { onBackPressedDispatcher.addCallback(it) }

    private var dirty by onBackPressedCallback::isEnabled

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ui.toolbar.apply {
            setTitle(title)
            setNavigationOnClickListener { _ -> onBackPressedDispatcher.onBackPressed() }
            inflateMenu(R.menu.fullscreen_edit_text)
            setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.action_save -> {
                        saveAndDismiss()
                        true
                    }
                    R.id.action_reset_to_default -> {
                        resetToDefaultValue()
                        true
                    }
                    else -> false
                }
            }
        }

        ui.text.apply {
            setHorizontallyScrolling(true)
            setText(preferences.getString(key, ""))
            afterTextChanged { _ -> dirty = true}
        }
    }

    private fun resetToDefaultValue() {
        ui.text.setText(defaultValue)
    }

    private fun saveAndDismiss() {
        val value = ui.text.text.toString()
        if (validate(value)) {
            preferences.edit { putString(key, value) }
            finish()
        }
    }

    // Return `false` if data is not valid. Can show snackbars.
    open fun validate(value: String) = true
}
