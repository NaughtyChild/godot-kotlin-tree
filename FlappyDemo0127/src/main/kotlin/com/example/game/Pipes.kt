package com.example.game

import NodeRef
import PipesScene
import com.example.game.gamemanager.GameManager
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.AnimationPlayer
import godot.api.Area2D
import godot.api.Node
import godot.api.Node2D
import godot.api.VisibleOnScreenNotifier2D
import godot.core.Callable
import godot.core.Vector2
import godot.core.connect
import godot.core.toGodotName
import godot.global.GD
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@RegisterClass
class Pipes : Node2D() {
    val speed = -150
    var passed = false

    lateinit var pipeBottom: Area2D
    lateinit var pipeTop: Area2D
    lateinit var coin: Area2D
    lateinit var animationPlayer: AnimationPlayer
    lateinit var visibleOnScreenNotifier2d: VisibleOnScreenNotifier2D

    @RegisterFunction
    override fun _ready() {
        GD.print("$TAG -- _ready:")
        initNode()
        //下面几种方式都可以
//        pipe_bottom.bodyEntered.connect(Callable(this, ::onPipeBodyEntered.toGodotName()))
        pipeBottom.bodyEntered.connect(this, Pipes::onPipeBodyEntered)
        pipeTop.bodyEntered.connect(this, Pipes::onPipeBodyEntered)
        visibleOnScreenNotifier2d.screenEntered.connect(Callable(this, ::onExit.toGodotName()))
        coin.bodyEntered.connect(this, Pipes::whenCoinBodyEntered)

    }

    @RegisterFunction
    override fun _process(delta: Double) {
        globalPosition = Vector2(globalPosition.x + delta * speed.toDouble(), globalPosition.y)
    }


    private fun initNode() {
        coin = getNode("Coin") as Area2D
        pipeBottom = getNode("PipeBottom") as Area2D
        pipeTop = getNode("PipeTop") as Area2D
        animationPlayer = coin.getNode("AnimationPlayer") as AnimationPlayer
        visibleOnScreenNotifier2d = getNode("VisibleOnScreenNotifier2D") as VisibleOnScreenNotifier2D
    }


    @RegisterFunction
    fun onPipeBodyEntered(body: Node2D) {
        GD.print("$TAG -- onPipeBodyEntered:")

        if (body.isInGroup("bird").not()) {
            return
        }

        GameManager.instance.gameOver.emit()
    }

    @RegisterFunction
    fun onExit() {
        GD.print("$TAG -- onExists:")
//        queueFree()
    }

    @RegisterFunction
    fun whenCoinBodyEntered(body: Node2D) {
        GD.print("$TAG -- whenCoinBodyEntered:")
        if (body.isInGroup("bird").not()) {
            return
        }
        if (passed) {
            return
        }
        passed = true
        GameManager.instance.updateScore.emit()
        animationPlayer.play("coin")
        animationPlayer.animationStarted.connect {
            coin.queueFree()
        }
    }



    companion object {
        private const val TAG = "Pipes"
    }
}
