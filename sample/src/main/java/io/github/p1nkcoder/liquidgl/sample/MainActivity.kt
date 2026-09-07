package io.github.p1nkcoder.liquidgl.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.p1nkcoder.liquidgl.compose.LiquidGlass
import io.github.p1nkcoder.liquidgl.compose.LiquidGlassCapabilities
import io.github.p1nkcoder.liquidgl.compose.LiquidGlassPresets
import io.github.p1nkcoder.liquidgl.compose.LiquidGlassScene
import io.github.p1nkcoder.liquidgl.compose.liquidGlassSource
import io.github.p1nkcoder.liquidgl.compose.rememberLiquidGlassSceneState

public class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LiquidGlassDemo()
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassDemo(): Unit {
    val sceneState = rememberLiquidGlassSceneState()

    LiquidGlassScene(
        state = sceneState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassSource(sceneState),
        ) {
            DemoBackdrop()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 72.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = "Native liquidGL",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (LiquidGlassCapabilities.supportsNativeRefraction) {
                        "RuntimeShader refraction is active"
                    } else {
                        "Native fallback is active (full refraction requires Android 13+)"
                    },
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 16.sp,
                )

                repeat(5) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(22.dp),
                            )
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(demoColors[index], CircleShape),
                        )
                        Column {
                            Text(
                                text = "Glass source ${index + 1}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "This content is sampled by the lens",
                                color = Color.White.copy(alpha = 0.62f),
                            )
                        }
                    }
                }
            }
        }

        LiquidGlass(
            state = sceneState,
            style = LiquidGlassPresets.Frost,
            cornerRadius = 28.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth()
                .height(76.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Home", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Explore", color = Color.White.copy(alpha = 0.78f))
                Text("Profile", color = Color.White.copy(alpha = 0.78f))
            }
        }
    }
}

@Composable
private fun DemoBackdrop(): Unit {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF111A35), Color(0xFF28113D), Color(0xFF061E32)),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        drawGlow(Color(0xFFFE6D73), Offset(size.width * 0.18f, size.height * 0.22f), 180.dp.toPx())
        drawGlow(Color(0xFF00D4FF), Offset(size.width * 0.86f, size.height * 0.48f), 220.dp.toPx())
        drawGlow(Color(0xFF9C6BFF), Offset(size.width * 0.34f, size.height * 0.82f), 200.dp.toPx())
    }
}

private fun DrawScope.drawGlow(color: Color, center: Offset, radius: Float): Unit {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private val demoColors: List<Color> = listOf(
    Color(0xFFFF7A8A),
    Color(0xFF52D9FF),
    Color(0xFFA783FF),
    Color(0xFFFFC857),
    Color(0xFF5EE6A8),
)
