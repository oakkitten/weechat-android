package com.ubergeek42.WeechatAndroid.upload

import com.ubergeek42.WeechatAndroid.BubbleActivity
import com.ubergeek42.WeechatAndroid.WeechatActivity
import com.ubergeek42.WeechatAndroid.utils.Toaster
import com.ubergeek42.WeechatAndroid.views.onMainActivityResumed
import com.ubergeek42.WeechatAndroid.views.resumedActivities
import com.ubergeek42.WeechatAndroid.views.snackbar.showSnackbar
import java.io.IOException

/**
 * Uploads can be finished or fail while the buffer or the app itself is not in focus.
 * To rectify this, this observer tracks successful uploads errors independently of the fragment.
 *
 * Notes:
 *   * This observer is meant to be used per-buffer for several reasons:
 *     * completed uploads are actionable on per-buffer basis;
 *     * tracking global uploads would require additional logic in the manager;
 *     * concurrent uploads from multiple buffers are simply unlikely.
 *
 *   * If the fragment is still paused when the uploads finish,
 *     this observer will try notify the user of *all* failures that happened during upload.
 *     The user, however, might have already received some snackbars about errors
 *     that occurred while they had the fragment focused.
 *
 *   * The notifying will also happen if the fragment is resumed
 *     but was paused when some of the failures occurred.
 *
 *  TODO The flag `externalObserverPresent` below, if true, does not mean
 *    that the buffer fragment in question is shown to user.
 *    The buffer fragment can be one of the immediately adjacent to the current one.
 *    It is useful for the purposes of updating the progress circle,
 *    but we might want to narrow it down for the purposes of showing errors or success info.
 */
class UnreportedUploadEventsTracker {
    private val exceptions = mutableListOf<Exception>()
    private var hasUnreportedExceptions = false
    private var hasDoneUploads = false

    fun onUploadFinished(suri: Suri, result: Upload.Result, externalObserverPresent: Boolean) {
        when (result) {
            is Upload.Result.Done -> { hasDoneUploads = true }
            is Upload.Result.Failed -> {
                exceptions.add(result.e)
                if (!externalObserverPresent) hasUnreportedExceptions = true
            }
            is Upload.Result.Cancelled -> {}
        }
    }

    @Suppress("KotlinConstantConditions") // for clarity
    fun onAllUploadsFinished(externalObserverPresent: Boolean) {
        if (!externalObserverPresent || hasUnreportedExceptions) {
            val result = when {
                hasDoneUploads && exceptions.isEmpty() -> UploadsFinishedResult.AllUploadsDone
                hasDoneUploads && exceptions.isNotEmpty() -> UploadsFinishedResult.SomeUploadsFailed(exceptions.toList())
                !hasDoneUploads && exceptions.isNotEmpty() -> UploadsFinishedResult.AllUploadsFailed(exceptions.toList())
                else -> null // !hasDoneUploads && exceptions.isEmpty() — shouldn't happen

            }
            result?.let { notifyUserOfFinishedUploads(it) }
        }

        exceptions.clear()
        hasUnreportedExceptions = false
        hasDoneUploads = false
    }
}


private sealed interface UploadsFinishedResult {
    object AllUploadsDone : UploadsFinishedResult
    interface UploadsFailed : UploadsFinishedResult { val exceptions: List<Exception> }
    class SomeUploadsFailed(override val exceptions: List<Exception>) : UploadsFailed
    class AllUploadsFailed(override val exceptions: List<Exception>) : UploadsFailed
}


/**
 * If the activity is resumed, this will a snackbar immediately.
 * If the activity is paused, this will show a toast *now*,
 * and in case of any failures schedule a snackbar to be shown when the activity is resumed.
 *
 * TODO Perhaps don't show snackbars for unrelated downloads in bubbles?
 */
private fun notifyUserOfFinishedUploads(result: UploadsFinishedResult) {
    val resumedActivityOrNull = resumedActivities
            .firstOrNull { it is WeechatActivity || it is BubbleActivity }

    if (result is UploadsFinishedResult.AllUploadsDone) {
        if (resumedActivityOrNull != null) {
            resumedActivityOrNull.showSnackbar("Upload complete")
        } else {
            Toaster.InfoToast.show("Upload complete")
        }
    } else if (result is UploadsFinishedResult.UploadsFailed) {
        val message = if (result.exceptions.size == 1) "Could not upload file" else "Could not upload files"

        if (resumedActivityOrNull != null) {
            resumedActivityOrNull.showSnackbar(message, result.exceptions.combineIntoOne())
        } else {
            Toaster.InfoToast.show(message)
            onMainActivityResumed { showSnackbar(message, result.exceptions.combineIntoOne()) }
        }
    }
}


private fun List<Exception>.combineIntoOne() =
    if (size == 1) {
        first()
    } else {
        IOException("Could not upload files").apply { forEach { addSuppressed(it) } }
    }
