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
import kotlin.math.pow

class PerspectivePlayfieldRenderer(
    width: Int,
    height: Int,
    pivotX: Float,
    private val segments: Int = 32
) {

    /*
     * 1.0 = tamaño normal.
     * Menor valor = playfield más angosto al llegar al receptor.
     */
    var topScaleX = 0.58f
        set(value) {
            field = value
            updateMesh()
        }

    /*
     * Desplaza el punto lejano horizontalmente.
     *
     * negativo = hacia izquierda
     * positivo = hacia derecha
     */
    var topShiftXPercent = -0.08f
        set(value) {
            field = value
            updateMesh()
        }

    /*
     * Controla cómo se va cerrando horizontalmente.
     */
    var horizontalPower = 1.15f
        set(value) {
            field = value
            updateMesh()
        }

    /*
     * ESTA es la que da mucha más sensación de profundidad.
     *
     * > 1 comprime el espacio vertical cerca del receptor.
     */
    var verticalPower = 1.45f
        set(value) {
            field = value
            updateMesh()
        }

    private var width = width
    private var height = height
    private var pivotX = pivotX

    private var frameBuffer = createFrameBuffer(width, height)
    private val shader = createShader()
    private var mesh = createMesh()

    init {
        require(segments >= 1)
        updateMesh()
    }

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

    fun draw(projectionMatrix: Matrix4) {
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

        mesh = createMesh()

        updateMesh()
    }

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

    private fun createMesh(): Mesh {
        val vertexCount =
            (segments + 1) * 2

        val indexCount =
            segments * 6

        val result = Mesh(
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

    private fun updateMesh() {
        if (
            width <= 0 ||
            height <= 0
        ) {
            return
        }

        /*
         * Cada vértice:
         *
         * x
         * y
         * z
         * u
         * v
         */
        val vertices =
            FloatArray(
                (segments + 1) * 2 * 5
            )

        val topShiftPx =
            width * topShiftXPercent

        var p = 0

        for (row in 0..segments) {

            /*
             * IMPORTANTE:
             *
             * Nuestra cámara usa Y-DOWN.
             *
             * t = 0 -> arriba / receptor
             * t = 1 -> abajo / jugador
             */
            val t =
                row.toFloat() /
                        segments.toFloat()

            /*
             * Profundidad:
             *
             * 1 arriba
             * 0 abajo
             */
            val depth =
                1f - t

            val horizontalEffect =
                depth.pow(
                    horizontalPower
                        .coerceAtLeast(0.01f)
                )

            /*
             * Arriba:
             * scaleX ~= topScaleX
             *
             * Abajo:
             * scaleX = 1
             */
            val scaleX =
                1f -
                        ((1f - topScaleX) *
                                horizontalEffect)

            /*
             * El desplazamiento también desaparece
             * gradualmente hacia abajo.
             */
            val shiftX =
                topShiftPx *
                        horizontalEffect

            val leftX =
                pivotX +
                        (0f - pivotX) *
                        scaleX +
                        shiftX

            val rightX =
                pivotX +
                        (width.toFloat() - pivotX) *
                        scaleX +
                        shiftX

            /*
             * PERSPECTIVA VERTICAL.
             *
             * Como Y=0 está arriba:
             *
             * t^1.45 comprime las distancias verticales
             * cerca del receptor.
             *
             * Esto hace que las notas también parezcan
             * reducir su ALTURA cuando llegan al fondo.
             */
            val perspectiveT =
                t.pow(
                    verticalPower
                        .coerceAtLeast(0.01f)
                )

            val y =
                height *
                        perspectiveT

            /*
             * FrameBuffer de LibGDX viene invertido
             * verticalmente.
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

        mesh.setVertices(vertices)
    }

    private fun createShader(): ShaderProgram {
        ShaderProgram.pedantic = false

        val vertexShader = """
            attribute vec4 a_position;
            attribute vec2 a_texCoord0;

            uniform mat4 u_projTrans;

            varying vec2 v_texCoords;

            void main() {
                v_texCoords = a_texCoord0;
                gl_Position = u_projTrans * a_position;
            }
        """.trimIndent()

        val fragmentShader = """
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

    fun dispose() {
        frameBuffer.dispose()
        mesh.dispose()
        shader.dispose()
    }
}