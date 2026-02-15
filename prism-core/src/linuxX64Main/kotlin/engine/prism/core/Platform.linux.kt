package engine.prism.core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.gettimeofday
import platform.posix.timeval

actual object Platform {
    actual val name: String = "Linux"

    @OptIn(ExperimentalForeignApi::class)
    actual fun currentTimeMillis(): Long = memScoped {
        val tv = alloc<timeval>()
        val result = gettimeofday(tv.ptr, null)
        check(result == 0) { "gettimeofday failed with return code $result" }
        tv.tv_sec * 1000L + tv.tv_usec / 1000L
    }
}
