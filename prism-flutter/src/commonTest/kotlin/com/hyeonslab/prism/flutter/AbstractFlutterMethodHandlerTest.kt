package com.hyeonslab.prism.flutter

import com.hyeonslab.prism.core.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeHandlerStore : Store<Unit, Nothing> {
    override val state: StateFlow<Unit> = MutableStateFlow(Unit)
    override fun dispatch(event: Nothing) = error("no events")
}

private class FakeBridge : PrismBridge<String, FakeHandlerStore>(FakeHandlerStore())

/**
 * Concrete handler that records domain-call invocations.
 * Built-in lifecycle methods (isInitialized, shutdown, getState) are handled by the superclass.
 */
private class PassThroughHandler(bridge: PrismBridge<*, *>) : AbstractFlutterMethodHandler(bridge) {
    var domainCallCount = 0
    var lastDomainMethod: String? = null

    override fun handleDomainCall(method: String, args: Map<String, Any?>): Any? {
        domainCallCount++
        lastDomainMethod = method
        return null
    }

    override fun getState(): Map<String, Any?> = mapOf("initialized" to bridge.isInitialized)
}

class AbstractFlutterMethodHandlerTest {

    private val bridge = FakeBridge()
    private val handler = PassThroughHandler(bridge)

    @Test
    fun isInitializedReturnsFalseBeforeSceneAttach() {
        assertEquals(false, handler.handleMethodCall("isInitialized", emptyMap()))
    }

    @Test
    fun isInitializedReturnsTrueAfterSceneAttach() {
        bridge.attachScene("scene")
        assertEquals(true, handler.handleMethodCall("isInitialized", emptyMap()))
    }

    @Test
    fun shutdownCallsBridgeShutdownAndReturnsTrue() {
        bridge.attachScene("scene")
        val result = handler.handleMethodCall("shutdown", emptyMap())
        assertEquals(true, result)
        assertFalse(bridge.isInitialized)
    }

    @Test
    fun getStateReturnsMapWithInitializedKey() {
        @Suppress("UNCHECKED_CAST")
        val state = handler.handleMethodCall("getState", emptyMap()) as Map<String, Any?>
        assertTrue(state.containsKey("initialized"))
        assertEquals(false, state["initialized"])
    }

    @Test
    fun domainMethodIsForwardedToHandleDomainCall() {
        handler.handleMethodCall("togglePause", emptyMap())
        assertEquals(1, handler.domainCallCount)
        assertEquals("togglePause", handler.lastDomainMethod)
    }

    @Test
    fun unknownMethodThrowsMethodNotImplementedException() {
        // Use the base AbstractFlutterMethodHandler with no handleDomainCall override
        // so that the default implementation (throws MethodNotImplementedException) is exercised.
        val strictHandler = object : AbstractFlutterMethodHandler(bridge) {}
        assertFailsWith<MethodNotImplementedException> {
            strictHandler.handleMethodCall("doesNotExist", emptyMap())
        }
    }

    @Test
    fun shutdownThenIsInitializedReturnsFalse() {
        bridge.attachScene("scene")
        handler.handleMethodCall("shutdown", emptyMap())
        assertEquals(false, handler.handleMethodCall("isInitialized", emptyMap()))
    }
}
