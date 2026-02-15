package com.hyeonslab.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time

/**
 * Embeds a Prism 3D rendering surface inside a Compose layout.
 *
 * Platform implementations create a GPU-backed surface and run a render loop that calls [onFrame]
 * each frame with timing information.
 *
 * @param engine The Prism engine instance (used for lifecycle management).
 * @param modifier Compose modifier for layout and sizing.
 * @param onFrame Called each frame with [Time] data. Callers typically update ECS systems here.
 * @param onResize Called when the rendering surface is resized with the new width and height in
 *   pixels. Callers should propagate this to their renderer (e.g., `renderer.resize(w, h)`).
 */
@Composable
expect fun PrismView(
  engine: Engine,
  modifier: Modifier = Modifier,
  onFrame: ((Time) -> Unit)? = null,
  onResize: ((width: Int, height: Int) -> Unit)? = null,
)
