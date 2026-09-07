# liquidGL Android/Kotlin Port Plan

- Status: proposed
- Source baseline: `naughtyduk/liquidGL` v2.0.2 at `b79845de77299c3fad3f05c470997719d31fbc9c`
- Primary target: native Kotlin and Jetpack Compose
- License: MIT; retain the upstream copyright and license in derived source and artifacts

## 1. Outcome

Build a native Android library that reproduces liquidGL's refraction, bevel, frost,
chromatic aberration, magnification, highlights, reveal, shadow, tilt, and shared
multi-lens rendering without shipping JavaScript or a WebView.

The first stable release should provide:

- An idiomatic Kotlin API.
- A first-class Jetpack Compose integration.
- A Kotlin Android Views adapter.
- One renderer and one captured background per scene, shared by every lens.
- OpenGL ES rendering on `minSdk 23` and an API 33+ `RuntimeShader`/AGSL path when
  it is demonstrably faster and visually equivalent.
- Static, on-demand, and continuously invalidated background modes.
- A sample app, visual regression coverage, benchmarks, documentation, and
  publishable Maven artifacts.

The implementation must not be a mechanical JavaScript translation. Roughly the
first 1,800 lines of the upstream file rasterize DOM/CSS. Android already owns a
native display tree, so that code should become a small Android scene-capture
abstraction rather than a Kotlin port of `NaughtyDOM`.

## 2. Scope and support policy

### In scope for 1.0

- Native Kotlin implementation; no JavaScript runtime or WebView.
- Compose-first public API, with Views interoperability implemented in Kotlin.
- Rectangular and rounded-rectangle lenses, including circles and pills.
- Multiple lenses inside one explicit scene boundary.
- Scrolling, resizing, density changes, rotation, and animated lens bounds.
- Touch-driven tilt and programmatic tilt values.
- Lifecycle-safe pause, resume, resource recreation, and disposal.
- Graceful non-refracting fallback if shader setup or capture fails.

### Explicitly out of scope for 1.0

- Refracting pixels from other apps, system UI, another window, or secure content.
- Guaranteed capture of `SurfaceView`, DRM video, camera preview, or protected
  buffers. Document `TextureView`/capture-compatible alternatives where possible.
- DOM concepts such as selectors, CSS stacking contexts, CORS, sticky/fixed
  elements, GSAP, Lenis, or Locomotive Scroll.
- Kotlin Multiplatform. Keep shader math and style models portable where cheap,
  but do not let KMP delay the Android release.
- Arbitrary paths in the first release. Begin with rounded rectangles; add an
  Android `Shape`/mask extension after the render architecture is stable.

### Recommended compatibility baseline

- `minSdk 23`.
- Compile and target the latest stable Android SDK available when implementation
  starts; do not encode a preview SDK in a release artifact.
- OpenGL ES 2.0 as the required compatibility renderer. The upstream shader is
  already close to GLSL ES 1.00, which reduces porting risk.
- API 33+ AGSL backend behind the same internal renderer interface. Android's
  `RuntimeShader` was added in API 33, so it cannot be the only backend if the
  library keeps `minSdk 23`.

## 3. Upstream-to-Android mapping

| Upstream responsibility | Kotlin/Android replacement |
| --- | --- |
| `NaughtyDOM` measurement and rasterization | Explicit scene/source capture owned by Compose or a ViewGroup |
| One fixed WebGL canvas | One lifecycle-aware renderer per `LiquidGlassScene` |
| `liquidGLRenderer` | Internal `LiquidGlassRenderer` plus pluggable render backend |
| `liquidGLLens` | Stable `LiquidGlassLensState` and immutable `LiquidGlassStyle` |
| Fragment shader uniforms | Typed frame/lens uniform models with range validation |
| `requestAnimationFrame` | Compose frame clock or Android `Choreographer` |
| `getBoundingClientRect()` | Coordinates translated into scene-local pixels |
| `ResizeObserver` and scroll polling | Layout callbacks, snapshot observation, and draw invalidation |
| `registerDynamic()` | `CapturePolicy` and explicit `state.invalidateSource()` |
| Hover/touch tilt | Compose pointer input and View touch handling |
| CSS shadow/reveal fallback | Native draw layer, alpha animation, blur/tint fallback |
| Shared canvas/z-index constraint | Explicit scene layering and one lens registry |

