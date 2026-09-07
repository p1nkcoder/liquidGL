package io.github.p1nkcoder.liquidgl.compose

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

public class LiquidGlassStyleTest {
    @Test
    public fun upstreamDefaultsArePreserved(): Unit {
        val style = LiquidGlassStyle()

        assertEquals(0.01f, style.refraction)
        assertEquals(0.08f, style.bevelDepth)
        assertEquals(0.15f, style.bevelWidth)
        assertEquals(1f, style.magnification)
    }

    @Test(expected = IllegalArgumentException::class)
    public fun negativeFrostIsRejected(): Unit {
        LiquidGlassStyle(frost = (-1).dp)
    }
}
