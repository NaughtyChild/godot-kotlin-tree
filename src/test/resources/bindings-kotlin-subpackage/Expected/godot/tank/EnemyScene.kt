package godot.tank

import godot.tank.enemy.Enemy
import godot.api.PackedScene
import godot.api.ResourceLoader

object EnemyScene {
    const val PATH = "res://scenes/enemy/Enemy.tscn"

    fun instantiate(): Enemy =
        (ResourceLoader.load(PATH) as PackedScene).instantiate() as Enemy
}
