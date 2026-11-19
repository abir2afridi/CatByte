package com.example.engine.gl

import android.opengl.GLES30
import android.opengl.GLES20
import android.util.Log
import androidx.annotation.Keep

/**
 * Manages OpenGL ES 3.0 shader program compilation, linking, and uniform/attribute locations.
 * Thread-safe for use on the GL render thread.
 */
@Keep
class GLShaderProgram private constructor(
    val programId: Int,
    val attributes: Map<String, Int>,
    val uniforms: Map<String, Int>
) {

    companion object {
        private const val TAG = "GLShaderProgram"

        /**
         * Creates a shader program from vertex and fragment shader sources.
         * Returns null if compilation or linking fails (logs error).
         */
        fun create(vertexSource: String, fragmentSource: String): GLShaderProgram? {
            val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource) ?: return null
            val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource) ?: return null

            val program = GLES30.glCreateProgram()
            if (program == 0) {
                Log.e(TAG, "Failed to create program")
                GLES30.glDeleteShader(vertexShader)
                GLES30.glDeleteShader(fragmentShader)
                return null
            }

            GLES30.glAttachShader(program, vertexShader)
            GLES30.glAttachShader(program, fragmentShader)
            GLES30.glLinkProgram(program)

            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == GLES20.GL_FALSE) {
                val log = GLES30.glGetProgramInfoLog(program)
                Log.e(TAG, "Program link failed: $log")
                GLES30.glDeleteProgram(program)
                GLES30.glDeleteShader(vertexShader)
                GLES30.glDeleteShader(fragmentShader)
                return null
            }

            // Cache attribute locations
            val attributes = mutableMapOf<String, Int>()
            val attrCount = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_ACTIVE_ATTRIBUTES, attrCount, 0)
            for (i in 0 until attrCount[0]) {
                val nameBuf = ByteArray(64)
                val length = IntArray(1)
                val size = IntArray(1)
                val type = IntArray(1)
                GLES30.glGetActiveAttrib(program, i, nameBuf.size, length, 0, size, 0, type, 0, nameBuf, 0)
                val name = String(nameBuf, 0, length[0]).trim()
                val location = GLES30.glGetAttribLocation(program, name)
                if (location >= 0) attributes[name] = location
            }

            // Cache uniform locations
            val uniforms = mutableMapOf<String, Int>()
            val uniformCount = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_ACTIVE_UNIFORMS, uniformCount, 0)
            for (i in 0 until uniformCount[0]) {
                val nameBuf = ByteArray(64)
                val length = IntArray(1)
                val size = IntArray(1)
                val type = IntArray(1)
                GLES30.glGetActiveUniform(program, i, nameBuf.size, length, 0, size, 0, type, 0, nameBuf, 0)
                val name = String(nameBuf, 0, length[0]).trim()
                val location = GLES30.glGetUniformLocation(program, name)
                if (location >= 0) uniforms[name] = location
            }

            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)

            return GLShaderProgram(program, attributes, uniforms)
        }

        private fun compileShader(type: Int, source: String): Int? {
            val shader = GLES30.glCreateShader(type)
            if (shader == 0) return null

            GLES30.glShaderSource(shader, source)
            GLES30.glCompileShader(shader)

            val compileStatus = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == GLES20.GL_FALSE) {
                val log = GLES30.glGetShaderInfoLog(shader)
                Log.e(TAG, "Shader compile failed (${shaderTypeName(type)}): $log\nSource:\n$source")
                GLES30.glDeleteShader(shader)
                return null
            }
            return shader
        }

        private fun shaderTypeName(type: Int): String = when (type) {
            GLES30.GL_VERTEX_SHADER -> "VERTEX"
            GLES30.GL_FRAGMENT_SHADER -> "FRAGMENT"
            else -> "UNKNOWN($type)"
        }
    }

    fun use() {
        GLES30.glUseProgram(programId)
    }

    fun getAttribLocation(name: String): Int = attributes[name] ?: -1
    fun getUniformLocation(name: String): Int = uniforms[name] ?: -1

    fun setUniform1f(name: String, value: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform1f(loc, value)
    }

    fun setUniform2f(name: String, x: Float, y: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform2f(loc, x, y)
    }

    fun setUniform3f(name: String, x: Float, y: Float, z: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform3f(loc, x, y, z)
    }

    fun setUniform4f(name: String, x: Float, y: Float, z: Float, w: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform4f(loc, x, y, z, w)
    }

    fun setUniform1i(name: String, value: Int) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform1i(loc, value)
    }

    fun setUniformMatrix4fv(name: String, matrix: FloatArray) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniformMatrix4fv(loc, 1, false, matrix, 0)
    }

    fun setUniformMatrix3fv(name: String, matrix: FloatArray) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniformMatrix3fv(loc, 1, false, matrix, 0)
    }

    fun release() {
        GLES30.glDeleteProgram(programId)
    }
}