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
 * Consumers register per-frame logic through the [com.hyeonslab.prism.core.Engine] API (subsystems
 * or game-loop callbacks) rather than through view-level callbacks, keeping the composable
 * stateless.
 *
 * @param store The MVI store managing engine state. Must be created via [rememberEngineStore] or
 *   [rememberExternalEngineStore].
 * @param modifier Compose modifier for layout and sizing.
 * @param onSurfaceReady Optional callback invoked once when the GPU surface is ready. Receives the
 *   [WGPUContext], surface width, and surface height. Use this to initialize scene resources that
 *   require a live GPU context (e.g. uploading meshes, creating a renderer). Defaults to null.
 */
@Composable
expect fun PrismView(
  store: EngineStore,
  modifier: Modifier = Modifier,
  onSurfaceReady: ((WGPUContext, Int, Int) -> Unit)? = null,
)
