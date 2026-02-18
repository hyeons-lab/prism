package com.hyeonslab.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.ygdrasil.webgpu.WGPUContext

/**
 * Embeds a Prism 3D rendering surface inside a Compose layout.
 *
 * Platform implementations create a GPU-backed surface and drive the engine's game loop each frame
 * via [com.hyeonslab.prism.core.GameLoop.tick]. Frame timing, FPS, and surface dimensions are
 * dispatched as [EngineStateEvent]s through [EngineStore.dispatch].
 *
 * Consumers can register per-frame logic through the [com.hyeonslab.prism.core.Engine] API
 * (subsystems or game-loop callbacks). The [onSurfaceReady] callback fires once when the GPU
 * surface is available, allowing callers to set up renderers and scene graphs before the render
 * loop starts.
 *
 * @param store The MVI store managing engine state. Must be created via [rememberEngineStore] or
 *   [rememberExternalEngineStore].
 * @param modifier Compose modifier for layout and sizing.
 * @param onSurfaceReady Called once when the GPU surface is available, before the render loop
 *   starts. Receives the [WGPUContext] and the initial surface dimensions.
 * @param onSurfaceResized Called when the rendering surface is resized after initial creation.
 */
@Composable
expect fun PrismView(
  store: EngineStore,
  modifier: Modifier = Modifier,
  onSurfaceReady: ((WGPUContext, Int, Int) -> Unit)? = null,
  onSurfaceResized: ((Int, Int) -> Unit)? = null,
)
