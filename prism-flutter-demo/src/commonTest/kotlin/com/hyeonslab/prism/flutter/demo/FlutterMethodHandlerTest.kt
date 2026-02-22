package com.hyeonslab.prism.flutter.demo

import com.hyeonslab.prism.flutter.MethodNotImplementedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlutterMethodHandlerTest {

  private val bridge = DemoBridge()
  private val handler = FlutterMethodHandler(bridge)

  // --- lifecycle methods routed by AbstractFlutterMethodHandler ---

  @Test
  fun isInitializedReturnsFalseWhenNoSceneAttached() {
    val result = handler.handleMethodCall("isInitialized", emptyMap())
    assertEquals(false, result)
  }

  @Test
  fun getStateReturnsMapWithExpectedKeys() {
    @Suppress("UNCHECKED_CAST")
    val state = handler.handleMethodCall("getState", emptyMap()) as Map<String, Any?>
    assertTrue(state.containsKey("rotationSpeed"))
    assertTrue(state.containsKey("isPaused"))
    assertTrue(state.containsKey("metallic"))
    assertTrue(state.containsKey("roughness"))
    assertTrue(state.containsKey("envIntensity"))
    assertTrue(state.containsKey("fps"))
  }

  @Test
  fun getStateReflectsInitialStoreValues() {
    @Suppress("UNCHECKED_CAST")
    val state = handler.handleMethodCall("getState", emptyMap()) as Map<String, Any?>
    assertEquals(45.0, state["rotationSpeed"])
    assertEquals(false, state["isPaused"])
    assertEquals(0.0, state["fps"])
  }

  // --- domain calls ---

  @Test
  fun togglePauseFlipsPauseState() {
    assertFalse(bridge.store.state.value.isPaused)
    handler.handleMethodCall("togglePause", emptyMap())
    assertTrue(bridge.store.state.value.isPaused)
    handler.handleMethodCall("togglePause", emptyMap())
    assertFalse(bridge.store.state.value.isPaused)
  }

  @Test
  fun setRotationSpeedUpdatesStore() {
    handler.handleMethodCall("setRotationSpeed", mapOf("speed" to 90.0))
    assertEquals(90f, bridge.store.state.value.rotationSpeed)
  }

  @Test
  fun setRotationSpeedThrowsWhenArgMissing() {
    assertFailsWith<IllegalArgumentException> {
      handler.handleMethodCall("setRotationSpeed", emptyMap())
    }
  }

  @Test
  fun setMetallicUpdatesStore() {
    handler.handleMethodCall("setMetallic", mapOf("metallic" to 0.8))
    val metallic = bridge.store.state.value.metallic
    assertTrue(metallic > 0.79f && metallic < 0.81f)
  }

  @Test
  fun setRoughnessUpdatesStore() {
    handler.handleMethodCall("setRoughness", mapOf("roughness" to 0.3))
    val roughness = bridge.store.state.value.roughness
    assertTrue(roughness > 0.29f && roughness < 0.31f)
  }

  @Test
  fun setEnvIntensityUpdatesStore() {
    handler.handleMethodCall("setEnvIntensity", mapOf("intensity" to 2.0))
    val intensity = bridge.store.state.value.envIntensity
    assertTrue(intensity > 1.99f && intensity < 2.01f)
  }

  @Test
  fun unknownMethodThrowsMethodNotImplemented() {
    assertFailsWith<MethodNotImplementedException> {
      handler.handleMethodCall("doesNotExist", emptyMap())
    }
  }

  @Test
  fun shutdownCallSetsSceneNull() {
    handler.handleMethodCall("shutdown", emptyMap())
    assertFalse(bridge.isInitialized)
  }
}
