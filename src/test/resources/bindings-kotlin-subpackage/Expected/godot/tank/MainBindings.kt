package godot.tank

import godot.tank.enemy.Enemy
import godot.api.*

object MainBindings {
    val Timer = ChildRef<Timer>("Timer")
    val Enemy = ChildRef<Enemy>("Enemy")
}
