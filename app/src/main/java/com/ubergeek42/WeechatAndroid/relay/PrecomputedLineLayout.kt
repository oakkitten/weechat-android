package com.ubergeek42.WeechatAndroid.relay

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import androidx.core.text.PrecomputedTextCompat
import com.ubergeek42.WeechatAndroid.service.P
import com.ubergeek42.WeechatAndroid.utils.SHOULD_EMOJIFY
import com.ubergeek42.weechat.ColorScheme


// public final static int ALIGN_NONE = 0;
// public final static int ALIGN_LEFT = 1;
// public final static int ALIGN_RIGHT = 2;
// public final static int ALIGN_TIMESTAMP = 3;
enum class Alignment {
    Left,
    Timestamp,
    PrefixLeft,
    PrefixRight,
}


class PrecomputedLineLayout(private val line: Line) {
    val textPaint: TextPaint = P.textPaint
    private val timeStampPaint: Paint
    private val changingTextPaint: Paint

    private val timeStamp: String? = P.dateFormat?.print(line.timestamp)
    private val prefix: Spannable = line.getPrefixSpannable()
    private val message: Spannable = line.getMessageSpannable()
    private val maxPrefixWidthInChars: Int = P.maxWidth
    private val enclosePrefix: Boolean = P.encloseNick && line.displayAs == LineSpec.DisplayAs.Say
    private val alignment: Alignment = Alignment.PrefixRight

    private val colorScheme: ColorScheme = ColorScheme.get()

    private fun getTimestampColor() = colorScheme.chat_time[0] or -0x1000000 // TODO bg
    private fun getChatNickPrefixColor() = colorScheme.chat_nick_prefix[0] or -0x1000000 // TODO bg
    private fun getChatNickSuffixColor() = colorScheme.chat_nick_prefix[0] or -0x1000000 // TODO bg
    private fun getMoreColor() = colorScheme.chat_prefix_more[0] or -0x1000000 // TODO bg


    val nickPrefixOffset: Float
    val prefixOffset: Float
    val nickSuffixOffset: Float
    val moreOffset: Float

    val messageOffset: Float
    val leadingMessageIndent: Float

    val prefixLayout: StaticLayout
    val messagePrecomputedText: PrecomputedTextCompat

    init {
        timeStampPaint = Paint(textPaint).apply {
            fontFeatureSettings = "tnum"
            color = getTimestampColor()
        }

        changingTextPaint = Paint(textPaint)

        val spaceWidth = textPaint.measureText(" ")
        val plusWidth = textPaint.measureText("+")

        val drawingTimestamp = timeStamp != null

        val timeStampAndSpaceWidth = when {
            drawingTimestamp -> timeStampPaint.measureText(timeStamp) + spaceWidth
            else -> 0f
        }

        val maxPrefixWidth = textPaint.measureText("a") * maxPrefixWidthInChars

        val maxPrefixWithWithALittleExtra = when {
            alignment == Alignment.PrefixRight && drawingTimestamp -> maxPrefixWidth + 0.33f * spaceWidth
            else -> maxPrefixWidth
        }

        val nickPrefixWidth = if (enclosePrefix) textPaint.measureText("<") else 0f
        val nickSuffixWidth = if (enclosePrefix) textPaint.measureText(">") else 0f

        val (fittingPrefixChars, consumedPrefixWidth) = if (SHOULD_EMOJIFY) {
            prefix.cutToFitWidthUsingLayout(textPaint, maxPrefixWithWithALittleExtra - nickPrefixWidth - nickSuffixWidth)
        } else {
            prefix.cutToFitWidthUsingPaint(textPaint, maxPrefixWithWithALittleExtra - nickPrefixWidth - nickSuffixWidth)
        }

        prefixOffset = when {
            alignment == Alignment.PrefixRight -> timeStampAndSpaceWidth + (maxPrefixWidth - consumedPrefixWidth) - nickSuffixWidth
            else -> timeStampAndSpaceWidth + nickPrefixWidth
        }

        nickPrefixOffset = if (enclosePrefix) prefixOffset - nickPrefixWidth else -1f
        nickSuffixOffset = if (enclosePrefix) prefixOffset + consumedPrefixWidth else -1f

        moreOffset = if (fittingPrefixChars < prefix.length) prefixOffset + consumedPrefixWidth + nickSuffixWidth else -1f

        messageOffset = when (alignment) {
            Alignment.Left      -> 0f
            Alignment.Timestamp -> timeStampAndSpaceWidth
            else                -> prefixOffset + consumedPrefixWidth + nickSuffixWidth + plusWidth
        }

        leadingMessageIndent = prefixOffset + consumedPrefixWidth + nickSuffixWidth + plusWidth - messageOffset

        prefixLayout = StaticLayout.Builder.obtain(prefix, 0, fittingPrefixChars, textPaint, Int.MAX_VALUE)
                .build()

        val messagePrecomputedTextParams = PrecomputedTextCompat.Params.Builder(textPaint)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL)
                .build()
        messagePrecomputedText = PrecomputedTextCompat.create(message, messagePrecomputedTextParams)
    }

    fun obtainMessageLayout(viewWidth: Int): StaticLayout {
        return StaticLayout.Builder.obtain(messagePrecomputedText, 0, messagePrecomputedText.length, textPaint, viewWidth - messageOffset.toInt())
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL)
                .build()
    }

    fun draw(canvas: Canvas) {
        if (timeStamp != null) {
            canvas.drawText(timeStamp, 0f, -timeStampPaint.fontMetrics.top, timeStampPaint)
        }

        if (nickPrefixOffset != -1f) {
            canvas.drawText("<", nickPrefixOffset, -textPaint.fontMetrics.top, changingTextPaint.apply { color = getChatNickPrefixColor() }) // TODO paint
        }

        canvas.withTranslation(prefixOffset, 0f) {
            prefixLayout.draw(canvas)
        }

        if (nickSuffixOffset != -1f) {
            canvas.drawText(">", nickSuffixOffset, -textPaint.fontMetrics.top, changingTextPaint.apply { color = getChatNickSuffixColor() }) // TODO paint
        }

        if (moreOffset != -1f) {
            canvas.drawText("+", moreOffset, -textPaint.fontMetrics.top, changingTextPaint.apply { color = getMoreColor() }) // TODO paint
        }
    }
}


data class FittingCharsAndWidth(val fittingChars: Int, val consumedWidth: Float)

fun Spannable.cutToFitWidthUsingPaint(textPaint: TextPaint, maxWidth: Float): FittingCharsAndWidth {
    val charWidths = FloatArray(this.length)

    textPaint.getTextWidths(this, 0, this.length, charWidths)

    var fittingChars = 0
    var consumedWidth = 0f

    for (charWidth in charWidths) {
        if (consumedWidth + charWidth > maxWidth) {
            break
        } else {
            consumedWidth += charWidth
            fittingChars++
        }
    }

    return FittingCharsAndWidth(fittingChars, consumedWidth)
}

fun Spannable.cutToFitWidthUsingLayout(textPaint: TextPaint, maxWidth: Float): FittingCharsAndWidth {
    val tempLayout = StaticLayout.Builder.obtain(this, 0, this.length, textPaint, Int.MAX_VALUE)
            .build()

    var fittingChars = 0
    var consumedWidth = 0f

    for (i in this.indices) {
        val offsetForChar = tempLayout.getPrimaryHorizontal(i + 1)

        if (offsetForChar > maxWidth) {
            break
        } else {
            consumedWidth = offsetForChar
            fittingChars++
        }
    }

    return FittingCharsAndWidth(fittingChars, consumedWidth)
}