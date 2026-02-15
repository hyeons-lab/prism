package com.hyeonslab.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time

private val log = Logger.withTag("PrismView.WASM")

@Composable
actual fun PrismView(
  engine: Engine,
  modifier: Modifier,
  onFrame: ((Time) -> Unit)?,
  onResize: ((width: Int, height: Int) -> Unit)?,
) {
  log.w { "PrismView WASM stub — HTML Canvas/WebGPU integration not yet implemented" }
  DisposableEffect(engine) { onDispose {} }
}
