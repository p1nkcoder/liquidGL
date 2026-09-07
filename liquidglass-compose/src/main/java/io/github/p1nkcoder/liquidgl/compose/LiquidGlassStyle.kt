package io.github.p1nkcoder.liquidgl.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Visual parameters for a native liquid-glass lens. */
@Immutable
public data class LiquidGlassStyle(
    public val refraction: Float = 0.01f,
    public val aberration: Float = 0f,
    public val bevelDepth: Float = 0.08f,
    public val bevelWidth: Float = 0.15f,
    public val frost: Dp = 0.dp,
    public val magnification: Float = 1f,
    public val specular: Boolean = true,
    public val tint: Color = Color.White.copy(alpha = 0.07f),
    public val borderColor: Color = Color.White.copy(alpha = 0.24f),
    public val borderWidth: Dp = 1.dp,
    public val shadowElevation: Dp = 10.dp,
) {
    init {
        require(refraction in 0f..1f) { "refraction must be between 0 and 1" }
        require(aberration in 0f..1f) { "aberration must be between 0 and 1" }
        require(bevelDepth in 0f..1f) { "bevelDepth must be between 0 and 1" }
        require(bevelWidth in 0f..1f) { "bevelWidth must be between 0 and 1" }
        require(frost.value >= 0f) { "frost cannot be negative" }
        require(magnification in 0.001f..3f) { "magnification must be between 0.001 and 3" }
        require(borderWidth.value >= 0f) { "borderWidth cannot be negative" }
        require(shadowElevation.value >= 0f) { "shadowElevation cannot be negative" }
    }
}

/** Presets ported from the upstream liquidGL project. */
public object LiquidGlassPresets {
    public val Default: LiquidGlassStyle = LiquidGlassStyle(
        refraction = 0f,
        bevelDepth = 0.052f,
        bevelWidth = 0.211f,
        frost = 2.dp,
    )

    public val Alien: LiquidGlassStyle = LiquidGlassStyle(
        refraction = 0.073f,
        bevelDepth = 0.2f,
        bevelWidth = 0.156f,
        frost = 2.dp,
        specular = false,
    )

    public val Pulse: LiquidGlassStyle = LiquidGlassStyle(
        refraction = 0.03f,
        bevelDepth = 0f,
        bevelWidth = 0.273f,
        specular = false,
        shadowElevation = 0.dp,
    )

    public val Frost: LiquidGlassStyle = LiquidGlassStyle(
        refraction = 0f,
        bevelDepth = 0.035f,
        bevelWidth = 0.119f,
        frost = 0.9.dp,
    )

    public val Edge: LiquidGlassStyle = LiquidGlassStyle(
        refraction = 0.047f,
        bevelDepth = 0.136f,
        bevelWidth = 0.076f,
        frost = 2.dp,
        specular = false,
    )
}
