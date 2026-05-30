package com.example.game

import com.example.game.gamemanager.GameManager
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Button
import godot.api.CanvasLayer
import godot.api.Label
import godot.api.ParallaxBackground
import godot.global.GD

@RegisterClass
class Hud : CanvasLayer() {

    lateinit var scoreLabel: Label
    lateinit var messageLabel: Label
    lateinit var startButton: Button

    @RegisterFunction
    override fun _ready() {
        initNodes()
        updateScore()
        GameManager.instance.gameOver.connect(this, Hud::whenGameOver)
        GameManager.instance.updateScore.connect(this, Hud::updateScore)
        startButton.pressed.connect(this, Hud::onStartPressed)
    }

    @RegisterFunction
    fun updateScore() {
        GD.print("$TAG -- updateScore:")
        scoreLabel.text = "Score: ${GameManager.instance.score}"

    }

    private fun initNodes() {
        scoreLabel = getNode("MarginContainer/VBoxContainer/ScoreLabel") as Label
        messageLabel = getNode("MarginContainer/VBoxContainer/Message") as Label
        startButton = getNode("MarginContainer/VBoxContainer/Button") as Button
    }

    @RegisterFunction
    fun whenGameOver() {
        updateScore()
        messageLabel.text = "Game Over"
        messageLabel.show()
        startButton.visible = true
    }


    @RegisterFunction
    fun onStartPressed() {
        GD.print("$TAG -- onPressed:")
        messageLabel.hide()
        startButton.hide()
        GameManager.instance.gameStart.emit()
    }

    companion object {
        private const val TAG = "Hud"
    }
}
