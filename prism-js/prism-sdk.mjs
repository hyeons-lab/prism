/**
 * prism-sdk.mjs — Object-oriented façade over the prism-js @JsExport API.
 *
 * Mirrors the Kotlin class hierarchy so the JavaScript API surface matches
 * the Kotlin one:
 *
 *   Kotlin                          JavaScript
 *   ──────────────────────────────────────────
 *   PrismEngine(targetFps = 60)  →  new PrismEngine('MyApp', 60)
 *   engine.deltaTime             →  engine.deltaTime
 *   World()                      →  new PrismWorld()
 *   world.createEntity()         →  world.createEntity()
 *   world.addTransform(e, …)     →  world.addTransform(e, x, y, z)
 *   world.getComponent(e)        →  world.getTransform(e)  → { x, y, z }
 *   PrismScene("main")           →  new PrismScene('main')
 *   scene.addNode(n)             →  scene.addNode(n)
 *   scene.setActiveCamera(n)     →  scene.setActiveCamera(n)
 *   PrismMeshNode("mesh")        →  new PrismMeshNode('mesh')
 *   node.setPosition(x, y, z)   →  node.setPosition(x, y, z)
 *
 * Usage:
 *   import { PrismEngine, PrismWorld, PrismScene,
 *            PrismMeshNode, PrismCameraNode } from './prism-sdk.mjs';
 */

import {
  prismCreateEngine, prismEngineInitialize, prismEngineIsAlive,
  prismEngineGetDeltaTime, prismEngineGetTotalTime, prismDestroyEngine,
  prismCreateWorld, prismWorldCreateEntity, prismWorldDestroyEntity,
  prismWorldAddTransformComponent,
  prismWorldGetTransformX, prismWorldGetTransformY, prismWorldGetTransformZ,
  prismDestroyWorld,
  prismCreateScene, prismSceneUpdate, prismDestroyScene,
  prismCreateNode, prismCreateMeshNode, prismCreateCameraNode, prismCreateLightNode,
  prismSceneAddNode, prismSceneSetActiveCamera, prismDestroyNode,
  prismNodeSetPosition, prismNodeSetRotation, prismNodeSetScale,
  prismMeshBuilderCreate, prismMeshBuilderAddVertexFloat, prismMeshBuilderAddIndex,
  prismMeshBuilderVertexFloatCount, prismMeshBuilderIndexCount,
  prismMeshBuilderFinalize,
} from './prism.mjs';

// ── Engine ────────────────────────────────────────────────────────────────────

export class PrismEngine {
  #h;
  /** @param {string} appName @param {number} targetFps */
  constructor(appName = 'Prism', targetFps = 60) {
    this.#h = prismCreateEngine(appName, targetFps);
    prismEngineInitialize(this.#h);
  }
  get isAlive()   { return prismEngineIsAlive(this.#h); }
  get deltaTime() { return prismEngineGetDeltaTime(this.#h); }
  get totalTime() { return prismEngineGetTotalTime(this.#h); }
  destroy()       { prismDestroyEngine(this.#h); }
}

// ── ECS World ─────────────────────────────────────────────────────────────────

export class PrismWorld {
  #h;
  constructor() { this.#h = prismCreateWorld(); }

  /** Create a new entity and return its numeric ID. */
  createEntity()                    { return prismWorldCreateEntity(this.#h); }
  destroyEntity(entityId)           { prismWorldDestroyEntity(this.#h, entityId); }

  /** Add a TransformComponent to [entityId] at position (x, y, z). */
  addTransform(entityId, x, y, z)   { prismWorldAddTransformComponent(this.#h, entityId, x, y, z); }

  /** Returns the current transform position as { x, y, z }. */
  getTransform(entityId) {
    return {
      x: prismWorldGetTransformX(this.#h, entityId),
      y: prismWorldGetTransformY(this.#h, entityId),
      z: prismWorldGetTransformZ(this.#h, entityId),
    };
  }

  destroy() { prismDestroyWorld(this.#h); }
}

// ── Scene ─────────────────────────────────────────────────────────────────────

export class PrismScene {
  #h;
  /** @param {string} name */
  constructor(name)          { this.#h = prismCreateScene(name); }
  addNode(node)              { prismSceneAddNode(this.#h, node._handle); }
  setActiveCamera(camNode)   { prismSceneSetActiveCamera(this.#h, camNode._handle); }
  update(deltaTime)          { prismSceneUpdate(this.#h, deltaTime); }
  destroy()                  { prismDestroyScene(this.#h); }
}

// ── Nodes ─────────────────────────────────────────────────────────────────────

class _PrismNode {
  #h;
  constructor(h)              { this.#h = h; }
  /** @internal */ get _handle()          { return this.#h; }
  setPosition(x, y, z)       { prismNodeSetPosition(this.#h, x, y, z); }
  setRotation(x, y, z, w)    { prismNodeSetRotation(this.#h, x, y, z, w); }
  setScale(x, y, z)          { prismNodeSetScale(this.#h, x, y, z); }
  destroy()                   { prismDestroyNode(this.#h); }
}

export class PrismMeshNode   extends _PrismNode { constructor(name) { super(prismCreateMeshNode(name));   } }
export class PrismCameraNode extends _PrismNode { constructor(name) { super(prismCreateCameraNode(name)); } }
export class PrismLightNode  extends _PrismNode { constructor(name) { super(prismCreateLightNode(name));  } }
export class PrismEmptyNode  extends _PrismNode { constructor(name) { super(prismCreateNode(name));       } }

// ── Mesh Builder ──────────────────────────────────────────────────────────────

export class PrismMeshBuilder {
  #h;
  constructor()          { this.#h = prismMeshBuilderCreate(); }
  addVertexFloat(value)  { prismMeshBuilderAddVertexFloat(this.#h, value); }
  addIndex(index)        { prismMeshBuilderAddIndex(this.#h, index); }
  get vertexFloatCount() { return prismMeshBuilderVertexFloatCount(this.#h); }
  get indexCount()       { return prismMeshBuilderIndexCount(this.#h); }
  /** Finalizes the builder and returns the mesh ID. Invalidates the builder. */
  finalize()             { return prismMeshBuilderFinalize(this.#h); }
}
