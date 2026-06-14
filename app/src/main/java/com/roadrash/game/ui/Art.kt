package com.roadrash.game.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.sin

/**
 * Procedurally draws the vehicles (no bitmap assets needed): the player's bike
 * seen from behind, rival bikes, and civilian traffic cars.
 */
object Art {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()
    private val rect = RectF()

    private const val TIRE = 0xFF15171A.toInt()
    private const val SHADOW = 0x66000000

    private fun shadow(canvas: Canvas, cx: Float, groundY: Float, w: Float) {
        fill.color = SHADOW
        rect.set(cx - w * 0.62f, groundY - w * 0.10f, cx + w * 0.62f, groundY + w * 0.10f)
        canvas.drawOval(rect, fill)
    }

    private fun roundRect(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int) {
        fill.color = color
        rect.set(l, t, r, b)
        canvas.drawRoundRect(rect, rad, rad, fill)
    }

    /** Rear view of a motorcycle and rider, base of wheels at [groundY]. */
    fun drawRivalBike(
        canvas: Canvas,
        cx: Float,
        groundY: Float,
        w: Float,
        color: Int,
        lean: Float,
        hurt: Boolean,
        swinging: Boolean,
        knocked: Boolean
    ) {
        if (w < 4f) return
        shadow(canvas, cx, groundY, w)

        if (knocked) {
            // tipped-over bike: a dim sideways blob with skid
            fill.color = withAlpha(color, 150)
            rect.set(cx - w * 0.7f, groundY - w * 0.35f, cx + w * 0.7f, groundY - w * 0.02f)
            canvas.drawRoundRect(rect, w * 0.2f, w * 0.2f, fill)
            fill.color = TIRE
            canvas.drawCircle(cx - w * 0.45f, groundY - w * 0.12f, w * 0.22f, fill)
            canvas.drawCircle(cx + w * 0.45f, groundY - w * 0.12f, w * 0.22f, fill)
            return
        }

        val h = w * 1.7f
        val tilt = lean * w * 0.18f

        // rear tyre
        roundRect(canvas, cx - w * 0.18f, groundY - h * 0.46f, cx + w * 0.18f, groundY, w * 0.12f, TIRE)
        // exhaust / lower body
        roundRect(canvas, cx - w * 0.30f, groundY - h * 0.62f, cx + w * 0.30f, groundY - h * 0.40f, w * 0.1f, 0xFF2B2B2B.toInt())
        // bike body (color)
        roundRect(canvas, cx - w * 0.34f + tilt, groundY - h * 0.78f, cx + w * 0.34f + tilt, groundY - h * 0.55f, w * 0.12f, color)

        // rider torso (jacket)
        val jacket = darken(color, 0.7f)
        path.reset()
        path.moveTo(cx - w * 0.30f + tilt, groundY - h * 0.55f)
        path.lineTo(cx + w * 0.30f + tilt, groundY - h * 0.55f)
        path.lineTo(cx + w * 0.22f + tilt * 1.3f, groundY - h * 0.92f)
        path.lineTo(cx - w * 0.22f + tilt * 1.3f, groundY - h * 0.92f)
        path.close()
        fill.color = jacket
        canvas.drawPath(path, fill)

        // arm (extends sideways when swinging)
        fill.color = jacket
        if (swinging) {
            roundRect(canvas, cx + w * 0.18f, groundY - h * 0.86f, cx + w * 0.78f, groundY - h * 0.72f, w * 0.07f, jacket)
            fill.color = 0xFFE8C39E.toInt()
            canvas.drawCircle(cx + w * 0.80f, groundY - h * 0.79f, w * 0.12f, fill)
        }

        // helmet
        fill.color = withAlpha(color, 255)
        canvas.drawCircle(cx + tilt * 1.4f, groundY - h * 0.99f, w * 0.22f, fill)
        fill.color = 0x55101010
        canvas.drawCircle(cx + tilt * 1.4f, groundY - h * 1.02f, w * 0.13f, fill)

        if (hurt) {
            fill.color = 0x88FFFFFF.toInt()
            canvas.drawCircle(cx + tilt, groundY - h * 0.7f, w * 0.7f, fill)
        }
    }

