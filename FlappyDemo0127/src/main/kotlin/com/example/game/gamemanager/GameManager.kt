package com.example.game.gamemanager

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.api.Node
import godot.core.signal0
import godot.core.signal1
import godot.global.GD

@RegisterClass
class GameManager : Node() {

    /**

    # 定义三种全局信号
    signal GameStart    # 游戏开始
    signal GameOver     # 游戏结束
    signal UpdateScore  # 分数变动

     */
    @RegisterSignal
    val gameStart by signal0()

    @RegisterSignal
    val gameOver by signal0()

    @RegisterSignal
    val updateScore by signal0()

    var score = 0

    @RegisterFunction
    override fun _ready() {
        instance = this
        gameOver.connect(this, GameManager::whenGameOver)
        updateScore.connect(this, GameManager::whenUpdateScore)
    }

    @RegisterFunction
    fun whenGameOver() {
        GD.print("$TAG -- whenGameOver:")
    }

    @RegisterFunction
    fun whenUpdateScore() {
        GD.print("$TAG -- whenUpdateScore:")
        score += 1
    }

    companion object {
        private const val TAG = "GameManager"
        lateinit var instance: GameManager
    }
}
