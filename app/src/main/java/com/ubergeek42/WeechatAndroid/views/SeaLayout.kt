package com.ubergeek42.WeechatAndroid.views

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.FloatRange
import androidx.core.graphics.withTranslation
import com.ubergeek42.WeechatAndroid.service.P
import com.ubergeek42.WeechatAndroid.upload.f
import com.ubergeek42.WeechatAndroid.utils.SHOULD_EMOJIFY
import com.ubergeek42.weechat.ColorScheme


class SeaLayout(
    val timeStamp: String?,
    val prefix: Spannable,
    val message: Spannable,
    val width: Int,
    val colorScheme: ColorScheme,
    val textPaint: TextPaint,
) {
    val timeStampPaint: Paint get() = Paint(P.textPaint).apply { //-get
        fontFeatureSettings = "tnum"
        color = colorScheme.chat_time[0] or -0x1000000// bg?
    }

    val prefixOffset: Float
    val moreOffset: Float
    val messageOffset: Float

    val prefixLayout: Layout
    val messageLayout: Layout

    val timeStampWidth: Float
    val spaceWidth: Float

    val height get() = messageLayout.height

    init {
        val plusWidth = timeStampPaint.measureText("+")
        spaceWidth = timeStampPaint.measureText(" ")

        if (timeStamp != null) {
            timeStampWidth = timeStampPaint.measureText(timeStamp)
        } else {
            timeStampWidth = 0f
        }

        val prefixWidth = timeStampPaint.measureText("a") * 3
        val prefixWithWithALittleExtra = prefixWidth + 0.33 * spaceWidth

        val (fittingChars, consumedWidth) = if (SHOULD_EMOJIFY) {
            cutPrefixUsingLayout(prefixWithWithALittleExtra)
        } else {
            cutPrefixUsingPaint(prefixWithWithALittleExtra)
        }

        prefixOffset = timeStampWidth + spaceWidth + (prefixWidth - consumedWidth)

        prefixLayout = StaticLayout.Builder.obtain(prefix, 0, fittingChars, P.textPaint, Int.MAX_VALUE)
                .build()

        moreOffset = if (fittingChars < prefix.length) prefixOffset + consumedWidth else -1f

        messageOffset = prefixOffset + consumedWidth + plusWidth

        messageLayout = StaticLayout.Builder.obtain(message, 0, message.length, P.textPaint, width - messageOffset.toInt())
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL)
                .build()
    }

    fun draw(canvas: Canvas) {
        val fontMetrics = textPaint.fontMetrics
        val lineHeight = fontMetrics.bottom - fontMetrics.top + fontMetrics.leading
        println("!!! drawing: $timeStamp ${prefixLayout.text} w=${prefixLayout.width} [pref ofs: $prefixOffset, msg ofs: $messageOffset] $$$$ = ${prefixOffset - timeStampWidth -spaceWidth}")
        if (timeStamp != null) {
            canvas.drawText(timeStamp, 0f, -fontMetrics.top, timeStampPaint)
        }
        //canvas.drawRect(timeStampWidth, 0f, prefixOffset, 10f, timeStampPaint)
        //canvas.drawRect(timeStampWidth + spaceWidth, 10f, prefixOffset, 20f, timeStampPaint)
        canvas.withTranslation(prefixOffset, 0f) {
            prefixLayout.draw(canvas)
        }
        if (moreOffset != -1f) {
            canvas.withTranslation(moreOffset, 0f) {
                canvas.drawText("+", 0f, -fontMetrics.top, timeStampPaint)
            }
        }
        canvas.withTranslation(messageOffset, 0f) {
            messageLayout.draw(canvas)
        }
    }

    data class FittingChars(val amount: Int, val width: Float)

    fun cutPrefixUsingPaint(maxWidth: Double): FittingChars {
        val widths = FloatArray(prefix.length)

        // getTextWidths natively respects Spanned markup, including RelativeSizeSpan,
        // AbsoluteSizeSpan, and ReplacementSpans (like EmojiSpan / ImageSpan)
        textPaint.getTextWidths(prefix, 0, prefix.length, widths)

        var fittingChars = 0
        var consumedWidth = 0f

        for (charWidth in widths) {
            if (consumedWidth + charWidth > maxWidth) {
                break
            } else {
                consumedWidth += charWidth
                fittingChars ++
            }
        }

        return FittingChars(fittingChars, consumedWidth)
    }

    fun cutPrefixUsingLayout(maxWidth: Double): FittingChars {
        val tempLayout = StaticLayout.Builder.obtain(prefix, 0, prefix.length, P.textPaint, Int.MAX_VALUE)
                .build()

        var fittingChars = 0
        var consumedWidth = 0f
        for (i in prefix.indices) {
            val offsetForChar = tempLayout.getPrimaryHorizontal(i + 1)
            println("!!! ${prefix.substring(0, i+1)} -> $offsetForChar}")

            if (offsetForChar > maxWidth) {
                //consumedWidth = offsetForChar;
                //fittingChars ++
                break

            } else {
                consumedWidth = offsetForChar;
                fittingChars ++
            }
        }

        return FittingChars(fittingChars, consumedWidth)
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

//        println("   !!! ${prefix} ${message} :: fitting=$fittingChars, consumed=$consumedWidth, prefixWidth=$prefixWidth, plusWidth=$plusWidth spaceWidth=$spaceWidth ")