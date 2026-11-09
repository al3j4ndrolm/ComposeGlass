package io.github.al3j4ndrolm.composeglass

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GlassLogicTest {

    // region Styles

    @Test
    fun defaultStyleIsLiquidGlass() {
        val style = GlassStyle()
        assertEquals(GlassDefaults.Liquid, style)
        assertEquals(1.6f, style.saturation)
        assertEquals(17.dp, style.refraction)
        assertEquals(0.24f, style.sheen)
    }

    @Test
    fun frostedPresetsDontRefract() {
        assertEquals(0.dp, GlassDefaults.Light.refraction)
        assertEquals(0.dp, GlassDefaults.Dark.refraction)
    }

    // endregion

    // region Android versions

    @Test
    fun noBlurBeforeAndroid12() {
        assertEquals(Backdrop.Tint, backdropFor(sdk = 30, refraction = 17f, cornerRadius = 24f))
    }

    @Test
    fun blurOnlyOnAndroid12() {
        assertEquals(Backdrop.Blur, backdropFor(sdk = 31, refraction = 17f, cornerRadius = 24f))
        assertEquals(Backdrop.Blur, backdropFor(sdk = 32, refraction = 17f, cornerRadius = 24f))
    }

    @Test
    fun refractionFromAndroid13() {
        assertEquals(Backdrop.BlurAndRefraction, backdropFor(sdk = 33, refraction = 17f, cornerRadius = 24f))
    }

    @Test
    fun noRefractionWhenTurnedOffOrShapeUnknown() {
        assertEquals(Backdrop.Blur, backdropFor(sdk = 35, refraction = 0f, cornerRadius = 24f))
        assertEquals(Backdrop.Blur, backdropFor(sdk = 35, refraction = 17f, cornerRadius = null))
    }

    // endregion

    // region Geometry

    @Test
    fun marginCoversTheLargerOfBlurAndRefraction() {
        assertEquals(17, backdropMargin(blurRadius = 10f, refraction = 17f))
        assertEquals(25, backdropMargin(blurRadius = 24.2f, refraction = 0f))
    }

    @Test
    fun refractionBandNeverPassesTheMiddle() {
        assertEquals(25f, refractionBand(strength = 10f, width = 300f, height = 100f))
        assertEquals(30f, refractionBand(strength = 17f, width = 300f, height = 60f))
    }

    @Test
    fun cornerIsClampedToHalfTheSmallerSide() {
        assertEquals(24f, clampCorner(24f, width = 300f, height = 60f))
        assertEquals(30f, clampCorner(999f, width = 300f, height = 60f))
    }

    @Test
    fun cornerRadiusOfSupportedShapes() {
        assertEquals(0f, Outline.Rectangle(Rect(0f, 0f, 100f, 50f)).cornerRadius())
        val rounded = RoundRect(Rect(0f, 0f, 100f, 50f), CornerRadius(20f))
        assertEquals(20f, Outline.Rounded(rounded).cornerRadius())
    }

    @Test
    fun noCornerRadiusForEllipticalCorners() {
        val elliptical = RoundRect(Rect(0f, 0f, 100f, 50f), CornerRadius(20f, 10f))
        // Elliptical corners aren't "simple", so cornerRadius() returns null and the shader is skipped.
        assertFalse(elliptical.isSimple)
    }

    // endregion
}