The numerical meaning and default values of upstream options should be preserved
where practical. Android-only units must be explicit: public size values use `Dp`
in Compose, while the renderer receives pixels.

## 4. Proposed repository layout

```text
liquidGL/
  build-logic/                    convention plugins and publishing setup
  gradle/libs.versions.toml
  liquidglass-core/               Kotlin models, validation, math, renderer contracts
  liquidglass-renderer/           Android capture, EGL/GLES, AGSL, lifecycle
  liquidglass-compose/            Compose scene, modifiers/composables, state
  liquidglass-views/              ViewGroup/View adapter written in Kotlin
  sample/                         interactive Compose and Views demos
  benchmark/                      Macrobenchmark and representative scenes
  screenshots/                    golden-image fixtures and comparison tooling
  docs/                           usage, limitations, architecture, migration notes
  ANDROID_KOTLIN_PORT_PLAN.md
```

Publish separate artifacts so a Views-only consumer does not pull Compose:

```text
io.github.p1nkcoder.liquidgl:liquidglass-core
io.github.p1nkcoder.liquidgl:liquidglass-compose
io.github.p1nkcoder.liquidgl:liquidglass-views
```

Confirm the group ID before the first public release. The package namespace can
initially be `io.github.p1nkcoder.liquidgl`.

## 5. Public Kotlin API

Keep renderer types internal. Consumers should configure a scene, a style, and a
lens rather than manage textures or GL contexts.

```kotlin
@Immutable
data class LiquidGlassStyle(
    val refraction: Float = 0.01f,
    val aberration: Float = 0f,
    val bevelDepth: Float = 0.08f,
    val bevelWidth: Float = 0.15f,
    val frost: Dp = 0.dp,
    val magnification: Float = 1f,
    val specular: Boolean = true,
    val shadow: LiquidGlassShadow? = LiquidGlassShadow.Default,
)

sealed interface LiquidGlassCapturePolicy {
    data object Static : LiquidGlassCapturePolicy
    data object OnInvalidation : LiquidGlassCapturePolicy
    data class Continuous(val maxFramesPerSecond: Int = 60) : LiquidGlassCapturePolicy
}

@Composable
fun rememberLiquidGlassSceneState(
    capturePolicy: LiquidGlassCapturePolicy = LiquidGlassCapturePolicy.OnInvalidation,
): LiquidGlassSceneState

@Composable
fun LiquidGlassScene(
    state: LiquidGlassSceneState,
    modifier: Modifier = Modifier,
    content: @Composable LiquidGlassSceneScope.() -> Unit,
)

fun Modifier.liquidGlassSource(state: LiquidGlassSceneState): Modifier

@Composable
fun LiquidGlass(
    state: LiquidGlassSceneState,
    style: LiquidGlassStyle = LiquidGlassStyle(),
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    interaction: LiquidGlassInteraction = LiquidGlassInteraction.None,
    content: @Composable BoxScope.() -> Unit = {},
)
```

Representative use:

```kotlin
val glass = rememberLiquidGlassSceneState()

LiquidGlassScene(state = glass, modifier = Modifier.fillMaxSize()) {
    Feed(modifier = Modifier.liquidGlassSource(glass))

    LiquidGlass(
        state = glass,
        style = LiquidGlassPresets.Frost,
        shape = RoundedCornerShape(28.dp),
        interaction = LiquidGlassInteraction.TouchTilt(maxDegrees = 5f),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(24.dp)
            .fillMaxWidth()
            .height(72.dp),
    ) {
        NavigationContent()
    }
}
```

API rules:

- `LiquidGlassStyle` is immutable and validates/clamps the same ranges as upstream.
- `LiquidGlassSceneState` owns invalidation, not Android resources; it is safe to
  remember/save where appropriate.
