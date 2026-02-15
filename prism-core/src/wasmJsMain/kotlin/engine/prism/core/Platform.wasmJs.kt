package engine.prism.core

import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
private fun dateNow(): Double = js("Date.now()")

actual object Platform {
    actual val name: String = "WasmJS"
    actual fun currentTimeMillis(): Long = dateNow().toLong()
}
