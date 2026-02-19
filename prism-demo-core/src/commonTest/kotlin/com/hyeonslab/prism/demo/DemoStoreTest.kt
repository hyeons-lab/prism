package com.hyeonslab.prism.demo

import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class DemoStoreTest {

  @Test
  fun initialStateHasDefaults() {
    val store = DemoStore()
    val state = store.state.value

    state.rotationSpeed shouldBe 45f
    state.isPaused shouldBe false
    state.metallic shouldBe (0.5f plusOrMinus 0.01f)
    state.roughness shouldBe (0.5f plusOrMinus 0.01f)
    state.envIntensity shouldBe (1.0f plusOrMinus 0.01f)
    state.fps shouldBe 0f
  }

  @Test
  fun setRotationSpeedUpdatesState() {
    val store = DemoStore()

    store.dispatch(DemoIntent.SetRotationSpeed(120f))

    store.state.value.rotationSpeed shouldBe 120f
  }

  @Test
  fun togglePauseFlipsPausedState() {
    val store = DemoStore()

    store.dispatch(DemoIntent.TogglePause)
    store.state.value.isPaused shouldBe true

    store.dispatch(DemoIntent.TogglePause)
    store.state.value.isPaused shouldBe false
  }

  @Test
  fun setMetallicUpdatesState() {
    val store = DemoStore()

    store.dispatch(DemoIntent.SetMetallic(0.8f))

    store.state.value.metallic shouldBe (0.8f plusOrMinus 0.01f)
  }

  @Test
  fun setRoughnessUpdatesState() {
    val store = DemoStore()

    store.dispatch(DemoIntent.SetRoughness(0.3f))

    store.state.value.roughness shouldBe (0.3f plusOrMinus 0.01f)
  }

  @Test
  fun setEnvIntensityUpdatesState() {
    val store = DemoStore()

    store.dispatch(DemoIntent.SetEnvIntensity(1.5f))

    store.state.value.envIntensity shouldBe (1.5f plusOrMinus 0.01f)
  }

  @Test
  fun updateFpsUpdatesState() {
    val store = DemoStore()

    store.dispatch(DemoIntent.UpdateFps(59.5f))

    store.state.value.fps shouldBe (59.5f plusOrMinus 0.01f)
  }

  @Test
  fun multipleIntentsApplySequentially() {
    val store = DemoStore()

    store.dispatch(DemoIntent.SetRotationSpeed(200f))
    store.dispatch(DemoIntent.SetMetallic(0.8f))
    store.dispatch(DemoIntent.SetRoughness(0.3f))
    store.dispatch(DemoIntent.TogglePause)
    store.dispatch(DemoIntent.UpdateFps(30f))

    val state = store.state.value
    state.rotationSpeed shouldBe 200f
    state.metallic shouldBe (0.8f plusOrMinus 0.01f)
    state.roughness shouldBe (0.3f plusOrMinus 0.01f)
    state.isPaused shouldBe true
    state.fps shouldBe (30f plusOrMinus 0.01f)
  }

  @Test
  fun stateFlowEmitsLatestValue() = runTest {
    val store = DemoStore()

    store.dispatch(DemoIntent.SetRotationSpeed(90f))

    val emitted = store.state.first()
    emitted.rotationSpeed shouldBe 90f
  }

  @Test
  fun intentsDoNotAffectUnrelatedFields() {
    val store = DemoStore()
    val initial = store.state.value

    store.dispatch(DemoIntent.SetRotationSpeed(180f))

    val updated = store.state.value
    updated.isPaused shouldBe initial.isPaused
    updated.metallic shouldBe initial.metallic
    updated.roughness shouldBe initial.roughness
    updated.fps shouldBe initial.fps
  }
}
