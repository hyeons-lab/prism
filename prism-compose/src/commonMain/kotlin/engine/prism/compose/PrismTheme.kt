package engine.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import engine.prism.core.Engine

val LocalEngine = staticCompositionLocalOf<Engine?> { null }

@Composable
fun PrismTheme(engineState: EngineState, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalEngine provides engineState.engine) { content() }
}
