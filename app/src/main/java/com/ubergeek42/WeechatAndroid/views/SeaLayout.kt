package com.ubergeek42.WeechatAndroid.views

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import com.ubergeek42.WeechatAndroid.upload.f
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

class SeaLayout(
    val timeStamp: String?,
    val prefix: Spannable,
    val message: Spannable,
    val width: Int,
    val colorScheme: ColorScheme,
    val textPaint: TextPaint,
    val alignment: Alignment = Alignment.PrefixRight,

    ) {
    fun getTimestampColor() = colorScheme.chat_time[0] or -0x1000000 // TODO bg
    fun getChatNickPrefixColor() = colorScheme.chat_nick_prefix[0] or -0x1000000 // TODO bg

    val timeStampPaint: Paint get() = Paint(textPaint).apply { //-get
        fontFeatureSettings = "tnum"
        color = colorScheme.chat_time[0] or -0x1000000// bg?
    }

    val prefixOffset: Float
    val moreOffset: Float
    val messageOffset: Float

    val prefixLayout: Layout
    val messageLayout: Layout

    val height get() = messageLayout.height

    init {
        val spaceWidth = textPaint.measureText(" ")
        val plusWidth = textPaint.measureText("+")

        val drawingTimestamp = timeStamp != null

        val timeStampAndSpaceWidth = when {
            drawingTimestamp -> timeStampPaint.measureText(timeStamp) + spaceWidth
            else -> 0f
        }

        val maxPrefixWidth = textPaint.measureText("a") * 3

        val maxPrefixWithWithALittleExtra = when {
            alignment == Alignment.PrefixRight && drawingTimestamp -> maxPrefixWidth + 0.33f * spaceWidth
            else -> maxPrefixWidth
        }

        val (fittingPrefixChars, actualPrefixWidth) = if (SHOULD_EMOJIFY) {
            cutPrefixToFitWidthUsingLayout(maxPrefixWithWithALittleExtra)
        } else {
            cutPrefixToFitWidthUsingPaint(maxPrefixWithWithALittleExtra)
        }

        prefixOffset = when {
            alignment == Alignment.PrefixRight -> timeStampAndSpaceWidth + (maxPrefixWidth - actualPrefixWidth)
            else -> timeStampAndSpaceWidth
        }

        prefixLayout = StaticLayout.Builder.obtain(prefix, 0, fittingPrefixChars, textPaint, Int.MAX_VALUE)
                .build()

        moreOffset = if (fittingPrefixChars < prefix.length) prefixOffset + actualPrefixWidth else -1f

        messageOffset = when (alignment) {
            Alignment.Left -> 0f
            Alignment.Timestamp -> timeStampAndSpaceWidth
            else -> prefixOffset + actualPrefixWidth + plusWidth
        }

        val leadingMargin = prefixOffset + actualPrefixWidth + plusWidth - messageOffset

        messageLayout = StaticLayout.Builder.obtain(message, 0, message.length, textPaint, width - messageOffset.toInt())
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL)
                .apply {
                    if (leadingMargin != 0f) setIndents(intArrayOf(leadingMargin.toInt(), 0), null)
                }
                .build()
    }

    fun draw(canvas: Canvas) {
        if (timeStamp != null) {
            canvas.drawText(timeStamp, 0f, -timeStampPaint.fontMetrics.top, timeStampPaint)
        }

        canvas.withTranslation(prefixOffset, 0f) {
            prefixLayout.draw(canvas)
        }

        if (moreOffset != -1f) {
            canvas.drawText("+", moreOffset, -textPaint.fontMetrics.top, timeStampPaint) // TODO paint
        }

        canvas.withTranslation(messageOffset, 0f) {
            messageLayout.draw(canvas)
        }
    }

    data class FittingCharsAndWidth(val fittingPrefixChars: Int, val actualPrefixWidth: Float)

    fun cutPrefixToFitWidthUsingPaint(maxWidth: Float): FittingCharsAndWidth {
        val charWidths = FloatArray(prefix.length)

        textPaint.getTextWidths(prefix, 0, prefix.length, charWidths)

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

    fun cutPrefixToFitWidthUsingLayout(maxWidth: Float): FittingCharsAndWidth {
        val tempLayout = StaticLayout.Builder.obtain(prefix, 0, prefix.length, textPaint, Int.MAX_VALUE)
                .build()

        var fittingChars = 0
        var consumedWidth = 0f

        for (i in prefix.indices) {
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

    fun getLineForVertical(vertical: Int): Int {
        return messageLayout.getLineForVertical(vertical)
    }

    fun getOffsetForHorizontal(line: Int, horiz: Float): Int {
        return messageLayout.getOffsetForHorizontal(line, horiz + messageOffset)
    }

    fun getHorizontalTextCoordinatesForLine(line: Int): ClosedFloatingPointRange<Float> {
        return (messageOffset + messageLayout.getParagraphLeft(line).f)..(messageOffset + messageLayout.getLineMax(line))
    }
}
