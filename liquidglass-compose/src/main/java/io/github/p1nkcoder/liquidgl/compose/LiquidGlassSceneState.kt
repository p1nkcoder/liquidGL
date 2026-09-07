package io.github.p1nkcoder.liquidgl.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.IntSize

/**
 * Connects one captured source with all liquid-glass lenses that sample it.
 *
 * Create this with [rememberLiquidGlassSceneState] and use the same instance for
 * [LiquidGlassScene], [liquidGlassSource], and each [LiquidGlass] lens.
 */
@Stable
public class LiquidGlassSceneState internal constructor() {
    internal val sourcePosition = mutableStateOf(Offset.Unspecified)
    internal val sourceSize = mutableStateOf(IntSize.Zero)
    internal val sourceLayer = mutableStateOf<GraphicsLayer?>(null)

    internal fun attach(layer: GraphicsLayer) {
        sourceLayer.value = layer
    }

    internal fun detach(layer: GraphicsLayer) {
        if (sourceLayer.value === layer) {
            sourceLayer.value = null
            sourcePosition.value = Offset.Unspecified
            sourceSize.value = IntSize.Zero
        }
    }

    internal fun updateSource(position: Offset, size: IntSize) {
        if (sourcePosition.value != position) sourcePosition.value = position
        if (sourceSize.value != size) sourceSize.value = size
    }
}

/** Creates state shared by a [LiquidGlassScene] and its lenses. */
@Composable
public fun rememberLiquidGlassSceneState(): LiquidGlassSceneState =
    remember { LiquidGlassSceneState() }
