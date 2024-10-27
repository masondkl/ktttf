package me.mason.ktttf.graphics

import org.lwjgl.BufferUtils.createFloatBuffer
import org.lwjgl.BufferUtils.createIntBuffer
import org.lwjgl.opengl.GL43.*
import org.lwjgl.stb.STBImage
import java.lang.Float.intBitsToFloat
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.max
import kotlin.math.min

interface Shader {
    val program: Int
    val attrs: IntArray
    val stride: Int
    val tri: Int
    val quad: Int
}

interface Atlas {
    val width: Int
    val height: Int
    val id: Int
}

interface GraphicsBuffer {
    val shader: Shader
    val size: Int
    val vertexData: FloatBuffer
    val elementData: IntBuffer
    var element: Int
    val vao: Int
    val vbo: Int
    val ebo: Int
    fun clear()
}

fun <T> guard(value: T): T {
    val error = glGetError()
    if (error != GL_NO_ERROR) error("Errored with result: $error")
    return value
}

fun shader(vertex: String, fragment: String, vararg attrs: Int) = object : Shader {
    val vertexId = guard(glCreateShader(GL_VERTEX_SHADER).compile(vertex))
    val fragmentId = guard(glCreateShader(GL_FRAGMENT_SHADER).compile(fragment))
    override val program = guard(glCreateProgram())
    override val attrs = attrs
    override val stride = attrs.sum()
    override val tri = stride * 3
    override val quad = stride * 4
    fun Int.compile(src: String): Int {
        guard(glShaderSource(this, src))
        guard(glCompileShader(this))
        if (glGetShaderi(this, GL_COMPILE_STATUS) == GL_FALSE) {
            val len = glGetShaderi(this, GL_INFO_LOG_LENGTH)
            error(glGetShaderInfoLog(this, len))
        }; return this
    }
    init {
        guard(glAttachShader(program, vertexId))
        guard(glAttachShader(program, fragmentId))
        guard(glLinkProgram(program))
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            val len = glGetProgrami(program, GL_INFO_LOG_LENGTH)
            error(glGetProgramInfoLog(program, len))
        }
    }
}

fun atlas(buffer: ByteBuffer): Atlas {
    val id = guard(glGenTextures())
    guard(glBindTexture(GL_TEXTURE_2D, id))
    guard(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT))
    guard(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT))
    guard(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST))
    guard(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST))
    val width = createIntBuffer(1)
    val height = createIntBuffer(1)
    val channels = createIntBuffer(1)
    val image = STBImage.stbi_load_from_memory(buffer, width, height, channels, 0)
    if (image != null) {
        when (channels.get(0)) {
            3 -> guard(glTexImage2D(
                GL_TEXTURE_2D, 0, GL_RGB, width.get(0), height.get(0),
                0, GL_RGB, GL_UNSIGNED_BYTE, image
            ))
            4 -> guard(glTexImage2D(
                GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0),
                0, GL_RGBA, GL_UNSIGNED_BYTE, image
            ))
            else -> error("Unknown number of channels")
        }
    } else error("Could not load image")
    STBImage.stbi_image_free(image)
    return object : Atlas {
        override val width = width.get(0)
        override val height = height.get(0)
        override val id = id
    }
}

fun graphicsBuffer(shader: Shader, size: Int) = object : GraphicsBuffer {
    override val shader = shader
    override val size = size
    override val vertexData = createFloatBuffer(size)
    override val elementData = createIntBuffer(size)
    override var element = 0
    override val vao = guard(glGenVertexArrays())
    override val vbo = guard(glGenBuffers())
    override val ebo = guard(glGenBuffers())
    override fun clear() {
        element = 0
        vertexData.clear()
        elementData.clear()
    }
    init {
        guard(glBindVertexArray(vao))
        guard(glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo))
        guard(glBufferData(GL_ELEMENT_ARRAY_BUFFER, size * 4L, GL_DYNAMIC_DRAW))
        guard(glBindBuffer(GL_ARRAY_BUFFER, vbo))
        guard(glBufferData(GL_ARRAY_BUFFER, size * 4L, GL_DYNAMIC_DRAW))
        var offset = 0L
        shader.attrs.forEachIndexed { index, size ->
            guard(glVertexAttribPointer(index, size, GL_FLOAT, false, shader.stride * 4, offset * 4L))
            offset += size
        }
        // Without unbinding vertex array here, buffers will always draw the amount of elements in the first buffer bound
        // no matter the count passed into glDrawElements!
        guard(glBindVertexArray(0))
    }
}

