package engine.prism.input

import engine.prism.core.Engine
import engine.prism.core.Subsystem
import engine.prism.core.Time

class InputManager : Subsystem {

  override val name: String = "Input"

  private val pressedKeys = mutableSetOf<Key>()
  private val pressedMouseButtons = mutableSetOf<MouseButton>()

  private val _events = mutableListOf<InputEvent>()
  private val _touchEvents = mutableListOf<TouchEvent>()

  var mouseX: Float = 0f
    private set

  var mouseY: Float = 0f
    private set

  val events: List<InputEvent>
    get() = _events

  val touchEvents: List<TouchEvent>
    get() = _touchEvents

  fun isKeyDown(key: Key): Boolean = key in pressedKeys

  fun isKeyUp(key: Key): Boolean = key !in pressedKeys

  fun isMouseButtonDown(button: MouseButton): Boolean = button in pressedMouseButtons

  fun processEvent(event: InputEvent) {
    _events.add(event)
    when (event) {
      is InputEvent.KeyDown -> pressedKeys.add(event.key)
      is InputEvent.KeyUp -> pressedKeys.remove(event.key)
      is InputEvent.MouseMove -> {
        mouseX = event.x
        mouseY = event.y
      }
      is InputEvent.MouseButtonDown -> {
        pressedMouseButtons.add(event.button)
        mouseX = event.x
        mouseY = event.y
      }
      is InputEvent.MouseButtonUp -> {
        pressedMouseButtons.remove(event.button)
        mouseX = event.x
        mouseY = event.y
      }
      is InputEvent.MouseScroll -> {
        /* scroll events are available via the events list */
      }
    }
  }

  fun processTouchEvent(event: TouchEvent) {
    _touchEvents.add(event)
  }

  override fun initialize(engine: Engine) {
    pressedKeys.clear()
    pressedMouseButtons.clear()
    _events.clear()
    _touchEvents.clear()
    mouseX = 0f
    mouseY = 0f
  }

  override fun update(time: Time) {
    _events.clear()
    _touchEvents.clear()
  }

  override fun shutdown() {
    pressedKeys.clear()
    pressedMouseButtons.clear()
    _events.clear()
    _touchEvents.clear()
  }
}
