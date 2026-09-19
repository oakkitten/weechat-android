package com.ubergeek42.WeechatAndroid.views

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.StaticLayout
import android.text.style.URLSpan
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.graphics.withTranslation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.ubergeek42.WeechatAndroid.media.Cache
import com.ubergeek42.WeechatAndroid.media.Config
import com.ubergeek42.WeechatAndroid.media.Engine
import com.ubergeek42.WeechatAndroid.media.Strategy
import com.ubergeek42.WeechatAndroid.media.Utils.isContextValidForGlide
import com.ubergeek42.WeechatAndroid.relay.EmptyLine
import com.ubergeek42.WeechatAndroid.relay.Line
import com.ubergeek42.WeechatAndroid.relay.PrecomputedLineLayout
import com.ubergeek42.WeechatAndroid.service.P
import com.ubergeek42.WeechatAndroid.upload.f
import com.ubergeek42.WeechatAndroid.upload.i
import com.ubergeek42.WeechatAndroid.upload.main
import com.ubergeek42.WeechatAndroid.utils.applicationContext
import com.ubergeek42.WeechatAndroid.utils.invalidatableLazy
import kotlin.reflect.KMutableProperty0


private const val ANIMATION_DURATION = 2500L // ms


private enum class State(
    val withImage: Boolean,
    val animatingText: Boolean,
    val animatingImage: Boolean,
) {
    TextOnly(false, false, false),                  // wide layout, no image
    TextWithImage(true, false, false),              // narrow layout. image might not be present but expected to be loaded soon
    AnimatingToTextWithImage(true, true, true),     // text only → image
    AnimatingToTextOnly(false, true, false),        // image (without image) → text. runs when the image fails to load
    AnimatingOnlyImage(true, false, true),          // narrow layout; animating only the image
}

class LineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {
    private val glide = getSafeGlide()

    private var messageSpannable: Spannable = NoSpannable
    private var precomputedLayout: PrecomputedLineLayout = NoPrecomputedLayout

    fun setText(line: Line) {
        if (precomputedLayout == line.getPrecomputedLayout() && precomputedLayout.textPaint == P.textPaint) return

        invalidateInternal()
        setTextInternal(line)
        invalidate()
    }

    private fun invalidateInternal() {
        wideMessageLayoutDelegate.invalidate()
        narrowMessageLayoutDelegate.invalidate()
        precomputedLayout = NoPrecomputedLayout
        messageSpannable = NoSpannable
        image = null
        firstDrawAt = HAVE_NOT_DRAWN
        state = State.TextOnly
        lastRequestedUrl = null
        animatedValue = 0f
        animator?.cancel()
        animator = null
        glide?.clear(target)    // will call the listener!
        target = null
    }

    private fun setTextInternal(line: Line) {
        this.messageSpannable = line.getMessageSpannable()
        this.precomputedLayout = line.getPrecomputedLayout()

        val (url, cacheInfo) = getUrlInfo(line)
        val prettySureWillShowImage = cacheInfo == Cache.Info.FETCHED_RECENTLY
        state = if (prettySureWillShowImage) State.TextWithImage else State.TextOnly

        maybeRequestLayout()

        if (url != null && cacheInfo != Cache.Info.FAILED_RECENTLY) requestThumbnail(url)
    }

    private fun maybeRequestLayout() {
        if (measuredWidth != wideLayoutWidth || measuredHeight != measureViewHeight())
            requestLayout()
    }

    private var state = State.TextOnly

    val urls: Array<URLSpan> get() = messageSpannable
            .let { it.getSpans(0, it.length, URLSpan::class.java) }


    ////////////////////////////////////////////////////////////////////////////////////////////////

    private val wideMessageLayoutDelegate = invalidatableLazy {
        precomputedLayout.obtainMessageLayout(wideLayoutWidth)
    }
    private val narrowMessageLayoutDelegate = invalidatableLazy {
        precomputedLayout.obtainMessageLayout(narrowLayoutWidth)
    }

    private val wideMessageLayout by wideMessageLayoutDelegate
    private val narrowMessageLayout by narrowMessageLayoutDelegate

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////// image
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private var image: Bitmap? = null
    private var target: Target? = null

    private fun getUrlInfo(line: Line): Pair<Strategy.Url?, Cache.Info?> {
        if (Engine.isEnabledAtAll() &&
            Engine.isEnabledForLocation(Engine.Location.CHAT) &&
            Engine.isEnabledForLine(line)) {
            val candidates = Engine.getPossibleMediaCandidates(urls, Strategy.Size.SMALL)
            if (candidates.isNotEmpty()) {
                val url = candidates[0]
                return Pair(url, Cache.info(url))
            }
        }
        return Pair(null, null)
    }

