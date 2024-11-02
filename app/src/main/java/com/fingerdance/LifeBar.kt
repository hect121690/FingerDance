package com.fingerdance

import android.app.Activity
import android.content.Intent
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor

class LifeBar(screenHeight: Float, activity: GameScreenActivity) : Actor() {
    private val a = activity
    private val backgroundTexture = Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barLife1.png"))
    private val barLifeTexture = Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barLife.png"))

    private val backgroundSprite = Sprite(backgroundTexture)
    private val barLifeSprite = Sprite(barLifeTexture)

    private val maxWidth = medidaFlechas * 5f
    private val minWidth = maxWidth / 20f
    private val originalHeight = medidaFlechas / 2f

    private val maxLife = barLifeTexture.width
    private val minLife = maxLife / 20

    private val targetPercentageTexture = (maxLife * 0.1).toInt() // 10% del tamaño total
    private val targetPercentage = (maxWidth * 0.1).toInt() // 10% del tamaño total
    private var currentLifeTexture = (maxLife * 0.5).toInt() // Comienza en 50%
    var currentLife = maxWidth * 0.5f // Comienza en 50%

    var msPerBeat: Float = (60000f / displayBPM)
    private var elapsedTime: Float = 0f
    private var growing = true
    private val shineDuration: Float = 500f
    init {
        backgroundSprite.setSize(maxWidth, originalHeight)
        barLifeSprite.setSize(currentLife, originalHeight)

        backgroundSprite.setPosition(medidaFlechas, screenHeight - (medidaFlechas / 2))
        barLifeSprite.setPosition(medidaFlechas, screenHeight - (medidaFlechas / 2))

        barLifeSprite.setRegion(0, 0, currentLifeTexture, barLifeTexture.height)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        backgroundSprite.draw(batch)
        barLifeSprite.draw(batch)
    }

    override fun act(delta: Float) {
        super.act(delta)
        elapsedTime += delta * 1000

        if (currentLifeTexture < maxLife) {
            if (growing) {
                val incrementAmount = ((targetPercentageTexture / msPerBeat) * delta * 1000).toInt()
                currentLifeTexture = (currentLifeTexture + incrementAmount).coerceAtMost(maxLife)
                currentLife = (currentLife + (targetPercentage / msPerBeat) * delta * 1000).coerceAtMost(maxWidth)
            } else {
                val decrementAmount = ((targetPercentageTexture / msPerBeat) * delta * 1000).toInt()
                currentLifeTexture = (currentLifeTexture - decrementAmount).coerceAtLeast(0)
                currentLife = (currentLife - (targetPercentage / msPerBeat) * delta * 1000).coerceAtLeast(0f)
            }

            barLifeSprite.setRegion(0, 0, currentLifeTexture, barLifeTexture.height)
            barLifeSprite.setSize(currentLife, originalHeight)
            barLifeSprite.setColor(1f, 1f, 1f, 1f)

            if (elapsedTime >= msPerBeat) {
                elapsedTime = 0f
                growing = !growing
            }
        } else {
            val timeSinceShine = elapsedTime % shineDuration
            val shineIntensity = Math.sin((timeSinceShine / shineDuration) * Math.PI * 2).toFloat()
            barLifeSprite.setColor(1f + shineIntensity * 0.5f, 1f + shineIntensity * 0.5f, 1f + shineIntensity * 0.5f, 1.5f)
        }
    }


    fun increaseLife(percentage: Float) {
        val increaseAmountTexture = (maxLife * (percentage / 100)).toInt()
        val increaseAmount = maxWidth * (percentage / 100)

        currentLifeTexture += increaseAmountTexture
        currentLife += increaseAmount

        if (currentLifeTexture >= maxLife) {
            currentLifeTexture = maxLife
            currentLife = maxWidth
            elapsedTime = 0f
        }

        barLifeSprite.setRegion(0, 0, currentLifeTexture, barLifeTexture.height)
        barLifeSprite.setSize(currentLife, originalHeight)
    }

    fun decreaseLife(percentage: Float) {
        val decreaseAmountTexture = (maxLife * (percentage / 100)).toInt()
        val decreaseAmount = maxWidth * (percentage / 100)

        currentLifeTexture -= decreaseAmountTexture
        currentLife -= decreaseAmount

        if (currentLifeTexture < 0) {
            currentLifeTexture = minLife
            currentLife = minWidth
        }

        barLifeSprite.setRegion(0, 0, currentLifeTexture, barLifeTexture.height)
        barLifeSprite.setSize(currentLife, originalHeight)

        if (currentLife <= 3) {
            a.runOnUiThread {
                a.finish()
                a.breakDance()
            }
        }

    }
}
