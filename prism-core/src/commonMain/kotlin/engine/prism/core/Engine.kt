package engine.prism.core

class Engine(val config: EngineConfig = EngineConfig()) {

    val gameLoop: GameLoop = GameLoop()

    var time: Time = Time(fixedDeltaTime = config.fixedTimeStep)
        private set

    private val subsystems: MutableList<Subsystem> = mutableListOf()
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
        gameLoop.onFixedUpdate = { frameTime ->
            time = frameTime
            for (subsystem in subsystems) {
                subsystem.update(frameTime)
            }
        }

        gameLoop.onUpdate = { frameTime ->
            time = frameTime
        }

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

    inline fun <reified T : Subsystem> getSubsystem(): T? {
        return subsystems.filterIsInstance<T>().firstOrNull()
    }
}
