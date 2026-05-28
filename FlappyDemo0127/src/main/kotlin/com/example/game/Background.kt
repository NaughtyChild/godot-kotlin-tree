package com.example.game

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.ParallaxBackground
import godot.core.Vector2

@RegisterClass
class Background : ParallaxBackground() {
    val speed = -150

    @RegisterFunction
    override fun _process(delta: Double) {
        scrollOffset = Vector2( scrollOffset.x+speed * delta.toDouble(),scrollOffset.y)
    }
}
