package me.mason.ktttf

import java.nio.ByteBuffer
import kotlin.math.max

const val FONT_DPI = 96

const val ON_CURVE: UByte = 1u
const val X_IS_BYTE: UByte = 2u
const val Y_IS_BYTE: UByte = 4u
const val REPEAT: UByte = 8u
const val X_DELTA: UByte = 16u
const val Y_DELTA: UByte = 32u

const val ARG_1_AND_2_ARE_WORDS = 1u
const val ARGS_ARE_XY_VALUES = 2u
const val ROUND_XY_TO_GRID = 4u
const val WE_HAVE_A_SCALE = 8u
const val MORE_COMPONENTS = 32u
const val WE_HAVE_AN_X_AND_Y_SCALE = 64u
const val WE_HAVE_A_TWO_BY_TWO = 128u
const val WE_HAVE_INSTRUCTIONS = 256u
const val USE_MY_METRICS = 512u
const val OVERLAP_COMPOUND = 1024u

typealias Fixed = Float
typealias FWord = Short
typealias UFWord = UShort
typealias Offset16 = UShort
typealias Offset32 = UInt

class Font(
    val head: Head,
    val os2: OS2,
    val maxp: MaxProfile,
    val hhea: HorizontalHeader,
    val hmtx: HorizontalMetrics,
    val loca32: UIntArray,
    val glyf: Array<Glyph>,
    val cmap: CharacterMap
)

class Table(
    val checksum: UInt,
    val offset: UInt,
    val length: UInt
)

class Head(
    buffer: ByteBuffer? = null,
    val majorVersion: UShort = buffer?.getUShort() ?: 0u,
    val minorVersion: UShort = buffer?.getUShort() ?: 0u,
    val fontRevision: Fixed = buffer?.getFixed() ?: 0f,
    val checksumAdjustment: UInt = buffer?.getUInt() ?: 0u,
    val magicNumber: UInt = buffer?.getUInt() ?: 0u,
    val flags: UShort = buffer?.getUShort() ?: 0u,
    val unitsPerEm: UShort = buffer?.getUShort() ?: 0u,
    val created: Long = buffer?.getLong() ?: 0L,
    val modified: Long = buffer?.getLong() ?: 0L,
    val xMin: FWord = buffer?.getFWord() ?: 0,
    val yMin: FWord = buffer?.getFWord() ?: 0,
    val xMax: FWord = buffer?.getFWord() ?: 0,
    val yMax: FWord = buffer?.getFWord() ?: 0,
    val macStyle: UShort = buffer?.getUShort() ?: 0u,
    val lowestRecPPEM: UShort = buffer?.getUShort() ?: 0u,
    val fontDirectionHint: Short = buffer?.getShort() ?: 0,
    val indexToLocFormat: Short = buffer?.getShort() ?: 0,
    val glyphDataFormat: Short = buffer?.getShort() ?: 0
)

class OS2(buffer: ByteBuffer? = null) {
    var breakChar: UShort = 0u
    var capHeight: Short = 0
    init {
        buffer?.skip(88)
        capHeight = buffer?.getShort() ?: 0
        buffer?.skip(2)
        breakChar = buffer?.getUShort() ?: 0u
    }
}


class MaxProfile(
    buffer: ByteBuffer? = null,
    val version: Fixed = buffer?.getFixed() ?: 0f,
    val numGlyphs: UShort = buffer?.getUShort() ?: 0u,
    val maxPoints: UShort = buffer?.getUShort() ?: 0u,
    val maxContours: UShort = buffer?.getUShort() ?: 0u,
    val maxCompositePoints: UShort = buffer?.getUShort() ?: 0u,
    val maxCompositeContours: UShort = buffer?.getUShort() ?: 0u,
    val maxZones: UShort = buffer?.getUShort() ?: 0u,
    val maxTwilightPoints: UShort = buffer?.getUShort() ?: 0u,
    val maxStorage: UShort = buffer?.getUShort() ?: 0u,
    val maxFunctionDefs: UShort = buffer?.getUShort() ?: 0u,
    val maxInstructionDefs: UShort = buffer?.getUShort() ?: 0u,
    val maxStackElements: UShort = buffer?.getUShort() ?: 0u,
    val maxSizeOfInstructions: UShort = buffer?.getUShort() ?: 0u,
    val maxComponentElements: UShort = buffer?.getUShort() ?: 0u,
    val maxComponentDepth: UShort = buffer?.getUShort() ?: 0u
)

