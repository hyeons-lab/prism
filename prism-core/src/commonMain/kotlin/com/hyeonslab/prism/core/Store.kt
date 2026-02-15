package com.hyeonslab.prism.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Minimal MVI store contract. Holds immutable [State] in a [StateFlow] and processes [Event]s
 * through a reducer.
 *
 * @param State Immutable state snapshot type (typically a data class).
 * @param Event Sealed event/intent type that drives state transitions.
 */
interface Store<State, Event> {
  val state: StateFlow<State>

  fun dispatch(event: Event)
}
