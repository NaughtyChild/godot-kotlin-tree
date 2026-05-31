package com.example.test

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Node2D

@RegisterClass
class Main : Node2D() {
    @RegisterFunction
    override fun _ready() {
    }
}