- GL objects, bitmaps, views, and coroutine scopes stay lifecycle-owned and internal.
- Lens content remains regular Compose/View content above the visual effect so it
  preserves accessibility, focus, semantics, and input handling.
- Motion honors the system animator duration scale; touch tilt is disabled when
  motion is disabled unless the caller explicitly overrides it.
- Provide presets matching upstream: Default, Alien, Pulse, Frost, and Edge.

The Views API should mirror these concepts with `LiquidGlassLayout`,
`LiquidGlassView`, `LiquidGlassSceneController`, and Kotlin properties rather than
exposing the Compose types.

## 6. Rendering architecture

```text
source subtree
    -> scene capture controller
    -> one bounded source texture
    -> one renderer per scene
    -> lens registry + immutable frame state
    -> clip/scissor and shader draw for each lens
    -> ordinary Compose/View foreground content
```

### Scene and capture contract

Require an explicit scene/source instead of attempting to read arbitrary pixels
behind any composable. This makes ordering deterministic, prevents the lens from
capturing itself, and makes capture work measurable and testable.

At each frame:

1. The UI thread publishes scene size, source bounds, lens bounds, shape, and style
   as one immutable frame snapshot.
2. The capture controller updates the source only when its policy says it is dirty.
3. The render thread uploads or reuses the source texture.
4. The renderer clears once, then draws every visible lens using viewport/scissor
   rectangles and shared shader state.
5. Compose or the View hierarchy draws accessible foreground content normally.

Never synchronously read GPU pixels in the steady-state render path. A capture
implementation that requires `toImageBitmap()`/bitmap readback is acceptable for
the static prototype, but continuous mode must move to a render-thread-native
recording/copy path before 1.0.

### Backend interface

Introduce a small internal contract early:

```kotlin
internal interface LiquidGlassBackend : Closeable {
    fun resize(widthPx: Int, heightPx: Int)
    fun updateSource(source: CapturedSource)
    fun render(frame: LiquidGlassFrame)
}
```

Implementations:

- `GlesBackend`: compatibility path, based on EGL/OpenGL ES 2.0. Use one transparent
  scene surface/texture and one GL context, not one context per lens.
- `AgslBackend`: API 33+ path using `RuntimeShader`. Ship only after parity,
  clipping, and GPU timing tests prove it is worthwhile.
- `FallbackBackend`: translucent tint, border highlight, and supported blur where
  available; no fake claim of true refraction.

Select a backend through capability detection and an internal policy. Add a debug
override in the sample app so every backend can be tested on capable devices.

### Shader port

Port the upstream math before changing its appearance:

- Rounded-box signed distance mask.
- Edge factor and bevel width/depth.
- Refraction vector and out-of-bounds blending.
- Five-tap clear sampling and 16-sample frost path.
- Chromatic channel offsets.
- Time-based specular highlights.
- Reveal alpha, magnification, tilt offset, and subpixel correction.

Keep the source shader readable in `res/raw` or generated source files. Do not bury
it in a giant Kotlin string. Put coordinate-system adaptation in named helpers and
golden tests because WebGL and Android texture origins differ.

AGSL is similar to GLSL but runs inside Android's graphics pipeline and uses a
different shader entry/input model. Maintain two thin shader front ends around the
same documented equations rather than attempting unsafe textual conversion at
runtime.

### Texture and memory policy

- Query the device maximum texture size.
- Bound capture scale by source dimensions, max texture size, and a configurable
  memory budget.
- Default to the smallest scale that is sharp at device density; do not blindly
  copy the web default of `resolution = 2` onto xxhdpi/xxxhdpi devices.
- Reuse textures and buffers; no per-frame bitmap allocation.
- Upload only dirty source regions when the capture backend makes them available.
- Pause continuous rendering when the scene is invisible, detached, stopped, or
  has no visible lenses.

## 7. Delivery phases

### Phase 0 - Preserve a reference baseline

- Record upstream commit/version and retain its MIT attribution.
- Extract the shader into annotated reference fixtures without modifying behavior.
- Recreate three deterministic web reference scenes: clear rounded card, frosted
  card, and high-refraction/chromatic card.
