package engine.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import engine.prism.core.Engine
import engine.prism.core.Time

@Composable
actual fun PrismView(engine: Engine, modifier: Modifier, onFrame: ((Time) -> Unit)?) {
  // TODO: Integrate with HTML Canvas and WebGPU surface
  DisposableEffect(engine) { onDispose {} }
}
