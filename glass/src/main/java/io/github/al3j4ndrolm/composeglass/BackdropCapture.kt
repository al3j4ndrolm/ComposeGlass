package io.github.al3j4ndrolm.composeglass

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.WeakHashMap
import kotlin.math.ceil

/**
 * Captures the window behind glass surfaces, once per frame, shared by every glass in that window.
 *
 * Just before each frame is drawn, the window's root view is drawn into a downsampled bitmap.
 * While that happens [isCapturing] is true and glass surfaces draw nothing, so they never appear
 * in their own backdrop. Same technique as Dimezis' BlurView for Android Views.
 */
internal class BackdropCapture private constructor(
    private val view: View,
) : ViewTreeObserver.OnPreDrawListener {

    var isCapturing: Boolean = false
        private set

    /** The last captured window, at 1 / [DOWNSAMPLE] scale. */
    var image: ImageBitmap? = null
        private set

    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val listeners = mutableListOf<() -> Unit>()
    private var observer: ViewTreeObserver? = null

    override fun onPreDraw(): Boolean {
        if (capture()) listeners.forEach { it() }
        return true
    }

    private fun capture(): Boolean {
        val root = view.rootView
        val width = ceil(root.width / DOWNSAMPLE).toInt()
        val height = ceil(root.height / DOWNSAMPLE).toInt()
        if (width <= 0 || height <= 0) return false

        val target = bitmap?.takeIf { it.width == width && it.height == height }
            ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                bitmap = it
                canvas = Canvas(it)
                image = it.asImageBitmap()
            }
        val c = canvas ?: return false

        target.eraseColor(android.graphics.Color.TRANSPARENT)
        val checkpoint = c.save()
        c.scale(1f / DOWNSAMPLE, 1f / DOWNSAMPLE)
        isCapturing = true
        try {
            root.draw(c)
        } finally {
            isCapturing = false
            c.restoreToCount(checkpoint)
        }
        return true
    }

    private fun add(listener: () -> Unit) {
        if (listeners.isEmpty()) {
            observer = view.viewTreeObserver.also { it.addOnPreDrawListener(this) }
        }
        listeners += listener
    }

    private fun remove(listener: () -> Unit): Boolean {
        listeners -= listener
        if (listeners.isNotEmpty()) return false
        observer?.takeIf { it.isAlive }?.removeOnPreDrawListener(this)
        observer = null
        bitmap = null
        canvas = null
        image = null
        return true
    }

    companion object {
        /** Capture at 1/4 resolution: 16x fewer pixels, invisible once blurred. */
        const val DOWNSAMPLE: Float = 4f

        private val captures = WeakHashMap<View, BackdropCapture>()

        fun acquire(view: View, listener: () -> Unit): BackdropCapture =
            captures.getOrPut(view) { BackdropCapture(view) }.also { it.add(listener) }

        fun release(view: View, listener: () -> Unit) {
            val capture = captures[view] ?: return
            if (capture.remove(listener)) captures.remove(view)
        }
    }
}
