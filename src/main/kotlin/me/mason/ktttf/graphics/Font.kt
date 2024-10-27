package me.mason.ktttf.graphics

import me.mason.ktttf.*
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL43.*
import java.lang.Float.intBitsToFloat

fun GraphicsBuffer.drawGlyphElements(
    projection: FloatArray, view: FloatArray,
    shader: Shader, fontData: FontData
) {
    val vertexMark = vertexData.position()
    val elementMark = elementData.position()
    guard(glBindBuffer(GL_ARRAY_BUFFER, vbo))
    guard(glBufferSubData(GL_ARRAY_BUFFER, 0L, vertexData.flip()))
    guard(glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo))
    guard(glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0L, elementData.flip()))
    vertexData.limit(vertexData.capacity()); vertexData.position(vertexMark)
    elementData.limit(elementData.capacity()); elementData.position(elementMark)
    guard(glUseProgram(shader.program))
    val block = glGetProgramResourceIndex(shader.program, GL_SHADER_STORAGE_BLOCK, "ssbo")
    if (block == GL_INVALID_INDEX) {
        error("invalid block index")
    }
    guard(glBindBufferBase(GL_UNIFORM_BUFFER, 0, fontData.ssbo))
    guard(glShaderStorageBlockBinding(shader.program, block, 0))
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

fun GraphicsBuffer.drawText(
    x: Float,
    y: Float,
    text: String,
    pt: Float,
    rgba: Int,
    detail: Float,
    fontData: FontData
) {
    var cursorX = x
    text.forEach {
        drawGlyph(cursorX, y, it.code, pt, rgba, detail, fontData)
        val index = fontData.font.cmap.format4.glyphIndexMap[it.code] ?: return@forEach
        val multiplier = ((pt * FONT_DPI) / (72 * fontData.font.head.unitsPerEm.toInt()))
        cursorX += fontData.font.hmtx.hMetrics[index].advanceWidth.toInt() * multiplier
    }
}

fun GraphicsBuffer.drawGlyph(
    x: Float,
    y: Float,
    code: Int,
    pt: Float,
    rgba: Int,
    detail: Float,
    fontData: FontData
) {
    val index = fontData.font.cmap.format4.glyphIndexMap[code] ?: return
    val glyph = fontData.font.glyf[index]
    val multiplier = (pt * FONT_DPI) / (72 * fontData.font.head.unitsPerEm.toInt())
    val width = (glyph.xMax - glyph.xMin) * multiplier
    val height = (glyph.yMax - glyph.yMin) * multiplier
    val blx = x + glyph.xMin * multiplier
    val bly = y + glyph.yMin * multiplier
    val prev = fontData.used.size
    val mapped = fontData.used.getOrPut(code) { prev }
    if (prev == mapped) {
        val contours = fontData.font.quantizeGlyph(code, detail)
        var pointCount = 0
        guard(glUseProgram(shader.program))
        guard(glBindBuffer(GL_SHADER_STORAGE_BUFFER, fontData.ssbo))

        fontData.buffer.clear()
        fontData.buffer.putInt(contours.size)
        contours.forEach { points ->
            pointCount += points.size
            fontData.buffer.putInt(pointCount - 1)
        }
        guard(glBufferSubData(GL_SHADER_STORAGE_BUFFER, (64 * 4 * mapped).toLong(), fontData.buffer.flip()))

        fontData.buffer.clear()
        contours.forEach { points ->
            points.forEach {
                fontData.buffer.putFloat(it.x)
                fontData.buffer.putFloat(it.y)
            }
        }
        guard(glBufferSubData(GL_SHADER_STORAGE_BUFFER, (CONTOUR_BYTES+ mapped * 4096 * 8).toLong(), fontData.buffer.flip()))

        guard(glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0))
        guard(glUseProgram(0))
    }
    vertexData.apply {
        put(blx)
        put(bly + height)
        put(blx)
        put(bly)
        put(intBitsToFloat(rgba))
        put(multiplier)
        put(intBitsToFloat(mapped))

        put(blx)
        put(bly)
        put(blx)
        put(bly)
        put(intBitsToFloat(rgba))
        put(multiplier)
        put(intBitsToFloat(mapped))

        put(blx + width)
        put(bly)
        put(blx)
        put(bly)
        put(intBitsToFloat(rgba))
        put(multiplier)
        put(intBitsToFloat(mapped))

        put(blx + width)
        put(bly + height)
        put(blx)
        put(bly)
        put(intBitsToFloat(rgba))
        put(multiplier)
        put(intBitsToFloat(mapped))
    }
    elementData.apply {
        put(element); put(element + 1); put(element + 2)
        put(element + 2); put(element); put(element + 3)
    }
    element += 4
}

//val blockIndex = glGetProgramResourceIndex(shader.program, GL_SHADER_STORAGE_BLOCK, "ssbo")
//if (blockIndex == GL_INVALID_INDEX) {
//    println("error")
//}
//
//glBindBufferBase(GL_UNIFORM_BUFFER, 0, ssbo)
//glShaderStorageBlockBinding(shader.program, blockIndex, 0)


//fun drawOutline(textureBuffer: GraphicsBuffer, lineBuffer: GraphicsBuffer, x: Float, y: Float, font: Font, atlas: Atlas, text: String, pt: Float) {
//    var c = x
//    text.forEach {
//        drawOutline(textureBuffer, lineBuffer, c, y, font, atlas, it.code, pt)
//        val advance = font.scale(font.advance(it.code), pt)
//        val lsb = font.scale(font.lsb(it.code), pt)
//        c += advance + lsb
//    }
//}
//
//fun drawOutline(textureBuffer: GraphicsBuffer, lineBuffer: GraphicsBuffer, x: Float, y: Float, font: Font, atlas: Atlas, code: Int, pt: Float) {
//    if (!font.exists(code)) return
//    val glyph = font.glyph(code)
//    val lsb = font.scale(font.lsb(code), pt)
//    if (glyph.numberOfContours > 0) {
//        var startIndex = 0
//        var countour = 0
//        glyph.contourEnds.forEach { endIndexShort ->
//            val endIndex = endIndexShort.toInt()
//            val points = (startIndex..endIndex).toList()
//            points.indices.forEach {
//                val f = points[it]
//                val t = points[(it + 1) % points.size]
//                val fx = font.scale(glyph.xs[f], pt) + lsb
//                val fy = font.scale(glyph.ys[f], pt)
//                val tx = font.scale(glyph.xs[t], pt) + lsb
//                val ty = font.scale(glyph.ys[t], pt)
////                val color = if (it == 0) rgba(255, 0, 0, 255) else if (it == endIndex) rgba(0, 255, 0, 255) else rgba(0, 255, 0, 255)
//                lineBuffer.drawLine(
//                    x + fx,
//                    y + fy,
//                    x + tx,
//                    y + ty,
//                    0.1f,
//                    rgba(
//                        if (countour == 0) 255 else 0,
//                        if (countour == 1) 255 else 0,
//                        if (countour == 2) 255 else 0,
//                        255
//                    )
//                )
//                textureBuffer.drawQuad(x + fx, y + fy, 0.25f, 0.25f, 0, 0, 1, 1, atlas)
//            }
//            startIndex = endIndex + 1
//            countour++
//        }
//    }
//}