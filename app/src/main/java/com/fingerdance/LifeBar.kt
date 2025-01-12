package com.fingerdance

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor

class LifeBar(screenHeight: Float, activity: GameScreenActivity) : Actor() {
    private val a = activity
    private val backgroundTexture =
        Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barLife0.png"))
    private val barBlackTexture =
        Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barLife1.png"))
    private val barRedTexture =
        Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barLife2.png"))
    private val barLifeTexture =
        Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barLife3.png"))

    private val barFrame = Sprite(backgroundTexture)
    private val barBlack = Sprite(barBlackTexture)
    private val barRed = Sprite(barRedTexture)
    private val barColors = Sprite(barLifeTexture)

    private val maxWidth = medidaFlechas * 5f
    private val minWidth = maxWidth / 20f
    private val originalHeight = medidaFlechas / 2f

    private val maxLife = barLifeTexture.width
    private val minLife = maxLife / 20

    private val targetPercentageTexture = (maxLife * 0.1).toInt() // 10% del tamaño total
    private val targetPercentage = (maxWidth * 0.1).toInt() // 10% del tamaño total
    private var currentLifeTexture = (maxLife * 0.45).toInt() // Comienza en 50%
    var currentLife = maxWidth * 0.45f // Comienza en 50%

    var msPerBeat: Float = (60000f / displayBPM) / 2
    private var elapsedTime: Float = 0f
    private var growing = true

    init {
        barFrame.setSize(maxWidth, originalHeight)
        barBlack.setSize(maxWidth, originalHeight)
        barRed.setSize(maxWidth, originalHeight)
        barColors.setSize(currentLife, originalHeight)

        barFrame.setPosition(medidaFlechas, screenHeight - (medidaFlechas / 2))
        barBlack.setPosition(medidaFlechas, screenHeight - (medidaFlechas / 2))
        barRed.setPosition(medidaFlechas, screenHeight - (medidaFlechas / 2))
        barColors.setPosition(medidaFlechas, screenHeight - (medidaFlechas / 2))

        barColors.setRegion(0, 0, currentLifeTexture, barLifeTexture.height)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        val lifePercentage = currentLife / maxWidth

        if (lifePercentage <= 0.1f) {
            barRed.draw(batch)
        } else {
            barBlack.draw(batch)
        }
        barColors.draw(batch)
        if (lifePercentage >= 1f) {
            val currentTime = (System.currentTimeMillis() / 100L) % 2 == 0L

            if (currentTime) {
                val time = (System.currentTimeMillis() % 200L) / 200f
                val shine = 1f + 0.5f * Math.sin(time * Math.PI).toFloat() // Oscilación del brillo
                val previousSrcFunc = batch.blendSrcFunc
                val previousDstFunc = batch.blendDstFunc
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)  // Blending aditivo

                barColors.setColor(shine, shine, shine,1f) // Aumentamos el brillo
                barColors.draw(batch) // Dibujamos la barra con el brillo
                batch.setBlendFunction(previousSrcFunc, previousDstFunc) // Restauramos la mezcla de blending
            } else {
                barColors.setColor(1f, 1f, 1f, 1f) // Sin efecto de brillo
                barColors.draw(batch)
            }
        } else {
            barColors.setColor(1f, 1f, 1f, 1f)
            barColors.draw(batch)
        }
        barFrame.draw(batch)
    }

    fun dispose() {
        backgroundTexture.dispose()
        barBlackTexture.dispose()
        barRedTexture.dispose()
        barLifeTexture.dispose()
    }

    override fun act(delta: Float) {
        super.act(delta)
        elapsedTime += delta * 1000

        // Incremento o decremento de vida
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

            // Actualizar el tamaño y región de la barra de vida
            barColors.setRegion(0, 0, currentLifeTexture, barLifeTexture.height)
            barColors.setSize(currentLife, originalHeight)

            // Alternar entre creciendo y decreciendo según el tiempo
            if (elapsedTime >= msPerBeat) {
                elapsedTime = 0f
                growing = !growing
            }
        } else {
            // Si la vida está al máximo, asegúrate de detener las oscilaciones
            growing = false // Detener el cambio de dirección
            elapsedTime = 0f // Resetear el tiempo para evitar ciclos innecesarios
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

        barColors.setRegion(0, 0, currentLifeTexture, barLifeTexture.height)
        barColors.setSize(currentLife, originalHeight)
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

        barColors.setRegion(0, 0, currentLifeTexture, barLifeTexture.height)
        barColors.setSize(currentLife, originalHeight)


        if (currentLife <= 4) {
            a.runOnUiThread {
                //a.finish()
                a.breakDance()
            }
        }

    }
}