    /** Rear view of a civilian car, base at [groundY]. */
    fun drawTrafficCar(canvas: Canvas, cx: Float, groundY: Float, w: Float, color: Int) {
        if (w < 4f) return
        shadow(canvas, cx, groundY, w * 1.1f)
        val h = w * 0.85f
        // body
        roundRect(canvas, cx - w * 0.55f, groundY - h * 0.55f, cx + w * 0.55f, groundY - h * 0.02f, w * 0.12f, color)
        // cabin
        roundRect(canvas, cx - w * 0.42f, groundY - h, cx + w * 0.42f, groundY - h * 0.50f, w * 0.12f, darken(color, 0.8f))
        // rear window
        roundRect(canvas, cx - w * 0.34f, groundY - h * 0.92f, cx + w * 0.34f, groundY - h * 0.58f, w * 0.06f, 0xFF1B2631.toInt())
        // tail lights
        fill.color = 0xFFE74C3C.toInt()
        canvas.drawCircle(cx - w * 0.42f, groundY - h * 0.22f, w * 0.08f, fill)
        canvas.drawCircle(cx + w * 0.42f, groundY - h * 0.22f, w * 0.08f, fill)
        // tyres
        fill.color = TIRE
        canvas.drawCircle(cx - w * 0.42f, groundY - h * 0.02f, w * 0.13f, fill)
        canvas.drawCircle(cx + w * 0.42f, groundY - h * 0.02f, w * 0.13f, fill)
    }

    /**
     * Player's bike seen from behind, fixed near the bottom of the screen.
     * [lean] tips the bike, [bob] animates engine vibration, [attackPhase]
     * 0..1 drives the punch swing toward [attackDir] (-1 left, +1 right).
     */
    fun drawPlayerBike(
        canvas: Canvas,
        cx: Float,
        groundY: Float,
        w: Float,
        color: Int,
        lean: Float,
        bob: Float,
        hurt: Boolean,
        attackPhase: Float,
        attackDir: Float
    ) {
        val wobble = sin(bob) * w * 0.012f
        val baseY = groundY + wobble
        shadow(canvas, cx, baseY, w * 1.05f)

        val h = w * 1.6f
        val tilt = lean * w * 0.16f

        // rear tyre
        roundRect(canvas, cx - w * 0.16f, baseY - h * 0.42f, cx + w * 0.16f, baseY, w * 0.12f, TIRE)
        // swingarm / engine
        roundRect(canvas, cx - w * 0.34f, baseY - h * 0.60f, cx + w * 0.34f, baseY - h * 0.38f, w * 0.1f, 0xFF2B2B2B.toInt())
        // bike tail (color)
        roundRect(canvas, cx - w * 0.30f + tilt, baseY - h * 0.74f, cx + w * 0.30f + tilt, baseY - h * 0.52f, w * 0.12f, color)
        // brake light
        fill.color = 0xFFE74C3C.toInt()
        canvas.drawCircle(cx + tilt, baseY - h * 0.63f, w * 0.06f, fill)

        // rider torso
        val jacket = 0xFF34495E.toInt()
        path.reset()
        path.moveTo(cx - w * 0.30f + tilt, baseY - h * 0.52f)
        path.lineTo(cx + w * 0.30f + tilt, baseY - h * 0.52f)
        path.lineTo(cx + w * 0.24f + tilt * 1.3f, baseY - h * 0.92f)
        path.lineTo(cx - w * 0.24f + tilt * 1.3f, baseY - h * 0.92f)
        path.close()
        fill.color = jacket
        canvas.drawPath(path, fill)

        // handlebars hint
        roundRect(canvas, cx - w * 0.42f + tilt * 1.6f, baseY - h * 0.70f, cx + w * 0.42f + tilt * 1.6f, baseY - h * 0.64f, w * 0.04f, 0xFF1C1C1C.toInt())

        // punching arm
        if (attackPhase > 0f) {
            val reach = w * (0.2f + 0.6f * attackPhase)
            val dir = if (attackDir >= 0f) 1f else -1f
            val ax = cx + dir * 0.2f * w
            roundRect(
                canvas,
                minOf(ax, ax + dir * reach), baseY - h * 0.84f,
                maxOf(ax, ax + dir * reach), baseY - h * 0.74f,
                w * 0.06f, jacket
            )
            fill.color = 0xFFE8C39E.toInt()
            canvas.drawCircle(ax + dir * reach, baseY - h * 0.79f, w * 0.13f, fill)
        }

        // helmet
        fill.color = color
        canvas.drawCircle(cx + tilt * 1.4f, baseY - h * 0.99f, w * 0.22f, fill)
        fill.color = 0x66101010
        canvas.drawCircle(cx + tilt * 1.4f, baseY - h * 1.02f, w * 0.13f, fill)

        if (hurt) {
            fill.color = 0x66FFFFFF
            canvas.drawCircle(cx, baseY - h * 0.6f, w * 0.8f, fill)
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun darken(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255)
    )
}
