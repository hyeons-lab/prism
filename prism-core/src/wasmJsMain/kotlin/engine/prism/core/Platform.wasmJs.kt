package engine.prism.core

actual object Platform {
    actual val name: String = "WasmJS"
    actual fun currentTimeMillis(): Long = js("Date.now()").toLong()
}
