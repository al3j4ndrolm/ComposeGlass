package io.github.al3j4ndrolm.composeglass

import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Outline
import kotlin.math.ceil
import kotlin.math.max

/** What a glass surface can draw behind its content on a given Android version. */
internal enum class Backdrop {
    /** API < 31: no blur available, a translucent fill instead. */
    Tint,

    /** API 31+: blurred, saturated backdrop. */
    Blur,

    /** API 33+ with a rounded or rectangular shape: blur plus lens refraction at the edges. */
    BlurAndRefraction,
}

internal fun backdropFor(sdk: Int, refraction: Float, cornerRadius: Float?): Backdrop = when {
    sdk < 31 -> Backdrop.Tint
    sdk >= 33 && refraction > 0f && cornerRadius != null -> Backdrop.BlurAndRefraction
    else -> Backdrop.Blur
}

/** Extra pixels recorded around the glass, so blur and refraction sample real backdrop, not clamped edges. */
internal fun backdropMargin(blurRadius: Float, refraction: Float): Int = ceil(max(blurRadius, refraction)).toInt()

/** Width of the bending rim: 2.5x the displacement, but never past the middle of the glass. */
internal fun refractionBand(strength: Float, width: Float, height: Float): Float =
    (strength * 2.5f).coerceAtMost(minOf(width, height) / 2f)

/** A corner can't be rounder than half the smaller side. */
internal fun clampCorner(radius: Float, width: Float, height: Float): Float =
    radius.coerceAtMost(minOf(width, height) / 2f)

/** Corner radius for shapes the refraction shader understands (rectangles and uniform rounded corners), or null. */
internal fun Outline.cornerRadius(): Float? = when (this) {
    is Outline.Rectangle -> 0f
    is Outline.Rounded -> if (roundRect.isSimple) roundRect.topLeftCornerRadius.x else null
    is Outline.Generic -> null
}
