package io.github.p1nkcoder.liquidgl.compose

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/** Runtime capabilities of the native renderer on the current device. */
public object LiquidGlassCapabilities {
    /** True when Android's GPU-backed RuntimeShader API can render full refraction. */
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    public val supportsNativeRefraction: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
