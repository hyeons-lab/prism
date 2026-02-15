package com.hyeonslab.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.hyeonslab.prism.core.Engine

val LocalEngine = staticCompositionLocalOf<Engine?> { null }

@Composable
fun PrismTheme(store: EngineStore, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalEngine provides store.engine) { content() }
}
