package com.example.game

import com.example.game.gamemanager.GameManager
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.Area2D
import godot.api.Marker2D
import godot.api.Node2D
import godot.api.PackedScene
import godot.api.ResourceLoader
import godot.api.Timer
import godot.core.Vector2
import godot.global.GD

@RegisterClass
class Main : Node2D() {
    val gap = 180

    //var pipes_scene: PackedScene = preload("res://pipes/pipes.tscn")

    /*
        @onready var timer: Timer = $Timer
        @onready var spawn_point: Marker2D = $SpawnPoint
        @onready var bird: CharacterBody2D = $Bird
        @onready var ceil: Area2D = $Ceil
        @onready var floor: Area2D = $Floor
        @onready var pipes_group: Node2D = $PipesGroup
        */
    lateinit var pipes: PackedScene
    lateinit var timer: Timer
    lateinit var spawn_point: Marker2D
//    lateinit var bird: Bird
val bird by MainBindings.Bird
    lateinit var ceil: Area2D
    lateinit var floor: Area2D
    lateinit var pipes_group: Node2D


    @RegisterFunction
    override fun _ready() {
        GD.print("$TAG -- _ready:")
        initNodes()
        GameManager.instance.gameStart.connect(this, Main::whenGameStart)
        GameManager.instance.gameOver.connect(this, Main::whenGameOver)
        ceil.bodyEntered.connect(this, Main::whenBodyEntered)
        floor.bodyEntered.connect(this, Main::whenBodyEntered)
        timer.timeout.connect(this, Main::whenTimeOut)

    }

    @RegisterFunction
    fun whenGameOver() {
        timer.stop()
        resetGame()
    }

    @RegisterFunction
    fun whenGameStart() {
        newPipes()
        timer.start()
    }


    @RegisterFunction
    fun whenBodyEntered(body: Node2D) {
        if (body is Bird && body.isDead.not()) {
            GameManager.instance.gameOver.emit()
        }
    }

    @RegisterFunction
    fun whenTimeOut() {
        newPipes()
    }


    fun newPipes() {
        GD.print("$TAG -- newPipes:")

        val pipes = pipes.instantiate() as Pipes
        val pos_y = GD.randfRange(spawn_point.position.y.toFloat() - gap, spawn_point.position.y.toFloat() + gap)
        pipes.globalPosition = Vector2(spawn_point.position.x, pos_y)
        pipes_group.addChild(pipes)
        val size = pipes_group.getChildren().size
        GD.print("$TAG -- newPipes:size=$size")
    }

    private fun initNodes() {
        pipes = ResourceLoader.load("res://scene/pipe/Pipes.tscn") as PackedScene
        timer = getNode("Timer") as Timer
        spawn_point = getNode("SpawnPoint") as Marker2D
//        bird = getNode("Bird") as Bird
        ceil = getNode("Ceil") as Area2D
        floor = getNode("Floor") as Area2D
        pipes_group = getNode("PipesGroup") as Node2D
    }

    fun resetGame() {
        bird.globalPosition = Vector2(160, 300)
        pipes_group.getChildren().forEach {
            it.queueFree()
        }
    }

    companion object {
        private const val TAG = "Main"
    }
}
