package com.hyeonslab.prism.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger

private val log = Logger.withTag("PrismView.Android")

/**
 * Android implementation of [PrismView]. Placeholder that will be replaced with SurfaceView-backed
 * wgpu rendering in a future Compose-Android integration milestone.
 */
@Composable
actual fun PrismView(store: EngineStore, modifier: Modifier) {
  log.w { "PrismView Android: Compose integration not yet implemented" }
  Box(modifier = modifier)
}
