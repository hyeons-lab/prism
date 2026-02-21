/**
 * prism-sdk.mjs — Object-oriented façade over the prism-js @JsExport API.
 *
 * Class names match the Kotlin library exactly so code patterns are nearly
 * identical across Kotlin, Swift, Dart (prism_sdk.dart), and JavaScript:
 *
 *   Kotlin                               JavaScript (this file)
 *   ──────────────────────────────────────────────────────────────
 *   Engine(EngineConfig(targetFps=60)) → new Engine({ targetFps: 60 })
 *   engine.time.deltaTime              → engine.time.deltaTime
 *   World()                            → new World()
 *   world.createEntity()               → world.createEntity()
 *   world.addComponent(e, Transform…)  → world.addComponent(e, new TransformComponent(…))
 *   Scene("main")                      → new Scene('main')
 *   scene.addNode(n)                   → scene.addNode(n)
 *   scene.activeCamera = cam           → scene.activeCamera = cam
 *   MeshNode("mesh")                   → new MeshNode('mesh')
 *
 * Usage:
 *   import { Engine, World, Scene, MeshNode, CameraNode,
 *            TransformComponent, Vec3 } from './prism-sdk.mjs';
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
  prismMeshBuilderGetVertexAt, prismMeshBuilderGetIndexAt,
  prismMeshBuilderDestroy,
} from './prism.mjs';

// ── Value types ───────────────────────────────────────────────────────────────

export class Vec3 {
  /** @param {number} x @param {number} y @param {number} z */
  constructor(x = 0, y = 0, z = 0) { this.x = x; this.y = y; this.z = z; }
  static get zero() { return new Vec3(0, 0, 0); }
  static get one()  { return new Vec3(1, 1, 1); }
}

export class EngineConfig {
  /** @param {{ appName?: string, targetFps?: number }} opts */
  constructor({ appName = 'Prism', targetFps = 60 } = {}) {
    this.appName = appName;
    this.targetFps = targetFps;
  }
}

export class TransformComponent {
  /** @param {{ position?: Vec3 }} opts */
  constructor({ position = Vec3.zero } = {}) {
    this.position = position;
  }
}

// ── Engine ────────────────────────────────────────────────────────────────────

export class Engine {
  #h; #time;
  /** @param {EngineConfig} [config] */
  constructor(config = new EngineConfig()) {
    this.#h = prismCreateEngine(config.appName, config.targetFps);
    prismEngineInitialize(this.#h);
    const h = this.#h;
    this.#time = {
      get deltaTime() { return prismEngineGetDeltaTime(h); },
      get totalTime() { return prismEngineGetTotalTime(h); },
    };
  }
  get time()    { return this.#time; }
  get isAlive() { return prismEngineIsAlive(this.#h); }
  destroy()     { prismDestroyEngine(this.#h); }
}

// ── ECS World ─────────────────────────────────────────────────────────────────

export class World {
  #h;
  constructor() { this.#h = prismCreateWorld(); }

  /** Create a new entity and return its numeric ID. */
  createEntity()              { return prismWorldCreateEntity(this.#h); }
  destroyEntity(entityId)     { prismWorldDestroyEntity(this.#h, entityId); }

  /**
   * Add a component to an entity.
   * Supported: TransformComponent (sets position).
   * @param {number} entityId
   * @param {TransformComponent} component
   */
  addComponent(entityId, component) {
    if (component instanceof TransformComponent) {
      const p = component.position;
      prismWorldAddTransformComponent(this.#h, entityId, p.x, p.y, p.z);
    }
  }

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

export class Scene {
  #h;
  /** @param {string} [name] */
  constructor(name = 'Scene') { this.#h = prismCreateScene(name); }

  addNode(node)              { prismSceneAddNode(this.#h, node._handle); }
  set activeCamera(camNode)  { prismSceneSetActiveCamera(this.#h, camNode._handle); }
  update(deltaTime)          { prismSceneUpdate(this.#h, deltaTime); }
  destroy()                  { prismDestroyScene(this.#h); }
}

// ── Nodes ─────────────────────────────────────────────────────────────────────

export class Node {
  #h;
  constructor(h)              { this.#h = h; }
  /** @internal */ get _handle()         { return this.#h; }
  setPosition(x, y, z)       { prismNodeSetPosition(this.#h, x, y, z); }
  setRotation(x, y, z, w)    { prismNodeSetRotation(this.#h, x, y, z, w); }
  setScale(x, y, z)          { prismNodeSetScale(this.#h, x, y, z); }
  destroy()                   { prismDestroyNode(this.#h); }
}

export class MeshNode   extends Node { constructor(name = 'MeshNode')   { super(prismCreateMeshNode(name));   } }
export class CameraNode extends Node { constructor(name = 'CameraNode') { super(prismCreateCameraNode(name)); } }
export class LightNode  extends Node { constructor(name = 'LightNode')  { super(prismCreateLightNode(name));  } }
export class EmptyNode  extends Node { constructor(name = 'EmptyNode')  { super(prismCreateNode(name));       } }

// ── Mesh Builder ──────────────────────────────────────────────────────────────

export class MeshBuilder {
  #h;
  constructor()               { this.#h = prismMeshBuilderCreate(); }
  addVertexFloat(value)       { prismMeshBuilderAddVertexFloat(this.#h, value); }
  addIndex(index)             { prismMeshBuilderAddIndex(this.#h, index); }
  get vertexFloatCount()      { return prismMeshBuilderVertexFloatCount(this.#h); }
  get indexCount()            { return prismMeshBuilderIndexCount(this.#h); }
  /**
   * Freezes the builder (no more adds allowed) and returns the vertex float count.
   * The handle stays valid; use [getVertexAt] / [getIndexAt] to read the data back,
   * then call [destroy] to release the handle.
   */
  finalize()                  { return prismMeshBuilderFinalize(this.#h); }
  getVertexAt(index)          { return prismMeshBuilderGetVertexAt(this.#h, index); }
  getIndexAt(index)           { return prismMeshBuilderGetIndexAt(this.#h, index); }
  destroy()                   { prismMeshBuilderDestroy(this.#h); }
}
