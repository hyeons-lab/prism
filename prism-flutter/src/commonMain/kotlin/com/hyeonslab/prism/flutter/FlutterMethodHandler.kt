package com.hyeonslab.prism.flutter

/** Thrown when a Flutter MethodChannel call references an unknown method name. */
class MethodNotImplementedException(method: String) : Exception("Unknown method: $method")

/**
 * Abstract Flutter MethodChannel handler.
 *
 * Handles the built-in lifecycle methods (isInitialized, shutdown, getState) and
 * delegates domain-specific calls to [handleDomainCall].
 * Subclasses hold a typed bridge reference to access the store and dispatch events.
 */
abstract class AbstractFlutterMethodHandler(protected val bridge: PrismBridge<*, *>) {

    fun handleMethodCall(method: String, args: Map<String, Any?>): Any? = when (method) {
        "isInitialized" -> bridge.isInitialized
        "shutdown" -> { bridge.shutdown(); true }
        "getState" -> getState()
        else -> handleDomainCall(method, args)
    }

    /** Returns the current state map for "getState". Override to include domain fields. */
    protected open fun getState(): Map<String, Any?> =
        mapOf("initialized" to bridge.isInitialized)

    /**
     * Override to handle domain-specific method calls. Access the typed store via
     * the subclass's own bridge reference. Throw [MethodNotImplementedException]
     * for unsupported methods.
     */
    protected open fun handleDomainCall(method: String, args: Map<String, Any?>): Any? =
        throw MethodNotImplementedException(method)
}
