package io.github.p1nkcoder.liquidgl.compose.internal

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.unit.IntSize
import io.github.p1nkcoder.liquidgl.compose.R
import io.github.p1nkcoder.liquidgl.compose.LiquidGlassStyle

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class LiquidGlassRuntimeShader(context: Context) {
    private val shader = RuntimeShader(
        context.resources.openRawResource(R.raw.liquid_glass).bufferedReader().use { it.readText() },
    )

    fun update(
        layerSize: IntSize,
        lensOrigin: Offset,
        lensSize: IntSize,
        sourceSize: IntSize,
        cornerRadiusPx: Float,
        frostPx: Float,
        timeSeconds: Float,
        style: LiquidGlassStyle,
    ): ComposeRenderEffect {
        shader.setFloatUniform("uLayerSize", layerSize.width.toFloat(), layerSize.height.toFloat())
        shader.setFloatUniform("uLensOrigin", lensOrigin.x, lensOrigin.y)
        shader.setFloatUniform("uLensSize", lensSize.width.toFloat(), lensSize.height.toFloat())
        shader.setFloatUniform("uSourceSize", sourceSize.width.toFloat(), sourceSize.height.toFloat())
        shader.setFloatUniform("uRefraction", style.refraction)
        shader.setFloatUniform("uAberration", style.aberration)
        shader.setFloatUniform("uBevelDepth", style.bevelDepth)
        shader.setFloatUniform("uBevelWidth", style.bevelWidth)
        shader.setFloatUniform("uFrost", frostPx)
        shader.setFloatUniform("uRadius", cornerRadiusPx)
        shader.setFloatUniform("uMagnification", style.magnification)
        shader.setFloatUniform("uTime", timeSeconds)
        shader.setFloatUniform("uSpecular", if (style.specular) 1f else 0f)
        shader.setFloatUniform(
            "uTint",
            style.tint.red,
            style.tint.green,
            style.tint.blue,
            style.tint.alpha,
        )

        // Recreate the RenderEffect after updating the live RuntimeShader. This also
        // prompts GraphicsLayer to re-record animated uniform changes on Android.
        return RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }
}