- Save known option values, viewport sizes, and reference screenshots.

Exit criterion: repeatable reference images and a written option/feature matrix.

### Phase 1 - Kotlin/Gradle skeleton

- Add the multi-module Gradle build, version catalog, Android/Kotlin conventions,
  lint, formatting, Detekt, Dokka, and CI.
- Add immutable style/state models, presets, validation, coordinate math, and unit
  tests in `liquidglass-core`.
- Configure explicit API mode and binary API checks for public modules.

Exit criterion: clean build/test/lint on CI with empty renderer implementations.

### Phase 2 - Rendering and capture spike

- Render a supplied bitmap through the GLSL shader in an Android test scene.
- Port the same scene to AGSL on API 33+.
- Prototype Compose source recording and Views source capture.
- Measure bitmap readback, upload time, GPU time, and end-to-end frame time.
- Validate alpha composition and shared rendering with at least three overlapping
  lenses.

Decision gate: keep both backends only if both are maintainable. If continuous
Compose capture cannot avoid readback, ship `Static`/`OnInvalidation` first and
keep `Continuous` experimental rather than hiding a janky implementation.

Exit criterion: one working lens over a static native Android scene with golden
image parity and no recursive capture.

### Phase 3 - Compose MVP

- Implement `LiquidGlassScene`, source registration, lens registration, layout
  coordinate tracking, clipping, and disposal.
- Add all style parameters and upstream presets.
- Add reveal and touch tilt using Compose animation/frame APIs.
- Support multiple lenses and scrolling source/lens content.
- Add fallback behavior and actionable diagnostics.

Exit criterion: the sample app can reproduce the five upstream presets with one
and multiple Compose lenses across orientation and density changes.

### Phase 4 - Dynamic content and lifecycle

- Add explicit invalidation and capture policies.
- Coalesce source changes to at most one capture per frame.
- Handle activity/fragment lifecycle, backgrounding, low-memory signals, context
  loss, detach/reattach, and configuration changes.
- Test animated Compose content and capture-compatible video. Clearly document
  unsupported `SurfaceView`/secure-buffer cases.

Exit criterion: no leaked scene, thread, bitmap, surface, or GL resource after
repeated navigation and recreation; stable animation without self-capture.

### Phase 5 - Android Views adapter

- Implement `LiquidGlassLayout` as the explicit scene boundary.
- Implement Kotlin-friendly `LiquidGlassView` configuration and lens content
  layering.
- Track layout/scroll/invalidation without polling.
- Add XML attributes only for common visual values; keep advanced configuration in
  Kotlin.

Exit criterion: equivalent static and dynamic sample screens in Compose and Views.

### Phase 6 - Performance, compatibility, and polish

- Add macrobenchmarks for 1, 5, 10, and 30 lenses; static and dynamic sources;
  clear and frosted styles.
- Profile capture, upload, render, and UI-thread work separately.
- Test representative Adreno and Mali devices plus API 23, 29, 33, and the latest
  stable API emulator/device.
- Add automatic quality reduction before allocation failure or texture overflow.
- Complete accessibility, reduced-motion, RTL, clipping, and touch tests.

Exit criterion: agreed frame/memory budgets pass on the documented reference
devices and degradation is deterministic on weaker devices.

### Phase 7 - Release

- Write Compose and Views quick starts, API reference, troubleshooting, performance
  guidance, limitations, and a web-to-Android option mapping.
- Publish signed sources, documentation, and AAR artifacts to Maven Central.
- Add semantic versioning, changelog, release workflow, dependency update policy,
  and a minimal consumer ProGuard/R8 verification app.
- Tag `0.1.0` for API feedback, then stabilize API and publish `1.0.0` only after
  real-device validation.

## 8. Verification matrix

### Unit tests

- Style range validation and defaults.
- Web-to-Android uniform mapping.
- Scene/lens coordinate transforms, density conversion, and texture UVs.
- Rounded-box distance, edge factor, magnification, and tilt calculations.
- Dirty-region merging and capture-policy scheduling.
- Renderer selection and fallback behavior.

