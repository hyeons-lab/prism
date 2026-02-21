@file:OptIn(ExperimentalJsExport::class)

package engine.prism.js

import kotlin.js.JsExport

/**
 * In-memory mesh builder. Actual GPU upload is done by the integration layer (e.g. prism-flutter).
 * Arrays cannot cross the WASM/JS boundary in Kotlin/WASM, so vertices and indices are added
 * one element at a time via the builder API.
 *
 * Workflow: create → add floats/indices → finalize (freezes for read-back) →
 *   read back with [prismMeshBuilderGetVertexAt] / [prismMeshBuilderGetIndexAt] →
 *   [prismMeshBuilderDestroy] when done.
 */
private class MeshBuilder {
    val vertices = mutableListOf<Float>()
    val indices = mutableListOf<Int>()
    var finalized = false
}

/** Creates a new mesh builder and returns its handle. */
@JsExport
fun prismMeshBuilderCreate(): String = Registry.put(MeshBuilder())

/** Appends one float to the vertex buffer of the builder identified by [handle]. */
@JsExport
fun prismMeshBuilderAddVertexFloat(handle: String, value: Float) {
    Registry.get<MeshBuilder>(handle)?.takeIf { !it.finalized }?.vertices?.add(value)
}

/** Appends one index to the index buffer of the builder identified by [handle]. */
@JsExport
fun prismMeshBuilderAddIndex(handle: String, index: Int) {
    Registry.get<MeshBuilder>(handle)?.takeIf { !it.finalized }?.indices?.add(index)
}

/** Returns the current vertex float count of the builder. */
@JsExport
fun prismMeshBuilderVertexFloatCount(handle: String): Int =
    Registry.get<MeshBuilder>(handle)?.vertices?.size ?: 0

/** Returns the current index count of the builder. */
@JsExport
fun prismMeshBuilderIndexCount(handle: String): Int =
    Registry.get<MeshBuilder>(handle)?.indices?.size ?: 0

/**
 * Freezes the builder so no more vertices or indices can be added.
 * The handle remains valid; use [prismMeshBuilderGetVertexAt] and
 * [prismMeshBuilderGetIndexAt] to read the data back one element at a time,
 * then call [prismMeshBuilderDestroy] to release the handle.
 * Returns the total vertex float count, or 0 if the handle is invalid.
 */
@JsExport
fun prismMeshBuilderFinalize(handle: String): Int {
    val builder = Registry.get<MeshBuilder>(handle) ?: return 0
    builder.finalized = true
    return builder.vertices.size
}

/** Returns the vertex float at [index], or 0 if out of range or handle is invalid. */
@JsExport
fun prismMeshBuilderGetVertexAt(handle: String, index: Int): Float =
    Registry.get<MeshBuilder>(handle)?.vertices?.getOrNull(index) ?: 0f

/** Returns the index value at [index], or 0 if out of range or handle is invalid. */
@JsExport
fun prismMeshBuilderGetIndexAt(handle: String, index: Int): Int =
    Registry.get<MeshBuilder>(handle)?.indices?.getOrNull(index) ?: 0

/** Releases the builder handle. Must be called to avoid a memory leak. */
@JsExport
fun prismMeshBuilderDestroy(handle: String) {
    Registry.remove(handle)
}
