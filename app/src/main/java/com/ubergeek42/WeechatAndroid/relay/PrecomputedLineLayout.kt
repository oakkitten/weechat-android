package com.ubergeek42.WeechatAndroid.relay

import android.graphics.Canvas
import android.graphics.Rect
import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.AnyThread
import androidx.core.graphics.withTranslation
import androidx.core.text.PrecomputedTextCompat
import com.ubergeek42.WeechatAndroid.service.P
import com.ubergeek42.WeechatAndroid.upload.f
import com.ubergeek42.WeechatAndroid.utils.SHOULD_EMOJIFY
import com.ubergeek42.WeechatAndroid.views.BLOOM
import com.ubergeek42.WeechatAndroid.views.drawRoundedBackground
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


private class Cached private constructor(private val textPaint: TextPaint) {
    val threadLocalTextPaint: ThreadLocal<TextPaint> = ThreadLocal.withInitial { TextPaint(textPaint) }
    val threadLocalTimestampPaint: ThreadLocal<TextPaint> = ThreadLocal.withInitial { TextPaint(textPaint).apply { fontFeatureSettings = "tnum" } }

    val spaceWidth: Float = textPaint.measureText(" ")
    val aWidth: Float = textPaint.measureText("+")
    val lessThanWidth: Float = textPaint.measureText("<")
    val moreThanWidth: Float = textPaint.measureText(">")
    val plusWidth: Float
    val plusShiftForBloom: Float

    init {
        val plusBounds = Rect()
        textPaint.getTextBounds("+", 0, 1, plusBounds)
        val plusAdvanceWidth = textPaint.measureText("+")
        val plusBearingLeft = plusBounds.left.f
        val plusBearingRight = plusAdvanceWidth - plusBounds.right
        plusWidth = maxOf(plusBearingLeft, BLOOM) + maxOf(plusBearingRight, BLOOM) + plusBounds.width()
        plusShiftForBloom = maxOf(0f, BLOOM - plusBearingLeft)
    }

    companion object {
        @Volatile private var cached: Cached? = null

        @AnyThread fun get(textPaint: TextPaint): Cached {
            cached?.let { if (it.textPaint == textPaint) return it }
            return Cached(textPaint).also { cached = it }
        }
    }
}


class PrecomputedLineLayout(line: Line) {
    val originalTextPaint: TextPaint = P.textPaint
    private val timeStampPaint: TextPaint
    private val textPaint: TextPaint

    private val timestamp: String? = P.dateFormat?.print(line.timestamp)
    private val prefix: Spannable = line.getPrefixSpannable()
    private val message: Spannable = line.getMessageSpannable()
    private val maxPrefixWidthInChars: Int = P.maxWidth
    private val enclosePrefix: Boolean = P.encloseNick && line.displayAs == LineSpec.DisplayAs.Say
    private val alignment: Alignment = Alignment.Timestamp

    private val colorScheme: ColorScheme = ColorScheme.get()

    private fun getTimestampColor() = colorScheme.chat_time[0] or -0x1000000 // TODO bg
    private fun getChatNickPrefixColor() = colorScheme.chat_nick_prefix[0] or -0x1000000 // TODO bg
    private fun getChatNickSuffixColor() = colorScheme.chat_nick_prefix[0] or -0x1000000 // TODO bg
    private fun getMoreColor() = colorScheme.chat_prefix_more[0] or -0x1000000 // TODO bg

    private val cached = Cached.get(originalTextPaint)

    val nickPrefixOffset: Float
    val prefixOffset: Float
    val nickSuffixOffset: Float
    val moreOffset: Float

    val messageOffset: Float
    val leadingMessageIndent: Float

    val prefixLayout: StaticLayout
    val messagePrecomputedText: PrecomputedTextCompat

    init {
        timeStampPaint = cached.threadLocalTimestampPaint.get() as TextPaint
        textPaint = cached.threadLocalTextPaint.get() as TextPaint

        val timeStampAndSpaceWidth = when {
            timestamp != null -> timeStampPaint.measureText(timestamp) + cached.spaceWidth
            else -> 0f
        }

        val maxPrefixWidth = cached.aWidth * maxPrefixWidthInChars

        val maxPrefixWithWithALittleExtra = when {
            alignment == Alignment.PrefixRight && timestamp != null -> maxPrefixWidth + 0.33f * cached.spaceWidth
            else -> maxPrefixWidth
        }

        val nickPrefixWidth = if (enclosePrefix) cached.lessThanWidth else 0f
        val nickSuffixWidth = if (enclosePrefix) cached.moreThanWidth else 0f

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

        moreOffset = if (fittingPrefixChars < prefix.length) prefixOffset + consumedPrefixWidth + nickSuffixWidth + cached.plusShiftForBloom else -1f

        messageOffset = when (alignment) {
            Alignment.Left      -> 0f
            Alignment.Timestamp -> timeStampAndSpaceWidth
            else                -> prefixOffset + consumedPrefixWidth + nickSuffixWidth + cached.plusWidth
        }

        leadingMessageIndent = prefixOffset + consumedPrefixWidth + nickSuffixWidth + cached.plusWidth - messageOffset

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
                .apply { if (leadingMessageIndent > 0) setIndents(intArrayOf(leadingMessageIndent.toInt(), 0), null) }
                .build()
    }

    fun draw(canvas: Canvas, messageLayout: StaticLayout) {
        val textY = -timeStampPaint.fontMetrics.top

        if (timestamp != null) {
            canvas.drawText(timestamp, 0f, textY, timeStampPaint.apply { color = getTimestampColor() })
        }

        if (nickPrefixOffset != -1f) {
            canvas.drawText("<", nickPrefixOffset, textY, textPaint.apply { color = getChatNickPrefixColor() }) // TODO paint
        }

        canvas.withTranslation(prefixOffset, 0f) {
            prefixLayout.drawRoundedBackground(canvas, textPaint, 0f)
            prefixLayout.draw(canvas)
        }

        if (nickSuffixOffset != -1f) {
            canvas.drawText(">", nickSuffixOffset, textY, textPaint.apply { color = getChatNickSuffixColor() }) // TODO paint
        }

        if (moreOffset != -1f) {
            canvas.drawText("+", moreOffset, textY, textPaint.apply { color = getMoreColor() }) // TODO paint
        }

        canvas.withTranslation(messageOffset, 0f) {
            messageLayout.drawRoundedBackground(canvas, textPaint, leadingMessageIndent)
            messageLayout.draw(canvas)
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