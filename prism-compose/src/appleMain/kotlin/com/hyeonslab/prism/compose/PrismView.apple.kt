package com.hyeonslab.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger

private val log = Logger.withTag("PrismView.Apple")

@Composable
actual fun PrismView(store: EngineStore, modifier: Modifier) {
  LaunchedEffect(Unit) {
    log.w { "PrismView Apple stub \u2014 native Metal integration not yet implemented" }
  }
  DisposableEffect(store) { onDispose {} }
}
