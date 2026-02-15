package engine.prism.core

import kotlinx.cinterop.*
import platform.posix.gettimeofday
import platform.posix.timeval

actual object Platform {
    actual val name: String = "Linux"

    @OptIn(ExperimentalForeignApi::class)
    actual fun currentTimeMillis(): Long = memScoped {
        val tv = alloc<timeval>()
        gettimeofday(tv.ptr, null)
        return tv.tv_sec * 1000L + tv.tv_usec / 1000L
    }
}
