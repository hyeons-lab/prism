package com.hyeonslab.prism.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.ygdrasil.webgpu.WGPUContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun PrismOverlay(
  store: EngineStore,
  modifier: Modifier = Modifier,
  onSurfaceReady: ((WGPUContext, Int, Int) -> Unit)? = null,
  onSurfaceResized: ((Int, Int) -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val isInitialized by
    store.state.map { it.isInitialized }.distinctUntilChanged().collectAsStateWithLifecycle(false)
  Box(modifier = modifier) {
    if (isInitialized) {
      PrismView(
        store = store,
        modifier = Modifier.matchParentSize(),
        onSurfaceReady = onSurfaceReady,
        onSurfaceResized = onSurfaceResized,
      )
    }
    content()
  }
}