### Instrumented and visual tests

- Compile/link shaders on real Android graphics stacks.
- Golden images for every preset, corner radius, aspect ratio, and edge condition.
- Pixel comparisons against deterministic upstream reference images, allowing a
  documented tolerance for color-space and rasterizer differences.
- Scrolling, nested scrolling, animated bounds, rotation, split-screen, and density
  changes.
- Overlapping lenses, lenses partly off-screen, and zero-sized/detached lenses.
- Lifecycle/context loss and repeated creation/disposal.
- Capture-compatible image/video/animation sources and documented failure cases.

### Performance acceptance targets

Choose and publish named reference devices before freezing these thresholds:

- No steady-state heap allocations from the render loop.
- Static scenes perform no source capture/upload after they become clean.
- UI-thread lens bookkeeping stays below 2 ms at the 95th percentile for 10 visible
  lenses on the mid-range reference device.
- The default clear style sustains the display's 60 Hz frame budget for 10 lenses
  on that device; frost and continuous capture get separate documented budgets.
- Memory use is bounded and quality drops before exceeding texture/device limits.

## 9. Main risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Backdrop capture causes readback/jank | Explicit scene boundary, dirty capture, render-thread prototype, honest policy limits |
| Lens recursively captures itself | Separate source registration and foreground/lens layers |
| `SurfaceView`/secure video is absent | Document limitation; test `TextureView`; never promise protected content capture |
| AGSL and GLSL diverge visually | Shared equations, deterministic fixtures, backend-forced screenshot tests |
| OEM shader/driver differences | ES 2.0-compatible shader, compile tests, Adreno/Mali device matrix, safe fallback |
| Large scenes exceed texture/memory limits | Query limits, adaptive scale, bounded regions, reuse allocations |
| Too many lenses create contexts/layers | One renderer/context/texture per scene and one lens registry |
| Compose churn breaks binary compatibility | Separate Compose artifact, explicit API checks, stable state/style surface |
| Upstream evolves during the port | Keep `upstream` remote, record baseline, port upstream shader changes selectively |

## 10. Definition of done for 1.0

- A Kotlin consumer adds the Compose artifact from Maven Central and renders a
  glass lens with no JavaScript, WebView, copied source, or manual GL setup.
- All documented styles, presets, interactions, capture policies, and fallback
  behavior work on the declared API/device matrix.
- Compose and Views samples build from a clean checkout.
- Visual, lifecycle, unit, lint, API compatibility, and benchmark gates pass in CI
  or the documented device lab step.
- Resource ownership is leak-free and context loss recovers without app restart.
- Public docs state what can and cannot be captured, especially video, secure
  content, and cross-window pixels.
- MIT attribution from the upstream project is present in the repository and every
  distributed artifact.

## 11. First implementation slice

Start with the smallest end-to-end proof rather than all modules at once:

1. Create `liquidglass-core`, `liquidglass-renderer`, `liquidglass-compose`, and
   `sample`.
2. Implement `LiquidGlassStyle`, one scene, one static source, and one rounded lens.
3. Port refraction, bevel, mask, magnification, and clear five-tap sampling to GLES.
4. Add a forced-backend AGSL equivalent on API 33+.
5. Compare both Android outputs with one deterministic upstream fixture.
6. Measure capture/readback/upload before committing to continuous animation.

That slice resolves the two highest-risk questions—native backdrop capture and
shader parity—before public API or feature work becomes expensive to change.

## References

- Upstream source: <https://github.com/naughtyduk/liquidGL>
- Android `RuntimeShader` reference (API 33+):
  <https://developer.android.com/reference/android/graphics/RuntimeShader>
- Android Graphics Shading Language guide:
  <https://developer.android.com/develop/ui/views/graphics/agsl>
- Android OpenGL ES guide:
  <https://developer.android.com/develop/ui/views/graphics/opengl/about-opengl>
- Compose graphics modifiers and layer recording:
  <https://developer.android.com/develop/ui/compose/graphics/draw/modifiers>
