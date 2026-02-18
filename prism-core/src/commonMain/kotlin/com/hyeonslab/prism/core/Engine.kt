package com.hyeonslab.prism.core

class Engine(val config: EngineConfig = EngineConfig()) {

  val gameLoop: GameLoop = GameLoop()

  var time: Time = Time(fixedDeltaTime = config.fixedTimeStep)
    private set

  @PublishedApi internal val subsystems: MutableList<Subsystem> = mutableListOf()
  private var initialized: Boolean = false

  fun initialize() {
    if (initialized) return
    initialized = true

    gameLoop.fixedTimeStep = config.fixedTimeStep

    // Initialize all registered subsystems
    for (subsystem in subsystems) {
      subsystem.initialize(this)
    }

    // Wire up the game loop callbacks
    // Iterate over a snapshot to allow safe add/remove during callbacks
    // (e.g. DemoScene.dispose() calling removeSubsystem during fold/unfold).
    gameLoop.onFixedUpdate = { frameTime ->
      time = frameTime
      for (subsystem in subsystems.toList()) {
        subsystem.update(frameTime)
      }
    }

    gameLoop.onUpdate = { frameTime -> time = frameTime }

    if (config.enableDebug) {
      println("[Engine] Initialized '${config.appName}' on ${Platform.name}")
      println("[Engine] Target FPS: ${config.targetFps}, Fixed timestep: ${config.fixedTimeStep}")
      println("[Engine] Subsystems: ${subsystems.map { it.name }}")
    }
  }

  fun shutdown() {
    if (!initialized) return

    gameLoop.stop()

    // Shutdown subsystems in reverse order
    for (subsystem in subsystems.reversed()) {
      subsystem.shutdown()
    }
    subsystems.clear()

    if (config.enableDebug) {
      println("[Engine] Shut down '${config.appName}'")
    }

    initialized = false
  }

  fun addSubsystem(subsystem: Subsystem) {
    subsystems.add(subsystem)
    if (initialized) {
      subsystem.initialize(this)
    }
  }

  fun removeSubsystem(subsystem: Subsystem) {
    if (subsystems.remove(subsystem)) {
      subsystem.shutdown()
    }
  }

  inline fun <reified T : Subsystem> getSubsystem(): T? {
    return subsystems.filterIsInstance<T>().firstOrNull()
  }
}
