package com.ubergeek42.WeechatAndroid.relay

import android.annotation.SuppressLint
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

class PrecomputedLineLayout(
    val timeStamp: String?,
    val prefix: Spannable,
    val message: Spannable,
    val width: Int,
    val colorScheme: ColorScheme,
    val textPaint: TextPaint,
    val dim: Boolean = false,
    val enclosePrefix: Boolean = false, // P.encloseNick && displayAs == LineSpec.DisplayAs.Say
    val alignment: Alignment = Alignment.PrefixRight,
) {
    fun getTimestampColor() = colorScheme.chat_time[0] or -0x1000000 // TODO bg
    fun getChatNickPrefixColor() = colorScheme.chat_nick_prefix[0] or -0x1000000 // TODO bg
    fun getChatNickSuffixColor() = colorScheme.chat_nick_prefix[0] or -0x1000000 // TODO bg
    fun getMoreColor() = colorScheme.chat_prefix_more[0] or -0x1000000 // TODO bg

    val timeStampPaint: Paint get() = Paint(textPaint).apply { //-get
        fontFeatureSettings = "tnum"
        color = getTimestampColor()
    }

    val changingTextPaint = Paint(textPaint)

    val prefixOffset: Float
    val moreOffset: Float
    val messageOffset: Float
    val nickPrefixOffset: Float
    val nickSuffixOffset: Float

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

        val maxPrefixWidth = textPaint.measureText("a") * 5

        val maxPrefixWithWithALittleExtra = when {
            alignment == Alignment.PrefixRight && drawingTimestamp -> maxPrefixWidth + 0.33f * spaceWidth
            else                                                                                                          -> maxPrefixWidth
        }

        val nickPrefixWidth = if (enclosePrefix) textPaint.measureText("<") else 0f
        val nickSuffixWidth = if (enclosePrefix) textPaint.measureText(">") else 0f

        val (fittingPrefixChars, actualPrefixWidth) = if (SHOULD_EMOJIFY) {
            cutPrefixToFitWidthUsingLayout(maxPrefixWithWithALittleExtra - nickPrefixWidth - nickSuffixWidth)
        } else {
            cutPrefixToFitWidthUsingPaint(maxPrefixWithWithALittleExtra - nickPrefixWidth - nickSuffixWidth)
        }

        prefixOffset = when {
            alignment == Alignment.PrefixRight -> timeStampAndSpaceWidth + (maxPrefixWidth - actualPrefixWidth) - nickSuffixWidth
            else                                                                                      -> timeStampAndSpaceWidth + nickPrefixWidth
        }

        nickPrefixOffset = if (enclosePrefix) prefixOffset - nickPrefixWidth else -1f
        nickSuffixOffset = if (enclosePrefix) prefixOffset + actualPrefixWidth else -1f

        prefixLayout = StaticLayout.Builder.obtain(prefix, 0, fittingPrefixChars, textPaint, Int.MAX_VALUE)
                .build()

        moreOffset = if (fittingPrefixChars < prefix.length) prefixOffset + actualPrefixWidth + nickSuffixWidth else -1f

        messageOffset = when (alignment) {
            Alignment.Left      -> 0f
            Alignment.Timestamp -> timeStampAndSpaceWidth
            else                                                                       -> prefixOffset + actualPrefixWidth + nickSuffixWidth + plusWidth
        }

        val leadingMargin = prefixOffset + actualPrefixWidth + nickSuffixWidth + plusWidth - messageOffset

        @SuppressLint("WrongConstant") // Layout.BREAK_STRATEGY_HIGH_QUALITY
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
