package com.hyeonslab.prism.scene

class Scene(val name: String = "Scene") {
  val root: Node = Node("Root")
  var activeCamera: CameraNode? = null

  fun addNode(node: Node) {
    root.addChild(node)
  }

  fun removeNode(node: Node): Boolean {
    return root.removeChild(node)
  }

  fun findNode(name: String): Node? {
    return root.findChild(name)
  }

  fun update(deltaTime: Float) {
    root.update(deltaTime)
  }

  fun traverse(visitor: (Node) -> Unit) {
    traverseNode(root, visitor)
  }

  private fun traverseNode(node: Node, visitor: (Node) -> Unit) {
    visitor(node)
    for (child in node.children) {
      traverseNode(child, visitor)
    }
  }
}