fun GraphicsBuffer.drawTextureElements(projection: FloatArray, view: FloatArray, atlas: Int) {
    val vertexMark = vertexData.position()
    val elementMark = elementData.position()
    guard(glBindBuffer(GL_ARRAY_BUFFER, vbo))
    guard(glBufferSubData(GL_ARRAY_BUFFER, 0L, vertexData.flip()))
    guard(glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo))
    guard(glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0L, elementData.flip()))
    vertexData.limit(vertexData.capacity()); vertexData.position(vertexMark)
    elementData.limit(elementData.capacity()); elementData.position(elementMark)
    guard(glUseProgram(shader.program))
    guard(glEnable(GL_TEXTURE_2D))
    guard(glUniform1i(glGetUniformLocation(shader.program, "SAMPLER"), 0))
    guard(glUniformMatrix4fv(glGetUniformLocation(shader.program, "projection"), false, projection))
    guard(glUniformMatrix4fv(glGetUniformLocation(shader.program, "view"), false, view))
    guard(glActiveTexture(GL_TEXTURE0))
    guard(glBindTexture(GL_TEXTURE_2D, atlas))
    guard(glBindVertexArray(vao))
    shader.attrs.indices.forEach { guard(glEnableVertexAttribArray(it)) }
    guard(glDrawElements(GL_TRIANGLES, elementMark, GL_UNSIGNED_INT, 0))
    shader.attrs.indices.forEach { guard(glDisableVertexAttribArray(it)) }
    guard(glBindVertexArray(0))
    guard(glBindTexture(GL_TEXTURE_2D, 0))
    guard(glDisable(GL_TEXTURE_2D))
    guard(glUseProgram(0))
    guard(glBindBuffer(GL_ARRAY_BUFFER, 0))
    guard(glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0))
}

fun GraphicsBuffer.drawLineElements(projection: FloatArray, view: FloatArray) {
    val vertexMark = vertexData.position()
    val elementMark = elementData.position()
    guard(glBindBuffer(GL_ARRAY_BUFFER, vbo))
    guard(glBufferSubData(GL_ARRAY_BUFFER, 0L, vertexData.flip()))
    guard(glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo))
    guard(glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0L, elementData.flip()))
    vertexData.limit(vertexData.capacity()); vertexData.position(vertexMark)
    elementData.limit(elementData.capacity()); elementData.position(elementMark)
    guard(glUseProgram(shader.program))
    guard(glUniformMatrix4fv(glGetUniformLocation(shader.program, "projection"), false, projection))
    guard(glUniformMatrix4fv(glGetUniformLocation(shader.program, "view"), false, view))
    guard(glBindVertexArray(vao))
    shader.attrs.indices.forEach { glEnableVertexAttribArray(it) }
    guard(glDrawElements(GL_TRIANGLES, elementMark, GL_UNSIGNED_INT, 0))
    shader.attrs.indices.forEach { glDisableVertexAttribArray(it) }
    guard(glBindVertexArray(0))
    guard(glUseProgram(0))
    guard(glBindBuffer(GL_ARRAY_BUFFER, 0))
    guard(glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0))
}

fun GraphicsBuffer.drawQuad(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    xAtlas: Int,
    yAtlas: Int,
    widthAtlas: Int,
    heightAtlas: Int,
    atlas: Atlas,
    align: Int = CC
) {
    val rx = width / 2f
    val ry = height / 2f
    val ax = align.x
    val ay = align.y
    val blx = when(ax) {
        C -> x - rx
        L -> x
        R -> x - width
        else -> x - rx
    }
    val bly = when(ay) {
        C -> y - ry
        T -> y - height
        B -> y
        else -> y - ry
    }

    vertexData.apply {
        put(blx)
        put(bly + height)
        put(xAtlas.toFloat() / atlas.width)
        put(yAtlas.toFloat() / atlas.height)

        put(blx)
        put(bly)
        put(xAtlas.toFloat() / atlas.width)
        put((yAtlas + heightAtlas).toFloat() / atlas.height)

        put(blx + width)
        put(bly)
        put((xAtlas + widthAtlas).toFloat() / atlas.width)
        put((yAtlas + heightAtlas).toFloat() / atlas.height)

        put(blx + width)
        put(bly + height)
        put((xAtlas + widthAtlas).toFloat() / atlas.width)
        put(yAtlas.toFloat() / atlas.height)
    }

    elementData.apply {
        put(element); put(element + 1); put(element + 2)
        put(element + 2); put(element); put(element + 3)
    }

    element += 4
}

fun GraphicsBuffer.drawLine(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    thickness: Float,
    rgba: Int
) {
    val minX = min(startX, endX) - thickness
    val minY = min(startY, endY) - thickness
    val maxX = max(startX, endX) + thickness
    val maxY = max(startY, endY) + thickness
    vertexData.apply {
        put(minX)
        put(maxY)
        put(startX)
        put(startY)
        put(endX)
        put(endY)
        put(intBitsToFloat(rgba))
        put(thickness)

        put(minX)
        put(minY)
        put(startX)
        put(startY)
        put(endX)
        put(endY)
        put(intBitsToFloat(rgba))
        put(thickness)

        put(maxX)
        put(minY)
        put(startX)
        put(startY)
        put(endX)
        put(endY)
        put(intBitsToFloat(rgba))
        put(thickness)

        put(maxX)
        put(maxY)
        put(startX)
        put(startY)
        put(endX)
        put(endY)
        put(intBitsToFloat(rgba))
        put(thickness)
    }

    elementData.apply {
        put(element); put(element + 1); put(element + 2)
        put(element + 2); put(element); put(element + 3)
    }

    element += 4
}