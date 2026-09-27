package com.fingerdance.ssc

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

class PerspectivePlayfieldRenderer(
    width: Int,
    height: Int,
    pivotX: Float,
    private val segments: Int = 96
) {

    // =========================================================
    // NX / PERSPECTIVA BASE
    // =========================================================

    var progress = 0f
        set(value) {
            val newValue = value.coerceIn(0f, 1f)
            if (field == newValue) return
            field = newValue
            updateMesh()
        }

    var topScaleX = 0.42f
        set(value) {
            if (field == value) return
            field = value
            updateMesh()
        }

    var topShiftXPercent = 0.00f
        set(value) {
            if (field == value) return
            field = value
            updateMesh()
        }

    var horizontalPower = 1.30f
        set(value) {
            if (field == value) return
            field = value
            updateMesh()
        }

    var verticalPower = 1.80f
        set(value) {
            if (field == value) return
            field = value
            updateMesh()
        }

    var meshOffsetY = height * 0.08f
        set(value) {
            if (field == value) return
            field = value
            updateMesh()
        }

    // =========================================================
    // ATTACK PERSPECTIVE
    // =========================================================

    /*
     * attackSkew genera una deformación centrada alrededor de pivotX.
     *
     * No trasladamos todo el playfield lateralmente.
     * La deformación:
     *
     * t = 0   -> 0
     * t = .5  -> máxima
     * t = 1   -> 0
     *
     * Esto conserva los extremos del playfield y evita que el receptor
     * se desplace por una transformación global.
     */
    var attackSkew = 0f
        set(value) {
            if (field == value) return
            field = value
            updateMesh()
        }

    /*
     * Se conserva porque AttackState lo usa para:
     *
     * Space
     * Incoming
     * Hallway
     * Distant
     *
     * IMPORTANTE:
     * attackTilt NO modifica Y en esta implementación.
     *
     * En la revisión donde los receptores funcionaban correctamente,
     * Tilt estaba neutralizado dentro del mesh.
     */
    var attackTilt = 0f
        set(value) {
            if (field == value) return
            field = value
            updateMesh()
        }

    /*
     * Intensidad de la curvatura horizontal ATTACK.
     */
    var attackCurveAmount = 0.30f
        set(value) {
            if (field == value) return
            field = value
            updateMesh()
        }

    /*
     * Forma de la curva:
     *
     * 1.0 = seno
     * >1  = concentra más el efecto hacia la zona media.
     */
    var attackCurvePower = 1.0f
        set(value) {
            val newValue =
                value.coerceAtLeast(0.01f)

            if (field == newValue) return

            field = newValue
            updateMesh()
        }

    // =========================================================
    // ESTADO INTERNO
    // =========================================================

    private var width = width
    private var height = height
    private var pivotX = pivotX

    private var frameBuffer =
        createFrameBuffer(
            width,
            height
        )

    private val shader =
        createShader()

    private var mesh =
        createMesh()

    init {
        require(segments >= 1)
        updateMesh()
    }

    // =========================================================
    // FRAMEBUFFER
    // =========================================================

    fun begin() {
        frameBuffer.begin()

        Gdx.gl.glClearColor(
            0f,
            0f,
            0f,
            0f
        )

        Gdx.gl.glClear(
            GL20.GL_COLOR_BUFFER_BIT
        )
    }

    fun end() {
        frameBuffer.end()
    }

    // =========================================================
    // DRAW
    // =========================================================

    fun draw(
        projectionMatrix: Matrix4
    ) {
        val texture =
            frameBuffer.colorBufferTexture

        Gdx.gl.glDisable(
            GL20.GL_DEPTH_TEST
        )

        Gdx.gl.glEnable(
            GL20.GL_BLEND
        )

        Gdx.gl.glBlendFunc(
            GL20.GL_SRC_ALPHA,
            GL20.GL_ONE_MINUS_SRC_ALPHA
        )

        texture.bind(0)

        shader.bind()

        shader.setUniformMatrix(
            "u_projTrans",
            projectionMatrix
        )

        shader.setUniformi(
            "u_texture",
            0
        )

        mesh.render(
            shader,
            GL20.GL_TRIANGLES
        )
    }

    // =========================================================
    // RESIZE
    // =========================================================

    fun resize(
        width: Int,
        height: Int,
        pivotX: Float = this.pivotX
    ) {
        if (
            width <= 0 ||
            height <= 0
        ) {
            return
        }

        this.width =
            width

        this.height =
            height

        this.pivotX =
            pivotX

        frameBuffer.dispose()

        frameBuffer =
            createFrameBuffer(
                width,
                height
            )

        mesh.dispose()

        mesh =
            createMesh()

        updateMesh()
    }

    // =========================================================
    // FRAMEBUFFER CREATION
    // =========================================================

    private fun createFrameBuffer(
        width: Int,
        height: Int
    ): FrameBuffer {
        return FrameBuffer(
            Pixmap.Format.RGBA8888,
            width,
            height,
            false
        ).also {
            it.colorBufferTexture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
            )
        }
    }

    // =========================================================
    // MESH CREATION
    // =========================================================

    private fun createMesh(): Mesh {
        val vertexCount =
            (segments + 1) * 2

        val indexCount =
            segments * 6

        val result =
            Mesh(
                true,
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
                )
            )

        val indices =
            ShortArray(
                indexCount
            )

        var p = 0

        for (
        i in
        0 until segments
        ) {
            val topLeft =
                (i * 2)
                    .toShort()

            val topRight =
                (i * 2 + 1)
                    .toShort()

            val bottomLeft =
                ((i + 1) * 2)
                    .toShort()

            val bottomRight =
                ((i + 1) * 2 + 1)
                    .toShort()

            indices[p++] =
                topLeft

            indices[p++] =
                bottomLeft

            indices[p++] =
                topRight

            indices[p++] =
                topRight

            indices[p++] =
                bottomLeft

            indices[p++] =
                bottomRight
        }

        result.setIndices(
            indices
        )

        return result
    }

    // =========================================================
    // MESH UPDATE
    // =========================================================

    private fun updateMesh() {
        if (
            width <= 0 ||
            height <= 0
        ) {
            return
        }

        val vertices =
            FloatArray(
                (segments + 1) *
                        2 *
                        5
            )

        val nxTopShiftPx =
            width *
                    topShiftXPercent

        var p = 0

        for (
        row in
        0..segments
        ) {
            val t =
                row.toFloat() /
                        segments.toFloat()

            /*
             * 0 = arriba
             * 1 = abajo
             */
            val depth =
                1f - t

            // =================================================
            // NX HORIZONTAL
            // =================================================

            val horizontalEffect =
                depth.pow(
                    horizontalPower
                        .coerceAtLeast(
                            0.01f
                        )
                )

            val perspectiveScaleX =
                1f -
                        (
                                (1f - topScaleX) *
                                        horizontalEffect
                                )

            /*
             * progress:
             *
             * 0 = NX apagado
             * 1 = NX completo
             */
            val nxScaleX =
                1f +
                        (
                                perspectiveScaleX -
                                        1f
                                ) *
                        progress

            val nxShiftX =
                nxTopShiftPx *
                        horizontalEffect *
                        progress

            // =================================================
            // ATTACK SKEW
            // =================================================

            /*
             * Curva centrada:
             *
             * t=0   -> 0
             * t=.5  -> 1
             * t=1   -> 0
             *
             * Muy importante:
             * el receptor está cerca de uno de los extremos del mesh,
             * por eso el ATTACK no lo arrastra fuera de pantalla.
             */
            val rawSphereCurve =
                sin(
                    PI.toFloat() *
                            t
                )
                    .coerceAtLeast(
                        0f
                    )

            val sphereCurve =
                rawSphereCurve.pow(
                    attackCurvePower
                )

            val attackScaleX =
                1f +
                        (
                                attackSkew *
                                        attackCurveAmount *
                                        sphereCurve
                                )

            /*
             * attackTilt permanece neutralizado.
             *
             * No usar:
             *
             * 1 - (0.45f * attackTilt * depth)
             *
             * Tampoco usar attackTilt para modificar Y.
             *
             * Es precisamente lo que queremos evitar para que
             * Hallway / Distant / Incoming no saquen el receptor
             * del viewport.
             */
            val attackTiltScaleX =
                1f

            // =================================================
            // X FINAL
            // =================================================

            val finalScaleX =
                nxScaleX *
                        attackScaleX *
                        attackTiltScaleX

            /*
             * No existe attackShiftX.
             *
             * Sólo conservamos el shift propio de NX.
             */
            val finalShiftX =
                nxShiftX

            val leftX =
                pivotX +
                        (
                                0f -
                                        pivotX
                                ) *
                        finalScaleX +
                        finalShiftX

            val rightX =
                pivotX +
                        (
                                width.toFloat() -
                                        pivotX
                                ) *
                        finalScaleX +
                        finalShiftX

            // =================================================
            // Y
            // =================================================

            val fullPerspectiveT =
                t.pow(
                    verticalPower
                        .coerceAtLeast(
                            0.01f
                        )
                )

            /*
             * CRÍTICO:
             *
             * Sólo NX modifica la distribución vertical.
             *
             * Los ATTACKS:
             *
             * Hallway
             * Distant
             * Incoming
             * Space
             *
             * NO modifican Y aquí.
             *
             * Reverse sigue manejándose fuera de este mesh,
             * en la geometría de notas/receptor de GameScreenSsc.
             */
            val perspectiveT =
                t +
                        (
                                fullPerspectiveT -
                                        t
                                ) *
                        progress

            val y =
                height *
                        perspectiveT +
                        meshOffsetY *
                        progress

            /*
             * FrameBuffer LibGDX:
             * coordenada V invertida.
             */
            val v =
                1f - t

            // LEFT
            vertices[p++] =
                leftX

            vertices[p++] =
                y

            vertices[p++] =
                0f

            vertices[p++] =
                0f

            vertices[p++] =
                v

            // RIGHT
            vertices[p++] =
                rightX

            vertices[p++] =
                y

            vertices[p++] =
                0f

            vertices[p++] =
                1f

            vertices[p++] =
                v
        }

        mesh.setVertices(
            vertices
        )
    }

    // =========================================================
    // SHADER
    // =========================================================

    private fun createShader(): ShaderProgram {
        ShaderProgram.pedantic =
            false

        val vertexShader =
            """
            attribute vec4 a_position;
            attribute vec2 a_texCoord0;

            uniform mat4 u_projTrans;

            varying vec2 v_texCoords;

            void main() {
                v_texCoords =
                    a_texCoord0;

                gl_Position =
                    u_projTrans *
                    a_position;
            }
            """.trimIndent()

        val fragmentShader =
            """
            #ifdef GL_ES
            precision mediump float;
            #endif

            varying vec2 v_texCoords;

            uniform sampler2D u_texture;

            void main() {
                gl_FragColor =
                    texture2D(
                        u_texture,
                        v_texCoords
                    );
            }
            """.trimIndent()

        return ShaderProgram(
            vertexShader,
            fragmentShader
        ).also { shader ->
            if (
                !shader.isCompiled
            ) {
                throw IllegalStateException(
                    "No se pudo compilar PerspectivePlayfieldRenderer: ${shader.log}"
                )
            }
        }
    }

    // =========================================================
    // DISPOSE
    // =========================================================

    fun dispose() {
        mesh.dispose()
        shader.dispose()
        frameBuffer.dispose()
    }
}