package engine.prism.core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.windows.FILETIME
import platform.windows.GetSystemTimeAsFileTime

actual object Platform {
  actual val name: String = "Windows"

  @OptIn(ExperimentalForeignApi::class)
  actual fun currentTimeMillis(): Long = memScoped {
    val fileTime = alloc<FILETIME>()
    GetSystemTimeAsFileTime(fileTime.ptr)
    val high = fileTime.dwHighDateTime.toLong() and 0xFFFFFFFFL
    val low = fileTime.dwLowDateTime.toLong() and 0xFFFFFFFFL
    val time = (high shl 32) or low
    // Convert from 100-nanosecond intervals since 1601 to milliseconds since 1970
    (time / 10000L) - 11644473600000L
  }
}