class HorizontalHeader(
    buffer: ByteBuffer? = null,
    val version: Fixed = buffer?.getFixed() ?: 0f,
    val ascent: FWord = buffer?.getFWord() ?: 0,
    val descent: FWord = buffer?.getFWord() ?: 0,
    val lineGap: FWord = buffer?.getFWord() ?: 0,
    val advanceWidthMax: UFWord = buffer?.getUFWord() ?: 0u,
    val minLeftSideBearing: FWord = buffer?.getFWord() ?: 0,
    val minRightSideBearing: FWord = buffer?.getFWord() ?: 0,
    val xMaxExtent: FWord = buffer?.getFWord() ?: 0,
    val caretSlopeRise: Short = buffer?.getShort() ?: 0,
    val caretSlopeRun: Short = buffer?.getShort() ?: 0,
    val caretOffset: FWord = buffer?.getFWord() ?: 0,
    val metricDataFormat: Short = buffer?.skip(8)?.getShort() ?: 0,
    val numOfLongHorMetrics: UShort = buffer?.getUShort() ?: 0u,
)

class HorizontalMetric(
    val advanceWidth: UShort,
    val leftSideBearing: Short
)

class HorizontalMetrics(
    val hMetrics: Array<HorizontalMetric>,
    val leftSideBearing: Array<FWord>
)

class Glyph(
    var numberOfContours: Short,
    val xMin: Short,
    val yMin: Short,
    val xMax: Short,
    val yMax: Short,
    val contourEnds: UShortArray,
    var numPoints: Int,
    val flags: UByteArray,
    val xs: ShortArray,
    val ys: ShortArray
)

class EncodingRecord(
    val platformId: UShort,
    val encodingId: UShort,
    val offset: Offset32
)

class CharacterMap(
    val version: UShort,
    val numTables: UShort,
    val encodingRecords: Array<EncodingRecord>,
    val format4: Format4GlyphIndexMap
)

class Format4GlyphIndexMap(
    val length: UShort,
    val language: UShort,
    val segCountX2: UShort,
    val searchRange: UShort,
    val entrySelector: UShort,
    val rangeShift: UShort,
    val endCode: UShortArray,
    val startCode: UShortArray,
    val idDelta: ShortArray,
    val idRangeOffset: UShortArray,
    val glyphIndexMap: HashMap<Int, Int>
)

fun lerp(x0: Float, x1: Float, t: Float): Float {
    return (1f - t) * x0 + t * x1
}

fun bezier(t: Float, p0: Short, p1: Short, p2: Short): Float {
    val u = 1 - t
    val p = u * u * p0 + 2 * u * t * p1 + t * t * p2
    return p
}

