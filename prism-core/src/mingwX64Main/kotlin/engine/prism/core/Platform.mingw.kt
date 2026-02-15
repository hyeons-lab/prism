package engine.prism.core

import kotlinx.cinterop.*
import platform.windows.GetSystemTimeAsFileTime
import platform.windows.FILETIME

actual object Platform {
    actual val name: String = "Windows"

    @OptIn(ExperimentalForeignApi::class)
    actual fun currentTimeMillis(): Long = memScoped {
        val fileTime = alloc<FILETIME>()
        GetSystemTimeAsFileTime(fileTime.ptr)
        val time = (fileTime.dwHighDateTime.toLong() shl 32) or fileTime.dwLowDateTime.toLong()
        // Convert from 100-nanosecond intervals since 1601 to milliseconds since 1970
        (time / 10000L) - 11644473600000L
    }
}
