package engine.prism.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PrismOverlay(
  engineState: EngineState,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(modifier = modifier) {
    if (engineState.isInitialized) {
      PrismView(engine = engineState.engine, modifier = Modifier.matchParentSize())
    }
    content()
  }
}
