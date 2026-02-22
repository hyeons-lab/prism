package com.hyeonslab.prism.flutter

import com.hyeonslab.prism.core.Store

/**
 * Generic lifecycle bridge between the Prism engine and a Flutter platform channel.
 *
 * [T] — the scene/renderable type (e.g. DemoScene).
 * [S] — the MVI store type, must implement [Store] (e.g. DemoStore :
 *       Store<DemoUiState, DemoIntent>). Subclasses access [store] with full type
 *       information for state observation and typed event dispatch.
 *
 * Subclass and override [shutdown] to release scene resources.
 */
open class PrismBridge<T : Any, S : Store<*, *>>(val store: S) {
    var scene: T? = null
        protected set

    internal fun attachScene(scene: T) { this.scene = scene }
    internal fun detachScene() { scene = null }
    val isInitialized: Boolean get() = scene != null
    open fun shutdown() { scene = null }
}
