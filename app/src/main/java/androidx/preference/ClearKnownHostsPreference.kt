package androidx.preference

import android.content.Context
import android.util.AttributeSet
import com.ubergeek42.WeechatAndroid.R
import com.ubergeek42.WeechatAndroid.preferences.Pref
import com.ubergeek42.WeechatAndroid.utils.Toaster.Companion.SuccessToast

class ClearKnownHostsPreference(context: Context, attrs: AttributeSet) : ClearPreference(context, attrs) {
    override val message = R.string.pref__ClearKnownHostsPreference__prompt
    override val negativeButton = R.string.pref__ClearKnownHostsPreference__button_cancel
    override val positiveButton = R.string.pref__ClearKnownHostsPreference__button_clear

    override fun update() {
        val entries = Pref.connection.ssh.serverKeyVerifier.value.numberOfRecords

        summary = if (entries == 0) context.getString(R.string.pref__ClearKnownHostsPreference__0_entries) else
                context.resources.getQuantityString(R.plurals.pref__ClearKnownHostsPreference__n_entries, entries, entries)
    }

    override fun clear() {
        Pref.connection.ssh.serverKeyVerifier.value.clear()
        SuccessToast.show(R.string.pref__ClearKnownHostsPreference__success_cleared)
    }
}