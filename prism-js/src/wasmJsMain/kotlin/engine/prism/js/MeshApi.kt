@file:OptIn(ExperimentalJsExport::class)

package engine.prism.js

import kotlin.js.JsExport

/**
 * In-memory mesh builder. Actual GPU upload is done by the integration layer (e.g. prism-flutter).
 * Arrays cannot cross the WASM/JS boundary in Kotlin/WASM, so vertices and indices are added
 * one element at a time via the builder API.
 */
private class MeshBuilder {
    val vertices = mutableListOf<Float>()
    val indices = mutableListOf<Int>()
}

/** Creates a new mesh builder and returns its handle. */
@JsExport
fun prismMeshBuilderCreate(): String = Registry.put(MeshBuilder())

/** Appends one float to the vertex buffer of the builder identified by [handle]. */
@JsExport
fun prismMeshBuilderAddVertexFloat(handle: String, value: Float) {
    Registry.get<MeshBuilder>(handle)?.vertices?.add(value)
}

/** Appends one index to the index buffer of the builder identified by [handle]. */
@JsExport
fun prismMeshBuilderAddIndex(handle: String, index: Int) {
    Registry.get<MeshBuilder>(handle)?.indices?.add(index)
}

/** Returns the current vertex float count of the builder. */
@JsExport
fun prismMeshBuilderVertexFloatCount(handle: String): Int =
    Registry.get<MeshBuilder>(handle)?.vertices?.size ?: 0

/** Returns the current index count of the builder. */
@JsExport
fun prismMeshBuilderIndexCount(handle: String): Int =
    Registry.get<MeshBuilder>(handle)?.indices?.size ?: 0

/** Finalizes the builder and releases its handle. Returns the total vertex float count. */
@JsExport
fun prismMeshBuilderFinalize(handle: String): Int {
    val builder = Registry.get<MeshBuilder>(handle) ?: return 0
    val count = builder.vertices.size
    Registry.remove(handle)
    return count
}
