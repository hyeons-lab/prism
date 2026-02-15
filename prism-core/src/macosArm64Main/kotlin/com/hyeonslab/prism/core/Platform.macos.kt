package com.hyeonslab.prism.core

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual object Platform {
  actual val name: String = "macOS"

  actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}
