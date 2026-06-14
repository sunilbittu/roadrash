package com.roadrash.game.engine

import com.roadrash.game.engine.MathUtil.easeIn
import com.roadrash.game.engine.MathUtil.easeInOut
import kotlin.math.floor

/**
 * Builds and stores the procedural race track as a list of [Segment]s and
 * exposes helpers for locating segments and the player along the track.
 */
class Road {

    val segmentLength = 200f
    val rumbleLength = 3
    val lanes = 3

    val segments = ArrayList<Segment>()
    var trackLength = 0f
        private set

    // Curve strengths
    private val curveNone = 0f
    private val curveEasy = 2f
    private val curveMedium = 4f
    private val curveHard = 6f

    // Hill heights
    private val hillNone = 0f
    private val hillLow = 20f
    private val hillMedium = 40f
    private val hillHigh = 60f

    // Section lengths (in segments)
    private val lenShort = 25
    private val lenMedium = 50
    private val lenLong = 100

    fun findSegment(z: Float): Segment {
        val idx = floor(z / segmentLength).toInt()
        return segments[((idx % segments.size) + segments.size) % segments.size]
    }

    private fun lastY(): Float = if (segments.isEmpty()) 0f else segments[segments.size - 1].p2.worldY

    private fun addSegment(curve: Float, y: Float) {
        val n = segments.size
        val seg = Segment(n)
        seg.p1.worldZ = (n * segmentLength)
        seg.p1.worldY = lastY()
        seg.p2.worldZ = ((n + 1) * segmentLength)
        seg.p2.worldY = y
        seg.curve = curve
        seg.color = if ((n / rumbleLength) % 2 == 0) ColorSet.DARK else ColorSet.LIGHT
        segments.add(seg)
    }

    private fun addRoad(enter: Int, hold: Int, leave: Int, curve: Float, height: Float) {
        val startY = lastY()
        val endY = startY + height
        val total = enter + hold + leave
        for (n in 0 until enter)
            addSegment(easeIn(0f, curve, n.toFloat() / enter), easeInOut(startY, endY, n.toFloat() / total))
        for (n in 0 until hold)
            addSegment(curve, easeInOut(startY, endY, (enter + n).toFloat() / total))
        for (n in 0 until leave)
            addSegment(easeInOut(curve, 0f, n.toFloat() / leave), easeInOut(startY, endY, (enter + hold + n).toFloat() / total))
    }

    private fun addStraight(num: Int = lenMedium) = addRoad(num, num, num, 0f, 0f)

    private fun addCurve(num: Int = lenMedium, curve: Float = curveMedium, height: Float = hillNone) =
        addRoad(num, num, num, curve, height)

    private fun addHill(num: Int = lenMedium, height: Float = hillMedium) =
        addRoad(num, num, num, 0f, height)

    private fun addLowRollingHills(num: Int = lenShort, height: Float = hillLow) {
        addRoad(num, num, num, 0f, height / 2f)
        addRoad(num, num, num, 0f, -height)
        addRoad(num, num, num, curveEasy, height)
        addRoad(num, num, num, 0f, 0f)
        addRoad(num, num, num, -curveEasy, height / 2f)
        addRoad(num, num, num, 0f, 0f)
    }

    private fun addSCurves() {
        addRoad(lenMedium, lenMedium, lenMedium, -curveEasy, hillNone)
        addRoad(lenMedium, lenMedium, lenMedium, curveMedium, hillMedium)
        addRoad(lenMedium, lenMedium, lenMedium, curveEasy, -hillLow)
        addRoad(lenMedium, lenMedium, lenMedium, -curveEasy, hillMedium)
        addRoad(lenMedium, lenMedium, lenMedium, -curveMedium, -hillMedium)
    }

    private fun addDownhillToEnd(num: Int = 200) {
        addRoad(num, num, num, -curveEasy, -lastY() / segmentLength)
    }

    /** Build a fresh, varied track. */
    fun build() {
        segments.clear()

        addStraight(lenShort / 2)
        addLowRollingHills()
        addSCurves()
        addCurve(lenMedium, curveMedium, hillLow)
        addLowRollingHills()
        addCurve(lenLong * 2, curveMedium, hillMedium)
        addStraight()
        addHill(lenMedium, hillHigh)
        addSCurves()
        addCurve(lenLong, -curveMedium, hillNone)
        addHill(lenLong, hillHigh)
        addCurve(lenLong, curveMedium, -hillLow)
        addHill(lenLong, -hillMedium)
        addStraight()
        addSCurves()
        addCurve(lenLong, -curveEasy, hillLow)
        addStraight()

        // Start / finish line decoration
        for (n in 0 until rumbleLength) segments[n].color = ColorSet.START
        for (n in 1..rumbleLength) segments[segments.size - n].color = ColorSet.FINISH

        trackLength = segments.size * segmentLength
    }
}
