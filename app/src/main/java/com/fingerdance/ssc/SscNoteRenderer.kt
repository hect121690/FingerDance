package com.fingerdance.ssc

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.fingerdance.luaNotes

/**
 * Render compartido de TAP, MINE y HOLD para los Player SSC.
 *
 * Mantiene separadas las funciones NORMAL / VANISH / AP porque cada una
 * conserva sus reglas visuales específicas.
 *
 * Prioridad de efecto:
 * 1) isAp global
 * 2) isVanish global o note.isVanish del chart
 * 3) NORMAL
 *
 * AP no depende de isMidLine.
 */
class SscNoteRenderer(
    private val batch: SpriteBatch,
    private val arrowSize: Float,
    private val arrows: Array<Array<TextureRegion>>,
    private val bodies: Array<Array<TextureRegion>>,
    private val bottoms: Array<Array<TextureRegion>>,
    private val mines: Array<TextureRegion>,
    private val initArrow: Float?,
    private val measure: Double,
    private val measureVanish: Double,
    private val rangeAlpha: Float,
    private val middleSize: Float,
    private val heightBodyHead: Float,
    private val normalUsesMidLine: Boolean,
    private val vanishUsesMidLine: Boolean,
    private val clipVanishBodyAtInitArrow: Boolean,
    private val mineUsesMidLine: Boolean,
    private val computeLeft: (column: Int, y: Int) -> Float,
    private val computeY: (column: Int, y: Float) -> Float = { _, y -> y },
    private val computeScaleY: () -> Float = { 1f },
    private val computeScale: () -> Float = { 1f },

    /**
     * Alpha global de las notas para ATTACK Stealth.
     * 1f = visible, 0f = invisible.
     */
    private val computeAlpha: () -> Float = { 1f },

    /** Escala perspectiva segura de MoveZ para la columna activa. */
    private val computeDepthScale: (column: Int) -> Float = { 1f },

    private val computeRotation: (note: Parser.Note) -> Float = { 0f },

    private val cellMetrics: Array<GameScreenSsc.NoteCellMetrics>
) {

    private val normalFlipShader = createNormalFlipShader()
    private val appearFadeShader = createAppearFadeShader()
    private val vanishFadeShader = createVanishFadeShader()
    private val vanishMidLineFadeShader = createVanishMidLineFadeShader()

    // TAP / MINE / HEAD / BOTTOM: una vuelta completa cada 6 alturas de flecha.
    // El BODY NO usa esta longitud: siempre hace una sola media vuelta de head a bottom.
    private val flipWaveLength = arrowSize * 6f

    /*
     * Se actualiza al entrar a drawTap/drawMine/drawHold.
     * Los fades NORMAL/VANISH/AP se multiplican por este valor.
     */
    private var activeNoteAlpha = 1f
    private var activeDepthScale = 1f


    private data class CellDraw(val x: Float, val y: Float, val width: Float, val height: Float)

    private fun getCellDraw(column: Int, logicalX: Float, logicalY: Float): CellDraw {
        val metrics = cellMetrics[column]
        return CellDraw(metrics.drawX(logicalX, arrowSize), metrics.drawY(logicalY, arrowSize), metrics.drawWidth(arrowSize), metrics.drawHeight(arrowSize))
    }

    private fun getBodyX(column: Int, logicalX: Float): Float {
        val metrics = cellMetrics[column]
        return metrics.drawX(logicalX, arrowSize)
    }

    private fun getBodyWidth(column: Int): Float = cellMetrics[column].drawWidth(arrowSize)
    private fun transformedY(column: Int, y: Int): Float = computeY(column, y.toFloat())

    // -------------------------------------------------------------------------
    // ENTRADAS PUBLICAS
    // -------------------------------------------------------------------------

    fun drawTap(
        note: Parser.Note,
        column: Int,
        y: Int,
        frame: Int,
        isAp: Boolean,
        isVanish: Boolean
    ) {
        val previousColor = batch.color.cpy()
        activeNoteAlpha = computeAlpha().coerceIn(0f, 1f)
        activeDepthScale = computeDepthScale(column).coerceIn(0.60f, 1.60f)

        try {
            /*
             * Asegura que incluso los paths que dibujan sin fade explícito
             * reciban Stealth desde el primer draw.
             */
            batch.setColor(1f, 1f, 1f, activeNoteAlpha)

            val rotation = computeRotation(note)

            when {
                isAp -> drawNoteAp(column, y, frame, rotation)
                isVanish || note.isVanish -> drawNoteVanish(column, y, frame, rotation)
                else -> drawNoteNormal(column, y, frame, rotation)
            }
        } finally {
            batch.color = previousColor
            activeNoteAlpha = 1f
            activeDepthScale = 1f
        }
    }

    fun drawMine(
        note: Parser.Note,
        column: Int,
        y: Int,
        frame: Int,
        isAp: Boolean,
        isVanish: Boolean
    ) {
        val previousColor = batch.color.cpy()
        activeNoteAlpha = computeAlpha().coerceIn(0f, 1f)
        activeDepthScale = computeDepthScale(column).coerceIn(0.60f, 1.60f)

        try {
            batch.setColor(1f, 1f, 1f, activeNoteAlpha)

            val rotation = computeRotation(note)

            when {
                isAp -> drawNoteMineAp(column, y, frame, rotation)
                isVanish || note.isVanish -> drawNoteMineVanish(column, y, frame, rotation)
                else -> drawNoteMine(column, y, frame, rotation)
            }
        } finally {
            batch.color = previousColor
            activeNoteAlpha = 1f
            activeDepthScale = 1f
        }
    }

    fun drawHold(
        note: Parser.Note,
        column: Int,
        y: Int,
        y2: Int,
        frame: Int,
        isAp: Boolean,
        isVanish: Boolean
    ) {
        val previousColor = batch.color.cpy()
        activeNoteAlpha = computeAlpha().coerceIn(0f, 1f)
        activeDepthScale = computeDepthScale(column).coerceIn(0.60f, 1.60f)

        try {
            batch.setColor(1f, 1f, 1f, activeNoteAlpha)

            val rotation = computeRotation(note)

            when {
                isAp -> drawLongNoteAp(column, y, y2, frame, rotation)
                isVanish || note.isVanish -> drawLongNoteVanish(column, y, y2, frame, rotation)
                else -> drawLongNoteNormal(column, y, y2, frame, rotation)
            }
        } finally {
            batch.color = previousColor
            activeNoteAlpha = 1f
            activeDepthScale = 1f
        }
    }

    private fun drawFlipRegion(
        region: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Float = 0f,
        reverseY: Boolean = true
    ) {
        val miniScale = computeScale() * activeDepthScale
        val reverseScaleY = if (reverseY) computeScaleY() else 1f

        if (!luaNotes.flipX) {
            batch.draw(
                region,
                x,
                y,
                width * 0.5f,
                height * 0.5f,
                width,
                height,
                miniScale,
                miniScale * reverseScaleY,
                rotation
            )
            return
        }

        val previousShader = batch.shader
        batch.shader = normalFlipShader

        applyFlipUniforms(
            shader = normalFlipShader,
            region = region,
            ribbon = false,
            objectCenterY = y + height * 0.5f
        )

        batch.draw(
            region,
            x,
            y,
            width * 0.5f,
            height * 0.5f,
            width,
            height,
            miniScale,
            miniScale * reverseScaleY,
            rotation
        )

        batch.shader = previousShader
    }

    private fun drawFlipBody(
        region: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        if (height <= 0f) return

        val miniScale = computeScale() * activeDepthScale

        val scaledWidth = width * miniScale
        val centeredX = x + (width - scaledWidth) * 0.5f

        if (!luaNotes.flipX) {
            batch.draw(
                region,
                centeredX,
                y,
                scaledWidth,
                height
            )
            return
        }

        val previousShader = batch.shader
        batch.shader = normalFlipShader

        applyFlipUniforms(
            shader = normalFlipShader,
            region = region,
            ribbon = true,
            objectCenterY = y + height * 0.5f,
            bodyY = y,
            bodyHeight = height
        )

        batch.draw(
            region,
            centeredX,
            y,
            scaledWidth,
            height
        )

        batch.shader = previousShader
    }

    private fun applyFlipUniforms(
        shader: ShaderProgram,
        region: TextureRegion,
        ribbon: Boolean,
        objectCenterY: Float,
        bodyY: Float = 0f,
        bodyHeight: Float = 1f
    ) {
        shader.setUniformi("u_flipEnabled", if (luaNotes.flipX) 1 else 0)
        shader.setUniformi("u_flipRibbon", if (ribbon) 1 else 0)
        shader.setUniformf("u_flipWaveLength", flipWaveLength)
        shader.setUniformf("u_flipCenterY", objectCenterY)
        shader.setUniformf("u_bodyY", bodyY)
        shader.setUniformf("u_bodyHeight", bodyHeight.coerceAtLeast(0.001f))
        shader.setUniformf("u_regionU", region.u)
        shader.setUniformf("u_regionU2", region.u2)
    }

    // -------------------------------------------------------------------------
    // HOLD NORMAL
    // -------------------------------------------------------------------------
    private fun drawLongNoteNormal(column: Int, y: Int, y2: Int, frame: Int, rotation: Float) {
        val logicalX = computeLeft(column, y)
        val headY = transformedY(column, y)
        val bottomY = transformedY(column, y2)
        val head = getCellDraw(column, logicalX, headY)
        val bottom = getCellDraw(column, logicalX, bottomY)

        val isReverse = bottomY < headY
        val remainingLength = kotlin.math.abs(bottomY - headY)

        val bodyStartY =
            if (isReverse) {
                bottomY
            } else {
                headY + middleSize
            }

        val bodyEndY =
            if (isReverse) {
                headY + middleSize
            } else {
                bottomY + middleSize
            }

        val bodyHeight =
            (bodyEndY - bodyStartY)
                .coerceAtLeast(0f)
        val widthBody = getBodyWidth(column)
        val leftBody = getBodyX(column, logicalX)

        if (normalUsesMidLine) {
            val limit = initArrow ?: return

            val visibleBodyStart = bodyStartY.coerceAtLeast(0f)
            val visibleBodyEnd = minOf(bodyEndY, limit)
            val visibleBodyHeight = visibleBodyEnd - visibleBodyStart

            if (visibleBodyHeight > 0f) {
                drawWithAppearShader(
                    region = bodies[column][frame],
                    x = leftBody,
                    y = visibleBodyStart,
                    width = widthBody,
                    height = visibleBodyHeight,
                    fadeLimit = limit
                )
            }

            if (bottomY > 0f && bottomY < limit && remainingLength > heightBodyHead) {
                batch.setColor(1f, 1f, 1f, getAlpha(bottomY, limit.toDouble()) * activeNoteAlpha)
                drawFlipRegion(
                    region = bottoms[column][frame],
                    x = bottom.x,
                    y = bottom.y,
                    width = bottom.width,
                    height = bottom.height
                )
            }

            if (headY > 0f && headY < limit) {
                batch.setColor(1f, 1f, 1f, getAlpha(headY, limit.toDouble()) * activeNoteAlpha)
                drawFlipRegion(
                    region = arrows[column][frame],
                    x = head.x,
                    y = head.y,
                    width = head.width,
                    height = head.height,
                    rotation = rotation
                )
            }

            resetColor()
        } else {
            if (bodyHeight > 0f) {
                drawFlipBody(
                    region = bodies[column][frame],
                    x = leftBody,
                    y = bodyStartY,
                    width = widthBody,
                    height = bodyHeight
                )
            }

            if (remainingLength > heightBodyHead && bottomY > 0f) {
                drawFlipRegion(
                    region = bottoms[column][frame],
                    x = bottom.x,
                    y = bottom.y,
                    width = bottom.width,
                    height = bottom.height
                )
            }

            if (headY > 0f) {
                drawFlipRegion(
                    region = arrows[column][frame],
                    x = head.x,
                    y = head.y,
                    width = head.width,
                    height = head.height,
                    rotation = rotation
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // HOLD VANISH
    // -------------------------------------------------------------------------
    private fun drawLongNoteVanish(column: Int, y: Int, y2: Int, frame: Int, rotation: Float) {
        val logicalX = computeLeft(column, y)
        val headY = transformedY(column, y)
        val bottomY = transformedY(column, y2)
        val head = getCellDraw(column, logicalX, headY)
        val bottom = getCellDraw(column, logicalX, bottomY)

        val isReverse = bottomY < headY
        val remainingLength = kotlin.math.abs(bottomY - headY)

        val bodyStartY =
            if (isReverse) {
                bottomY
            } else {
                headY + middleSize
            }

        val bodyEndY =
            if (isReverse) {
                headY + middleSize
            } else {
                bottomY + middleSize
            }

        val bodyHeight =
            (bodyEndY - bodyStartY)
                .coerceAtLeast(0f)
        val widthBody = getBodyWidth(column)
        val leftBody = getBodyX(column, logicalX)
        val vanishBodyFadeEnd = (measureVanish + (arrowSize * 2f)).toFloat()

        if (vanishUsesMidLine) {
            val appearLimit = initArrow ?: return

            val visibleBodyStart = maxOf(bodyStartY, measureVanish.toFloat())
            val unclippedBodyEnd = bodyEndY
            val visibleBodyEnd = if (clipVanishBodyAtInitArrow) {
                minOf(unclippedBodyEnd, appearLimit)
            } else {
                unclippedBodyEnd
            }

            val visibleBodyHeight = visibleBodyEnd - visibleBodyStart

            if (visibleBodyHeight > 0f) {
                drawWithVanishMidLineShader(
                    region = bodies[column][frame],
                    x = leftBody,
                    y = visibleBodyStart,
                    width = widthBody,
                    height = visibleBodyHeight,
                    appearLimit = appearLimit,
                    vanishEnd = vanishBodyFadeEnd
                )
            }

            if (
                remainingLength > heightBodyHead &&
                bottomY > measureVanish &&
                bottomY < appearLimit
            ) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getVanishMidLineAlpha(
                        y = bottomY,
                        appearLimit = appearLimit
                    )
                )

                drawFlipRegion(
                    region = bottoms[column][frame],
                    x = bottom.x,
                    y = bottom.y,
                    width = bottom.width,
                    height = bottom.height
                )
            }

            if (
                headY > 0f &&
                headY > measureVanish &&
                headY < appearLimit
            ) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getVanishMidLineAlpha(
                        y = headY,
                        appearLimit = appearLimit
                    )
                )

                drawFlipRegion(
                    region = arrows[column][frame],
                    x = head.x,
                    y = head.y,
                    width = head.width,
                    height = head.height,
                    rotation = rotation
                )
            }

            resetColor()
        } else {
            if (bodyHeight > 0f) {
                drawWithVanishShader(
                    region = bodies[column][frame],
                    x = leftBody,
                    y = bodyStartY,
                    width = widthBody,
                    height = bodyHeight,
                    fadeEnd = vanishBodyFadeEnd
                )
            }

            if (
                remainingLength > heightBodyHead &&
                bottomY > measureVanish
            ) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getVanishAlpha(bottomY)
                )

                drawFlipRegion(
                    region = bottoms[column][frame],
                    x = bottom.x,
                    y = bottom.y,
                    width = bottom.width,
                    height = bottom.height
                )
            }

            if (
                headY > 0f &&
                headY > measureVanish
            ) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getVanishAlpha(headY)
                )

                drawFlipRegion(
                    region = arrows[column][frame],
                    x = head.x,
                    y = head.y,
                    width = head.width,
                    height = head.height,
                    rotation = rotation
                )
            }

            resetColor()
        }
    }

    // -------------------------------------------------------------------------
    // HOLD AP
    // -------------------------------------------------------------------------
    private fun drawLongNoteAp(column: Int, y: Int, y2: Int, frame: Int, rotation: Float) {
        val logicalX = computeLeft(column, y)
        val headY = transformedY(column, y)
        val bottomY = transformedY(column, y2)
        val head = getCellDraw(column, logicalX, headY)
        val bottom = getCellDraw(column, logicalX, bottomY)

        val isReverse = bottomY < headY
        val remainingLength = kotlin.math.abs(bottomY - headY)

        val bodyStartY =
            if (isReverse) {
                bottomY
            } else {
                headY + middleSize
            }

        val bodyEndY =
            if (isReverse) {
                headY + middleSize
            } else {
                bottomY + middleSize
            }

        val widthBody = getBodyWidth(column)
        val leftBody = getBodyX(column, logicalX)
        val limit = measure.toFloat()

        val visibleBodyStart = bodyStartY.coerceAtLeast(0f)
        val visibleBodyEnd = minOf(bodyEndY, limit)
        val visibleBodyHeight = visibleBodyEnd - visibleBodyStart

        if (visibleBodyHeight > 0f) {
            drawWithAppearShader(
                region = bodies[column][frame],
                x = leftBody,
                y = visibleBodyStart,
                width = widthBody,
                height = visibleBodyHeight,
                fadeLimit = limit
            )
        }

        if (
            remainingLength > heightBodyHead &&
            bottomY > 0f &&
            bottomY < limit
        ) {
            batch.setColor(
                1f,
                1f,
                1f,
                getAlpha(bottomY, measure) * activeNoteAlpha
            )

            drawFlipRegion(
                region = bottoms[column][frame],
                x = bottom.x,
                y = bottom.y,
                width = bottom.width,
                height = bottom.height
            )
        }

        if (
            headY > 0f &&
            headY < limit
        ) {
            batch.setColor(
                1f,
                1f,
                1f,
                getAlpha(headY, measure) * activeNoteAlpha
            )

            drawFlipRegion(
                region = arrows[column][frame],
                x = head.x,
                y = head.y,
                width = head.width,
                height = head.height,
                rotation = rotation
            )
        }

        resetColor()
    }

    // -------------------------------------------------------------------------
    // TAP NORMAL
    // -------------------------------------------------------------------------

    private fun drawNoteNormal(column: Int, y: Int, frame: Int, rotation: Float) {
        val logicalX = computeLeft(column, y)
        val finalY = transformedY(column, y)
        val draw = getCellDraw(column, logicalX, finalY)

        if (normalUsesMidLine) {
            val limit = initArrow ?: return
            if (finalY < limit) {
                batch.setColor(1f, 1f, 1f, getAlpha(finalY, limit.toDouble()) * activeNoteAlpha)
                drawFlipRegion(region = arrows[column][frame], x = draw.x, y = draw.y, width = draw.width, height = draw.height, rotation = rotation)
                resetColor()
            }
        } else {
            drawFlipRegion(region = arrows[column][frame], x = draw.x, y = draw.y, width = draw.width, height = draw.height, rotation = rotation)
        }
    }
    // -------------------------------------------------------------------------
    // TAP VANISH
    // -------------------------------------------------------------------------

    private fun drawNoteVanish(column: Int, y: Int, frame: Int, rotation: Float) {
        val logicalX = computeLeft(column, y)
        val finalY = transformedY(column, y)
        val draw = getCellDraw(column, logicalX, finalY)

        if (vanishUsesMidLine) {
            val appearLimit = initArrow ?: return
            if (finalY < appearLimit && finalY > measureVanish) {
                batch.setColor(1f, 1f, 1f, getVanishMidLineAlpha(y = finalY, appearLimit = appearLimit) * activeNoteAlpha)
                drawFlipRegion(region = arrows[column][frame], x = draw.x, y = draw.y, width = draw.width, height = draw.height, rotation = rotation)
                resetColor()
            }
        } else {
            if (finalY > measureVanish) {
                batch.setColor(1f, 1f, 1f, getVanishAlpha(finalY) * activeNoteAlpha)
                drawFlipRegion(region = arrows[column][frame], x = draw.x, y = draw.y, width = draw.width, height = draw.height, rotation = rotation)
                resetColor()
            }
        }
    }
    // -------------------------------------------------------------------------
    // TAP AP
    // -------------------------------------------------------------------------

    private fun drawNoteAp(column: Int, y: Int, frame: Int, rotation: Float) {
        val logicalX = computeLeft(column, y)
        val finalY = transformedY(column, y)
        val draw = getCellDraw(column, logicalX, finalY)

        if (finalY < measure) {
            batch.setColor(1f, 1f, 1f, getAlpha(finalY, measure) * activeNoteAlpha)
            drawFlipRegion(region = arrows[column][frame], x = draw.x, y = draw.y, width = draw.width, height = draw.height, rotation = rotation)
            resetColor()
        }
    }
    // -------------------------------------------------------------------------
    // MINE NORMAL
    // -------------------------------------------------------------------------

    private fun drawNoteMine(column: Int, y: Int, frame: Int, rotation: Float) {
        val logicalX = computeLeft(column, y)
        val finalY = transformedY(column, y)
        val draw = getCellDraw(column, logicalX, finalY)

        if (mineUsesMidLine) {
            val limit = initArrow ?: return
            if (finalY < limit) {
                batch.setColor(1f, 1f, 1f, getAlpha(finalY, limit.toDouble()) * activeNoteAlpha)
                drawFlipRegion(region = mines[frame], x = draw.x, y = draw.y, width = draw.width, height = draw.height, rotation = rotation)
                resetColor()
            }
        } else {
            drawFlipRegion(region = mines[frame], x = draw.x, y = draw.y, width = draw.width, height = draw.height, rotation = rotation)
        }
    }
    // -------------------------------------------------------------------------
    // MINE VANISH
    // -------------------------------------------------------------------------

    private fun drawNoteMineVanish(column: Int, y: Int, frame: Int, rotation: Float) {
        val logicalX = computeLeft(column, y)
        val finalY = transformedY(column, y)
        val draw = getCellDraw(column, logicalX, finalY)

        if (vanishUsesMidLine) {
            val appearLimit = initArrow ?: return
            if (finalY < appearLimit && finalY > measureVanish) {
                batch.setColor(1f, 1f, 1f, getVanishMidLineAlpha(y = finalY, appearLimit = appearLimit) * activeNoteAlpha)
                drawFlipRegion(region = mines[frame], x = draw.x, y = draw.y, width = draw.width, height = draw.height, rotation = rotation)
                resetColor()
            }
        } else {
            if (finalY > measureVanish) {
                batch.setColor(1f, 1f, 1f, getVanishAlpha(finalY) * activeNoteAlpha)
                drawFlipRegion(region = mines[frame], x = draw.x, y = draw.y, width = draw.width, height = draw.height, rotation = rotation)
                resetColor()
            }
        }
    }
    // -------------------------------------------------------------------------
    // MINE AP
    // -------------------------------------------------------------------------

    private fun drawNoteMineAp(column: Int, y: Int, frame: Int, rotation: Float) {
        val logicalX = computeLeft(column, y)
        val finalY = transformedY(column, y)
        val draw = getCellDraw(column, logicalX, finalY)

        if (finalY < measure) {
            batch.setColor(1f, 1f, 1f, getAlpha(finalY, measure) * activeNoteAlpha)
            drawFlipRegion(region = mines[frame], x = draw.x, y = draw.y, width = draw.width, height = draw.height, rotation = rotation)
            resetColor()
        }
    }
    // -------------------------------------------------------------------------
    // ALPHA
    // -------------------------------------------------------------------------

    private fun getAlpha(y: Float, init: Double): Float {
        return (
                (init - y) / rangeAlpha
                )
            .toFloat()
            .coerceIn(0f, 1f)
    }

    private fun getVanishAlpha(y: Float): Float {
        return ((y - measureVanish) / rangeAlpha).toFloat().coerceIn(0f, 1f)
    }

    /**
     * VANISH + isMidLine=true:
     * - entrada desde initArrow 0 -> 1
     * - salida hacia measureVanish 1 -> 0
     */
    private fun getVanishMidLineAlpha(
        y: Float,
        appearLimit: Float
    ): Float {
        val appearAlpha =
            getAlpha(y, appearLimit.toDouble())

        val vanishAlpha =
            getVanishAlpha(y)

        return minOf(
            appearAlpha,
            vanishAlpha
        )
    }

    private fun resetColor() {
        /*
         * Dentro del render de una nota, "reset" significa volver al alpha
         * base de Stealth, no a alpha=1. Al salir del draw público restauramos
         * el color original del SpriteBatch.
         */
        batch.setColor(
            1f,
            1f,
            1f,
            activeNoteAlpha
        )
    }

    // -------------------------------------------------------------------------
    // SHADER: APARICION
    // NORMAL + MIDLINE y AP
    // -------------------------------------------------------------------------

    private fun drawWithAppearShader(
        region: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        fadeLimit: Float
    ) {
        if (height <= 0f) return

        val miniScale = computeScale()
        val scaledWidth = width * miniScale
        val centeredX = x + (width - scaledWidth) * 0.5f

        val previousShader = batch.shader
        batch.shader = appearFadeShader

        appearFadeShader.setUniformf("u_fadeLimit", fadeLimit)
        appearFadeShader.setUniformf("u_fadeRange", rangeAlpha)

        applyFlipUniforms(
            appearFadeShader,
            region,
            ribbon = true,
            objectCenterY = y + height * 0.5f,
            bodyY = y,
            bodyHeight = height
        )

        resetColor()

        batch.draw(
            region,
            centeredX,
            y,
            scaledWidth,
            height
        )

        batch.shader = previousShader
    }

    // -------------------------------------------------------------------------
    // SHADER: VANISH SIN MIDLINE
    // -------------------------------------------------------------------------

    private fun drawWithVanishShader(
        region: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        fadeEnd: Float
    ) {
        if (height <= 0f) return

        val miniScale = computeScale()
        val scaledWidth = width * miniScale
        val centeredX = x + (width - scaledWidth) * 0.5f

        val previousShader = batch.shader
        batch.shader = vanishFadeShader

        vanishFadeShader.setUniformf("u_fadeEnd", fadeEnd)
        vanishFadeShader.setUniformf("u_fadeRange", rangeAlpha)
        applyFlipUniforms(vanishFadeShader, region, ribbon = true, objectCenterY = y + height * 0.5f, bodyY = y, bodyHeight = height)

        resetColor()
        batch.draw(region, centeredX, y, scaledWidth, height)
        batch.shader = previousShader
    }

    // -------------------------------------------------------------------------
    // SHADER: VANISH + MIDLINE
    // entrada en initArrow + salida en Vanish
    // -------------------------------------------------------------------------

    private fun drawWithVanishMidLineShader(
        region: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        appearLimit: Float,
        vanishEnd: Float
    ) {
        if (height <= 0f) return

        val miniScale = computeScale()
        val scaledWidth = width * miniScale
        val centeredX = x + (width - scaledWidth) * 0.5f

        val previousShader = batch.shader
        batch.shader = vanishMidLineFadeShader

        vanishMidLineFadeShader.setUniformf("u_appearLimit", appearLimit)
        vanishMidLineFadeShader.setUniformf("u_vanishEnd", vanishEnd)
        vanishMidLineFadeShader.setUniformf("u_fadeRange", rangeAlpha)
        applyFlipUniforms(vanishMidLineFadeShader, region, ribbon = true, objectCenterY = y + height * 0.5f, bodyY = y, bodyHeight = height)

        resetColor()
        batch.draw(region, centeredX, y, scaledWidth, height)
        batch.shader = previousShader
    }

    // -------------------------------------------------------------------------
    // CREACION DE SHADERS
    // -------------------------------------------------------------------------

    private fun createNormalFlipShader(): ShaderProgram {
        val vertexShader = COMMON_VERTEX_SHADER
        val fragmentShader = """
            #ifdef GL_ES
            precision mediump float;
            #endif

            varying vec4 v_color;
            varying vec2 v_texCoords;
            varying float v_worldY;

            uniform sampler2D u_texture;

            uniform int u_flipEnabled;
            uniform int u_flipRibbon;
            uniform float u_flipWaveLength;
            uniform float u_flipCenterY;
            uniform float u_bodyY;
            uniform float u_bodyHeight;
            uniform float u_regionU;
            uniform float u_regionU2;

            vec2 applyFlip(vec2 uv, float worldY, out float visible) {
                visible = 1.0;
                if (u_flipEnabled == 0) return uv;

                float flipScale;

                if (u_flipRibbon == 1) {
                    // HOLD BODY: una sola media vuelta a lo largo de TODO el body.
                    // La cintura queda en el 50% y no se repite aunque la hold sea muy larga.
                    float safeBodyHeight = max(u_bodyHeight, 0.0001);
                    float t = clamp((worldY - u_bodyY) / safeBodyHeight, 0.0, 1.0);
                    flipScale = cos(t * 3.14159265359);
                } else {
                    // TAP / MINE / HEAD / BOTTOM: giro rigido segun su posicion Y.
                    float safeWaveLength = max(u_flipWaveLength, 0.0001);
                    flipScale = cos((u_flipCenterY / safeWaveLength) * 6.28318530718);
                }

                float widthScale = max(abs(flipScale), 0.015);

                float regionWidth = u_regionU2 - u_regionU;
                if (abs(regionWidth) < 0.000001) return uv;

                float localX = (uv.x - u_regionU) / regionWidth;
                float centeredX = localX - 0.5;

                if (abs(centeredX) > 0.5 * widthScale) {
                    visible = 0.0;
                    return uv;
                }

                float remappedX = centeredX / widthScale + 0.5;
                if (flipScale < 0.0) remappedX = 1.0 - remappedX;

                float finalU = u_regionU + remappedX * regionWidth;
                return vec2(finalU, uv.y);
            }

            void main() {
                float flipVisible;
                vec2 finalUv = applyFlip(v_texCoords, v_worldY, flipVisible);
                vec4 texColor = texture2D(u_texture, finalUv);
                gl_FragColor = vec4(texColor.rgb * v_color.rgb, texColor.a * v_color.a * flipVisible);
            }
        """.trimIndent()

        return compileShader("NormalFlipShader", vertexShader, fragmentShader)
    }

    private fun createAppearFadeShader(): ShaderProgram {
        val vertexShader = COMMON_VERTEX_SHADER
        val fragmentShader = """
            #ifdef GL_ES
            precision mediump float;
            #endif

            varying vec4 v_color;
            varying vec2 v_texCoords;
            varying float v_worldY;

            uniform sampler2D u_texture;
            uniform float u_fadeLimit;
            uniform float u_fadeRange;

            uniform int u_flipEnabled;
            uniform int u_flipRibbon;
            uniform float u_flipWaveLength;
            uniform float u_flipCenterY;
            uniform float u_bodyY;
            uniform float u_bodyHeight;
            uniform float u_regionU;
            uniform float u_regionU2;

            vec2 applyFlip(vec2 uv, float worldY, out float visible) {
                visible = 1.0;
                if (u_flipEnabled == 0) return uv;

                float flipScale;

                if (u_flipRibbon == 1) {
                    // HOLD BODY: una sola media vuelta a lo largo de TODO el body.
                    // La cintura queda en el 50% y no se repite aunque la hold sea muy larga.
                    float safeBodyHeight = max(u_bodyHeight, 0.0001);
                    float t = clamp((worldY - u_bodyY) / safeBodyHeight, 0.0, 1.0);
                    flipScale = cos(t * 3.14159265359);
                } else {
                    // TAP / MINE / HEAD / BOTTOM: giro rigido segun su posicion Y.
                    float safeWaveLength = max(u_flipWaveLength, 0.0001);
                    flipScale = cos((u_flipCenterY / safeWaveLength) * 6.28318530718);
                }

                float widthScale = max(abs(flipScale), 0.015);

                float regionWidth = u_regionU2 - u_regionU;
                if (abs(regionWidth) < 0.000001) return uv;

                float localX = (uv.x - u_regionU) / regionWidth;
                float centeredX = localX - 0.5;

                if (abs(centeredX) > 0.5 * widthScale) {
                    visible = 0.0;
                    return uv;
                }

                float remappedX = centeredX / widthScale + 0.5;
                if (flipScale < 0.0) remappedX = 1.0 - remappedX;

                float finalU = u_regionU + remappedX * regionWidth;
                return vec2(finalU, uv.y);
            }

            void main() {
                float flipVisible;
                vec2 finalUv = applyFlip(v_texCoords, v_worldY, flipVisible);
                vec4 texColor = texture2D(u_texture, finalUv);
                float safeRange = max(u_fadeRange, 0.0001);
                float fadeAlpha = clamp((u_fadeLimit - v_worldY) / safeRange, 0.0, 1.0);
                gl_FragColor = vec4(texColor.rgb * v_color.rgb, texColor.a * v_color.a * fadeAlpha * flipVisible);
            }
        """.trimIndent()

        return compileShader("AppearFadeShader", vertexShader, fragmentShader)
    }

    private fun createVanishFadeShader(): ShaderProgram {
        val vertexShader = COMMON_VERTEX_SHADER
        val fragmentShader = """
            #ifdef GL_ES
            precision mediump float;
            #endif

            varying vec4 v_color;
            varying vec2 v_texCoords;
            varying float v_worldY;

            uniform sampler2D u_texture;
            uniform float u_fadeEnd;
            uniform float u_fadeRange;

            uniform int u_flipEnabled;
            uniform int u_flipRibbon;
            uniform float u_flipWaveLength;
            uniform float u_flipCenterY;
            uniform float u_bodyY;
            uniform float u_bodyHeight;
            uniform float u_regionU;
            uniform float u_regionU2;

            vec2 applyFlip(vec2 uv, float worldY, out float visible) {
                visible = 1.0;
                if (u_flipEnabled == 0) return uv;

                float flipScale;

                if (u_flipRibbon == 1) {
                    // HOLD BODY: una sola media vuelta a lo largo de TODO el body.
                    // La cintura queda en el 50% y no se repite aunque la hold sea muy larga.
                    float safeBodyHeight = max(u_bodyHeight, 0.0001);
                    float t = clamp((worldY - u_bodyY) / safeBodyHeight, 0.0, 1.0);
                    flipScale = cos(t * 3.14159265359);
                } else {
                    // TAP / MINE / HEAD / BOTTOM: giro rigido segun su posicion Y.
                    float safeWaveLength = max(u_flipWaveLength, 0.0001);
                    flipScale = cos((u_flipCenterY / safeWaveLength) * 6.28318530718);
                }

                float widthScale = max(abs(flipScale), 0.015);

                float regionWidth = u_regionU2 - u_regionU;
                if (abs(regionWidth) < 0.000001) return uv;

                float localX = (uv.x - u_regionU) / regionWidth;
                float centeredX = localX - 0.5;

                if (abs(centeredX) > 0.5 * widthScale) {
                    visible = 0.0;
                    return uv;
                }

                float remappedX = centeredX / widthScale + 0.5;
                if (flipScale < 0.0) remappedX = 1.0 - remappedX;

                float finalU = u_regionU + remappedX * regionWidth;
                return vec2(finalU, uv.y);
            }

            void main() {
                float flipVisible;
                vec2 finalUv = applyFlip(v_texCoords, v_worldY, flipVisible);
                vec4 texColor = texture2D(u_texture, finalUv);
                float safeRange = max(u_fadeRange, 0.0001);
                float fadeStart = u_fadeEnd - safeRange;
                float fadeAlpha = clamp((v_worldY - fadeStart) / safeRange, 0.0, 1.0);
                gl_FragColor = vec4(texColor.rgb * v_color.rgb, texColor.a * v_color.a * fadeAlpha * flipVisible);
            }
        """.trimIndent()

        return compileShader("VanishFadeShader", vertexShader, fragmentShader)
    }

    private fun createVanishMidLineFadeShader(): ShaderProgram {
        val vertexShader = COMMON_VERTEX_SHADER
        val fragmentShader = """
            #ifdef GL_ES
            precision mediump float;
            #endif

            varying vec4 v_color;
            varying vec2 v_texCoords;
            varying float v_worldY;

            uniform sampler2D u_texture;
            uniform float u_appearLimit;
            uniform float u_vanishEnd;
            uniform float u_fadeRange;

            uniform int u_flipEnabled;
            uniform int u_flipRibbon;
            uniform float u_flipWaveLength;
            uniform float u_flipCenterY;
            uniform float u_bodyY;
            uniform float u_bodyHeight;
            uniform float u_regionU;
            uniform float u_regionU2;

            vec2 applyFlip(vec2 uv, float worldY, out float visible) {
                visible = 1.0;
                if (u_flipEnabled == 0) return uv;

                float flipScale;

                if (u_flipRibbon == 1) {
                    // HOLD BODY: una sola media vuelta a lo largo de TODO el body.
                    // La cintura queda en el 50% y no se repite aunque la hold sea muy larga.
                    float safeBodyHeight = max(u_bodyHeight, 0.0001);
                    float t = clamp((worldY - u_bodyY) / safeBodyHeight, 0.0, 1.0);
                    flipScale = cos(t * 3.14159265359);
                } else {
                    // TAP / MINE / HEAD / BOTTOM: giro rigido segun su posicion Y.
                    float safeWaveLength = max(u_flipWaveLength, 0.0001);
                    flipScale = cos((u_flipCenterY / safeWaveLength) * 6.28318530718);
                }

                float widthScale = max(abs(flipScale), 0.015);

                float regionWidth = u_regionU2 - u_regionU;
                if (abs(regionWidth) < 0.000001) return uv;

                float localX = (uv.x - u_regionU) / regionWidth;
                float centeredX = localX - 0.5;

                if (abs(centeredX) > 0.5 * widthScale) {
                    visible = 0.0;
                    return uv;
                }

                float remappedX = centeredX / widthScale + 0.5;
                if (flipScale < 0.0) remappedX = 1.0 - remappedX;

                float finalU = u_regionU + remappedX * regionWidth;
                return vec2(finalU, uv.y);
            }

            void main() {
                float flipVisible;
                vec2 finalUv = applyFlip(v_texCoords, v_worldY, flipVisible);
                vec4 texColor = texture2D(u_texture, finalUv);
                float safeRange = max(u_fadeRange, 0.0001);
                float appearAlpha = clamp((u_appearLimit - v_worldY) / safeRange, 0.0, 1.0);
                float vanishStart = u_vanishEnd - safeRange;
                float vanishAlpha = clamp((v_worldY - vanishStart) / safeRange, 0.0, 1.0);
                float finalAlpha = min(appearAlpha, vanishAlpha);
                gl_FragColor = vec4(texColor.rgb * v_color.rgb, texColor.a * v_color.a * finalAlpha * flipVisible);
            }
        """.trimIndent()

        return compileShader("VanishMidLineFadeShader", vertexShader, fragmentShader)
    }

    private fun compileShader(
        name: String,
        vertexShader: String,
        fragmentShader: String
    ): ShaderProgram {
        val shader =
            ShaderProgram(
                vertexShader,
                fragmentShader
            )

        if (!shader.isCompiled) {
            throw IllegalStateException(
                "Error compilando $name:\n${shader.log}"
            )
        }

        return shader
    }

    // -------------------------------------------------------------------------
    // DISPOSE
    // -------------------------------------------------------------------------

    fun dispose() {
        normalFlipShader.dispose()
        appearFadeShader.dispose()
        vanishFadeShader.dispose()
        vanishMidLineFadeShader.dispose()
    }

    companion object {
        private val COMMON_VERTEX_SHADER = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;

            uniform mat4 u_projTrans;

            varying vec4 v_color;
            varying vec2 v_texCoords;
            varying float v_worldY;

            void main() {
                v_color = a_color;
                v_color.a =
                    v_color.a *
                    (255.0 / 254.0);

                v_texCoords =
                    a_texCoord0;

                v_worldY =
                    a_position.y;

                gl_Position =
                    u_projTrans *
                    a_position;
            }
        """.trimIndent()
    }
}
