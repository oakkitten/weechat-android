package com.ubergeek42.WeechatAndroid.views

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderNode
import android.os.Build
import android.view.View
import kotlin.text.toInt
import kotlin.times


fun View.getAlphaDrawer(): AlphaDrawer =
    if (Build.VERSION.SDK_INT >= 29 && isHardwareAccelerated) {
        AlphaDrawerImpl29()
    } else {
        AlphaDrawerImpl26()
    }

fun View.getAlphaDrawer(width: Int, height: Int, block: (recordingCanvas: Canvas) -> Unit): AlphaDrawer =
    if (Build.VERSION.SDK_INT >= 29 && isHardwareAccelerated) {
        AlphaDrawerImpl29().apply { record(width, height, block) }
    } else {
        AlphaDrawerImpl26().apply { record(width, height, block) }
    }

interface AlphaDrawer {
    fun record(width: Int, height: Int, block: (recordingCanvas: Canvas) -> Unit)
    fun drawWithAlpha(canvas: Canvas, alpha: Float)
    fun releaseResources()
}


// Canvas MUST be hardware accelerated!
class AlphaDrawerImpl29: AlphaDrawer {
    val renderNode = RenderNode("AlphaDrawerImpl29")

    override fun record(width: Int, height: Int, block: (recordingCanvas: Canvas) -> Unit) {
        renderNode.setPosition(0, 0, width, height)
        val recordingCanvas = renderNode.beginRecording()

        try {
            block(recordingCanvas)
        } finally {
            renderNode.endRecording()
        }

    }

    override fun drawWithAlpha(canvas: Canvas, alpha: Float) {
        renderNode.alpha = alpha
        canvas.drawRenderNode(renderNode)
    }

    override fun releaseResources() {
        renderNode.discardDisplayList()
    }
}


class AlphaDrawerImpl26: AlphaDrawer {
    var bitmap: Bitmap? = null

    // Canvas MUST be hardware accelerated!
    override fun record(width: Int, height: Int, block: (recordingCanvas: Canvas) -> Unit) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        block(Canvas(bitmap!!))
    }

    override fun drawWithAlpha(canvas: Canvas, alpha: Float) {
        alphaPaint.alpha = (alpha * 255).toInt()
        canvas.drawBitmap(bitmap!!, 0f, 0f, alphaPaint)

    }

    override fun releaseResources() {
        bitmap = null
    }
}

private val alphaPaint = Paint()
