package com.hyeonslab.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time

@Composable
expect fun PrismView(
  engine: Engine,
  modifier: Modifier = Modifier,
  onFrame: ((Time) -> Unit)? = null,
)
