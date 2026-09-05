package com.fingerdance.ssc

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram

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
    private val cellMetrics: Array<GameScreenSsc.NoteCellMetrics>
) {

    private val appearFadeShader = createAppearFadeShader()
    private val vanishFadeShader = createVanishFadeShader()
    private val vanishMidLineFadeShader = createVanishMidLineFadeShader()

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
        when {
            isAp ->
                drawNoteAp(column, y, frame)

            isVanish || note.isVanish ->
                drawNoteVanish(column, y, frame)

            else ->
                drawNoteNormal(column, y, frame)
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
        when {
            isAp ->
                drawNoteMineAp(column, y, frame)

            isVanish || note.isVanish ->
                drawNoteMineVanish(column, y, frame)

            else ->
                drawNoteMine(column, y, frame)
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
        when {
            isAp ->
                drawLongNoteAp(column, y, y2, frame)

            isVanish || note.isVanish ->
                drawLongNoteVanish(column, y, y2, frame)

            else ->
                drawLongNoteNormal(column, y, y2, frame)
        }
    }

    // -------------------------------------------------------------------------
    // HOLD NORMAL
    // -------------------------------------------------------------------------

    private fun drawLongNoteNormal(
        column: Int,
        y: Int,
        y2: Int,
        frame: Int
    ) {
        val logicalX = computeLeft(column, y)
        val head = getCellDraw(column, logicalX, y.toFloat())
        val bottom = getCellDraw(column, logicalX, y2.toFloat())
        val posY = y.toFloat() + middleSize
        val remainingLength = (y2 - y).toFloat()
        val bodyHeight = remainingLength - middleSize
        val widthBody = getBodyWidth(column)
        val leftBody = getBodyX(column, logicalX)

        if (normalUsesMidLine) {
            val limit = initArrow ?: return

            if (posY < limit) {
                val bodyEndY =
                    minOf(posY + bodyHeight, limit)

                val visibleBodyHeight =
                    bodyEndY - posY

                if (visibleBodyHeight > 0f) {
                    drawWithAppearShader(
                        region = bodies[column][frame],
                        x = leftBody,
                        y = posY,
                        width = widthBody,
                        height = visibleBodyHeight,
                        fadeLimit = limit
                    )
                }

                if (y2 < limit) {
                    batch.setColor(
                        1f,
                        1f,
                        1f,
                        getAlpha(y2.toFloat(), limit.toDouble())
                    )

                    if (remainingLength > heightBodyHead) {
                        batch.draw(
                            bottoms[column][frame],
                            bottom.x,
                            bottom.y,
                            bottom.width,
                            bottom.height
                        )
                    }
                }

                if (y > 0) {
                    batch.setColor(
                        1f,
                        1f,
                        1f,
                        getAlpha(y.toFloat(), limit.toDouble())
                    )

                    batch.draw(
                        arrows[column][frame],
                        head.x,
                        head.y,
                        head.width,
                        head.height
                    )
                }

                resetColor()
            }
        } else {
            if (bodyHeight > 0f) {
                batch.draw(
                    bodies[column][frame],
                    leftBody,
                    posY,
                    widthBody,
                    bodyHeight
                )
            }

            if (remainingLength > heightBodyHead) {
                batch.draw(
                    bottoms[column][frame],
                    bottom.x,
                    bottom.y,
                    bottom.width,
                    bottom.height
                )
            }

            if (y > 0) {
                batch.draw(
                    arrows[column][frame],
                    head.x,
                    head.y,
                    head.width,
                    head.height
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // HOLD VANISH
    // -------------------------------------------------------------------------

    private fun drawLongNoteVanish(
        column: Int,
        y: Int,
        y2: Int,
        frame: Int
    ) {
        val logicalX = computeLeft(column, y)
        val head = getCellDraw(column, logicalX, y.toFloat())
        val bottom = getCellDraw(column, logicalX, y2.toFloat())
        val posY = y.toFloat() + middleSize
        val remainingLength = (y2 - y).toFloat()
        val bodyHeight = remainingLength - middleSize
        val widthBody = getBodyWidth(column)
        val leftBody = getBodyX(column, logicalX)

        /*
         * IMPORTANTE:
         * El BODY Vanish conserva el desplazamiento que ya tenia tu PlayerSsc:
         * MEASUREVANISH + (arrowSize * 2).
         *
         * HEAD y BOTTOM usan MEASUREVANISH directamente mediante
         * getVanishAlpha().
         */
        val vanishBodyFadeEnd =
            (measureVanish + (arrowSize * 2f)).toFloat()

        if (vanishUsesMidLine) {
            val appearLimit = initArrow ?: return

            /*
             * VANISH + MIDLINE:
             *
             * 1) aparece al cruzar initArrow: 0 -> 1
             * 2) desaparece al llegar a measureVanish: 1 -> 0
             *
             * El BODY hace ambos fades por pixel con shader.
             */
            if (
                bodyHeight > 0f &&
                posY + bodyHeight > measureVanish &&
                posY < appearLimit
            ) {
                val bodyEndY =
                    if (clipVanishBodyAtInitArrow) {
                        minOf(posY + bodyHeight, appearLimit)
                    } else {
                        posY + bodyHeight
                    }

                val visibleBodyHeight =
                    bodyEndY - posY

                if (visibleBodyHeight > 0f) {
                    drawWithVanishMidLineShader(
                        region = bodies[column][frame],
                        x = leftBody,
                        y = posY,
                        width = widthBody,
                        height = visibleBodyHeight,
                        appearLimit = appearLimit,
                        vanishEnd = vanishBodyFadeEnd
                    )
                }
            }

            /*
             * BOTTOM:
             * Tiene que participar tanto en la aparicion de initArrow
             * como en la desaparicion de measureVanish.
             */
            if (
                remainingLength > heightBodyHead &&
                y2 < appearLimit &&
                y2 > measureVanish
            ) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getVanishMidLineAlpha(
                        y = y2.toFloat(),
                        appearLimit = appearLimit
                    )
                )

                batch.draw(
                    bottoms[column][frame],
                    bottom.x,
                    bottom.y,
                    bottom.width,
                    bottom.height
                )
            }

            /*
             * HEAD:
             * Misma regla visual que el bottom.
             */
            if (
                y > 0 &&
                y < appearLimit &&
                y > measureVanish
            ) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getVanishMidLineAlpha(
                        y = y.toFloat(),
                        appearLimit = appearLimit
                    )
                )

                batch.draw(
                    arrows[column][frame],
                    head.x,
                    head.y,
                    head.width,
                    head.height
                )
            }

            resetColor()
        } else {
            /*
             * VANISH SIN MIDLINE:
             * empieza visible y solo desaparece al llegar a measureVanish.
             */
            if (bodyHeight > 0f) {
                drawWithVanishShader(
                    region = bodies[column][frame],
                    x = leftBody,
                    y = posY,
                    width = widthBody,
                    height = bodyHeight,
                    fadeEnd = vanishBodyFadeEnd
                )
            }

            if (
                remainingLength > heightBodyHead &&
                y2 > measureVanish
            ) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getVanishAlpha(y2.toFloat())
                )

                batch.draw(
                    bottoms[column][frame],
                    bottom.x,
                    bottom.y,
                    bottom.width,
                    bottom.height
                )
            }

            if (
                y > 0 &&
                y > measureVanish
            ) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getVanishAlpha(y.toFloat())
                )

                batch.draw(
                    arrows[column][frame],
                    head.x,
                    head.y,
                    head.width,
                    head.height
                )
            }

            resetColor()
        }
    }

    // -------------------------------------------------------------------------
    // HOLD AP
    // -------------------------------------------------------------------------

    private fun drawLongNoteAp(
        column: Int,
        y: Int,
        y2: Int,
        frame: Int
    ) {
        val logicalX = computeLeft(column, y)
        val head = getCellDraw(column, logicalX, y.toFloat())
        val bottom = getCellDraw(column, logicalX, y2.toFloat())
        val posY = y.toFloat() + middleSize
        val remainingLength = (y2 - y).toFloat()
        val bodyHeight = remainingLength - middleSize
        val widthBody = getBodyWidth(column)
        val leftBody = getBodyX(column, logicalX)

        val limit = measure.toFloat()

        /*
         * AP NO depende de isMidLine.
         * Siempre aparece al cruzar MEASURE: alpha 0 -> 1.
         */
        if (posY < limit) {
            val bodyEndY =
                minOf(posY + bodyHeight, limit)

            val visibleBodyHeight =
                bodyEndY - posY

            if (visibleBodyHeight > 0f) {
                drawWithAppearShader(
                    region = bodies[column][frame],
                    x = leftBody,
                    y = posY,
                    width = widthBody,
                    height = visibleBodyHeight,
                    fadeLimit = limit
                )
            }

            if (y2 < measure) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getAlpha(y2.toFloat(), measure)
                )

                if (remainingLength > heightBodyHead) {
                    batch.draw(
                        bottoms[column][frame],
                        bottom.x,
                        bottom.y,
                        bottom.width,
                        bottom.height
                    )
                }
            }

            if (y > 0) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getAlpha(y.toFloat(), measure)
                )

                batch.draw(
                    arrows[column][frame],
                    head.x,
                    head.y,
                    head.width,
                    head.height
                )
            }

            resetColor()
        }
    }

    // -------------------------------------------------------------------------
    // TAP NORMAL
    // -------------------------------------------------------------------------

    private fun drawNoteNormal(
        column: Int,
        y: Int,
        frame: Int
    ) {
        val logicalX = computeLeft(column, y)
        val draw = getCellDraw(column, logicalX, y.toFloat())

        if (normalUsesMidLine) {
            val limit = initArrow ?: return

            if (y < limit) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getAlpha(y.toFloat(), limit.toDouble())
                )

                batch.draw(
                    arrows[column][frame],
                    draw.x,
                    draw.y,
                    draw.width,
                    draw.height
                )

                resetColor()
            }
        } else {
            batch.draw(
                arrows[column][frame],
                draw.x,
                draw.y,
                draw.width,
                draw.height
            )
        }
    }

    // -------------------------------------------------------------------------
    // TAP VANISH
    // -------------------------------------------------------------------------

    private fun drawNoteVanish(
        column: Int,
        y: Int,
        frame: Int
    ) {
        val logicalX = computeLeft(column, y)
        val draw = getCellDraw(column, logicalX, y.toFloat())

        if (vanishUsesMidLine) {
            val appearLimit = initArrow ?: return

            if (
                y < appearLimit &&
                y > measureVanish
            ) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getVanishMidLineAlpha(
                        y = y.toFloat(),
                        appearLimit = appearLimit
                    )
                )

                batch.draw(
                    arrows[column][frame],
                    draw.x,
                    draw.y,
                    draw.width,
                    draw.height
                )

                resetColor()
            }
        } else {
            if (y > measureVanish) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getVanishAlpha(y.toFloat())
                )

                batch.draw(
                    arrows[column][frame],
                    draw.x,
                    draw.y,
                    draw.width,
                    draw.height
                )

                resetColor()
            }
        }
    }

    // -------------------------------------------------------------------------
    // TAP AP
    // -------------------------------------------------------------------------

    private fun drawNoteAp(
        column: Int,
        y: Int,
        frame: Int
    ) {
        val logicalX = computeLeft(column, y)
        val draw = getCellDraw(column, logicalX, y.toFloat())

        if (y < measure) {
            batch.setColor(
                1f,
                1f,
                1f,
                getAlpha(y.toFloat(), measure)
            )

            batch.draw(
                arrows[column][frame],
                draw.x,
                draw.y,
                draw.width,
                draw.height
            )

            resetColor()
        }
    }

    // -------------------------------------------------------------------------
    // MINE NORMAL
    // -------------------------------------------------------------------------

    private fun drawNoteMine(
        column: Int,
        y: Int,
        frame: Int
    ) {
        val logicalX = computeLeft(column, y)
        val draw = getCellDraw(column, logicalX, y.toFloat())

        if (mineUsesMidLine) {
            val limit = initArrow ?: return

            if (y < limit) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getAlpha(y.toFloat(), limit.toDouble())
                )

                batch.draw(
                    mines[frame],
                    draw.x,
                    draw.y,
                    draw.width,
                    draw.height
                )

                resetColor()
            }
        } else {
            batch.draw(
                mines[frame],
                draw.x,
                draw.y,
                draw.width,
                draw.height
            )
        }
    }

    // -------------------------------------------------------------------------
    // MINE VANISH
    // -------------------------------------------------------------------------

    private fun drawNoteMineVanish(
        column: Int,
        y: Int,
        frame: Int
    ) {
        val logicalX = computeLeft(column, y)
        val draw = getCellDraw(column, logicalX, y.toFloat())

        if (vanishUsesMidLine) {
            val appearLimit = initArrow ?: return

            if (
                y < appearLimit &&
                y > measureVanish
            ) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getVanishMidLineAlpha(
                        y = y.toFloat(),
                        appearLimit = appearLimit
                    )
                )

                batch.draw(
                    mines[frame],
                    draw.x,
                    draw.y,
                    draw.width,
                    draw.height
                )

                resetColor()
            }
        } else {
            if (y > measureVanish) {
                batch.setColor(
                    1f,
                    1f,
                    1f,
                    getVanishAlpha(y.toFloat())
                )

                batch.draw(
                    mines[frame],
                    draw.x,
                    draw.y,
                    draw.width,
                    draw.height
                )

                resetColor()
            }
        }
    }

    // -------------------------------------------------------------------------
    // MINE AP
    // -------------------------------------------------------------------------

    private fun drawNoteMineAp(
        column: Int,
        y: Int,
        frame: Int
    ) {
        val logicalX = computeLeft(column, y)
        val draw = getCellDraw(column, logicalX, y.toFloat())

        if (y < measure) {
            batch.setColor(
                1f,
                1f,
                1f,
                getAlpha(y.toFloat(), measure)
            )

            batch.draw(
                mines[frame],
                draw.x,
                draw.y,
                draw.width,
                draw.height
            )

            resetColor()
        }
    }

    // -------------------------------------------------------------------------
    // ALPHA
    // -------------------------------------------------------------------------

    private fun getAlpha(
        y: Float,
        init: Double
    ): Float {
        return (
                (init - y) / rangeAlpha
                )
            .toFloat()
            .coerceIn(0f, 1f)
    }

    private fun getVanishAlpha(
        y: Float
    ): Float {
        return (
                (y - measureVanish) / rangeAlpha
                )
            .toFloat()
            .coerceIn(0f, 1f)
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
        batch.setColor(
            1f,
            1f,
            1f,
            1f
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

        val previousShader = batch.shader

        batch.shader = appearFadeShader

        appearFadeShader.setUniformf(
            "u_fadeLimit",
            fadeLimit
        )

        appearFadeShader.setUniformf(
            "u_fadeRange",
            rangeAlpha
        )

        resetColor()

        batch.draw(
            region,
            x,
            y,
            width,
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

        val previousShader = batch.shader

        batch.shader = vanishFadeShader

        vanishFadeShader.setUniformf(
            "u_fadeEnd",
            fadeEnd
        )

        vanishFadeShader.setUniformf(
            "u_fadeRange",
            rangeAlpha
        )

        resetColor()

        batch.draw(
            region,
            x,
            y,
            width,
            height
        )

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

        val previousShader = batch.shader

        batch.shader = vanishMidLineFadeShader

        vanishMidLineFadeShader.setUniformf(
            "u_appearLimit",
            appearLimit
        )

        vanishMidLineFadeShader.setUniformf(
            "u_vanishEnd",
            vanishEnd
        )

        vanishMidLineFadeShader.setUniformf(
            "u_fadeRange",
            rangeAlpha
        )

        resetColor()

        batch.draw(
            region,
            x,
            y,
            width,
            height
        )

        batch.shader = previousShader
    }

    // -------------------------------------------------------------------------
    // CREACION DE SHADERS
    // -------------------------------------------------------------------------

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

            void main() {
                vec4 texColor =
                    texture2D(
                        u_texture,
                        v_texCoords
                    );

                float safeRange =
                    max(
                        u_fadeRange,
                        0.0001
                    );

                float fadeAlpha =
                    clamp(
                        (u_fadeLimit - v_worldY) /
                            safeRange,
                        0.0,
                        1.0
                    );

                gl_FragColor =
                    vec4(
                        texColor.rgb * v_color.rgb,
                        texColor.a *
                            v_color.a *
                            fadeAlpha
                    );
            }
        """.trimIndent()

        return compileShader(
            name = "AppearFadeShader",
            vertexShader = vertexShader,
            fragmentShader = fragmentShader
        )
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

            void main() {
                vec4 texColor =
                    texture2D(
                        u_texture,
                        v_texCoords
                    );

                float safeRange =
                    max(
                        u_fadeRange,
                        0.0001
                    );

                float fadeStart =
                    u_fadeEnd - safeRange;

                float fadeAlpha =
                    clamp(
                        (v_worldY - fadeStart) /
                            safeRange,
                        0.0,
                        1.0
                    );

                gl_FragColor =
                    vec4(
                        texColor.rgb * v_color.rgb,
                        texColor.a *
                            v_color.a *
                            fadeAlpha
                    );
            }
        """.trimIndent()

        return compileShader(
            name = "VanishFadeShader",
            vertexShader = vertexShader,
            fragmentShader = fragmentShader
        )
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

            void main() {
                vec4 texColor =
                    texture2D(
                        u_texture,
                        v_texCoords
                    );

                float safeRange =
                    max(
                        u_fadeRange,
                        0.0001
                    );

                /*
                 * Entrada MidLine:
                 * y justo debajo de initArrow = alpha 0
                 * al seguir subiendo = alpha 1
                 */
                float appearAlpha =
                    clamp(
                        (u_appearLimit - v_worldY) /
                            safeRange,
                        0.0,
                        1.0
                    );

                /*
                 * Vanish del BODY:
                 * conserva el desplazamiento especifico del body.
                 */
                float vanishStart =
                    u_vanishEnd - safeRange;

                float vanishAlpha =
                    clamp(
                        (v_worldY - vanishStart) /
                            safeRange,
                        0.0,
                        1.0
                    );

                float finalAlpha =
                    min(
                        appearAlpha,
                        vanishAlpha
                    );

                gl_FragColor =
                    vec4(
                        texColor.rgb * v_color.rgb,
                        texColor.a *
                            v_color.a *
                            finalAlpha
                    );
            }
        """.trimIndent()

        return compileShader(
            name = "VanishMidLineFadeShader",
            vertexShader = vertexShader,
            fragmentShader = fragmentShader
        )
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