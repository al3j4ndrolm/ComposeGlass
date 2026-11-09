package io.github.al3j4ndrolm.composeglass

import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.random.Random
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect

/**
 * Lens refraction for rounded rectangles. Near the rim, each pixel samples the backdrop further
 * outward along the edge normal, so the background appears to bend around the glass.
 */
private const val REFRACTION_AGSL = """
uniform shader content;
uniform float2 origin;   // top-left of the glass inside the layer
uniform float2 size;     // glass size
uniform float radius;    // corner radius
uniform float band;      // width of the bending rim
uniform float strength;  // max displacement at the very edge

float sdRoundRect(float2 p, float2 h, float r) {
    float2 q = abs(p) - h + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 coord) {
    float2 h = size * 0.5;
    float2 p = coord - origin - h;
    float d = sdRoundRect(p, h, radius);
    if (d > 0.0 || d < -band) return content.eval(coord);

    float2 n = float2(
        sdRoundRect(p + float2(1.0, 0.0), h, radius) - sdRoundRect(p - float2(1.0, 0.0), h, radius),
        sdRoundRect(p + float2(0.0, 1.0), h, radius) - sdRoundRect(p - float2(0.0, 1.0), h, radius));
    n = n / max(length(n), 0.0001);

    float t = 1.0 + d / band;          // 0 at the inner end of the rim, 1 at the edge
    float bend = t * t * t * strength; // lens curve: steep near the edge
    return content.eval(coord + n * bend);
}
"""

@RequiresApi(31)
internal fun blurEffect(radius: Float): ComposeRenderEffect =
    RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()

@RequiresApi(33)
internal class Refraction {
    private val shader = RuntimeShader(REFRACTION_AGSL)

    fun effect(
        blurRadius: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        cornerRadius: Float,
        strength: Float,
    ): ComposeRenderEffect {
        val r = clampCorner(cornerRadius, width, height)
        shader.setFloatUniform("origin", originX, originY)
        shader.setFloatUniform("size", width, height)
        shader.setFloatUniform("radius", r)
        shader.setFloatUniform("band", refractionBand(strength, width, height))
        shader.setFloatUniform("strength", strength)
        val lens = RenderEffect.createRuntimeShaderEffect(shader, "content")
        // Blur first, then bend the blurred result.
        val combined = if (blurRadius > 0f) {
            RenderEffect.createChainEffect(
                lens,
                RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP),
            )
        } else {
            lens
        }
        return combined.asComposeRenderEffect()
    }
}

/** Small tileable grain texture, generated once and shared. */
internal val NoiseTexture: ImageBitmap by lazy {
    val size = 128
    val random = Random(7)
    val pixels = IntArray(size * size) {
        val v = random.nextInt(256)
        (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}
