package com.ubergeek42.WeechatAndroid.copypaste

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import com.ubergeek42.WeechatAndroid.R
import com.ubergeek42.WeechatAndroid.relay.BufferList
import com.ubergeek42.WeechatAndroid.relay.Line
import com.ubergeek42.WeechatAndroid.service.P
import com.ubergeek42.WeechatAndroid.views.EditTextActivity
import com.ubergeek42.weechat.relay.connection.find


private const val EXTRA_BUFFER_POINTER = "buffer_pointer"
private const val EXTRA_SELECTED_LINE_POINTER = "selected_line_pointer"


fun launchCopyActivity(context: Context, bufferPointer: Long, selectedLinePointer: Long) {
    val intent = Intent(context, CopyActivity::class.java).apply {
        putExtra(EXTRA_BUFFER_POINTER, bufferPointer)
        putExtra(EXTRA_SELECTED_LINE_POINTER, selectedLinePointer)
    }
    context.startActivity(intent)
}

class CopyActivity : EditTextActivity(allowSoftKeyboard = false) {
    private var bufferPointer: Long = 0
    private var selectedLinePointer: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.setBackgroundColor(P.colorPrimary)

        bufferPointer = intent.getLongExtra(EXTRA_BUFFER_POINTER, 0)
        selectedLinePointer = intent.getLongExtra(EXTRA_SELECTED_LINE_POINTER, 0)

        ui.toolbar.apply {
            setTitle(R.string.dialog__copy__title)
            inflateMenu(R.menu.copy_dialog_fullscreen)
            setOnMenuItemClickListener listener@{ menuItem ->
                Select::id.find(menuItem.itemId)?.let { setBody(it) }
                true
            }
        }

        setBody(Select.WithoutTimestamps)
    }

    private fun setBody(select: Select) {
        val buffer = BufferList.findByPointer(bufferPointer) ?: return
        val body = Body.build(buffer.getLinesCopy(), selectedLinePointer, select.textGetter)
        ui.text.setBody(body)
    }
}


private typealias TextGetter = (Line) -> String

@Suppress("unused")
private enum class Select(val id: Int, val textGetter: TextGetter) {
    WithTimestamps(R.id.menu_select_with_timestamps, Line::timestampedIrcLikeString),
    WithoutTimestamps(R.id.menu_select_without_timestamps, Line::ircLikeString),
    MessagesOnly(R.id.menu_select_messages_only, Line::messageString),
}


private data class Body(val text: CharSequence, val selectionStart: Int, val selectionEnd: Int) {
    companion object {
        fun build(lines: List<Line>, selectedLinePointer: Long, textGetter: TextGetter): Body {
            val text = StringBuilder()
            var selectionStart = -1
            var selectionEnd = -1

            lines.forEach { line ->
                if (line.pointer == selectedLinePointer) selectionStart = text.length
                text.append(textGetter(line))
                if (line.pointer == selectedLinePointer) selectionEnd = text.length
                text.append("\n")
            }

            return Body(text, selectionStart, selectionEnd)
        }
    }
}

private fun EditText.setBody(body: Body) {
    setText(body.text)
    if (body.selectionStart != -1 && body.selectionEnd != -1) post {
        requestFocus()
        setTextIsSelectable(true)
        selectTextCentering(body.selectionStart, body.selectionEnd)
    }
}


// this is a hacky way of centering the selection. EditText wants to show the whole selection,
// so selecting a bigger portion of text and then narrowing down the selection
// makes it appear closer to the center. this does not account for line height;
// also the number 400 is rather random and might not work as well on other devices
// todo find a better way of centering selection
private fun EditText.selectTextCentering(selectionStart: Int, selectionEnd: Int) {
    selectTextSafe(selectionStart - 400, selectionEnd + 400)
    post { selectTextSafe(selectionStart, selectionEnd) }
}


private fun EditText.selectTextSafe(selectionStart: Int, selectionEnd: Int) {
    setSelection(selectionStart.coerceIn(text.indices),
                 selectionEnd.coerceIn(text.indices))
}


