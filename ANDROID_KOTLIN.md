# liquidGL for native Kotlin apps

`liquidglass-compose` is a native Android library for Kotlin and Jetpack Compose. It captures a
Compose source layer and renders it through an Android GPU `RuntimeShader`. It does not bundle or
run the web implementation, JavaScript, or a WebView.

## Requirements

- A Kotlin app using Jetpack Compose
- `minSdk` 23 or newer
- Android 13 / API 33 or newer for GPU refraction

On API 23–32, the same component automatically renders a native tinted-glass fallback.

## Add the dependency

The alpha build is distributed from this GitHub repository through JitPack.

Add JitPack to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            content { includeGroup("com.github.p1nkcoder") }
        }
    }
}
```

Then add the library to the app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.p1nkcoder:liquidGL:cf35209")
}
```

`cf35209` is the immutable, remotely verified build for the first Android alpha.

## Use it in Compose

Create one scene state, mark the content that should be visible through the glass, and place the
lens above it:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.p1nkcoder.liquidgl.compose.LiquidGlass
import io.github.p1nkcoder.liquidgl.compose.LiquidGlassPresets
import io.github.p1nkcoder.liquidgl.compose.LiquidGlassScene
import io.github.p1nkcoder.liquidgl.compose.liquidGlassSource
import io.github.p1nkcoder.liquidgl.compose.rememberLiquidGlassSceneState

@Composable
fun HomeScreen() {
    val glassState = rememberLiquidGlassSceneState()

    LiquidGlassScene(
        state = glassState,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF15233E))
                .liquidGlassSource(glassState),
        ) {
            // Your normal screen content goes here.
        }

        LiquidGlass(
            state = glassState,
            style = LiquidGlassPresets.Frost,
            cornerRadius = 28.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp),
        ) {
            Text("Glass navigation")
        }
    }
}
```

The same `glassState` must be passed to `LiquidGlassScene`, `liquidGlassSource`, and every
`LiquidGlass` lens in that scene. The source should be drawn before the lenses so it is behind them.

## Customize the glass

Use a preset (`Default`, `Alien`, `Pulse`, `Frost`, or `Edge`) or create a style:

```kotlin
val appGlass = LiquidGlassStyle(
    refraction = 0.018f,
    aberration = 0.08f,
    bevelDepth = 0.06f,
    bevelWidth = 0.14f,
    frost = 1.5.dp,
    magnification = 1.02f,
    tint = Color.White.copy(alpha = 0.08f),
)
```

## Run the included sample

Clone this repository and open it in Android Studio, or build it from a terminal with JDK 17:

```bash
./gradlew :sample:assembleDebug
```

The sample APK is written to `sample/build/outputs/apk/debug/sample-debug.apk`.

## Use an unpublished checkout

For local development, publish the AAR and its POM to your machine's Maven Local repository:

```bash
./gradlew :liquidglass-compose:publishToMavenLocal
```

Then add `mavenLocal()` to the consuming project's repositories and use:

```kotlin
implementation("io.github.p1nkcoder.liquidgl:liquidglass-compose:0.1.0-SNAPSHOT")
```

Maven Local is machine-specific; JitPack is the option to use when another developer needs the
library without cloning its source.

## Current alpha limitations

- The public API currently targets Jetpack Compose. An XML/View app can host it in a `ComposeView`,
  but a dedicated custom `View` API is not included yet.
- A scene currently has one captured source. Multiple lenses can share it.
- API 23–32 receives the native fallback rather than refraction.

See [`sample/MainActivity.kt`](sample/src/main/java/io/github/p1nkcoder/liquidgl/sample/MainActivity.kt)
for a complete runnable screen and [`ANDROID_KOTLIN_PORT_PLAN.md`](ANDROID_KOTLIN_PORT_PLAN.md) for
the roadmap.
