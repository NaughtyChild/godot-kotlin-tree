package com.example.game

import com.example.game.gamemanager.GameManager
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.AnimatedSprite2D
import godot.api.AudioStream
import godot.api.AudioStreamPlayer2D
import godot.api.CPUParticles2D
import godot.api.CharacterBody2D
import godot.api.Input
import godot.api.ResourceLoader
import godot.core.Vector2
import godot.global.GD

@RegisterClass
class Bird : CharacterBody2D() {
    private val TAG = "Bird"

    var JUMP_VELOCITY = -300.0

    var GRAVITY = 700

    //旋转角度
    var rotDegree = 0F

    //是否死亡
    var isDead = true

    //最大下落速度
    @Export
    @RegisterProperty
    var maxSpeed = 700

    lateinit var animated_sprite_2d: AnimatedSprite2D
    lateinit var cpu_particles_2d: CPUParticles2D
    lateinit var fly_sound: AudioStreamPlayer2D
    lateinit var score_sound: AudioStreamPlayer2D

    @Export
    @RegisterProperty
    var spritePath = "AnimatedSprite2D"

    lateinit var hit: AudioStream
    lateinit var point: AudioStream
    lateinit var wing: AudioStream


    @RegisterFunction
    override fun _ready() {
        GD.print("$TAG -- _ready:")
        initNode()
        initResource()
        fly_sound.stream = wing
        GameManager.instance.gameStart.connect(this, Bird::whenGameStart)
        GameManager.instance.updateScore.connect(this, Bird::whenUpdateScore)
        GameManager.instance.gameOver.connect(this, Bird::whenGameOver)
    }

    private fun initResource() {
        GD.print("$TAG -- initResource:")
        hit = ResourceLoader.load("res://assets/hit.wav") as AudioStream
        point = ResourceLoader.load("res://assets/point.wav") as AudioStream
        wing = ResourceLoader.load("res://assets/wing.wav") as AudioStream
        score_sound.stream = point
        fly_sound.stream = wing
    }

    private fun initNode() {
        GD.print("$TAG -- initNode:")
        animated_sprite_2d = getNode(spritePath) as AnimatedSprite2D
        cpu_particles_2d = getNode("CPUParticles2D") as CPUParticles2D
        fly_sound = getNode("FlySound") as AudioStreamPlayer2D
        score_sound = getNode("ScoreSound") as AudioStreamPlayer2D
        GD.print("Bird -- initNode:score_sound name=${score_sound.name}")
    }


    @RegisterFunction
    override fun _physicsProcess(delta: Double) {
        if (isDead) {
            return
        }
        velocity= Vector2(velocity.x,velocity.y + GRAVITY * delta)
        if (Input.isActionJustPressed("fly")) {
            velocity= Vector2(velocity.x,JUMP_VELOCITY)
            fly_sound.stream = wing
            fly_sound.play()
        }
//        velocity.y = GD.clamp(velocity.y.toDouble(), -maxSpeed.toDouble(), maxSpeed.toDouble())
        rotDegree =
            GD.clamp(-30 * velocity.y.toFloat() / JUMP_VELOCITY.toFloat(), (-30).toFloat(), 30.toFloat())
        rotationDegrees= rotDegree
        val slide = moveAndSlide()
    }

    @RegisterFunction
    fun whenGameStart() {
        GD.print("$TAG -- whenGameStart:")
        isDead = false
        cpu_particles_2d.emitting = true
    }

    @RegisterFunction
    fun whenGameOver() {
        GD.print("$TAG -- whenGameOver:")
        fly_sound.stream = hit
        fly_sound.play()

        cpu_particles_2d.emitting = false
        isDead = true
        rotationDegrees=0F
        velocity= Vector2(0,0)
    }

    @RegisterFunction
    fun whenUpdateScore() {
        GD.print("$TAG -- whenUpdateScore:")
        score_sound.play()

    }
}
