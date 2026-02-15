package engine.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import engine.prism.core.Engine
import engine.prism.core.Time

@Composable
actual fun PrismView(engine: Engine, modifier: Modifier, onFrame: ((Time) -> Unit)?) {
  // TODO: Use UIKitView to embed MTKView/CAMetalLayer for wgpu rendering
  DisposableEffect(engine) { onDispose {} }
}