fun ByteBuffer.skip(n: Int): ByteBuffer { position(position() + n); return this }
fun ByteBuffer.getUByte() = get().toUByte()
fun ByteBuffer.getUShort() = getShort().toUShort()
fun ByteBuffer.getUInt() = getInt().toUInt()
fun ByteBuffer.getFWord() = getShort()
fun ByteBuffer.getUFWord() = getUShort()
fun ByteBuffer.getOffset16() = getUShort()
fun ByteBuffer.getOffset32() = getUInt()
fun ByteBuffer.getFixed() = getInt() / (1 shl 16).toFloat()
fun ByteBuffer.getF2Dot14() = getShort() / (1 shl 14).toFloat()
fun ByteBuffer.getString(n: Int) = String(ByteArray(n).also { get(it) })
fun ByteBuffer.getSimpleGlyph(getPosition: (Int) -> (Int), simpleIndex: Int, os2: OS2, cmap: CharacterMap): Glyph {
    val mark = position()
    position(getPosition(simpleIndex))
    val numberOfContours = getShort()
    val xMin = getShort()
    val yMin = getShort()
    val xMax = getShort()
    val yMax = getShort()
    if (simpleIndex == cmap.format4.glyphIndexMap[os2.breakChar.toInt()]!!) {
        return Glyph(
            0, xMin, yMin, xMax, yMax,
            UShortArray(0), 0, UByteArray(0), ShortArray(0), ShortArray(0)
        )
    }
    val contourEnds = UShortArray(numberOfContours.toInt()) { getUShort() }
    val numPoints = (contourEnds.maxOfOrNull { it } ?: 0u).toInt() + 1
    val flags = UByteArray(numPoints)
    val xs = ShortArray(numPoints)
    val ys = ShortArray(numPoints)
    skip(getUShort().toInt())
    var i = 0
    while (i < numPoints) {
        val flag = getUByte()
        flags[i++] = flag
        if (flag and REPEAT <= 0u) {
            continue
        }
        (0..<getUByte().toInt()).forEach { _ ->
            flags[i++] = flag
        }
    }
    fun pos(array: ShortArray, byteFlag: UByte, deltaFlag: UByte) {
        var value: Short = 0.toShort()
        (0..<numPoints).forEach {
            val flag = flags[it]
            if (byteFlag and flag > 0u) {
                val offset = getUByte()
                val sign = if (deltaFlag and flag > 0u) 1 else -1
                value = (value.toInt() + offset.toInt() * sign).toShort()
            } else if (deltaFlag and flag <= 0u) {
                value = (value.toInt() + getShort()).toShort()
            }
            array[it] = value
        }
    }
    pos(xs, X_IS_BYTE, X_DELTA)
    pos(ys, Y_IS_BYTE, Y_DELTA)
    val glyph = Glyph(numberOfContours, xMin, yMin, xMax, yMax, contourEnds, numPoints, flags, xs, ys)
    position(mark)
    return glyph
}
fun ByteBuffer.getGlyph(getPosition: (Int) -> (Int), index: Int, os2: OS2, cmap: CharacterMap): Glyph {
    val mark = position()
    position(getPosition(index))
    var numberOfContours = getShort()
    val result = if (index != cmap.format4.glyphIndexMap[os2.breakChar.toInt()] && numberOfContours.toInt() == -1) {
        var hasNext = true
        val parts = ArrayList<Glyph>()
        val xMin = getShort()
        val yMin = getShort()
        val xMax = getShort()
        val yMax = getShort()
        while (hasNext) {
            val flag = getUShort()
            val glyphIdx = getUShort()
            val isWords = flag and ARG_1_AND_2_ARE_WORDS.toUShort() > 0u
            val isXY = flag and ARGS_ARE_XY_VALUES.toUShort() > 0u
            val isRounded = flag and ROUND_XY_TO_GRID.toUShort() > 0u
            val hasScale = flag and WE_HAVE_A_SCALE.toUShort() > 0u
            val hasXYScale = flag and WE_HAVE_AN_X_AND_Y_SCALE.toUShort() > 0u
            val has2x2 = flag and WE_HAVE_A_TWO_BY_TWO.toUShort() > 0u
            val hasInstructions = flag and WE_HAVE_INSTRUCTIONS.toUShort() > 0u
            val myMetrics = flag and USE_MY_METRICS.toUShort() > 0u
            val overlap = flag and OVERLAP_COMPOUND.toUShort() > 0u
            //TODO: test how this recurses
            val glyph = if (glyphIdx.toInt() != index) getGlyph(getPosition, glyphIdx.toInt(), os2, cmap) else null
            if (!isXY) error("point indices")
            val dx = if (isWords) getShort() else get().toShort()
            val dy = if (isWords) getShort() else get().toShort()
            var sx = 1f; var sy = 1f
            hasNext = flag and MORE_COMPONENTS.toUShort() > 0u
            if (hasScale) {
                sx = getF2Dot14()
                sy = sx
            } else if (hasXYScale) {
                sx = getF2Dot14()
                sy = getF2Dot14()
            } else if (has2x2) error("2x2 matrix transform")
            if (glyph != null) {
                (0..<glyph.numPoints).forEach {
                    glyph.xs[it] = (glyph.xs[it] * sx + dx).toInt().toShort()
                    glyph.ys[it] = (glyph.ys[it] * sy + dy).toInt().toShort()
                }
                parts.add(glyph)
            }
        }
        numberOfContours = parts.sumOf { it.numberOfContours.toInt() }.toShort()
        val contourEnds = UShortArray(max(0, numberOfContours.toInt()))
        val numPoints = parts.sumOf { it.numPoints }
        val xs = ShortArray(numPoints)
        val ys = ShortArray(numPoints)
        val flags = UByteArray(numPoints)
        var ci = 0
        var cm = 0u
        var d = 0
        parts.forEach { part ->
            var nm: UInt = cm
            part.contourEnds.forEach {
                contourEnds[ci++] = (it + cm).toUShort()
                nm = max(nm, cm + it)
            }
            cm = nm + 1u
            part.flags.copyInto(flags, d)
            part.xs.copyInto(xs, d)
            part.ys.copyInto(ys, d)
            d += part.numPoints
        }
        Glyph(
            numberOfContours, xMin, yMin, xMax, yMax,
            contourEnds, numPoints, flags, xs, ys
        )
    } else {
        getSimpleGlyph(getPosition, index, os2, cmap)
    }
    position(mark)
    return result
}

