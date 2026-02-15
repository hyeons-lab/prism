package com.hyeonslab.prism.scene

import com.hyeonslab.prism.math.Mat4
import com.hyeonslab.prism.math.Transform

open class Node(val name: String = "Node") {
  var transform: Transform = Transform()
  var parent: Node? = null
    private set

  private val _children: MutableList<Node> = mutableListOf()
  val children: List<Node>
    get() = _children

  var isEnabled: Boolean = true

  fun addChild(child: Node) {
    child.parent?.removeChild(child)
    child.parent = this
    _children.add(child)
  }

  fun removeChild(child: Node): Boolean {
    if (_children.remove(child)) {
      child.parent = null
      return true
    }
    return false
  }

  fun worldTransformMatrix(): Mat4 {
    val local = transform.toModelMatrix()
    return parent?.worldTransformMatrix()?.let { it * local } ?: local
  }

  fun findChild(name: String): Node? {
    if (this.name == name) return this
    for (child in _children) {
      val found = child.findChild(name)
      if (found != null) return found
    }
    return null
  }

  open fun update(deltaTime: Float) {
    if (!isEnabled) return
    for (child in _children) {
      child.update(deltaTime)
    }
  }
}
