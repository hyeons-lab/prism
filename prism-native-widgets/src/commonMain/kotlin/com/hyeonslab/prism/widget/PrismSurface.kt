package com.hyeonslab.prism.widget

import com.hyeonslab.prism.core.Engine

expect class PrismSurface {
  fun attach(engine: Engine)

  fun detach()

  fun resize(width: Int, height: Int)

  val width: Int
  val height: Int
}
