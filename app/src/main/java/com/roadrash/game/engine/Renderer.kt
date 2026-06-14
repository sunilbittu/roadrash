package com.roadrash.game.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.min

/**
 * Low-level Canvas drawing helpers for the pseudo-3D road: trapezoids,
 * full segment bands (grass + rumble + road + lanes), sky and fog.
 */
class Renderer {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()
    private var skyShader: Shader? = null
    private var skyShaderHeight = -1

    /** Draw the sky gradient backdrop. */
    fun drawSky(canvas: Canvas, width: Int, height: Int) {
        if (skyShader == null || skyShaderHeight != height) {
            skyShader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                ColorSet.SKY_TOP, ColorSet.SKY_BOTTOM, Shader.TileMode.CLAMP
            )
            skyShaderHeight = height
        }
        fill.shader = skyShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
        fill.shader = null
    }

    /** Solid rectangle fill (used for the grass/ground beneath each band). */
    private fun rect(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int) {
        fill.color = color
        canvas.drawRect(x, y, x + w, y + h, fill)
    }

    /**
     * Draw a four-sided polygon (trapezoid). Points are given in screen space.
     */
    private fun polygon(
        canvas: Canvas,
        x1: Float, y1: Float, x2: Float, y2: Float,
        x3: Float, y3: Float, x4: Float, y4: Float, color: Int
    ) {
        fill.color = color
        path.reset()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        path.lineTo(x3, y3)
        path.lineTo(x4, y4)
        path.close()
        canvas.drawPath(path, fill)
    }

    /**
     * Render one road segment band given the projected near (1) and far (2)
     * edges. [fog] 0..1 blends the band toward the fog color in the distance.
     */
    fun drawSegment(
        canvas: Canvas,
        width: Int,
        lanes: Int,
        x1: Float, y1: Float, w1: Float,
        x2: Float, y2: Float, w2: Float,
        fog: Float,
        color: ColorSet
    ) {
        // Grass fills the whole horizontal band behind the road.
        rect(canvas, 0f, y2, width.toFloat(), y1 - y2, color.grass)

        val r1 = MathUtil.rumbleWidth(w1, lanes)
        val r2 = MathUtil.rumbleWidth(w2, lanes)
        val l1 = MathUtil.laneMarkerWidth(w1, lanes)
        val l2 = MathUtil.laneMarkerWidth(w2, lanes)

        // Rumble strips
        polygon(canvas, x1 - w1 - r1, y1, x1 - w1, y1, x2 - w2, y2, x2 - w2 - r2, y2, color.rumble)
        polygon(canvas, x1 + w1 + r1, y1, x1 + w1, y1, x2 + w2, y2, x2 + w2 + r2, y2, color.rumble)

        // Road surface
        polygon(canvas, x1 - w1, y1, x1 + w1, y1, x2 + w2, y2, x2 - w2, y2, color.road)

        // Lane markers
        if (color.lane != 0) {
            val lanew1 = w1 * 2 / lanes
            val lanew2 = w2 * 2 / lanes
            var lanex1 = x1 - w1 + lanew1
            var lanex2 = x2 - w2 + lanew2
            for (lane in 1 until lanes) {
                polygon(
                    canvas,
                    lanex1 - l1 / 2, y1, lanex1 + l1 / 2, y1,
                    lanex2 + l2 / 2, y2, lanex2 - l2 / 2, y2, color.lane
                )
                lanex1 += lanew1
                lanex2 += lanew2
            }
        }

        if (fog < 1f) {
            applyFog(canvas, 0f, y2, width.toFloat(), y1 - y2, fog)
        }
    }

    /** Overlay a translucent fog rectangle. [fog] 1 = clear, 0 = fully fogged. */
    private fun applyFog(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, fog: Float) {
        val alpha = ((1f - fog) * 255f).toInt().coerceIn(0, 255)
        if (alpha <= 0) return
        fill.color = Color.argb(
            alpha,
            Color.red(ColorSet.FOG), Color.green(ColorSet.FOG), Color.blue(ColorSet.FOG)
        )
        canvas.drawRect(x, max(0f, y), x + w, y + h, fill)
    }

    companion object {
        /** Exponential fog factor for a segment [n] of [total] visible segments. */
        fun fogFactor(n: Int, total: Int, density: Float): Float {
            val z = n.toFloat() / total
            return (1f / Math.exp((z * z * density).toDouble())).toFloat().let { min(1f, it) }
        }
    }
}
