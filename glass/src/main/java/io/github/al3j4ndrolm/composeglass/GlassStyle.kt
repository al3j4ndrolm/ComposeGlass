package io.github.al3j4ndrolm.composeglass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual configuration for [glassMorphism]. The defaults are the Liquid Glass look
 * ([GlassDefaults.Liquid]); start from any preset in [GlassDefaults] and `copy()` it.
 *
 * @property blurRadius Radius of the backdrop blur. Requires API 31+.
 * @property saturation Vibrancy of the backdrop. 1 = unchanged; Apple uses roughly 1.8.
 * @property refraction How far the backdrop bends at the edges, like a lens (Liquid Glass).
 *   Requires API 33+ and a rounded or rectangular shape. 0.dp disables it.
 * @property tint Translucent fill drawn over the backdrop.
 * @property fallbackTint Fill used instead of blur on API < 31.
 * @property sheen Opacity of the soft top-to-bottom light reflection. 0 disables it.
 * @property borderWidth Width of the rim light. 0.dp disables it.
 * @property highlight Brightest color of the rim light.
 * @property noise Opacity of the grain texture. 0 disables it.
 */
@Immutable
public data class GlassStyle(
    val blurRadius: Dp = 10.dp,
    val saturation: Float = 1.6f,
    val refraction: Dp = 17.dp,
    val tint: Color = Color.White.copy(alpha = 0.10f),
    val fallbackTint: Color = Color.White.copy(alpha = 0.72f),
    val sheen: Float = 0.24f,
    val borderWidth: Dp = 1.2.dp,
    val highlight: Color = Color.White.copy(alpha = 0.85f),
    val noise: Float = 0.02f,
)
