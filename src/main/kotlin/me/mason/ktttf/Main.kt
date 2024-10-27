package me.mason.ktttf

import kotlinx.coroutines.runBlocking
import me.mason.ktttf.graphics.*
import org.joml.Matrix4f
import org.joml.Vector2f
import org.lwjgl.BufferUtils.createByteBuffer
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER
import java.lang.System.nanoTime
import java.util.*

const val CONTOUR_BYTES = 1024 * 64 * 4
const val POINT_BYTES = 1024 * 4096 * 8
//const val GLYPH_DETAIL = 100
const val POLL_EVENTS = 1f / 60f

class FontData(val font: Font, val ssbo: Int) {
    val buffer = createByteBuffer(4096 * 8)
    val used = HashMap<Int, Int>()
}

fun main() = runBlocking {
    val lineVertex = resourceStream("/line.vert").bufferedReader().readText()
    val lineFragment = resourceStream("/line.frag").bufferedReader().readText()
    val glyphVertex = resourceStream("/glyph.vert").bufferedReader().readText()
    val glyphFragment = resourceStream("/glyph.frag").bufferedReader().readText()
    val calibriBytes = resourceStream("/calibri.ttf").readAllBytes()
    val calibriFont = parseTTF(calibriBytes)

    window("KtTTF", 1280, 720) {
        val keyState = BitSet()
        val lineShader = shader(lineVertex, lineFragment, 2, 2, 2, 1, 1)
        val lineBuffer = graphicsBuffer(lineShader, Short.MAX_VALUE.toInt())
        val glyphShader = shader(glyphVertex, glyphFragment, 2, 2, 1, 1, 1)
        val glyphBuffer = graphicsBuffer(glyphShader, Short.MAX_VALUE.toInt())
        val ssbo = guard(glGenBuffers())
        val fontData = FontData(calibriFont, ssbo)
        val projectionBuffer = FloatArray(4 * 4)
        val viewBuffer = FloatArray(4 * 4)
        val projection = Matrix4f().identity().ortho(-40f, 40f, -22.5f, 22.5f, 0f, 100f, false)
        val view = Matrix4f().identity()
        val camera = Vector2f()
        val input = Vector2f()
        var zoom = 1.0f
        var lastPoll = 0f
        var elapsed = 0f
        var last = nanoTime()
        guard(glUseProgram(glyphShader.program))
        guard(glBindBufferRange(GL_SHADER_STORAGE_BUFFER, 0, ssbo, 0, (CONTOUR_BYTES + POINT_BYTES).toLong()))
        guard(glBufferData(GL_SHADER_STORAGE_BUFFER, (CONTOUR_BYTES + POINT_BYTES).toLong(), GL_DYNAMIC_DRAW))
        guard(glUseProgram(0))
        glfwSetKeyCallback(id) { _, code, _, action, _ ->
            if (action == GLFW_PRESS) keyState.set(code)
            else if (action == GLFW_RELEASE) keyState.clear(code)
        }
        glfwSetScrollCallback(id) { _, _, y ->
            if (y > 0) zoom *= 1.1f else zoom /= 1.1f
        }
        while (!glfwWindowShouldClose(id)) {
            fontData.used.clear()
            val detail = ((elapsed % 100f) / 100f) * 40
            val now = nanoTime()
            val delta = (now - last) / 1.0E9f
            last = now
            input.set(
                if (keyState[GLFW_KEY_D]) delta else if (keyState[GLFW_KEY_A]) -delta else 0f,
                if (keyState[GLFW_KEY_W]) delta else if (keyState[GLFW_KEY_S]) -delta else 0f
            )
            camera.add(input.mul(50f).mul(1f / zoom))
            view.identity().lookAt(
                camera.x, camera.y, 20f,
                camera.x, camera.y, -1f,
                0f, 1f, 0f
            )
            projection.identity().ortho((1f / zoom) * -40f, (1f / zoom) * 40f, (1f / zoom) * -22.5f, (1f / zoom) * 22.5f, 0f, 100f, false)
            guard(glClearColor(0f, 0f, 0f, 1f))
            guard(glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT))
            glyphBuffer.apply {
                drawText(0f, 0f, "fps: ${((1/delta).toInt() / 100) * 100}", 2f, rgba(255, 255, 255, 255), detail, fontData)
            }
            projection.get(projectionBuffer)
            view.get(viewBuffer)
            lineBuffer.drawLineElements(projectionBuffer, viewBuffer)
            glyphBuffer.drawGlyphElements(projectionBuffer, viewBuffer, glyphShader, fontData)
            glfwSwapBuffers(id)
            lineBuffer.clear()
            glyphBuffer.clear()
            if (elapsed - lastPoll > POLL_EVENTS) {
                glfwPollEvents()
                lastPoll = elapsed
            }
            elapsed += delta
        }
    }
}