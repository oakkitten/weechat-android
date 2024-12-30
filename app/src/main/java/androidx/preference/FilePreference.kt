// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
package androidx.preference

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.ClipboardManager
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Base64
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ubergeek42.WeechatAndroid.R
import com.ubergeek42.WeechatAndroid.utils.Toaster
import com.ubergeek42.WeechatAndroid.utils.Utils
import com.ubergeek42.cats.Kitty
import com.ubergeek42.cats.Root

open class FilePreference(context: Context, attrs: AttributeSet?) :
        DialogPreference(context, attrs), DialogFragmentGetter {
    override fun getSummary(): CharSequence? {
        val set_not_set = context.getString(if (Utils.isEmpty(
                    getData(getPersistedString(null)))
                                            ) R.string.pref__FilePreference__summary_status_not_set else R.string.pref__FilePreference__summary_status_set)
        return context.getString(R.string.pref__FilePreference__summary_adapter,
                                 super.getSummary(), set_not_set)
    }

    // validate, if needed, and save data. throw anything on error—it will get printed.
    // returned string, if not null, will be displayed as a long toast
    @Throws(Exception::class) protected open fun saveData(bytes: ByteArray?): String? {
        if (callChangeListener(bytes)) {
            persistString(if (bytes == null) null else Base64.encodeToString(bytes, Base64.NO_WRAP))
            notifyChanged()
        }
        return null
    }

    private fun saveDataAndShowToast(bytesGetter: ThrowingGetter<ByteArray>) {
        try {
            val bytes = bytesGetter.get()
            val message = saveData(bytes)
            if (message != null) Toaster.SuccessToast.show(message)
        } catch (e: Exception) {
            kitty.error("error", e)
            Toaster.ErrorToast.show(e)
        }
    }

    /**///////////////////////////////////////////////////////////////////////////////////////////// */ // this gets called when a file has been picked
    fun onActivityResult(intent: Intent) {
        saveDataAndShowToast(ThrowingGetter<ByteArray> {
            Utils.readFromUri(
                context, intent.data)
        })
    }

    override fun getDialogFragment(): DialogFragment {
        return FilePreferenceFragment()
    }

    /**///////////////////////////////////////////////////////////////////////////////////////////// */
    open class FilePreferenceFragment : PreferenceDialogFragmentCompat() {
        override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
            val preference = preference as FilePreference
            builder.setNeutralButton(getString(R.string.pref__FilePreference__button_clear)
            ) { dialog: DialogInterface?, which: Int ->
                preference.saveDataAndShowToast(
                    ThrowingGetter<ByteArray> { null })
            }
                    .setNegativeButton(getString(R.string.pref__FilePreference__button_paste)
                    ) { dialog: DialogInterface?, which: Int ->
                        // noinspection deprecation
                        val cm =
                            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = cm.text
                        if (TextUtils.isEmpty(clip)) Toaster.ErrorToast.show(
                            R.string.error__pref__clipboard_empty)
                        else {
                            preference.saveDataAndShowToast(ThrowingGetter<ByteArray> {
                                clip.toString().toByteArray()
                            })
                        }
                    }
                    .setPositiveButton(getString(R.string.pref__FilePreference__button_choose_file)
                    ) { dialog: DialogInterface?, which: Int ->
                        val intent =
                            Intent(Intent.ACTION_GET_CONTENT)
                        intent.setType("*/*")
                        targetFragment!!.startActivityForResult(intent, arguments!!.getInt("code"))
                    }
        }

        override fun onDialogClosed(b: Boolean) {}
    }

    // this slightly simplifies code by allowing onActivityResult not to deal with exceptions
    internal interface ThrowingGetter<T> {
        @Throws(Exception::class) fun get(): T
    }

    companion object {
        @Root
        private val kitty: Kitty = Kitty.make()

        // a helper method that gets the original bytes from the strings
        fun getData(data: String): ByteArray? {
            return try {
                Base64.decode(data.toByteArray(), Base64.NO_WRAP)
            } catch (ignored: IllegalArgumentException) {
                null
            } catch (ignored: NullPointerException) {
                null
            }
        }
    }
}
