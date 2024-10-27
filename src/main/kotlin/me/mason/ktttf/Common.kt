package me.mason.ktttf

import org.joml.Vector2f
import kotlin.math.*

private val WEIGHTS = doubleArrayOf(0.236926885, 0.478628670, 0.568888889, 0.478628670, 0.236926885)
private val NODES = doubleArrayOf(-0.906179846, -0.538469310, 0.0, 0.538469310, 0.906179846)

fun resourceStream(name: String) = object {}.javaClass.getResourceAsStream(name) ?: error("Could not find resource: $name")

fun quadraticLength(p0: Vector2f, p1: Vector2f, p2: Vector2f): Float {
    var length = 0.0
    for (i in 0..4) {
        val t = 0.5 * (NODES[i] + 1)
        val dx = 2 * (1 - t) * (p1.x - p0.x) + 2 * t * (p2.x - p1.x)
        val dy = 2 * (1 - t) * (p1.y - p0.y) + 2 * t * (p2.y - p1.y)
        val integrand = sqrt(dx * dx + dy * dy)
        length += WEIGHTS[i] * integrand
    }
    return (0.5 * length).toFloat()
}

fun bezier(p0: Vector2f, p1: Vector2f, p2: Vector2f, t: Float): Vector2f {
    val a = Vector2f()
    val b = Vector2f()
    val u = 1 - t
    val p = p0.mul(u * u, a).add(p1.mul(2 * u * t, b)).add(p2.mul(t * t, b))
    return p
}

fun lerp(p0: Vector2f, p1: Vector2f, t: Float): Vector2f {
    return Vector2f(lerp(p0.x, p1.x, t), lerp(p0.y, p1.y, t))
}

fun Font.quantizeGlyph(code: Int, detail: Float): Array<MutableList<Vector2f>> {
    val index = cmap.format4.glyphIndexMap[code] ?: return emptyArray()
    val glyph = glyf[index]
    var startIndex = 0
    val distance = if (detail > 0) (1f / detail) * head.unitsPerEm.toInt() else head.unitsPerEm.toInt() * 3f
    if (glyph.numberOfContours.toInt() <= 0) return emptyArray()
    val contours = Array<MutableList<Vector2f>>(glyph.numberOfContours.toInt()) { mutableListOf() }
    var pointCount = 0
    (0..<glyph.numberOfContours).forEach { contour ->
        if (glyph.numPoints == 0) return@forEach
        val endIndex = glyph.contourEnds[contour].toInt()
        val lsb = hmtx.hMetrics[index].leftSideBearing
        val yMin = glyph.yMin
        val start =
            if (glyph.flags[endIndex] and ON_CURVE > 0u) Vector2f(
                glyph.xs[endIndex].toFloat() - lsb,
                glyph.ys[endIndex].toFloat() - yMin
            )
            else {
                lerp(
                    Vector2f(
                        glyph.xs[startIndex].toFloat() - lsb,
                        glyph.ys[startIndex].toFloat() - yMin
                    ),
                    Vector2f(
                        glyph.xs[endIndex].toFloat() - lsb,
                        glyph.ys[endIndex].toFloat() - yMin
                    ),
                    0.5f
                )
            }
        val points = contours[contour]
        (startIndex..endIndex).forEach {
            val curr = Vector2f(
                glyph.xs[it].toFloat() - lsb,
                glyph.ys[it].toFloat() - yMin
            )
            val currOnCurve = (glyph.flags[it] and ON_CURVE) > 0u
            if (currOnCurve) {
                points.add(curr)
            } else {
                val nextIndex = if (it + 1 > endIndex) startIndex else it + 1
                val next = Vector2f(
                    glyph.xs[nextIndex].toFloat() - lsb,
                    glyph.ys[nextIndex].toFloat() - yMin
                )
                val nextOnCurve = (glyph.flags[nextIndex] and ON_CURVE) > 0u
                val last = points.lastOrNull() ?: start
                if (!nextOnCurve) next.set(lerp(curr, next, 0.5f))
//                val length = quadraticLength(last, curr, next)
                val dirA = curr.sub(last, Vector2f()).normalize()
                val dirB = next.sub(curr, Vector2f()).normalize()
                val curveDot = dirA.dot(dirB)
                val radians = acos(curveDot)
                val threshold = PI.toFloat() / detail
                val count = (radians / threshold).toInt()
                if (count != 0) {
                    val step = 1f / count
                    for (t in 1..<count) {
                        points.add(bezier(last, curr, next, t * step))
                    }
                    if (!nextOnCurve) points.add(next)
                }
            }
        }
        pointCount += points.size
        startIndex = endIndex + 1
    }
    println("points after subdivision: ${pointCount}")
    return contours
}