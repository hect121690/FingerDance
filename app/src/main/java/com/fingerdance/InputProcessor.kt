package com.fingerdance

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch

private val KEY_NONE = 0
private val KEY_DOWN = 1
private val KEY_PRESS = 2
private val KEY_UP = 3

class InputProcessor() : InputAdapter() {
    private val btnOffPress = Texture(Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_off.png"))
    private val btnOnPress = Texture(Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_on.png"))

    val padStates = IntArray(padPositions.size) { KEY_NONE }
    private val pointerToPadMap = mutableMapOf<Int, Int>()  // Relación entre pointer y pad

    // Otros métodos (touchDown, touchUp, touchDragged, update, render) se mantienen iguales...

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val padIndex = getPadIndex(screenX.toFloat(), screenY.toFloat())
        padIndex?.let {
            padStates[it] = KEY_DOWN
            pointerToPadMap[pointer] = it  // Asociar pointer con el pad}
        }
        return padIndex != null
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val padIndex = getPadIndex(screenX.toFloat(), screenY.toFloat())
        padIndex?.let {
            padStates[it] = KEY_UP
            pointerToPadMap.remove(pointer)
        }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val padIndex = getPadIndex(screenX.toFloat(), screenY.toFloat())

        pointerToPadMap[pointer]?.let { previousPad ->
            if (padIndex != previousPad) {
                // Si el dedo salió del pad anterior, lo devolvemos a KEY_NONE
                padStates[previousPad] = KEY_NONE
                pointerToPadMap.remove(pointer)
            }
        }

        padIndex?.let {
            padStates[it] = KEY_PRESS
            pointerToPadMap[pointer] = it  // Actualizar la asociación con el nuevo pad
        }

        return true
    }

    private fun getPadIndex(x: Float, y: Float): Int? {
        return padPositions.indexOfFirst { pad ->
            x in pad[0]..(pad[0] + widthBtns) && y in pad[1]..(pad[1] + heightBtns)
        }.takeIf { it >= 0 }
    }

    fun update() {
        for (i in padStates.indices) {
            when (padStates[i]) {
                KEY_DOWN -> padStates[i] = KEY_PRESS
                KEY_UP -> padStates[i] = KEY_NONE
            }
        }
    }

    fun render(batch: SpriteBatch) {
        for (i in padPositions.indices) {
            val (x, y) = padPositions[i]
            val texture = if (padStates[i] == KEY_DOWN || padStates[i] == KEY_PRESS) {
                btnOnPress
            } else {
                btnOffPress
            }
            batch.draw(texture, x, y, widthBtns, heightBtns)
        }
    }

    // Obtener una copia de los estados actuales de los pads


    fun dispose() {
        btnOffPress.dispose()
        btnOnPress.dispose()
    }
}
