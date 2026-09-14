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
     * IMPORTANTE:
     *
     * Antes attackSkew se convertía en un shift horizontal:
     *
     *     width * 0.30f * attackSkew
     *
     * Eso empujaba TODO el playfield a la derecha con Space.
     *
     * Ahora attackSkew produce una deformación CENTRADA alrededor
     * de pivotX. La deformación es cero en los extremos verticales
     * y máxima aproximadamente a media pantalla.
     *
     * Eso hace que una nota que sube describa una trayectoria curva
     * hacia afuera/adentro, como si se deslizara sobre una superficie
     * redondeada, sin trasladar todo el campo lateralmente.
     */
    var attackSkew = 0f
        set(value) {
            if (field == value) return
            field = value
            updateMesh()
        }

    /*
     * Lo conservamos porque AttackState ya lo usa para
     * Space/Incoming/Hallway/Distant.
     *
     * Sigue neutralizado temporalmente. La implementación anterior
     * attackTilt -> scaleX fue la que hacía explotar el tamaño.
     */
    var attackTilt = 0f
        set(value) {
            if (field == value) return
            field = value
            updateMesh()
        }

    /*
     * Intensidad de la "curvatura esférica".
     *
     * 0.30 = a mitad de pantalla, Space 100% puede abrir el campo
     * aproximadamente un 30% respecto a pivotX.
     *
     * Si después del video se ve demasiado:
     *     0.22f - 0.25f
     *
     * Si se ve muy suave:
     *     0.35f
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
     * 1.0 = seno puro.
     * >1  = concentra más el efecto hacia la zona media.
     */
    var attackCurvePower = 1.0f
        set(value) {
            val newValue = value.coerceAtLeast(0.01f)
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
        createFrameBuffer(width, height)

    private val shader = createShader()
    private var mesh = createMesh()

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

        this.width = width
        this.height = height
        this.pivotX = pivotX

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
            ShortArray(indexCount)

        var p = 0

        for (i in 0 until segments) {
            val topLeft =
                (i * 2).toShort()

            val topRight =
                (i * 2 + 1).toShort()

            val bottomLeft =
                ((i + 1) * 2).toShort()

            val bottomRight =
                ((i + 1) * 2 + 1).toShort()

            indices[p++] = topLeft
            indices[p++] = bottomLeft
            indices[p++] = topRight

            indices[p++] = topRight
            indices[p++] = bottomLeft
            indices[p++] = bottomRight
        }

        result.setIndices(indices)

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
                (segments + 1) * 2 * 5
            )

        val nxTopShiftPx =
            width *
                    topShiftXPercent

        var p = 0

        for (row in 0..segments) {
            val t =
                row.toFloat() /
                        segments.toFloat()

            /*
             * t:
             * 0 = arriba
             * 1 = abajo
             */
            val depth =
                1f - t

            // -------------------------------------------------
            // NX: PERSPECTIVA ORIGINAL
            // -------------------------------------------------

            val horizontalEffect =
                depth.pow(
                    horizontalPower
                        .coerceAtLeast(0.01f)
                )

            val perspectiveScaleX =
                1f -
                        (
                                (1f - topScaleX) *
                                        horizontalEffect
                                )

            /*
             * progress=0 -> NX no afecta.
             * progress=1 -> NX completo.
             */
            val nxScaleX =
                1f +
                        (
                                perspectiveScaleX - 1f
                                ) * progress

            val nxShiftX =
                nxTopShiftPx *
                        horizontalEffect *
                        progress

            // -------------------------------------------------
            // ATTACK SKEW / SPACE:
            // CURVA CENTRADA, SIN SHIFT GLOBAL
            // -------------------------------------------------

            /*
             * Perfil "esférico":
             *
             * t=0   -> 0
             * t=.5  -> 1
             * t=1   -> 0
             *
             * De esta manera:
             * - no arrastramos todo el playfield a un lado;
             * - el efecto aparece gradualmente;
             * - es máximo en la zona media;
             * - vuelve a desaparecer hacia el otro extremo.
             */
            val rawSphereCurve =
                sin(
                    PI.toFloat() *
                            t
                ).coerceAtLeast(0f)

            val sphereCurve =
                rawSphereCurve.pow(
                    attackCurvePower
                )

            /*
             * Space actualmente da attackSkew positivo.
             *
             * scale > 1:
             * los lados se abren respecto al pivote.
             *
             * Si algún chart usa skew negativo:
             * se contrae hacia el centro.
             */
            val attackScaleX =
                1f +
                        (
                                attackSkew *
                                        attackCurveAmount *
                                        sphereCurve
                                )

            /*
             * Tilt permanece neutralizado.
             *
             * NO volver a hacer:
             *
             *     1 - (0.45 * attackTilt * depth)
             *
             * porque ya comprobamos que eso provoca un crecimiento
             * excesivo de notas/receptores.
             */
            val attackTiltScaleX = 1f

            // -------------------------------------------------
            // COMPOSICIÓN FINAL X
            // -------------------------------------------------

            val finalScaleX =
                nxScaleX *
                        attackScaleX *
                        attackTiltScaleX

            /*
             * Importante:
             * NO hay attackShiftX.
             *
             * Space ya no traslada el campo a la derecha.
             */
            val finalShiftX =
                nxShiftX

            val leftX =
                pivotX +
                        (0f - pivotX) *
                        finalScaleX +
                        finalShiftX

            val rightX =
                pivotX +
                        (width.toFloat() - pivotX) *
                        finalScaleX +
                        finalShiftX

            // -------------------------------------------------
            // Y
            // -------------------------------------------------

            val fullPerspectiveT =
                t.pow(
                    verticalPower
                        .coerceAtLeast(0.01f)
                )

            /*
             * Sólo NX modifica por ahora la distribución vertical.
             * Los ATTACK perspective no cambian Y hasta que
             * reimplementemos correctamente PerspectiveTilt.
             */
            val perspectiveT =
                t +
                        (
                                fullPerspectiveT - t
                                ) * progress

            val y =
                height *
                        perspectiveT +
                        meshOffsetY *
                        progress

            /*
             * El FrameBuffer de LibGDX está invertido verticalmente.
             */
            val v =
                1f - t

            // LEFT
            vertices[p++] = leftX
            vertices[p++] = y
            vertices[p++] = 0f
            vertices[p++] = 0f
            vertices[p++] = v

            // RIGHT
            vertices[p++] = rightX
            vertices[p++] = y
            vertices[p++] = 0f
            vertices[p++] = 1f
            vertices[p++] = v
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
                v_texCoords = a_texCoord0;
                gl_Position = u_projTrans * a_position;
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
            if (!shader.isCompiled) {
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
