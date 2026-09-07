package io.github.p1nkcoder.liquidgl.compose

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.p1nkcoder.liquidgl.compose.internal.LiquidGlassRuntimeShader
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Defines a scene in which one source can be sampled by one or more [LiquidGlass] lenses.
 */
@Composable
public fun LiquidGlassScene(
    state: LiquidGlassSceneState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val sourceLayer = rememberGraphicsLayer()

    DisposableEffect(state, sourceLayer) {
        state.attach(sourceLayer)
        onDispose { state.detach(sourceLayer) }
    }

    Box(modifier = modifier, content = content)
}

/**
 * Marks the content that lenses should refract.
 *
 * A state supports one source. Put this modifier on a container behind the lenses.
 */
public fun Modifier.liquidGlassSource(state: LiquidGlassSceneState): Modifier =
    this
        .onGloballyPositioned { coordinates ->
            state.updateSource(coordinates.positionInWindow(), coordinates.size)
        }
        .then(
            Modifier.drawWithLiquidGlassSource(state),
        )

private fun Modifier.drawWithLiquidGlassSource(state: LiquidGlassSceneState): Modifier =
    drawWithContent {
        val layer = state.sourceLayer.value
        if (layer == null || size.width <= 0f || size.height <= 0f) {
            drawContent()
            return@drawWithContent
        }

        layer.topLeft = IntOffset.Zero
        layer.clip = false
        layer.renderEffect = null
        layer.record(size = IntSize(size.width.roundToInt(), size.height.roundToInt())) {
            this@drawWithContent.drawContent()
        }
        drawLayer(layer)
    }

/**
 * Draws a native liquid-glass lens and then draws [content] above it.
 *
 * Full refraction uses Android's GPU-backed RuntimeShader on API 33+. Older
 * versions render a native tinted glass fallback and never use a WebView.
 */
@SuppressLint("NewApi")
@Composable
public fun LiquidGlass(
    state: LiquidGlassSceneState,
    modifier: Modifier = Modifier,
    style: LiquidGlassStyle = LiquidGlassStyle(),
    cornerRadius: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val effectLayer = rememberGraphicsLayer()
    val runtimeShader = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { LiquidGlassRuntimeShader(context.applicationContext) }.getOrNull()
        } else {
            null
        }
    }
    val timeSeconds by produceState(initialValue = 0f, style.specular) {
        if (!style.specular) return@produceState
        var startNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (startNanos == 0L) startNanos = frameNanos
                value = (frameNanos - startNanos) / 1_000_000_000f
            }
        }
    }
    val roundedShape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    var lensPosition by remember { mutableStateOf(Offset.Unspecified) }

    Box(
        modifier = modifier
            .shadow(style.shadowElevation, roundedShape, clip = false)
            .onGloballyPositioned { coordinates ->
                lensPosition = coordinates.positionInWindow()
            }
            .then(
                Modifier.drawLiquidGlass(
                    state = state,
                    style = style,
                    cornerRadius = cornerRadius,
                    lensPosition = { lensPosition },
                    effectLayer = effectLayer,
                    runtimeShader = runtimeShader,
                    timeSeconds = timeSeconds,
                ),
            ),
        content = content,
    )
}

@SuppressLint("NewApi")
private fun Modifier.drawLiquidGlass(
    state: LiquidGlassSceneState,
    style: LiquidGlassStyle,
    cornerRadius: Dp,
    lensPosition: () -> Offset,
    effectLayer: androidx.compose.ui.graphics.layer.GraphicsLayer,
    runtimeShader: LiquidGlassRuntimeShader?,
    timeSeconds: Float,
): Modifier = drawWithContent {
    val sourceLayer = state.sourceLayer.value
    val sourcePosition = state.sourcePosition.value
    val sourceSize = state.sourceSize.value
    val currentLensPosition = lensPosition()
    val lensSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
    val radiusPx = cornerRadius.toPx().coerceAtMost(min(size.width, size.height) * 0.5f)

    val canRefract = runtimeShader != null &&
        sourceLayer != null &&
        sourcePosition.isSpecified &&
        currentLensPosition.isSpecified &&
        sourceSize.width > 0 &&
        sourceSize.height > 0 &&
        lensSize.width > 0 &&
        lensSize.height > 0

    if (canRefract) {
        val sourceScale = min(sourceSize.width, sourceSize.height).toFloat()
        val requestedPadding =
            (style.refraction + style.bevelDepth) * sourceScale + style.frost.toPx() * 4f + 4f
        val padding = ceil(requestedPadding).toInt().coerceIn(4, 512)
        val paddedSize = IntSize(
            width = lensSize.width + padding * 2,
            height = lensSize.height + padding * 2,
        )
        val lensOffset = currentLensPosition - sourcePosition

        effectLayer.topLeft = IntOffset.Zero
        effectLayer.compositingStrategy = CompositingStrategy.Offscreen
        effectLayer.clip = true
        effectLayer.record(size = paddedSize) {
            drawContext.canvas.withSave {
                drawContext.canvas.translate(
                    padding - lensOffset.x,
                    padding - lensOffset.y,
                )
                drawLayer(sourceLayer)
            }
        }

        val renderEffect = runtimeShader.update(
            layerSize = paddedSize,
            lensOrigin = Offset(padding.toFloat(), padding.toFloat()),
            lensSize = lensSize,
            sourceSize = sourceSize,
            cornerRadiusPx = radiusPx,
            frostPx = style.frost.toPx(),
            timeSeconds = timeSeconds,
            style = style,
        )
        effectLayer.renderEffect = renderEffect
        withTransform({ translate(-padding.toFloat(), -padding.toFloat()) }) {
            drawLayer(effectLayer)
        }
    } else {
        drawRoundRect(
            color = style.tint.copy(alpha = maxOf(style.tint.alpha, 0.14f)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx),
        )
    }

    if (style.borderWidth.value > 0f && style.borderColor.alpha > 0f) {
        drawRoundRect(
            color = style.borderColor,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx),
            style = Stroke(width = style.borderWidth.toPx()),
        )
    }

    drawContent()
}
