package com.example.data.export

import android.opengl.GLES30
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Minimal ES3 quad renderer for the custom clip shaders (mask, chroma key,
 * effect library). Media3's own [androidx.media3.common.util.GlProgram] is
 * ES2-only, while the effect library sources are ES3, so this helper owns
 * program compilation and the fullscreen triangle-strip quad instead.
 *
 * Vertex contract (same as Media3's transformation vertex shader): the
 * attribute `aFramePosition` holds normalised device coordinates; fragments
 * receive `vUv` in 0..1.
 */
@UnstableApi
internal class GlQuadProgram(vertexSource: String, fragmentSource: String) {

    private val programId: Int
    private val positionAttrib: Int
    private val quad: FloatBuffer

    init {
        val vertex = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragment = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertex)
        GLES30.glAttachShader(programId, fragment)
        GLES30.glLinkProgram(programId)
        val linked = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linked, 0)
        if (linked[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(programId)
            GLES30.glDeleteProgram(programId)
            throw IllegalStateException("Shader link failed: $log")
        }
        positionAttrib = GLES30.glGetAttribLocation(programId, "aFramePosition")

        quad = ByteBuffer
            .allocateDirect(4 * GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        quad.put(GlUtil.getNormalizedCoordinateBounds())
        quad.position(0)
    }

    fun use() {
        GLES30.glUseProgram(programId)
        if (positionAttrib >= 0) {
            quad.position(0)
            GLES30.glEnableVertexAttribArray(positionAttrib)
            GLES30.glVertexAttribPointer(
                positionAttrib,
                GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
                GLES30.GL_FLOAT,
                /* normalized= */ false,
                /* stride= */ GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE * 4,
                quad
            )
        }
    }

    fun setFloat(name: String, value: Float) {
        GLES30.glUniform1f(location(name), value)
    }

    fun setInt(name: String, value: Int) {
        GLES30.glUniform1i(location(name), value)
    }

    fun setVec2(name: String, x: Float, y: Float) {
        GLES30.glUniform2f(location(name), x, y)
    }

    fun setVec3(name: String, x: Float, y: Float, z: Float) {
        GLES30.glUniform3f(location(name), x, y, z)
    }

    fun setVec4(name: String, x: Float, y: Float, z: Float, w: Float) {
        GLES30.glUniform4f(location(name), x, y, z, w)
    }

    fun setSampler(name: String, texId: Int, unit: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
        GLES30.glUniform1i(location(name), unit)
    }

    fun draw() {
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, /* first= */ 0, /* count= */ 4)
    }

    fun release() {
        GLES30.glDeleteProgram(programId)
    }

    private fun location(name: String): Int = GLES30.glGetUniformLocation(programId, name)

    companion object {
        /** Shared fullscreen quad vertex shader (ES3 twin of Media3's ES2 one). */
        const val VERTEX_SOURCE = """
            #version 300 es
            precision mediump float;
            in vec4 aFramePosition;
            uniform mat4 uTransformationMatrix;
            uniform mat4 uTexTransformationMatrix;
            out vec2 vUv;
            void main() {
              gl_Position = uTransformationMatrix * aFramePosition;
              vec4 texturePosition = vec4(aFramePosition.x * 0.5 + 0.5,
                                          aFramePosition.y * 0.5 + 0.5, 0.0, 1.0);
              vUv = (uTexTransformationMatrix * texturePosition).xy;
            }
            """

        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, source)
            GLES30.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val log = GLES30.glGetShaderInfoLog(shader)
                GLES30.glDeleteShader(shader)
                throw IllegalStateException("Shader compile failed: $log")
            }
            return shader
        }
    }
}
