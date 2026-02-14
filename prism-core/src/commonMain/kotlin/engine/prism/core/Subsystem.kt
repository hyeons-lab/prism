package engine.prism.core

interface Subsystem {
    val name: String
    fun initialize(engine: Engine)
    fun update(time: Time)
    fun shutdown()
}
