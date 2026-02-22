/**
 * prism-sdk.mts — Object-oriented façade over the prism-js @JsExport API.
 *
 * TypeScript source. Compiled to prism-sdk.mjs + prism-sdk.d.mts by tsc via
 * `./gradlew :prism-js:generateSdkTypes`. Do not edit the generated .mjs file.
 *
 * Class names match the Kotlin library exactly so code patterns are nearly
 * identical across Kotlin, Swift, Dart (prism_sdk.dart), and JavaScript:
 *
 *   Kotlin                               JavaScript / TypeScript
 *   ─────────────────────────────────────────────────────────────
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
  constructor(public x: number = 0, public y: number = 0, public z: number = 0) {}
  static readonly zero = Object.freeze(new Vec3(0, 0, 0));
  static readonly one  = Object.freeze(new Vec3(1, 1, 1));
}

export class EngineConfig {
  readonly appName: string;
  readonly targetFps: number;
  constructor({ appName = 'Prism', targetFps = 60 }: { appName?: string; targetFps?: number } = {}) {
    this.appName = appName;
    this.targetFps = targetFps;
  }
}

export class TransformComponent {
  readonly position: Vec3;
  constructor({ position = Vec3.zero }: { position?: Vec3 } = {}) {
    this.position = new Vec3(position.x, position.y, position.z);
  }
}

// ── Engine ────────────────────────────────────────────────────────────────────

export interface EngineTime {
  readonly deltaTime: number;
  readonly totalTime: number;
}

export class Engine {
  readonly #h: string;
  readonly time: EngineTime;

  static readonly #registry = new FinalizationRegistry((h: string) => {
    prismDestroyEngine(h);
  });

  constructor(config: EngineConfig = new EngineConfig()) {
    this.#h = prismCreateEngine(config.appName, config.targetFps);
    prismEngineInitialize(this.#h);
    const h = this.#h;
    this.time = {
      get deltaTime() { return prismEngineGetDeltaTime(h); },
      get totalTime() { return prismEngineGetTotalTime(h); },
    };
    Engine.#registry.register(this, this.#h);
  }

  get isAlive(): boolean { return prismEngineIsAlive(this.#h); }
  destroy(): void {
    Engine.#registry.unregister(this);
    prismDestroyEngine(this.#h);
  }
}

// ── ECS World ─────────────────────────────────────────────────────────────────

export class World {
  readonly #h: string;
  static readonly #registry = new FinalizationRegistry((h: string) => {
    prismDestroyWorld(h);
  });

  constructor() {
    this.#h = prismCreateWorld();
    World.#registry.register(this, this.#h);
  }

  /** Creates a new entity and returns its numeric ID. */
  createEntity(): number                              { return prismWorldCreateEntity(this.#h); }
  destroyEntity(entityId: number): void               { prismWorldDestroyEntity(this.#h, entityId); }

  /** Adds a TransformComponent to the given entity. */
  addComponent(entityId: number, component: TransformComponent): void {
    const p = component.position;
    prismWorldAddTransformComponent(this.#h, entityId, p.x, p.y, p.z);
  }

  /** Returns the current transform position of the entity as a Vec3. */
  getTransform(entityId: number): Vec3 {
    return new Vec3(
      prismWorldGetTransformX(this.#h, entityId),
      prismWorldGetTransformY(this.#h, entityId),
      prismWorldGetTransformZ(this.#h, entityId),
    );
  }

  destroy(): void {
    World.#registry.unregister(this);
    prismDestroyWorld(this.#h);
  }
}

// ── Scene ─────────────────────────────────────────────────────────────────────

export class Scene {
  readonly #h: string;
  static readonly #registry = new FinalizationRegistry((h: string) => {
    prismDestroyScene(h);
  });

  constructor(name: string = 'Scene') {
    this.#h = prismCreateScene(name);
    Scene.#registry.register(this, this.#h);
  }

  addNode(node: Node): void              { prismSceneAddNode(this.#h, node._handle); }
  set activeCamera(camNode: CameraNode) {
    if (!(camNode instanceof CameraNode)) {
      throw new TypeError(
        `activeCamera must be a CameraNode (got ${(camNode as { constructor?: { name?: string } })?.constructor?.name ?? typeof camNode})`,
      );
    }
    prismSceneSetActiveCamera(this.#h, camNode._handle);
  }
  update(deltaTime: number): void        { prismSceneUpdate(this.#h, deltaTime); }
  destroy(): void {
    Scene.#registry.unregister(this);
    prismDestroyScene(this.#h);
  }
}

// ── Nodes ─────────────────────────────────────────────────────────────────────

export class Node {
  readonly #h: string;
  static readonly #registry = new FinalizationRegistry((h: string) => {
    prismDestroyNode(h);
  });

  constructor(h: string) {
    this.#h = h;
    Node.#registry.register(this, this.#h);
  }

  /**
   * @internal Library-private handle; do not use outside of prism-sdk.
   * Subclassing Node outside this module is not supported — the constructor
   * takes a raw handle string obtained from the WASM layer.
   */
  get _handle(): string                                       { return this.#h; }
  setPosition(x: number, y: number, z: number): void          { prismNodeSetPosition(this.#h, x, y, z); }
  setRotation(x: number, y: number, z: number, w: number): void { prismNodeSetRotation(this.#h, x, y, z, w); }
  setScale(x: number, y: number, z: number): void             { prismNodeSetScale(this.#h, x, y, z); }
  destroy(): void {
    Node.#registry.unregister(this);
    prismDestroyNode(this.#h);
  }
}

export class MeshNode   extends Node { constructor(name: string = 'MeshNode')   { super(prismCreateMeshNode(name));   } }
export class CameraNode extends Node { constructor(name: string = 'CameraNode') { super(prismCreateCameraNode(name)); } }
export class LightNode  extends Node { constructor(name: string = 'LightNode')  { super(prismCreateLightNode(name));  } }
export class EmptyNode  extends Node { constructor(name: string = 'EmptyNode')  { super(prismCreateNode(name));       } }

// ── Mesh Builder ──────────────────────────────────────────────────────────────

export class MeshBuilder {
  readonly #h: string;
  constructor() { this.#h = prismMeshBuilderCreate(); }

  addVertexFloat(value: number): void  { prismMeshBuilderAddVertexFloat(this.#h, value); }
  addIndex(index: number): void        { prismMeshBuilderAddIndex(this.#h, index); }
  get vertexFloatCount(): number       { return prismMeshBuilderVertexFloatCount(this.#h); }
  get indexCount(): number             { return prismMeshBuilderIndexCount(this.#h); }

  /**
   * Freezes the builder (no more adds allowed) and returns the vertex float count.
   * The handle stays valid; use getVertexAt / getIndexAt to read the data back,
   * then call destroy to release the handle.
   */
  finalize(): number                   { return prismMeshBuilderFinalize(this.#h); }
  getVertexAt(index: number): number   { return prismMeshBuilderGetVertexAt(this.#h, index); }
  getIndexAt(index: number): number    { return prismMeshBuilderGetIndexAt(this.#h, index); }
  destroy(): void                      { prismMeshBuilderDestroy(this.#h); }
}