fun parseTTF(bytes: ByteArray): Font {
    val buffer = ByteBuffer.allocateDirect(bytes.size).put(bytes).flip()
    val tables = buffer.run {
        skip(4)
        val count = getUShort(); skip(6)
//        println("counttables: $count")
        (0..<count.toInt()).associate { _ ->
            getString(4) to Table(getUInt(), getUInt(), getUInt())
        }
    }
//    tables.keys.forEach {
//        println(it)
//    }
    val head = buffer.run {
        val table = tables["head"]!!
        position(table.offset.toInt())
        Head(this)
    }
    val maxp = buffer.run {
        val table = tables["maxp"]!!
        position(table.offset.toInt())
        MaxProfile(this)
    }
    val hhea = buffer.run {
        val table = tables["hhea"]!!
        position(table.offset.toInt())
        HorizontalHeader(this)
    }
    val hmtx = buffer.run {
        val table = tables["hmtx"]!!
        position(table.offset.toInt())
        val hMetrics = Array(hhea.numOfLongHorMetrics.toInt()) { HorizontalMetric(getUShort(), getShort()) }
        val leftSideBearing = Array((maxp.numGlyphs - hhea.numOfLongHorMetrics).toInt()) { getFWord() }
        HorizontalMetrics(hMetrics, leftSideBearing)
    }
    val loca = buffer.run {
        val table = tables["loca"]!!
        position(table.offset.toInt())
        UIntArray((maxp.numGlyphs).toInt() + 1) {
            if (head.indexToLocFormat.toInt() != 0) getUInt()
            else getUShort().toUInt()
        }
    }
    val os2 = buffer.run {
        val table = tables["OS/2"]!!
        position(table.offset.toInt())
        OS2(this)
    }
    val cmap = buffer.run {
        val table = tables["cmap"]!!
        position(table.offset.toInt())
        val version = getUShort()
        if (version.toInt() != 0) error("version should be 0?")
        val numTables = getUShort()
        val encodingRecords = Array(numTables.toInt()) { EncodingRecord(getUShort(), getUShort(), getOffset32()) }
        var selectedOffset = -1
        for (it in 0..<numTables.toInt()) {
            val record = encodingRecords[it]
            val isWindowsPlatform = record.run {
                platformId.toInt() == 3 && (
                        encodingId.toInt() == 0 ||
                                encodingId.toInt() == 1 ||
                                encodingId.toInt() == 10
                        )
            }
            val isUnicodePlatform = record.run {
                platformId.toInt() == 0 && (
                        encodingId.toInt() == 0 ||
                                encodingId.toInt() == 1 ||
                                encodingId.toInt() == 2 ||
                                encodingId.toInt() == 3 ||
                                encodingId.toInt() == 4
                        )
            }
            if (isWindowsPlatform || isUnicodePlatform) {
                selectedOffset = record.offset.toInt()
                break
            }
        }
        if (selectedOffset == -1) error("font doesnt contain recognized platform and encoding")
        position(table.offset.toInt() + selectedOffset)
        if (getUShort().toInt() != 4) error("not format 4")
        val length = getUShort()
        val language = getUShort()
        val segCountX2 = getUShort()
        val segCount = segCountX2.toInt() shr 1
        val glyphIndexMap = HashMap<Int, Int>()
        val format4 = Format4GlyphIndexMap(length, language, segCountX2, getUShort(), getUShort(), getUShort(),
            UShortArray(segCount), UShortArray(segCount), ShortArray(segCount), UShortArray(segCount), glyphIndexMap
        )
        (0..<segCount).forEach { format4.endCode[it] = getUShort() }
        getUShort()
        (0..<segCount).forEach { format4.startCode[it] = getUShort() }
        (0..<segCount).forEach { format4.idDelta[it] = getShort() }
        val idRangeOffsetsStart = buffer.position()
        (0..<segCount).forEach { format4.idRangeOffset[it] = getUShort() }
        (0..<segCount).forEach { i ->
            var glyphIndex: Int
            val endCode = format4.endCode[i]
            val startCode = format4.startCode[i]
            val idDelta = format4.idDelta[i]
            val idRangeOffset = format4.idRangeOffset[i]
            (startCode..endCode).forEach { c ->
                if (idRangeOffset.toInt() != 0) {
                    val startCodeOffset = (c - startCode).toInt() * 2
                    val currentRangeOffset = i * 2
                    val glyphIndexOffset = idRangeOffsetsStart +
                            currentRangeOffset +
                            idRangeOffset.toInt() +
                            startCodeOffset
                    position(glyphIndexOffset)
                    glyphIndex = getUShort().toInt()
                    if (glyphIndex != 0) {
                        glyphIndex = (glyphIndex + idDelta) and 0xFFFF
                    }
                } else {
                    glyphIndex = (c.toInt() + idDelta) and 0xFFFF
                }
                format4.glyphIndexMap[c.toInt()] = glyphIndex
            }
        }
        CharacterMap(version, numTables, encodingRecords, format4)
    }
    val glyf = buffer.run {
        val table = tables["glyf"]!!
        val multiplier = if (head.indexToLocFormat.toInt() == 0) 2 else 1
        val getPosition: (Int) -> (Int) = {
            (table.offset + loca[it] * multiplier.toUInt()).toInt()
        }
        Array(maxp.numGlyphs.toInt()) { getGlyph(getPosition, it, os2, cmap) }
    }
    return Font(head, os2, maxp, hhea, hmtx, loca, glyf, cmap)
}