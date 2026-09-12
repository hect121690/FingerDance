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
    private val segments: Int = 96
) {

    var topScaleX = 0.42f
        set(value) {
            field = value
            updateMesh()
        }

    var topShiftXPercent = 0.00f
        set(value) {
            field = value
            updateMesh()
        }

    var horizontalPower = 1.30f
        set(value) {
            field = value
            updateMesh()
        }

    var verticalPower = 1.80f
        set(value) {
            field = value
            updateMesh()
        }

    var meshOffsetY = height * 0.08f
        set(value) {
            field = value
            updateMesh()
        }

    var progress = 1f
        set(value) {
            val newValue = value.coerceIn(0f, 1f)
            if (field == newValue) return
            field = newValue
            updateMesh()
        }

    private var width = width
    private var height = height
    private var pivotX = pivotX

    private var frameBuffer =
        createFrameBuffer(
            width,
            height
        )

    private val shader = createShader()
    private var mesh = createMesh()

    init {
        require(segments >= 1)
        updateMesh()
    }

    /*
     * ------------------------------------------------------------
     * FRAMEBUFFER
     * ------------------------------------------------------------
     */

    fun begin() {
        frameBuffer.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    fun end() {
        frameBuffer.end()
    }

    /*
     * ------------------------------------------------------------
     * RENDER DEL MESH
     * ------------------------------------------------------------
     */

    fun draw(projectionMatrix: Matrix4) {
        val texture = frameBuffer.colorBufferTexture
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        texture.bind(0)

        shader.bind()
        shader.setUniformMatrix("u_projTrans", projectionMatrix)
        shader.setUniformi("u_texture", 0)
        mesh.render(shader, GL20.GL_TRIANGLES)
    }

    /*
     * ------------------------------------------------------------
     * RESIZE
     * ------------------------------------------------------------
     */

    fun resize(width: Int, height: Int, pivotX: Float = this.pivotX) {
        if (width <= 0 || height <= 0) {
            return
        }

        this.width = width
        this.height = height
        this.pivotX = pivotX
        frameBuffer.dispose()
        frameBuffer = createFrameBuffer(width, height)
        mesh.dispose()
        mesh = createMesh()

        updateMesh()
    }

    /*
     * ------------------------------------------------------------
     * FRAMEBUFFER CREATION
     * ------------------------------------------------------------
     */

    private fun createFrameBuffer(width: Int, height: Int): FrameBuffer {
        return FrameBuffer(Pixmap.Format.RGBA8888, width, height, false).also {
            it.colorBufferTexture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
            )
        }
    }

    /*
     * ------------------------------------------------------------
     * MESH CREATION
     * ------------------------------------------------------------
     */

    private fun createMesh(): Mesh {
        val vertexCount = (segments + 1) * 2
        val indexCount = segments * 6

        val result = Mesh(true, vertexCount, indexCount,
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

        val indices = ShortArray(indexCount)
        var p = 0
        for (i in 0 until segments) {
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

        result.setIndices(indices)

        return result
    }

    /*
     * ------------------------------------------------------------
     * ACTUALIZACION DE LA PERSPECTIVA
     * ------------------------------------------------------------
     */

    private fun updateMesh() {
        if (width <= 0 || height <= 0) {
            return
        }
        val vertices = FloatArray((segments + 1) * 2 * 5)
        val topShiftPx = width * topShiftXPercent
        var p = 0
        for (row in 0..segments) {
            val t = row.toFloat() / segments.toFloat()
            val depth = 1f - t
            val horizontalEffect = depth.pow(horizontalPower.coerceAtLeast(0.01f))
            val perspectiveScaleX = 1f - ((1f - topScaleX) * horizontalEffect)
            val scaleX = 1f + (perspectiveScaleX - 1f) * progress
            val shiftX = topShiftPx * horizontalEffect * progress
            val leftX = pivotX + (0f - pivotX) * scaleX + shiftX
            val rightX = pivotX + (width.toFloat() - pivotX) * scaleX + shiftX
            val fullPerspectiveT = t.pow(verticalPower.coerceAtLeast(0.01f))
            val perspectiveT = t + (fullPerspectiveT - t) * progress
            val y = (height * perspectiveT) + (meshOffsetY * progress)
            val v = 1f - t
            vertices[p++] = leftX
            vertices[p++] = y
            vertices[p++] = 0f
            vertices[p++] = 0f
            vertices[p++] = v
            vertices[p++] = rightX
            vertices[p++] = y
            vertices[p++] = 0f
            vertices[p++] = 1f
            vertices[p++] = v
        }

        mesh.setVertices(vertices)
    }

    /*
     * ------------------------------------------------------------
     * SHADER
     * ------------------------------------------------------------
     */

    private fun createShader(): ShaderProgram {
        ShaderProgram.pedantic = false

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

        return ShaderProgram(vertexShader, fragmentShader).also { shader ->
            if (!shader.isCompiled) {
                throw IllegalStateException(
                    "No se pudo compilar PerspectivePlayfieldRenderer: ${shader.log}"
                )
            }
        }
    }

    /*
     * ------------------------------------------------------------
     * DISPOSE
     * ------------------------------------------------------------
     */

    fun dispose() {
        frameBuffer.dispose()
        mesh.dispose()
        shader.dispose()
    }
}