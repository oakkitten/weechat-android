package com.ubergeek42.WeechatAndroid.upload

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import com.ubergeek42.WeechatAndroid.utils.ActionEditText
import com.ubergeek42.WeechatAndroid.utils.Toaster.Companion.ErrorToast
import com.ubergeek42.cats.Kitty
import com.ubergeek42.cats.Root


class MediaAcceptingEditText : ActionEditText {
    @Root private val kitty = Kitty.make()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {
        val inputConnection: InputConnection = super.onCreateInputConnection(editorInfo)
        EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf("*/*"))
        return InputConnectionCompat.createWrapper(inputConnection, editorInfo, callback)
    }

    private val callback = InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, _ ->
        val lacksPermission = (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
        val shouldRequestPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && lacksPermission

        if (shouldRequestPermission) {
            try {
                inputContentInfo.requestPermission()
            } catch (e: Exception) {
                kitty.error("Failed to acquire permission for %s", inputContentInfo.description, e)
                return@OnCommitContentListener false
            }
        }

        val suri: Suri = try {
            Suri.fromUri(inputContentInfo.contentUri)
        } catch (e: Exception) {
            kitty.error("Error while accessing uri", e)
            ErrorToast.show(e)
            return@OnCommitContentListener false
        }

        object : UrisShareObject(listOf(suri)) {
            protected fun finalize() {
                // todo if this causes issues, move this somewhere
                if (shouldRequestPermission) inputContentInfo.releasePermission()
            }
        }.insert(this, InsertAt.CURRENT_POSITION)

        true
    }

    private fun getSuris() : List<Suri> {
        val spans = text?.let { it.getSpans(0, it.length, ShareSpan::class.java) }
        return spans?.map { it.suri }?.toList() ?: emptyList()
    }

    fun getNotReadySuris() : List<Suri> {
        return getSuris().filter { !it.ready }
    }

    fun textifyReadySuris() {
        text?.let {
            for (span in it.getSpans(0, it.length, ShareSpan::class.java)) {
                span.suri.httpUri?.let { httpUri ->
                    it.replace(it.getSpanStart(span), it.getSpanEnd(span), httpUri)
                    it.removeSpan(span)
                }
            }
        }
    }
}
