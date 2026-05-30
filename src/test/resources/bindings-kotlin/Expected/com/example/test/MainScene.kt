package com.example.test

import godot.api.PackedScene
import godot.api.ResourceLoader

object MainScene {
    const val PATH = "res://scenes/Main.tscn"

    fun instantiate(): Main =
        (ResourceLoader.load(PATH) as PackedScene).instantiate() as Main
}
