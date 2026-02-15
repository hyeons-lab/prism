package engine.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import engine.prism.core.Engine
import engine.prism.core.Time

@Composable
expect fun PrismView(
  engine: Engine,
  modifier: Modifier = Modifier,
  onFrame: ((Time) -> Unit)? = null,
)
