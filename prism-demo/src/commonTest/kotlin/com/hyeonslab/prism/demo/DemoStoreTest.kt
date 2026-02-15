package com.hyeonslab.prism.demo

import com.hyeonslab.prism.renderer.Color
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
    state.cubeColor shouldBe Color(0.3f, 0.5f, 0.9f)
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
  fun setCubeColorUpdatesState() {
    val store = DemoStore()
    val red = Color(0.9f, 0.2f, 0.2f)

    store.dispatch(DemoIntent.SetCubeColor(red))

    store.state.value.cubeColor shouldBe red
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
    val gold = Color(1.0f, 0.84f, 0f)

    store.dispatch(DemoIntent.SetRotationSpeed(200f))
    store.dispatch(DemoIntent.SetCubeColor(gold))
    store.dispatch(DemoIntent.TogglePause)
    store.dispatch(DemoIntent.UpdateFps(30f))

    val state = store.state.value
    state.rotationSpeed shouldBe 200f
    state.cubeColor shouldBe gold
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
    updated.cubeColor shouldBe initial.cubeColor
    updated.fps shouldBe initial.fps
  }
}
