package io.github.al3j4ndrolm.composeglass

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Default values for [glassMorphism]. */
public object GlassDefaults {

    public val Shape: Shape = RoundedCornerShape(24.dp)

    /** iOS 26-style Liquid Glass: light blur, clear tint and lens-like refraction. The default. */
    public val Liquid: GlassStyle = GlassStyle()

    /** Frosted light material: strong blur, white tint, vibrant backdrop, no refraction. */
    public val Light: GlassStyle = GlassStyle(
        blurRadius = 24.dp,
        saturation = 1.8f,
        refraction = 0.dp,
        tint = Color.White.copy(alpha = 0.16f),
        sheen = 0.22f,
        borderWidth = 1.dp,
        highlight = Color.White.copy(alpha = 0.7f),
        noise = 0.035f,
    )

    /** Frosted dark material, for dark mode. */
    public val Dark: GlassStyle = Light.copy(
        tint = Color.Black.copy(alpha = 0.26f),
        fallbackTint = Color(0xFF1C1C1E).copy(alpha = 0.82f),
        sheen = 0.12f,
        highlight = Color.White.copy(alpha = 0.4f),
    )
}
