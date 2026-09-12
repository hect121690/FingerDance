package com.fingerdance.ssc

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.fingerdance.*

private const val KEY_NONE = 0
private const val KEY_DOWN = 1
private const val KEY_PRESS = 2
private const val KEY_UP = 3

class InputProcessorSscHorizontal : InputAdapter() {

    private val btnOffPress = Texture(Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_off.png"))
    private val btnOnPress = Texture(Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_on.png"))

    val getKeyBoard = IntArray(padPositionsHorizontal.size) { KEY_NONE }
    val logicalState = IntArray(5) { KEY_NONE }

    /*
     * Qué pointer pertenece actualmente a qué pad.
     * Un pointer sólo puede estar asociado a UN pad físico.
     */
    private val pointerToPadMap = mutableMapOf<Int, Int>()

    /*
     * Fuentes que mantienen cada pad pulsado.
     *
     * Touch:
     *   0, 1, 2...
     *
     * Teclado:
     *   valores negativos.
     */
    private val padPointers = Array(padPositionsHorizontal.size) { mutableSetOf<Int>() }

    /*
     * Eventos recibidos entre frames.
     *
     * Esto evita perder taps rápidos:
     *
     * touchDown()
     * touchUp()
     *
     * pueden ocurrir antes del siguiente update().
     */
    private val pendingDown = BooleanArray(padPositionsHorizontal.size)
    private val pendingUp = BooleanArray(padPositionsHorizontal.size)

    /*
     * Si DOWN y UP ocurrieron entre el mismo par de frames,
     * entregamos DOWN primero y UP en el siguiente.
     */
    private val deferredUp = BooleanArray(padPositionsHorizontal.size)

    private val keyToPadMap = mapOf(
        Keys.NUMPAD_1 to 0,
        Keys.NUMPAD_7 to 1,
        Keys.NUMPAD_5 to 2,
        Keys.NUMPAD_9 to 3,
        Keys.NUMPAD_3 to 4,

        Keys.Z to 5,
        Keys.Q to 6,
        Keys.S to 7,
        Keys.E to 8,
        Keys.C to 9
    )

    private val touchRadius: Float
        get() = widthBtnsHorizontal * MULTIPLER_TOUCH_RADIUS

    private fun getLogicalPad(physicalPad: Int): Int {
        return physicalPad % 5
    }

    /*
     * ---------------------------------------------------------
     * INPUT INTERNO
     * ---------------------------------------------------------
     */

    private fun pressPad(pad: Int, source: Int) {
        if (pad !in padPointers.indices) return

        val pointers = padPointers[pad]

        /*
         * Ya estaba registrada esta misma fuente.
         */
        if (!pointers.add(source)) return

        /*
         * Sólo generamos un DOWN cuando pasamos realmente
         * de cero fuentes a una fuente.
         */
        if (pointers.size == 1) {
            pendingDown[pad] = true

            /*
             * Si había un UP pendiente pero ya volvimos a tocar
             * antes del update, el estado final es PRESIONADO.
             */
            if (pendingUp[pad]) {
                pendingUp[pad] = false
            }
        }
    }

    private fun releasePad(pad: Int, source: Int) {
        if (pad !in padPointers.indices) return

        val pointers = padPointers[pad]

        if (!pointers.remove(source)) return

        /*
         * Sólo es UP cuando ya no queda ninguna fuente
         * sosteniendo ese pad.
         */
        if (pointers.isEmpty()) {
            pendingUp[pad] = true
        }
    }

    private fun clearPointer(pointer: Int) {
        val oldPad = pointerToPadMap.remove(pointer) ?: return
        releasePad(oldPad, pointer)
    }

    /*
     * ---------------------------------------------------------
     * TECLADO
     * ---------------------------------------------------------
     */

    override fun keyDown(keycode: Int): Boolean {
        val pad = keyToPadMap[keycode] ?: return false

        /*
         * Negativo para no colisionar con pointers táctiles.
         */
        pressPad(pad, -keycode)

        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        val pad = keyToPadMap[keycode] ?: return false

        releasePad(pad, -keycode)

        return true
    }

    /*
     * ---------------------------------------------------------
     * TOUCH
     * ---------------------------------------------------------
     */

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        /*
         * Seguridad por reutilización de pointer.
         *
         * Android normalmente entrega touchUp correctamente,
         * pero si el ID se reutilizó y quedó algo viejo,
         * aquí lo eliminamos primero.
         */
        clearPointer(pointer)

        val pad = getPadIndex(screenX.toFloat(), screenY.toFloat())

        if (pad == -1) return false

        pointerToPadMap[pointer] = pad
        pressPad(pad, pointer)

        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        clearPointer(pointer)
        return true
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        clearPointer(pointer)
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val newPad = getPadIndex(screenX.toFloat(), screenY.toFloat())
        val oldPad = pointerToPadMap[pointer]

        /*
         * Sigue dentro del mismo pad.
         */
        if (newPad == oldPad) return true

        /*
         * Salió del pad anterior.
         */
        if (oldPad != null) {
            releasePad(oldPad, pointer)
            pointerToPadMap.remove(pointer)
        }

        /*
         * Entró a un nuevo pad.
         */
        if (newPad != -1) {
            pointerToPadMap[pointer] = newPad
            pressPad(newPad, pointer)
        }

        return true
    }

    /*
     * ---------------------------------------------------------
     * HIT TEST
     * ---------------------------------------------------------
     *
     * Siempre devuelve UN SOLO pad.
     *
     * Primero tiene prioridad absoluta el rectángulo real.
     * Si estamos ligeramente fuera, toma únicamente
     * el pad más cercano dentro del radio.
     */

    private fun getPadIndex(x: Float, y: Float): Int {

        /*
         * 1. Rectángulo real.
         */
        for (i in padPositionsHorizontal.indices) {
            val pad = padPositionsHorizontal[i]

            val left = pad[0]
            val top = pad[1]
            val right = left + widthBtnsHorizontal
            val bottom = top + heightBtnsHorizontal

            if (x >= left && x <= right && y >= top && y <= bottom) {
                return i
            }
        }

        /*
         * 2. Tolerancia alrededor del pad.
         *
         * Nunca devuelve varios pads.
         */
        var closestPad = -1
        var closestDistanceSq = Float.MAX_VALUE

        val radius = touchRadius
        val radiusSq = radius * radius

        for (i in padPositionsHorizontal.indices) {
            val pad = padPositionsHorizontal[i]

            val left = pad[0]
            val top = pad[1]
            val right = left + widthBtnsHorizontal
            val bottom = top + heightBtnsHorizontal

            val closestX = x.coerceIn(left, right)
            val closestY = y.coerceIn(top, bottom)

            val dx = x - closestX
            val dy = y - closestY

            val distanceSq = dx * dx + dy * dy

            if (distanceSq <= radiusSq && distanceSq < closestDistanceSq) {
                closestDistanceSq = distanceSq
                closestPad = i
            }
        }

        return closestPad
    }

    /*
     * ---------------------------------------------------------
     * UPDATE
     * ---------------------------------------------------------
     */

    fun update() {

        /*
         * Primero calculamos los estados físicos.
         */
        for (physical in getKeyBoard.indices) {

            val pressedNow = padPointers[physical].isNotEmpty()

            val hadDown = pendingDown[physical]
            val hadUp = pendingUp[physical]

            val state = when {

                /*
                 * Un evento DOWN real siempre tiene prioridad.
                 */
                hadDown -> KEY_DOWN

                /*
                 * UP diferido por un tap demasiado rápido
                 * para caber entre dos frames.
                 */
                deferredUp[physical] -> KEY_UP

                /*
                 * UP recibido normalmente.
                 */
                hadUp -> KEY_UP

                /*
                 * Sigue sostenido.
                 */
                pressedNow -> KEY_PRESS

                else -> KEY_NONE
            }

            getKeyBoard[physical] = state

            /*
             * DOWN + UP ocurridos antes del mismo update().
             *
             * Ejemplo:
             *
             * frame
             * ↓
             * DOWN
             * UP
             * ↓
             * frame
             *
             * Este frame entregamos DOWN.
             * El siguiente entregamos UP.
             */
            if (hadDown && hadUp && !pressedNow) {
                deferredUp[physical] = true
            } else if (state == KEY_UP) {
                deferredUp[physical] = false
            } else if (hadDown && pressedNow) {
                deferredUp[physical] = false
            }

            pendingDown[physical] = false
            pendingUp[physical] = false
        }

        /*
         * -----------------------------------------------------
         * 10 pads físicos -> 5 columnas lógicas.
         * -----------------------------------------------------
         */

        logicalState.fill(KEY_NONE)

        for (physical in getKeyBoard.indices) {
            val logical = getLogicalPad(physical)
            val physicalState = getKeyBoard[physical]

            /*
             * Prioridad:
             *
             * DOWN > PRESS > UP > NONE
             *
             * Ejemplo:
             *
             * pad 0 está PRESS
             * pad 5 acaba de hacer DOWN
             *
             * logical 0 debe ser DOWN.
             */
            when (physicalState) {

                KEY_DOWN -> {
                    logicalState[logical] = KEY_DOWN
                }

                KEY_PRESS -> {
                    if (logicalState[logical] != KEY_DOWN) {
                        logicalState[logical] = KEY_PRESS
                    }
                }

                KEY_UP -> {
                    if (logicalState[logical] == KEY_NONE) {
                        logicalState[logical] = KEY_UP
                    }
                }
            }
        }
    }

    /*
     * Player usa esto solamente para saber
     * cuál pad físico debe dibujar presionado.
     */
    fun getPhysicalPadsForLogical(logical: Int): List<Int> {
        val result = mutableListOf<Int>()

        for (physical in getKeyBoard.indices) {
            if (
                physical % 5 == logical &&
                (getKeyBoard[physical] == KEY_DOWN || getKeyBoard[physical] == KEY_PRESS)
            ) {
                result.add(physical)
            }
        }

        return result
    }

    /*
     * ---------------------------------------------------------
     * RENDER
     * ---------------------------------------------------------
     */

    fun render(batch: SpriteBatch) {
        for (i in padPositionsHorizontal.indices) {
            val x = padPositionsHorizontal[i][0]
            val y = padPositionsHorizontal[i][1]

            val pressed =
                getKeyBoard[i] == KEY_DOWN ||
                        getKeyBoard[i] == KEY_PRESS

            batch.draw(
                if (pressed) btnOnPress else btnOffPress,
                x,
                y,
                widthBtnsHorizontal,
                heightBtnsHorizontal
            )
        }
    }

    /*
     * ---------------------------------------------------------
     * RESET
     * ---------------------------------------------------------
     */

    fun resetState() {
        pointerToPadMap.clear()

        for (i in padPointers.indices) {
            padPointers[i].clear()

            pendingDown[i] = false
            pendingUp[i] = false
            deferredUp[i] = false

            getKeyBoard[i] = KEY_NONE
        }

        logicalState.fill(KEY_NONE)
    }

    fun dispose() {
        btnOffPress.dispose()
        btnOnPress.dispose()
    }
}