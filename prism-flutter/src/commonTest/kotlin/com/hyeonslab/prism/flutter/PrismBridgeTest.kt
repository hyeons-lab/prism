package com.hyeonslab.prism.flutter

import com.hyeonslab.prism.core.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeStore : Store<Unit, Nothing> {
    override val state: StateFlow<Unit> = MutableStateFlow(Unit)
    override fun dispatch(event: Nothing) = error("no events")
}

class PrismBridgeTest {

    private val bridge = PrismBridge<String, FakeStore>(FakeStore())

    @Test
    fun isInitializedFalseBeforeAttach() {
        assertFalse(bridge.isInitialized())
    }

    @Test
    fun isInitializedTrueAfterAttach() {
        bridge.attachScene("hello")
        assertTrue(bridge.isInitialized())
    }

    @Test
    fun detachSceneSetsSceneNull() {
        bridge.attachScene("hello")
        bridge.detachScene()
        assertFalse(bridge.isInitialized())
        assertNull(bridge.scene)
    }

    @Test
    fun shutdownSetsSceneNull() {
        bridge.attachScene("world")
        bridge.shutdown()
        assertNull(bridge.scene)
        assertFalse(bridge.isInitialized())
    }

    @Test
    fun doubleShutdownIsSafe() {
        bridge.attachScene("world")
        bridge.shutdown()
        bridge.shutdown()
        assertFalse(bridge.isInitialized())
    }

    @Test
    fun shutdownOnUninitializedBridgeIsSafe() {
        bridge.shutdown()
        assertFalse(bridge.isInitialized())
    }
}
