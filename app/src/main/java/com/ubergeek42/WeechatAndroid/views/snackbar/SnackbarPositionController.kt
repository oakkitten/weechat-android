package com.ubergeek42.WeechatAndroid.views.snackbar

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import com.google.android.material.snackbar.Snackbar
import com.ubergeek42.WeechatAndroid.R
import com.ubergeek42.WeechatAndroid.fragments.BufferFragment
import com.ubergeek42.WeechatAndroid.upload.f
import com.ubergeek42.WeechatAndroid.utils.applicationContext
import com.ubergeek42.WeechatAndroid.utils.ulet


/**
 * This controls the position of the snackbar in the chat activities.
 * As buffers change, the height of the bottom bar may change, or it may completely disappear.
 * This moves the snackbar up and down to be above the bottom bar and the bottom gesture area,
 * animating the movement on pager changes.
 *
 * To achieve this, we overtake both the anchoring and inset-observing logic,
 * as you can't reliably reposition the snackbar by changing its anchor.
 * To overtake the inset-observing logic,
 * we are replacing the inset listener of the snackbar view with a dummy,
 * as setting it to `null` results in the view getting some random padding on top and bottom.
 * I couldn't figure out where this comes from.
 *
 * We reposition the snackbar by changing its margins.
 * While it's possible to simply translate it,
 * `ViewDragHelper` seem to be ignoring translation when capturing a view for dragging,
 * and the snackbar can't be dragged or flung away.
 *
 * When calculating margins, for simplicity we assume
 * that it has the same intrinsic margins on all sides.
 * The default implementation records all of the original margins.
 * The intrinsic margin is added to the extra bottom when the snackbar:
 *   * is sitting on top of an anchor view, or
 *   * is sitting on the very bottom of the screen;
 * it is not added when it:
 *   * is sitting on top of the bottom gesture area.
 *
 * TODO use weak reference for the snackbar?
 * TODO perhaps track any scheduled snackbars?
 */
class SnackbarPositionController {
    private var snackbar: Snackbar? = null
    private var anchor: View? = null
    private var insets: Insets = Insets.NONE

    private var animateOnNextLayout = false

    fun setSnackbar(snackbar: Snackbar) {
        this.snackbar = snackbar
        animateOnNextLayout = false
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.view) { _, insets -> insets }
        recalculateAndUpdateMargins(animate = false)
    }

    fun setAnchor(view: View?) {
        if (anchor == view) return

        anchor?.viewTreeObserver?.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        view?.viewTreeObserver?.addOnGlobalLayoutListener(onGlobalLayoutListener)
        anchor = view

        if (view == null || view.isLaidOut) {
            recalculateAndUpdateMargins(animate = true)
        } else {
            animateOnNextLayout = true
        }
    }

    fun setInsets(insets: Insets) {
        this.insets = insets
        recalculateAndUpdateMargins(animate = false)
    }

    // Note that this listener may be called a lot
    private val onGlobalLayoutListener =
        ViewTreeObserver.OnGlobalLayoutListener {
            recalculateAndUpdateMargins(animate = animateOnNextLayout)
            animateOnNextLayout = false
        }

    private fun recalculateAndUpdateMargins(animate: Boolean) = ulet (snackbar) { snackbar ->
        val snackbarView = snackbar.view
        val snackbarViewParent = snackbarView.parent
        val snackbarViewLayoutParams = snackbarView.layoutParams as MarginLayoutParams

        if (!snackbar.isShown || snackbarViewParent !is View) return

        val originalSnackbarViewMargin = snackbarView.marginTop

        val oldLeftMargin = snackbarViewLayoutParams.leftMargin
        val oldRightMargin = snackbarViewLayoutParams.rightMargin
        val oldBottomMargin = snackbarViewLayoutParams.bottomMargin

        val leftMargin = insets.left + originalSnackbarViewMargin
        val rightMargin = insets.right + originalSnackbarViewMargin
        val bottomMargin = anchor?.let { snackbarViewParent.height - it.top + originalSnackbarViewMargin }
            ?: if (insets.bottom > 0) insets.bottom else originalSnackbarViewMargin

        val marginsChanged = oldLeftMargin != leftMargin
            || oldRightMargin != rightMargin
            || oldBottomMargin != bottomMargin

        if (marginsChanged) {
            snackbarViewLayoutParams.leftMargin = leftMargin
            snackbarViewLayoutParams.rightMargin = rightMargin
            snackbarViewLayoutParams.bottomMargin = bottomMargin
            snackbarView.requestLayout()

            if (animate) {
                snackbarView.translationY = bottomMargin - oldBottomMargin.f
                snackbarView.animate().translationY(0f).setDuration(shortAnimTime).start()
            }
        }
    }
}


/**
 * When there's a change in the pager,
 * we might not have a buffer fragment that has its view ready,
 * or in fact we might not have it at all.
 * This schedules setting of the anchor at the time when it's available.
 *
 * Note that the anchor view may be not yet laid out at the time of setting it.
 */
fun SnackbarPositionController.setOrScheduleSettingAnchorAfterPagerChange(
    currentBufferPointer: Long,
    currentBufferFragment: BufferFragment?,
    fragmentManager: FragmentManager,
) {
    if (currentBufferPointer == 0L) {
        setAnchor(null)
    } else {
        if (currentBufferFragment != null && currentBufferFragment.isVisible) {
            setAnchor(currentBufferFragment.requireView().findViewById(R.id.bottom_bar))
        } else {
            fragmentManager.registerFragmentLifecycleCallbacks(object : FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                    if (f is BufferFragment && f.pointer == currentBufferPointer) {
                        setAnchor(f.requireView().findViewById(R.id.bottom_bar))
                        fragmentManager.unregisterFragmentLifecycleCallbacks(this)
                    }
                }
            }, false)
        }
    }
}


private val shortAnimTime = applicationContext
        .resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
