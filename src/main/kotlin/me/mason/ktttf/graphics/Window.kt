package me.mason.ktttf.graphics

import org.joml.Matrix4f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL43.glClearColor
import org.lwjgl.opengl.GL43.glViewport
import org.lwjgl.system.MemoryUtil

interface Window {
    val id: Long
    var title: String
    var width: Int
    var height: Int
}

suspend fun window(_title: String, _width: Int, _height: Int, block: suspend Window.() -> (Unit)) {
    var title = _title
    var width = _width
    var height = _height
    GLFWErrorCallback.createPrint(System.err).set()
    if (!glfwInit()) error("Unable to initialize GLFW")
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
    val id = glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL)
    if (id == MemoryUtil.NULL) error("Failed to create the GLFW window")
    val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!
    glfwSetWindowPos(
        id,
        (videoMode.width() - width) / 2,
        (videoMode.height() - height) / 2
    )
    glfwMakeContextCurrent(id)
    glfwSwapInterval(0)
    glfwShowWindow(id)
    GL.createCapabilities()
    guard(glClearColor(0.0f, 0.0f, 0.0f, 0.0f))
    glfwSetWindowSize(id, width, height)
    guard(glViewport(0, 0, width, height))
    glfwSetWindowSizeCallback(id) { _, nextWidth, nextHeight ->
        glfwSetWindowSize(id, nextWidth, nextHeight)
        glViewport(0, 0, nextWidth, nextHeight)
    }
    val window = object : Window {
        override val id = id
        override var title
            set(value) {
                title = value
                glfwSetWindowTitle(id, title)
            }
            get() = title
        override var width
            set(value) {
                width = value
                glfwSetWindowSize(id, width, height)
                guard(glViewport(0, 0, width, height))
            }
            get() = width
        override var height
            set(value) {
                height = value
                glfwSetWindowSize(id, width, height)
                guard(glViewport(0, 0, width, height))
            }
            get() = height
    }
    block(window)
    glfwDestroyWindow(id)
    glfwTerminate()
}