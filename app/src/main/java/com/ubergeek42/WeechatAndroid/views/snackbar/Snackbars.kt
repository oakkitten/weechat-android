package com.ubergeek42.WeechatAndroid.views.snackbar

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import com.ubergeek42.WeechatAndroid.BuildConfig
import com.ubergeek42.WeechatAndroid.R
import com.ubergeek42.WeechatAndroid.utils.FriendlyExceptions
import com.ubergeek42.WeechatAndroid.utils.Toaster
import com.ubergeek42.WeechatAndroid.utils.Utils
import com.ubergeek42.cats.Kitty
import com.ubergeek42.cats.Root


@Root private val kitty = Kitty.make("Snackbars")


// Create snackbars by calling `showSnackbar` on either an activity or a view.
// As `CoordinatorLayout` is responsible for proper placement and animation of snackbars,
//
//   * if calling on an activity, the activity **MUST** have a `CoordinatorLayout`
//     with id `coordinator_layout`;
//
//   * if calling on a view, the view **MUST** be either a `CoordinatorLayout`,
//     or a (possibly indirect) child of `CoordinatorLayout`.
//
// If passing a throwable, text or string resource ID is optional.
// If it is not present, a message will be constructed from the throwable itself.
//
// Additional configuration can be done in the configuration block, e.g.
//
//     showSnackbar(text) {
//         addCallback(callback)
//     }
//
// If an activity wants to alter behavior for all snackbars,
// it can extend `BaseSnackbarBuilderProvider`.


typealias SnackbarBuilder = Snackbar.() -> Unit

interface BaseSnackbarBuilderProvider {
    val baseSnackbarBuilder: SnackbarBuilder
}


/**************************************************************************************************/


fun Preference.showSnackbar(t: Throwable) {
    (context as Activity).showSnackbar(t)
}

fun Preference.showSnackbar(@StringRes textResource: Int, t: Throwable) {
    (context as Activity).showSnackbar(textResource, t)
}

fun Preference.showSnackbar(text: CharSequence, t: Throwable) {
    (context as Activity).showSnackbar(text, t)
}

@JvmOverloads
fun Preference.showSnackbar(@StringRes textResource: Int, snackbarBuilder: SnackbarBuilder? = null) {
    (context as Activity).showSnackbar(textResource, snackbarBuilder)
}

@JvmOverloads
fun Preference.showSnackbar(text: CharSequence, snackbarBuilder: SnackbarBuilder? = null) {
    (context as Activity).showSnackbar(text, snackbarBuilder)
}


/**************************************************************************************************/


fun Fragment.showSnackbar(t: Throwable) {
    requireActivity().showSnackbar(t)
}

fun Fragment.showSnackbar(@StringRes textResource: Int, t: Throwable) {
    requireActivity().showSnackbar(textResource, t)
}

fun Fragment.showSnackbar(text: CharSequence, t: Throwable) {
    requireActivity().showSnackbar(text, t)
}

@JvmOverloads
fun Fragment.showSnackbar(@StringRes textResource: Int, snackbarBuilder: SnackbarBuilder? = null) {
    requireActivity().showSnackbar(textResource, snackbarBuilder)
}

@JvmOverloads
fun Fragment.showSnackbar(text: CharSequence, snackbarBuilder: SnackbarBuilder? = null) {
    requireActivity().showSnackbar(text, snackbarBuilder)
}


/**************************************************************************************************/


fun Activity.showSnackbar(t: Throwable) {
    getCoordinatorLayoutOrNull()?.showSnackbar(t)
}

fun Activity.showSnackbar(@StringRes textResource: Int, t: Throwable) {
    getCoordinatorLayoutOrNull()?.showSnackbar(textResource, t)
}

fun Activity.showSnackbar(text: CharSequence, t: Throwable) {
    getCoordinatorLayoutOrNull()?.showSnackbar(text, t)
}

@JvmOverloads
fun Activity.showSnackbar(@StringRes textResource: Int, snackbarBuilder: SnackbarBuilder? = null) {
    getCoordinatorLayoutOrNull()?.showSnackbar(textResource, snackbarBuilder)
}

@JvmOverloads
fun Activity.showSnackbar(text: CharSequence, snackbarBuilder: SnackbarBuilder? = null) {
    getCoordinatorLayoutOrNull()?.showSnackbar(text, snackbarBuilder)
}

private fun Activity.getCoordinatorLayoutOrNull(): CoordinatorLayout? {
    val coordinatorLayout = findViewById(R.id.coordinator_layout) as? CoordinatorLayout

    return if (coordinatorLayout != null) {
        coordinatorLayout
    } else {
        val errorMessage = "While trying to show a snackbar, could not find a view with id coordinator_layout in $this"

        if (BuildConfig.DEBUG) {
            throw IllegalArgumentException(errorMessage)
        } else {
            kitty.error(errorMessage)
            Toaster.ErrorToast.show(errorMessage)
        }

        null
    }
}


/**************************************************************************************************/


fun View.showSnackbar(t: Throwable) {
    showSnackbar(context.getSnackbarTextForThrowable(t), t)
}

fun View.showSnackbar(@StringRes textResource: Int, t: Throwable) {
    showSnackbar(resources.getText(textResource), t)
}

fun View.showSnackbar(text: CharSequence, t: Throwable) {
    showSnackbar(text, getSnackbarBuilderForThrowable(t))
}

@JvmOverloads
fun View.showSnackbar(@StringRes textResource: Int, snackbarBuilder: SnackbarBuilder? = null) {
    showSnackbar(resources.getText(textResource), snackbarBuilder)
}

@JvmOverloads
fun View.showSnackbar(text: CharSequence, snackbarBuilder: SnackbarBuilder? = null) {
    val activity = Utils.getActivity(this)
    val baseSnackbarBuilder = (activity as? BaseSnackbarBuilderProvider)?.baseSnackbarBuilder

    val snackbar = Snackbar.make(this, text, Snackbar.LENGTH_LONG)
    snackbar.setMaxLines(4)
    snackbar.behavior = SwipeDismissBehaviorFix()

    baseSnackbarBuilder?.invoke(snackbar)
    snackbarBuilder?.invoke(snackbar)

    kitty.info("Showing a snackbar: '%s'", text)

    snackbar.show()
}


/**************************************************************************************************
 **************************************************************************************************
 **************************************************************************************************/


private fun Snackbar.setMaxLines(maxLines: Int) {
    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.maxLines = maxLines
}


class SnackbarDetailsDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?) =
        AlertDialog.Builder(requireActivity())
            .setMessage(requireArguments().getCharSequence("message"))
            .setPositiveButton("Ok") { _, _ -> }
            .create()

    companion object {
        fun make(message: CharSequence) = SnackbarDetailsDialogFragment()
            .apply { arguments = bundleOf("message" to message) }
    }
}

private fun Snackbar.addDetails(details: String, actionText: String = "Details") {
    setAction(actionText) {
        val fragmentManager = (context as AppCompatActivity).supportFragmentManager
        SnackbarDetailsDialogFragment
            .make(message = details)
            .show(fragmentManager, "snackbars-details-dialog-fragment")
    }
}


private fun Context.getSnackbarTextForThrowable(t: Throwable): CharSequence {
    val exceptionMessage = FriendlyExceptions(this).getFriendlyException(t).message
    return resources.getString(R.string.error__etc__prefix, exceptionMessage)
}

private fun getSnackbarBuilderForThrowable(t: Throwable): SnackbarBuilder =
    { addDetails(t.stackTraceToString()) }

