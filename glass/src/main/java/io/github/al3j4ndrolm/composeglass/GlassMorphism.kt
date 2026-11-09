package io.github.al3j4ndrolm.composeglass

import android.os.Build
import android.view.View
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize

/**
 * Draws an Apple-style frosted glass surface behind this composable's content.
 *
 * ```
 * Box(Modifier.glassMorphism()) { Text("Hello") }
 * ```
 *
 * No setup is needed: whatever is drawn behind the composable in the same window is blurred.
 * On API < 31 the blur is replaced by [GlassStyle.fallbackTint].
 *
 * @param shape Shape of the glass surface.
 * @param style Blur, tint and edge settings. See [GlassDefaults].
 */
public fun Modifier.glassMorphism(
    shape: Shape = GlassDefaults.Shape,
    style: GlassStyle = GlassDefaults.Liquid,
): Modifier = this then GlassElement(shape, style)

private data class GlassElement(
    val shape: Shape,
    val style: GlassStyle,
) : ModifierNodeElement<GlassNode>() {
    override fun create() = GlassNode(shape, style)
    override fun update(node: GlassNode) = node.update(shape, style)
    override fun InspectorInfo.inspectableProperties() {
        name = "glassMorphism"
        properties["shape"] = shape
        properties["style"] = style
    }
}

private class GlassNode(
    var shape: Shape,
    var style: GlassStyle,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    private var view: View? = null
    private var capture: BackdropCapture? = null
    private var blurLayer: GraphicsLayer? = null
    private var positionInWindow: Offset = Offset.Unspecified
    private var refraction: Refraction? = null
    private var saturationFilter: ColorFilter? = null
    private var saturationFor: Float = Float.NaN
    private val noiseBrush = ShaderBrush(ImageShader(NoiseTexture, TileMode.Repeated, TileMode.Repeated))
    private val onBackdropCaptured: () -> Unit = { invalidateDraw() }

    override fun onAttach() {
        // Below API 31 there is no blur, so skip the per-frame capture entirely.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val v = currentValueOf(LocalView)
        view = v
        capture = BackdropCapture.acquire(v, onBackdropCaptured)
        blurLayer = requireGraphicsContext().createGraphicsLayer().apply { clip = true }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) refraction = Refraction()
    }

    override fun onDetach() {
        view?.let { BackdropCapture.release(it, onBackdropCaptured) }
        blurLayer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        view = null
        capture = null
        blurLayer = null
        refraction = null
    }

    fun update(shape: Shape, style: GlassStyle) {
        this.shape = shape
        this.style = style
        invalidateDraw()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val newPosition = coordinates.positionInWindow()
        if (newPosition != positionInWindow) {
            positionInWindow = newPosition
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val capture = capture
        // Keep this surface (and its content) out of the backdrop being captured.
        if (capture?.isCapturing == true) return

        val outline = shape.createOutline(size, layoutDirection, this)
        val image = capture?.image
        val layer = blurLayer

        // 1. Blurred backdrop (API 31+), otherwise a translucent fallback.
        if (layer != null && image != null && positionInWindow.isSpecified) {
            drawBlurredBackdrop(layer, image, outline)
            drawOutline(outline, style.tint)
        } else {
            drawOutline(outline, style.fallbackTint)
        }

        // 2. Sheen: light falling on curved glass, brightest at the top.
        if (style.sheen > 0f) {
            val sheen = Brush.verticalGradient(
                0f to Color.White.copy(alpha = style.sheen),
                0.45f to Color.White.copy(alpha = style.sheen * 0.15f),
                1f to Color.Transparent,
            )
            drawOutline(outline, sheen)
        }

        // 3. Grain, so the surface reads as a material instead of a flat fill.
        if (style.noise > 0f) {
            drawOutline(outline, noiseBrush, alpha = style.noise, blendMode = BlendMode.Overlay)
        }

        // 4. Rim light: bright top-left, dim sides, a softer catch-light bottom-right.
        if (style.borderWidth.value > 0f) {
            val rim = Brush.linearGradient(
                0f to style.highlight,
                0.35f to style.highlight.copy(alpha = style.highlight.alpha * 0.12f),
                0.65f to style.highlight.copy(alpha = style.highlight.alpha * 0.08f),
                1f to style.highlight.copy(alpha = style.highlight.alpha * 0.55f),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
            val stroke = style.borderWidth.toPx()
            // Inset by half the stroke so the rim sits fully inside the shape.
            val inset = shape.createOutline(
                androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                layoutDirection,
                this,
            )
            translate(stroke / 2f, stroke / 2f) {
                drawOutline(inset, rim, style = Stroke(width = stroke))
            }
        }

        // 5. The composable's own content on top.
        drawContent()
    }

    private fun ContentDrawScope.drawBlurredBackdrop(
        layer: GraphicsLayer,
        backdrop: ImageBitmap,
        outline: Outline,
    ) {
        val radius = style.blurRadius.toPx()
        val bend = style.refraction.toPx()
        // Record with a margin so blur and refraction sample real pixels beyond our edges.
        val margin = backdropMargin(radius, bend)
        val factor = BackdropCapture.DOWNSAMPLE

        val lens = refraction
        val corner = outline.cornerRadius()
        layer.renderEffect = when (backdropFor(Build.VERSION.SDK_INT, bend, corner)) {
            Backdrop.BlurAndRefraction ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && lens != null && corner != null) {
                    lens.effect(radius, margin.toFloat(), margin.toFloat(), size.width, size.height, corner, bend)
                } else {
                    null
                }
            Backdrop.Blur ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radius > 0f) blurEffect(radius) else null
            Backdrop.Tint -> null
        }
        layer.record(size = IntSize(size.width.toInt() + margin * 2, size.height.toInt() + margin * 2)) {
            translate(left = margin - positionInWindow.x, top = margin - positionInWindow.y) {
                scale(factor, pivot = Offset.Zero) {
                    drawImage(backdrop, colorFilter = saturation())
                }
            }
        }

        clipPath(Path().apply { addOutline(outline) }) {
            translate(left = -margin.toFloat(), top = -margin.toFloat()) {
                drawLayer(layer)
            }
        }
    }

    private fun saturation(): ColorFilter? {
        val value = style.saturation
        if (value == 1f) return null
        if (value != saturationFor) {
            saturationFor = value
            saturationFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(value) })
        }
        return saturationFilter
    }
}
