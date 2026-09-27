package com.fingerdance.ssc

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
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

    /** fYOffset equivalente de StepMania, ya con ACCEL + scroll speed y sin Reverse/Tipsy. */
    private val computeStepManiaYOffset: (sourceY: Float) -> Float = { 0f },
    private val computeStepManiaFieldScaleY: () -> Float = { 1f },
    private val computeStepManiaArrowScale: () -> Float = { 1f },
    private val computeScaleY: (column: Int) -> Float = { 1f },
    private val computeScale: () -> Float = { 1f },

    /**
     * Alpha global de las notas para ATTACK Stealth.
     * 1f = visible, 0f = invisible.
     */
    private val computeAlpha: () -> Float = { 1f },

    /**
     * Hidden/Sudden/Blink/RandomVanish reciben la posición SOURCE de la pieza,
     * antes de Reverse y antes de la geometría visual final.  GameScreenSsc la
     * convierte al espacio lógico de StepMania (0..480 para el área útil).
     */
    private val computeAppearanceAlpha: (column: Int, sourceY: Float) -> Float = { _, _ -> 1f },
    private val computeAppearanceActive: () -> Boolean = { false },

    /** Escala perspectiva segura de MoveZ para la columna activa. */
    private val computeDepthScale: (column: Int) -> Float = { 1f },

    /** ATTACK 3D amounts. Se mantienen separados y gobernados por FeatureFlags. */
    private val computeBumpy: () -> Float = { 0f },
    private val computeTwirl: () -> Float = { 0f },
    private val computeRoll: () -> Float = { 0f },
    private val computeReceptorCenterY: (column: Int) -> Float = { 0f },

    private val computeRotation: (note: Parser.Note) -> Float = { 0f },

    private val cellMetrics: Array<GameScreenSsc.NoteCellMetrics>
) {

    /*
     * IMPORTANTE: debe inicializarse ANTES de crear cualquiera de los shaders
     * que lo reutilizan. Kotlin inicializa propiedades de instancia en orden
     * textual; si queda al final de la clase, createNormalFlipShader() lo lee
     * antes de inicializarse y ShaderProgram recibe vertexShader=null.
     */
    private val COMMON_VERTEX_SHADER = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;

            uniform mat4 u_projTrans;

            // Bumpy / Twirl / Roll
            uniform float u_bumpy;
            uniform float u_twirl;
            uniform float u_roll;
            uniform float u_stepmaniaYOffset;
            uniform float u_stepmaniaArrowScale;
            uniform float u_objectCenterX;
            uniform float u_objectCenterY;
            uniform float u_focalLength;

            varying vec4 v_color;
            varying vec2 v_texCoords;
            varying float v_worldY;

            const float DEG_TO_RAD = 0.017453292519943295;

            vec3 rotateX(vec3 p, float angle) {
                float c = cos(angle);
                float s = sin(angle);
                return vec3(
                    p.x,
                    p.y * c - p.z * s,
                    p.y * s + p.z * c
                );
            }

            vec3 rotateY(vec3 p, float angle) {
                float c = cos(angle);
                float s = sin(angle);
                return vec3(
                    p.x * c + p.z * s,
                    p.y,
                    -p.x * s + p.z * c
                );
            }

            void main() {
                v_color = a_color;
                v_color.a =
                    v_color.a *
                    (255.0 / 254.0);

                v_texCoords =
                    a_texCoord0;

                // Conservamos worldY original para AP/Vanish/Flip ribbon.
                v_worldY =
                    a_position.y;

                vec3 p = vec3(
                    a_position.x - u_objectCenterX,
                    a_position.y - u_objectCenterY,
                    0.0
                );

                // Este valor está en el mismo espacio lógico que ArrowEffects.cpp.
                // Ya incluye Boost/Expand/scrollSpeed y todavía no incluye Reverse/Tipsy.
                float yOffset = u_stepmaniaYOffset;

                // Bumpy usa una magnitud ligada a ARROW_SIZE. En StepMania
                // ARROW_SIZE=64; en Finger Dance 64 -> medidaFlechas.
                float bumpyZ =
                    u_bumpy *
                    40.0 *
                    sin(yOffset / 16.0) *
                    u_stepmaniaArrowScale;

                p.z += bumpyZ;

                // Los ángulos permanecen en grados StepMania: no se escalan por píxeles.
                float twirlAngle =
                    (u_twirl * yOffset * 0.5) *
                    DEG_TO_RAD;

                float rollAngle =
                    (u_roll * yOffset * 0.5) *
                    DEG_TO_RAD;

                p = rotateY(p, twirlAngle);
                p = rotateX(p, rollAngle);

                // Proyección perspectiva segura sobre el mismo playfield 2D.
                float focal =
                    max(u_focalLength, 1.0);

                float denom =
                    max(focal - p.z, focal * 0.20);

                float perspective =
                    clamp(
                        focal / denom,
                        0.35,
                        2.50
                    );

                vec2 finalPos =
                    vec2(
                        u_objectCenterX + p.x * perspective,
                        u_objectCenterY + p.y * perspective
                    );

                gl_Position =
                    u_projTrans *
                    vec4(
                        finalPos,
                        a_position.z,
                        a_position.w
                    );
            }
        """.trimIndent()

    private val normalFlipShader = createNormalFlipShader()
    private val appearFadeShader = createAppearFadeShader()
    private val vanishFadeShader = createVanishFadeShader()
    private val vanishMidLineFadeShader = createVanishMidLineFadeShader()

    /*
     * Ribbon 3D continuo para HOLD bodies.
     *
     * Antes cada segmento del body se enviaba como un SpriteBatch.draw separado
     * con un pivot/uniform distinto. Bumpy/Twirl/Roll podían separar físicamente
     * esas tiras y el HOLD terminaba viéndose "por cachos".
     *
     * Aquí todos los segmentos comparten vértices dentro de UN Mesh y se
     * renderizan en una sola llamada. La geometría queda continua.
     */
    private val holdRibbonShader = createHoldRibbonShader()
    private val holdRibbonCombinedMatrix = Matrix4()

    private companion object {
        const val HOLD_RIBBON_MAX_SEGMENTS = 64
    }

    private val holdRibbonMesh: Mesh =
        createHoldRibbonMesh()

    // TAP / MINE / HEAD / BOTTOM: una vuelta completa cada 6 alturas de flecha.
    // El BODY NO usa esta longitud: siempre hace una sola media vuelta de head a bottom.
    private val flipWaveLength = arrowSize * 6f

    /*
     * Se actualiza al entrar a drawTap/drawMine/drawHold.
     * Los fades NORMAL/VANISH/AP se multiplican por este valor.
     */
    private var activeNoteAlpha = 1f
    private var activeDepthScale = 1f
    private var activeColumn = 0
    private var activeSourceY = 0f
    private var activeHoldSourceStartY = 0f
    private var activeHoldSourceEndY = 0f

    /**
     * true mientras el engine mantiene una HOLD agarrada en el receptor.
     * Sólo modifica la geometría del HEAD/BODY de esa HOLD; no toca TAPs ni
     * la fórmula general de Reverse.
     */
    private var activeHoldAnchored = false

    // Sólo segmentamos bodies cuando alguno de estos tres ATTACKS está realmente activo.
    private val attack3DActive: Boolean
        get() =
            kotlin.math.abs(computeBumpy()) > 0.0001f ||
                    kotlin.math.abs(computeTwirl()) > 0.0001f ||
                    kotlin.math.abs(computeRoll()) > 0.0001f

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

    private fun appearanceAlphaAt(sourceY: Float): Float =
        computeAppearanceAlpha(
            activeColumn,
            sourceY
        ).coerceIn(0f, 1f)

    private fun withAppearanceAlpha(
        sourceY: Float,
        draw: () -> Unit
    ) {
        val previousColor = batch.color.cpy()
        val appearance =
            appearanceAlphaAt(sourceY)

        batch.setColor(
            previousColor.r,
            previousColor.g,
            previousColor.b,
            previousColor.a * appearance
        )

        try {
            draw()
        } finally {
            batch.color = previousColor
        }
    }

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
        activeColumn = column
        activeSourceY = y.toFloat()
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
        activeColumn = column
        activeSourceY = y.toFloat()
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
        isVanish: Boolean,
        isAnchored: Boolean = false
    ) {
        val previousColor = batch.color.cpy()
        activeColumn = column
        activeSourceY = y.toFloat()
        activeHoldSourceStartY = y.toFloat()
        activeHoldSourceEndY = y2.toFloat()
        activeHoldAnchored = isAnchored
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
            activeHoldAnchored = false
        }
    }

    /**
     * Posición lógica TOP del receptor real de la columna.
     *
     * computeReceptorCenterY() ya incluye Reverse/Split/Alternate/Cross/Tipsy.
     * Restamos media flecha porque SscNoteRenderer trabaja con Y lógico de TOP,
     * igual que getCellDraw()/drawReceptor.
     */
    private fun anchoredHoldHeadY(column: Int): Float =
        computeReceptorCenterY(column) - (arrowSize * 0.5f)

    /**
     * Recorte físico del BODY al viewport.
     *
     * NO es una regla de MidLine: evita dibujar/segmentar miles de píxeles
     * que están fuera de pantalla. tStart/tEnd conservan qué tramo de la
     * textura/cuerpo corresponde al fragmento visible.
     */
    private data class BodyViewportClip(
        val y: Float,
        val height: Float,
        val tStart: Float,
        val tEnd: Float
    )

    private fun clipBodyToViewport(
        y: Float,
        height: Float
    ): BodyViewportClip? {
        if (height <= 0f) return null

        val fullEnd =
            y + height

        val viewportTop = 0f
        val viewportBottom =
            Gdx.graphics.height.toFloat()

        val clippedStart =
            maxOf(
                y,
                viewportTop
            )

        val clippedEnd =
            minOf(
                fullEnd,
                viewportBottom
            )

        val clippedHeight =
            clippedEnd - clippedStart

        if (clippedHeight <= 0f) {
            return null
        }

        val tStart =
            ((clippedStart - y) / height)
                .coerceIn(0f, 1f)

        val tEnd =
            ((clippedEnd - y) / height)
                .coerceIn(0f, 1f)

        return BodyViewportClip(
            y = clippedStart,
            height = clippedHeight,
            tStart = tStart,
            tEnd = tEnd
        )
    }

    private fun isLogicalSpriteInsideViewport(
        logicalY: Float
    ): Boolean {
        val screenHeight =
            Gdx.graphics.height.toFloat()

        return logicalY + arrowSize > 0f &&
                logicalY < screenHeight
    }

    private fun drawRegionSlice(
        region: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        tStart: Float,
        tEnd: Float
    ) {
        val texture = region.texture
        val u1 = region.u
        val u2 = region.u2
        val v1 =
            region.v +
                    (region.v2 - region.v) *
                    tStart
        val v2 =
            region.v +
                    (region.v2 - region.v) *
                    tEnd

        batch.draw(
            texture,
            x,
            y,
            width,
            height,
            u1,
            v1,
            u2,
            v2
        )
    }

    private fun drawFlipRegion(
        region: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Float = 0f,
        reverseY: Boolean = true,
        allowRoll: Boolean = true,
        sourceY: Float = activeSourceY
    ) {
        val miniScale = computeScale() * activeDepthScale

        /*
         * StepMania Reverse modifica GetYPos, no el zoomY del sprite.
         * Antes computeScaleY=1-2*Reverse hacía que toda pieza desapareciera
         * verticalmente al cruzar Reverse=0.5 y luego se dibujara invertida.
         * Eso era especialmente destructivo durante ATTACK Approach.
         */
        val spriteScaleY = 1f
        val centerY = y + height * 0.5f

        withAppearanceAlpha(sourceY) {
            if (!luaNotes.flipX && !attack3DActive) {
                batch.draw(
                    region,
                    x,
                    y,
                    width * 0.5f,
                    height * 0.5f,
                    width,
                    height,
                    miniScale,
                    miniScale * spriteScaleY,
                    rotation
                )
                return@withAppearanceAlpha
            }

            val previousShader = batch.shader
            batch.shader = normalFlipShader

            applyFlipUniforms(
                shader = normalFlipShader,
                region = region,
                ribbon = false,
                objectCenterY = centerY,
                objectCenterX = x + width * 0.5f,
                allowRoll = allowRoll,
                stepManiaYOffset = computeStepManiaYOffset(sourceY)
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
                miniScale * spriteScaleY,
                rotation
            )

            batch.shader = previousShader
        }
    }

    private fun createHoldRibbonMesh(): Mesh {
        val vertexCount =
            (HOLD_RIBBON_MAX_SEGMENTS + 1) * 2

        val indexCount =
            HOLD_RIBBON_MAX_SEGMENTS * 6

        val mesh =
            Mesh(
                false,
                vertexCount,
                indexCount,
                VertexAttribute(
                    VertexAttributes.Usage.Position,
                    3,
                    ShaderProgram.POSITION_ATTRIBUTE
                ),
                VertexAttribute(
                    VertexAttributes.Usage.TextureCoordinates,
                    2,
                    ShaderProgram.TEXCOORD_ATTRIBUTE + "0"
                ),
                VertexAttribute(
                    VertexAttributes.Usage.Generic,
                    1,
                    "a_yOffset"
                ),
                VertexAttribute(
                    VertexAttributes.Usage.Generic,
                    1,
                    "a_alpha"
                )
            )

        val indices =
            ShortArray(indexCount)

        var p = 0

        for (i in 0 until HOLD_RIBBON_MAX_SEGMENTS) {
            val topLeft = (i * 2).toShort()
            val topRight = (i * 2 + 1).toShort()
            val bottomLeft = ((i + 1) * 2).toShort()
            val bottomRight = ((i + 1) * 2 + 1).toShort()

            indices[p++] = topLeft
            indices[p++] = bottomLeft
            indices[p++] = topRight

            indices[p++] = topRight
            indices[p++] = bottomLeft
            indices[p++] = bottomRight
        }

        mesh.setIndices(indices)
        return mesh
    }

    /**
     * HOLD body continuo para Bumpy/Twirl/Roll.
     *
     * - un solo Mesh;
     * - vértices compartidos entre segmentos;
     * - yOffset SM propio por fila;
     * - alpha de appearance propio por fila;
     * - sin batch.flush() por segmento.
     */
    private fun drawContinuous3DBody(
        region: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        fullBodyY: Float,
        fullBodyHeight: Float,
        sourceTStart: Float,
        sourceTEnd: Float
    ) {
        if (height <= 0f) return

        val targetSegmentHeight =
            (arrowSize / 6f)
                .coerceAtLeast(3f)

        val segmentCount =
            kotlin.math.ceil(
                height / targetSegmentHeight
            )
                .toInt()
                .coerceIn(
                    2,
                    HOLD_RIBBON_MAX_SEGMENTS
                )

        // x,y,z,u,v,yOffset,alpha = 7 floats por vértice.
        val floatsPerVertex = 7
        val vertexCount = (segmentCount + 1) * 2
        val vertices =
            FloatArray(
                vertexCount * floatsPerVertex
            )

        val uLeft = region.u
        val uRight = region.u2
        val vTop = region.v
        val vBottom = region.v2

        var p = 0

        for (row in 0..segmentCount) {
            val localT =
                row.toFloat() /
                        segmentCount.toFloat()

            val sourceT =
                sourceTStart +
                        (sourceTEnd - sourceTStart) *
                        localT

            val worldY =
                y +
                        height *
                        localT

            val sourceY =
                activeHoldSourceStartY +
                        (
                            activeHoldSourceEndY -
                                activeHoldSourceStartY
                            ) *
                        sourceT

            val yOffset =
                computeStepManiaYOffset(
                    sourceY
                )

            val rowAlpha =
                appearanceAlphaAt(
                    sourceY
                )

            val v =
                vTop +
                        (vBottom - vTop) *
                        sourceT

            // LEFT
            vertices[p++] = x
            vertices[p++] = worldY
            vertices[p++] = 0f
            vertices[p++] = uLeft
            vertices[p++] = v
            vertices[p++] = yOffset
            vertices[p++] = rowAlpha

            // RIGHT
            vertices[p++] = x + width
            vertices[p++] = worldY
            vertices[p++] = 0f
            vertices[p++] = uRight
            vertices[p++] = v
            vertices[p++] = yOffset
            vertices[p++] = rowAlpha
        }

        holdRibbonMesh.setVertices(
            vertices,
            0,
            vertices.size
        )

        /*
         * El Mesh se dibuja dentro de un SpriteBatch.begin().
         * Vaciamos una sola vez, renderizamos el ribbon y volvemos a dejar
         * ligado el shader de SpriteBatch.
         */
        batch.flush()

        region.texture.bind(0)

        holdRibbonShader.bind()

        holdRibbonCombinedMatrix
            .set(batch.projectionMatrix)
            .mul(batch.transformMatrix)

        holdRibbonShader.setUniformMatrix(
            "u_projTrans",
            holdRibbonCombinedMatrix
        )

        holdRibbonShader.setUniformi(
            "u_texture",
            0
        )

        val color = batch.color
        holdRibbonShader.setUniformf(
            "u_color",
            color.r,
            color.g,
            color.b,
            color.a
        )

        holdRibbonShader.setUniformf(
            "u_bumpy",
            computeBumpy()
        )

        holdRibbonShader.setUniformf(
            "u_twirl",
            computeTwirl()
        )

        holdRibbonShader.setUniformf(
            "u_roll",
            computeRoll()
        )

        holdRibbonShader.setUniformf(
            "u_stepmaniaArrowScale",
            computeStepManiaArrowScale()
        )

        holdRibbonShader.setUniformf(
            "u_objectCenterX",
            x + width * 0.5f
        )

        holdRibbonShader.setUniformf(
            "u_objectCenterY",
            y + height * 0.5f
        )

        holdRibbonShader.setUniformf(
            "u_focalLength",
            480f *
                    computeStepManiaFieldScaleY()
                        .coerceAtLeast(0.0001f)
        )

        holdRibbonShader.setUniformi(
            "u_flipEnabled",
            if (luaNotes.flipX) 1 else 0
        )

        holdRibbonShader.setUniformf(
            "u_bodyY",
            fullBodyY
        )

        holdRibbonShader.setUniformf(
            "u_bodyHeight",
            fullBodyHeight.coerceAtLeast(0.0001f)
        )

        holdRibbonShader.setUniformf(
            "u_regionU",
            region.u
        )

        holdRibbonShader.setUniformf(
            "u_regionU2",
            region.u2
        )

        holdRibbonMesh.render(
            holdRibbonShader,
            GL20.GL_TRIANGLES,
            0,
            segmentCount * 6
        )

        /*
         * Mesh.render deja su shader ligado. SpriteBatch ya tenía sus matrices
         * preparadas, así que basta con volver a ligar el shader anterior.
         */
        batch.shader.bind()
    }

    private fun drawFlipBody(
        region: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        fullBodyY: Float = y,
        fullBodyHeight: Float = height,
        sourceTStart: Float = 0f,
        sourceTEnd: Float = 1f
    ) {
        if (height <= 0f) return

        val miniScale =
            computeScale() *
                    activeDepthScale

        val scaledWidth =
            width * miniScale

        val centeredX =
            x +
                    (width - scaledWidth) *
                    0.5f

        /*
         * El path 3D usa un ribbon continuo. No volvemos a separar el body
         * en SpriteBatch.draw independientes.
         */
        if (attack3DActive) {
            drawContinuous3DBody(
                region = region,
                x = centeredX,
                y = y,
                width = scaledWidth,
                height = height,
                fullBodyY = fullBodyY,
                fullBodyHeight = fullBodyHeight,
                sourceTStart = sourceTStart,
                sourceTEnd = sourceTEnd
            )
            return
        }

        if (
            !luaNotes.flipX &&
            !computeAppearanceActive()
        ) {
            withAppearanceAlpha(activeSourceY) {
                drawRegionSlice(
                    region = region,
                    x = centeredX,
                    y = y,
                    width = scaledWidth,
                    height = height,
                    tStart = sourceTStart,
                    tEnd = sourceTEnd
                )
            }
            return
        }

        val previousShader =
            batch.shader

        batch.shader =
            normalFlipShader

        drawBodyPossiblySegmented(
            region = region,
            x = centeredX,
            y = y,
            width = scaledWidth,
            height = height,
            fullBodyY = fullBodyY,
            fullBodyHeight = fullBodyHeight,
            shader = normalFlipShader,
            sourceTStart = sourceTStart,
            sourceTEnd = sourceTEnd
        )

        batch.shader =
            previousShader
    }

    /**
     * Divide un HOLD body en tiras verticales cuando Bumpy/Twirl/Roll están activos.
     * El shader transforma cada tira en el mismo espacio 3D, evitando que head/body/bottom
     * tengan fórmulas distintas.
     */
    private fun drawBodyPossiblySegmented(
        region: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        fullBodyY: Float,
        fullBodyHeight: Float,
        shader: ShaderProgram,
        sourceTStart: Float = 0f,
        sourceTEnd: Float = 1f
    ) {
        if (height <= 0f) return

        if (!attack3DActive && !computeAppearanceActive()) {
            val centerY = y + height * 0.5f

            applyFlipUniforms(
                shader = shader,
                region = region,
                ribbon = true,
                objectCenterY = centerY,
                objectCenterX = x + width * 0.5f,
                bodyY = fullBodyY,
                bodyHeight = fullBodyHeight
            )

            withAppearanceAlpha(activeSourceY) {
                drawRegionSlice(
                    region = region,
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    tStart = sourceTStart,
                    tEnd = sourceTEnd
                )
            }
            return
        }

        /*
         * El shader necesita muestrear el BODY porque Bumpy/Twirl/Roll dependen
         * de fYOffset, pero un flush por cada 1/8 de flecha es demasiado caro
         * en Android. 1/4 conserva una curva suficientemente suave y reduce
         * aproximadamente a la mitad los flushes.
         */
        val targetSegmentHeight =
            (arrowSize / 4f).coerceAtLeast(6f)

        val segments =
            kotlin.math.ceil(height / targetSegmentHeight)
                .toInt()
                /*
                 * Guardrail para charts con HOLDs enormes + Bumpy/Twirl/Roll.
                 * Tras el clipping normalmente ya será pequeño; 48 evita un
                 * flush-storm accidental sin alterar la geometría visible.
                 */
                .coerceIn(1, 48)

        val texture = region.texture
        val u1 = region.u
        val u2 = region.u2
        val vTop = region.v
        val vBottom = region.v2

        for (i in 0 until segments) {
            val localT0 =
                i.toFloat() /
                        segments.toFloat()

            val localT1 =
                (i + 1).toFloat() /
                        segments.toFloat()

            val t0 =
                sourceTStart +
                        (sourceTEnd - sourceTStart) *
                        localT0

            val t1 =
                sourceTStart +
                        (sourceTEnd - sourceTStart) *
                        localT1

            val sy =
                y +
                        height *
                        localT0
            // pequeño solape para que el filtrado lineal no abra costuras visuales.
            val sh =
                (height * (localT1 - localT0) + 0.75f)
                    .coerceAtMost(
                        y + height - sy + 0.75f
                    )

            val sv1 = vTop + (vBottom - vTop) * t0
            val sv2 = vTop + (vBottom - vTop) * t1

            // Cada segmento usa un pivot distinto; hay que vaciar SpriteBatch
            // antes de cambiar uniforms o todos usarían los uniforms del último segmento.
            batch.flush()

            val sourceT = (t0 + t1) * 0.5f
            val segmentSourceY =
                activeHoldSourceStartY +
                        (activeHoldSourceEndY - activeHoldSourceStartY) * sourceT

            applyFlipUniforms(
                shader = shader,
                region = region,
                ribbon = true,
                objectCenterY = sy + sh * 0.5f,
                objectCenterX = x + width * 0.5f,
                bodyY = fullBodyY,
                bodyHeight = fullBodyHeight,
                stepManiaYOffset = computeStepManiaYOffset(segmentSourceY)
            )

            withAppearanceAlpha(segmentSourceY) {
                batch.draw(
                    texture,
                    x,
                    sy,
                    width,
                    sh,
                    u1,
                    sv1,
                    u2,
                    sv2
                )
            }
        }
    }

    private fun applyFlipUniforms(
        shader: ShaderProgram,
        region: TextureRegion,
        ribbon: Boolean,
        objectCenterY: Float,
        objectCenterX: Float = 0f,
        bodyY: Float = 0f,
        bodyHeight: Float = 1f,
        allowRoll: Boolean = true,
        stepManiaYOffset: Float = computeStepManiaYOffset(activeSourceY)
    ) {
        shader.setUniformi("u_flipEnabled", if (luaNotes.flipX) 1 else 0)
        shader.setUniformi("u_flipRibbon", if (ribbon) 1 else 0)
        shader.setUniformf("u_flipWaveLength", flipWaveLength)
        shader.setUniformf("u_flipCenterY", objectCenterY)
        shader.setUniformf("u_bodyY", bodyY)
        shader.setUniformf("u_bodyHeight", bodyHeight.coerceAtLeast(0.001f))
        shader.setUniformf("u_regionU", region.u)
        shader.setUniformf("u_regionU2", region.u2)

        applyAttack3DUniforms(
            shader = shader,
            objectCenterX = objectCenterX,
            objectCenterY = objectCenterY,
            allowRoll = allowRoll,
            stepManiaYOffset = stepManiaYOffset
        )
    }

    private fun applyAttack3DUniforms(
        shader: ShaderProgram,
        objectCenterX: Float,
        objectCenterY: Float,
        allowRoll: Boolean,
        stepManiaYOffset: Float
    ) {
        val bumpy = computeBumpy()
        val twirl = computeTwirl()
        val roll = computeRoll()

        shader.setUniformf("u_bumpy", bumpy)
        shader.setUniformf("u_twirl", twirl)
        val fieldScaleY =
            computeStepManiaFieldScaleY().coerceAtLeast(0.0001f)

        val arrowScale =
            computeStepManiaArrowScale().coerceAtLeast(0.0001f)

        shader.setUniformf("u_roll", if (allowRoll) roll else 0f)
        shader.setUniformf("u_stepmaniaYOffset", stepManiaYOffset)
        shader.setUniformf("u_stepmaniaArrowScale", arrowScale)
        shader.setUniformf("u_objectCenterX", objectCenterX)
        shader.setUniformf("u_objectCenterY", objectCenterY)
        shader.setUniformf(
            "u_focalLength",
            480f * fieldScaleY
        )
    }

    // -------------------------------------------------------------------------
    // HOLD NORMAL
    // -------------------------------------------------------------------------
    private fun drawLongNoteNormal(column: Int, y: Int, y2: Int, frame: Int, rotation: Float) {
        val logicalX = computeLeft(column, y)
        val transformedHeadY = transformedY(column, y)
        val headY =
            if (activeHoldAnchored) {
                anchoredHoldHeadY(column)
            } else {
                transformedHeadY
            }
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
                    height = bottom.height,
                    allowRoll = false,
                    sourceY = y2.toFloat()
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
                    rotation = rotation,
                    allowRoll = false,
                    sourceY = y.toFloat()
                )
            }

            resetColor()
        } else {
            /*
             * MISMA geometría que MidLine, distinta regla de visibilidad:
             * sin fade/limit de initArrow, pero nunca procesamos geometría
             * que está completamente fuera de la pantalla física.
             */
            val clip =
                clipBodyToViewport(
                    y = bodyStartY,
                    height = bodyHeight
                )

            if (clip != null) {
                drawFlipBody(
                    region = bodies[column][frame],
                    x = leftBody,
                    y = clip.y,
                    width = widthBody,
                    height = clip.height,
                    fullBodyY = bodyStartY,
                    fullBodyHeight = bodyHeight,
                    sourceTStart = clip.tStart,
                    sourceTEnd = clip.tEnd
                )
            }

            if (
                remainingLength > heightBodyHead &&
                isLogicalSpriteInsideViewport(bottomY)
            ) {
                drawFlipRegion(
                    region = bottoms[column][frame],
                    x = bottom.x,
                    y = bottom.y,
                    width = bottom.width,
                    height = bottom.height,
                    allowRoll = false,
                    sourceY = y2.toFloat()
                )
            }

            if (
                isLogicalSpriteInsideViewport(headY)
            ) {
                drawFlipRegion(
                    region = arrows[column][frame],
                    x = head.x,
                    y = head.y,
                    width = head.width,
                    height = head.height,
                    rotation = rotation,
                    allowRoll = false,
                    sourceY = y.toFloat()
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // HOLD VANISH
    // -------------------------------------------------------------------------
    private fun drawLongNoteVanish(column: Int, y: Int, y2: Int, frame: Int, rotation: Float) {
        val logicalX = computeLeft(column, y)
        val transformedHeadY = transformedY(column, y)
        val headY =
            if (activeHoldAnchored) {
                anchoredHoldHeadY(column)
            } else {
                transformedHeadY
            }
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
                    height = bottom.height,
                    allowRoll = false,
                    sourceY = y2.toFloat()
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
                    rotation = rotation,
                    allowRoll = false,
                    sourceY = y.toFloat()
                )
            }

            resetColor()
        } else {
            val clip =
                clipBodyToViewport(
                    y = bodyStartY,
                    height = bodyHeight
                )

            if (clip != null) {
                drawWithVanishShader(
                    region = bodies[column][frame],
                    x = leftBody,
                    y = clip.y,
                    width = widthBody,
                    height = clip.height,
                    fadeEnd = vanishBodyFadeEnd,
                    fullBodyY = bodyStartY,
                    fullBodyHeight = bodyHeight,
                    sourceTStart = clip.tStart,
                    sourceTEnd = clip.tEnd
                )
            }

            if (
                remainingLength > heightBodyHead &&
                bottomY > measureVanish &&
                isLogicalSpriteInsideViewport(bottomY)
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
                    height = bottom.height,
                    allowRoll = false,
                    sourceY = y2.toFloat()
                )
            }

            if (
                headY > measureVanish &&
                isLogicalSpriteInsideViewport(headY)
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
                    rotation = rotation,
                    allowRoll = false,
                    sourceY = y.toFloat()
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
        val transformedHeadY = transformedY(column, y)
        val headY =
            if (activeHoldAnchored) {
                anchoredHoldHeadY(column)
            } else {
                transformedHeadY
            }
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
                height = bottom.height,
                allowRoll = false,
                sourceY = y2.toFloat()
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
                rotation = rotation,
                allowRoll = false,
                sourceY = y.toFloat()
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
            if (isLogicalSpriteInsideViewport(finalY)) {
                drawFlipRegion(
                    region = arrows[column][frame],
                    x = draw.x,
                    y = draw.y,
                    width = draw.width,
                    height = draw.height,
                    rotation = rotation
                )
            }
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
            if (
                finalY > measureVanish &&
                isLogicalSpriteInsideViewport(finalY)
            ) {
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
            objectCenterX = x + width * 0.5f,
            bodyY = y,
            bodyHeight = height
        )

        resetColor()

        val bodyCenterY = y + height * 0.5f

        withAppearanceAlpha(bodyCenterY) {
            batch.draw(
                region,
                centeredX,
                y,
                scaledWidth,
                height
            )
        }

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
        fadeEnd: Float,
        fullBodyY: Float = y,
        fullBodyHeight: Float = height,
        sourceTStart: Float = 0f,
        sourceTEnd: Float = 1f
    ) {
        if (height <= 0f) return

        val miniScale = computeScale()
        val scaledWidth = width * miniScale
        val centeredX = x + (width - scaledWidth) * 0.5f

        val previousShader = batch.shader
        batch.shader = vanishFadeShader

        vanishFadeShader.setUniformf("u_fadeEnd", fadeEnd)
        vanishFadeShader.setUniformf("u_fadeRange", rangeAlpha)
        resetColor()
        drawBodyPossiblySegmented(
            region = region,
            x = centeredX,
            y = y,
            width = scaledWidth,
            height = height,
            fullBodyY = fullBodyY,
            fullBodyHeight = fullBodyHeight,
            shader = vanishFadeShader,
            sourceTStart = sourceTStart,
            sourceTEnd = sourceTEnd
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

        val miniScale = computeScale()
        val scaledWidth = width * miniScale
        val centeredX = x + (width - scaledWidth) * 0.5f

        val previousShader = batch.shader
        batch.shader = vanishMidLineFadeShader

        vanishMidLineFadeShader.setUniformf("u_appearLimit", appearLimit)
        vanishMidLineFadeShader.setUniformf("u_vanishEnd", vanishEnd)
        vanishMidLineFadeShader.setUniformf("u_fadeRange", rangeAlpha)
        resetColor()
        drawBodyPossiblySegmented(
            region = region,
            x = centeredX,
            y = y,
            width = scaledWidth,
            height = height,
            fullBodyY = y,
            fullBodyHeight = height,
            shader = vanishMidLineFadeShader
        )
        batch.shader = previousShader
    }

    // -------------------------------------------------------------------------
    // CREACION DE SHADERS
    // -------------------------------------------------------------------------

    private fun createHoldRibbonShader(): ShaderProgram {
        val vertexShader =
            """
        attribute vec3 a_position;
        attribute vec2 a_texCoord0;
        attribute float a_yOffset;
        attribute float a_alpha;

        uniform mat4 u_projTrans;

        varying vec2 v_texCoords;
        varying float v_worldY;
        varying float v_alpha;

        void main() {
            v_texCoords = a_texCoord0;
            v_worldY = a_position.y;
            v_alpha = a_alpha;

            /*
             * IMPORTANTE:
             * Para los HOLD bodies NO aplicamos Bumpy/Twirl/Roll.
             *
             * Eso evita:
             * - serpenteo lateral
             * - efecto reloj de arena
             * - torsiones raras al final del chart
             *
             * El mesh continuo se conserva, así que ya no se verá por cachos.
             */
            gl_Position =
                u_projTrans *
                vec4(
                    a_position.xy,
                    a_position.z,
                    1.0
                );
        }
        """.trimIndent()

        val fragmentShader =
            """
        #ifdef GL_ES
        precision mediump float;
        #endif

        varying vec2 v_texCoords;
        varying float v_worldY;
        varying float v_alpha;

        uniform sampler2D u_texture;
        uniform vec4 u_color;

        uniform int u_flipEnabled;
        uniform float u_bodyY;
        uniform float u_bodyHeight;
        uniform float u_regionU;
        uniform float u_regionU2;

        vec2 applyFlip(
            vec2 uv,
            float worldY,
            out float visible
        ) {
            visible = 1.0;

            if (u_flipEnabled == 0) {
                return uv;
            }

            float safeBodyHeight =
                max(
                    u_bodyHeight,
                    0.0001
                );

            float t =
                clamp(
                    (worldY - u_bodyY) /
                        safeBodyHeight,
                    0.0,
                    1.0
                );

            float flipScale =
                cos(
                    t *
                    3.14159265359
                );

            float widthScale =
                max(
                    abs(flipScale),
                    0.015
                );

            float regionWidth =
                u_regionU2 -
                u_regionU;

            if (
                abs(regionWidth) <
                0.000001
            ) {
                return uv;
            }

            float localX =
                (uv.x - u_regionU) /
                regionWidth;

            float centeredX =
                localX - 0.5;

            if (
                abs(centeredX) >
                0.5 * widthScale
            ) {
                visible = 0.0;
                return uv;
            }

            float remappedX =
                centeredX /
                widthScale +
                0.5;

            if (flipScale < 0.0) {
                remappedX =
                    1.0 -
                    remappedX;
            }

            float finalU =
                u_regionU +
                remappedX *
                regionWidth;

            return vec2(
                finalU,
                uv.y
            );
        }

        void main() {
            float visible;

            vec2 uv =
                applyFlip(
                    v_texCoords,
                    v_worldY,
                    visible
                );

            vec4 texColor =
                texture2D(
                    u_texture,
                    uv
                );

            gl_FragColor =
                vec4(
                    texColor.rgb *
                        u_color.rgb,
                    texColor.a *
                        u_color.a *
                        v_alpha *
                        visible
                );
        }
        """.trimIndent()

        return compileShader(
            "HoldRibbonShader",
            vertexShader,
            fragmentShader
        )
    }

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
        holdRibbonShader.dispose()
        holdRibbonMesh.dispose()
    }
}