    private var lastRequestedUrl: Strategy.Url? = null
    private fun requestThumbnail(url: Strategy.Url) {
        lastRequestedUrl = url

        glide?.clear(target)

        target = (glide ?: return)
                .asBitmap()
                .apply(Engine.defaultRequestOptions)
                .listener(Cache.bitmapListener)
                .load(url)
                .onlyRetrieveFromCache(Engine.isDisabledForCurrentNetwork())
                .into(Target(Config.thumbnailWidth, measureThumbnailHeight()))
    }

    private inner class Target constructor(width: Int, height: Int) : CustomTarget<Bitmap>(width, height) {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            setImage(resource)
        }

        // the request seems to be attempted once again on minimizing/restoring the app.
        // to avoid that, clear target soon, but not on current thread--the library doesn't allow it
        override fun onLoadFailed(errorDrawable: Drawable?) {
            target?.let { main { glide?.clear(it) } }
            setImage(null)
        }

        override fun onLoadCleared(placeholder: Drawable?) {
            image = null
        }
    }

    private fun setImage(newImage: Bitmap?) {
        val oldImage = this.image
        if (oldImage == newImage && !(image == null && state == State.TextWithImage)) return
        this.image = newImage

        if (shouldAnimateChange(oldImage, newImage)) {
            animateChange()
        } else {
            state = if (newImage != null) State.TextWithImage else State.TextOnly
            maybeRequestLayout()
            invalidate()
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////// measuring & drawing
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun measureViewHeight(): Int {
        fun getTextOnlyHeight() = if (precomputedLayout === NoPrecomputedLayout) 0 else wideMessageLayout.height

        fun getTextWithImageHeight(): Int {
            val narrowLayoutHeight = if (precomputedLayout === NoPrecomputedLayout) 0 else narrowMessageLayout.height
            return maxOf(narrowLayoutHeight, Config.thumbnailAreaMinHeight)
        }

        return when (state) {
            State.TextOnly -> getTextOnlyHeight()
            State.TextWithImage, State.AnimatingOnlyImage -> getTextWithImageHeight()
            State.AnimatingToTextOnly, State.AnimatingToTextWithImage -> {
                val textOnlyHeight = getTextOnlyHeight()
                val textWithImageHeight = getTextWithImageHeight()
                textOnlyHeight + ((textWithImageHeight - textOnlyHeight) * animatedValue).i
            }
        }
    }

    private fun measureThumbnailHeight(): Int {
        return (narrowMessageLayout.height - Config.THUMBNAIL_VERTICAL_MARGIN * 2)
                .coerceAtMost(Config.thumbnailMaxHeight)
                .coerceAtLeast(Config.thumbnailMinHeight)
                .coerceAtLeast(1)
    }

    private var wideLayoutWidth: Int = 0
        get() {
            if (field == 0) field = (context as? Activity)?.calculateApproximateWeaselWidth() ?: 1000
            return field
        }

    private val narrowLayoutWidth get() = wideLayoutWidth - Config.thumbnailAreaWidth

    private var oldViewWidth = wideLayoutWidth
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val viewWidth = if (parentWidth > 0) {
            parentWidth
        } else {
            (context as? Activity)?.calculateApproximateWeaselWidth() ?: 1000
        }

        wideLayoutWidth = viewWidth

        if (oldViewWidth != viewWidth) {
            oldViewWidth = viewWidth
            wideMessageLayoutDelegate.invalidate()
            narrowMessageLayoutDelegate.invalidate()
            if (state.withImage) lastRequestedUrl?.let { requestThumbnail(it) }
        }
        setMeasuredDimension(viewWidth, measureViewHeight())
    }

    var wideLayoutAlphaDrawer: AlphaDrawer? = null
    var narrowLayoutAlphaDrawer: AlphaDrawer? = null

    fun drawText(canvas: Canvas, messageLayout: StaticLayout) {
        precomputedLayout.draw(canvas, messageLayout)
    }

    fun drawThumbnail(canvas: Canvas) {
        image?.let {
            val left = narrowLayoutWidth + Config.THUMBNAIL_HORIZONTAL_MARGIN.f
            val top = Config.THUMBNAIL_VERTICAL_MARGIN.f

            val paint = if (state.animatingImage) {
                _paint.apply { alpha = (animatedValue * 255).i }
            } else {
                null
            }

            canvas.drawBitmap(it, left, top, paint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (state.animatingText) {
            wideLayoutAlphaDrawer?.drawWithAlpha(canvas, 1f - animatedValue)
            narrowLayoutAlphaDrawer?.drawWithAlpha(canvas, animatedValue)
        } else {
            val currentMessageLayout = when (state) {
                State.TextOnly, State.AnimatingToTextOnly -> wideMessageLayout
                State.TextWithImage, State.AnimatingToTextWithImage, State.AnimatingOnlyImage -> narrowMessageLayout
            }
            drawText(canvas, currentMessageLayout)
        }

        drawThumbnail(canvas)

        if (firstDrawAt == HAVE_NOT_DRAWN) firstDrawAt = System.currentTimeMillis()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////// animation
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // time in ms when the current text have been first drawn on canvas
    private var firstDrawAt = HAVE_NOT_DRAWN

    // a value that is used to animate view size and the crossfade. 0f corresponds to the initial
    // text-only layout, 1f to the layout with image. can go both directions
    private var animatedValue = 0f

    private var animator: ValueAnimator? = null

    // animate layout change—but only if the view is visible
    // and has been visible for some minimum period of time, to avoid too much animation.
    // also don't animate when changing an existing image for a new one due to view resize
    private fun shouldAnimateChange(oldImage: Bitmap?, newImage: Bitmap?): Boolean {
        if (!isAttachedToWindow || parent == null) return false
        if (firstDrawAt == HAVE_NOT_DRAWN) return false
        if (System.currentTimeMillis() - firstDrawAt < 50) return false
        (parent as View).getHitRect(_rect)      // see https://stackoverflow.com/a/12428154/1449683
        if (!getLocalVisibleRect(_rect)) return false
        if (oldImage != null && newImage != null) return false
        return true
    }

    private fun animateChange() {
        val animatingToImage = image != null
        val (from, to) = if (animatingToImage) 0f to 1f else 1f to 0f

        state = if (animatingToImage) {
            if (state == State.TextWithImage) State.AnimatingOnlyImage else State.AnimatingToTextWithImage
        } else {
            State.AnimatingToTextOnly
        }

        wideLayoutAlphaDrawer = getAlphaDrawer(wideLayoutWidth, wideMessageLayout.height) { canvas -> drawText(canvas, wideMessageLayout) }
        narrowLayoutAlphaDrawer = getAlphaDrawer(narrowLayoutWidth, narrowMessageLayout.height)  { canvas -> drawText(canvas, narrowMessageLayout) }

        animator = ValueAnimator.ofFloat(from, to).also {
            it.duration = ANIMATION_DURATION

            it.addUpdateListener { animation ->
                animatedValue = animation.animatedValue as Float
                maybeRequestLayout()
                invalidate()
            }

            it.doOnEnd {
                state = if (animatingToImage) State.TextWithImage else State.TextOnly
                if (animatingToImage) wideMessageLayoutDelegate.invalidate() else narrowMessageLayoutDelegate.invalidate() // TODO check??
                wideLayoutAlphaDrawer?.releaseResources()
                narrowLayoutAlphaDrawer?.releaseResources()
                wideLayoutAlphaDrawer = null
                narrowLayoutAlphaDrawer = null
                animator = null
            }
        }

        animator?.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.end()
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////// touch
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // AS suggests calling performClick(), which does stuff like playing tap sound and accessibility,
    // but LinkMovementMethod doesn't seem to be calling any methods like that, so we are not doing
    // it either. on long click, we call performLongClick(), which also does haptic feedback.
    // we are consuming all touch events mostly because it works well. perhaps only handle up/down?
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    private val gestureDetector = GestureDetector(applicationContext,
            object : SimpleOnGestureListener() {
                override fun onLongPress(event: MotionEvent) {
                    performLongClick()
                }

                override fun onDoubleTap(event: MotionEvent): Boolean {
                    onDoubleTapListener?.invoke()
                    return true
                }

                // see android.text.method.LinkMovementMethod.onTouchEvent
                override fun onSingleTapUp(event: MotionEvent): Boolean {
//                    val currentLayout = this@LineView.currentLayout
//                    val line = currentLayout.getLineForVertical(event.y.i)
//
//                    if (event.x in currentLayout.getHorizontalTextCoordinatesForLine(line)) {
//                        val offset = currentLayout.getOffsetForHorizontal(line, event.x)
//                        val links = this@LineView.line.getMessageSpannable().getSpans(offset, offset, ClickableSpan::class.java)
//
//                        if (links.isNotEmpty()) {
//                            links.first().onClick(this@LineView)
//                            return true
//                        }
//                    }

                    return false
                }
            })

    var onDoubleTapListener: (() -> Unit)? = null
}


// these two are only used in single methods on the main thread
@Suppress("ObjectPropertyName") private val _paint = Paint()
@Suppress("ObjectPropertyName") private val _rect = Rect()

const val HAVE_NOT_DRAWN = -1L

private val NoSpannable = SpannableString("error")   // just so that we don't need to say !!
private val NoPrecomputedLayout = EmptyLine.getPrecomputedLayout()   // just so that we don't need to say !!


fun View.getSafeGlide() = if (isContextValidForGlide(context)) {
                               Glide.with(context)
                           } else {
                               null
                           }

fun <V : Any> KMutableProperty0<V?>.getOrSet(initializer: () -> V): V {
    return this.get() ?: initializer().also { this.set(it) }
}