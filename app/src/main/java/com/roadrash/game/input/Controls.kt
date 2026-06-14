package com.roadrash.game.input

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import kotlin.math.hypot

/**
 * On-screen multi-touch controls: steer left/right, accelerate, brake and
 * punch. Lays itself out for the current screen size and both interprets and
 * draws the buttons.
 */
class Controls {

    enum class Kind { LEFT, RIGHT, ACCEL, BRAKE, ATTACK }

    private class Button(
        val kind: Kind,
        var cx: Float = 0f,
        var cy: Float = 0f,
        var r: Float = 0f,
        val color: Int
    ) {
        var pressed = false
        operator fun contains(p: FloatArray): Boolean =
            hypot(p[0] - cx, p[1] - cy) <= r
    }

    val input = InputState()

    private val buttons = listOf(
        Button(Kind.LEFT, color = 0xFFECF0F1.toInt()),
        Button(Kind.RIGHT, color = 0xFFECF0F1.toInt()),
        Button(Kind.ACCEL, color = 0xFF27AE60.toInt()),
        Button(Kind.BRAKE, color = 0xFFC0392B.toInt()),
        Button(Kind.ATTACK, color = 0xFFE67E22.toInt())
    )

    private var width = 0
    private var height = 0

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(160, 255, 255, 255)
    }
    private val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(230, 20, 20, 20)
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 20, 20, 20)
        textAlign = Paint.Align.CENTER
    }
    private val path = Path()

    fun layout(w: Int, h: Int) {
        width = w
        height = h
        val r = h * 0.085f
        val margin = r * 1.35f
        val baseY = h - margin
        stroke.strokeWidth = r * 0.08f
        label.textSize = r * 0.55f

        forEach(Kind.LEFT) { it.r = r; it.cx = margin; it.cy = baseY }
        forEach(Kind.RIGHT) { it.r = r; it.cx = margin + r * 2.4f; it.cy = baseY }
        forEach(Kind.ACCEL) { it.r = r * 1.15f; it.cx = w - margin; it.cy = baseY }
        forEach(Kind.BRAKE) { it.r = r * 0.95f; it.cx = w - margin - r * 2.6f; it.cy = baseY }
        forEach(Kind.ATTACK) { it.r = r; it.cx = w - margin - r * 0.6f; it.cy = baseY - r * 2.5f }
    }

    private inline fun forEach(kind: Kind, action: (Button) -> Unit) {
        buttons.first { it.kind == kind }.let(action)
    }

    private fun btn(kind: Kind) = buttons.first { it.kind == kind }

    /** Update [input] from a touch event. Returns true (event handled). */
    fun onTouch(event: MotionEvent): Boolean {
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            input.tapDown = true
        }

        buttons.forEach { it.pressed = false }
        input.clearMovement()

        val fullUp = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
        val upIndex = if (action == MotionEvent.ACTION_POINTER_UP) event.actionIndex else -1

        if (!fullUp) {
            val p = FloatArray(2)
            for (i in 0 until event.pointerCount) {
                if (i == upIndex) continue
                p[0] = event.getX(i)
                p[1] = event.getY(i)
                for (b in buttons) if (p in b) b.pressed = true
            }
        }

        input.left = btn(Kind.LEFT).pressed
        input.right = btn(Kind.RIGHT).pressed
        input.accel = btn(Kind.ACCEL).pressed
        input.brake = btn(Kind.BRAKE).pressed
        input.attack = btn(Kind.ATTACK).pressed
        return true
    }

    fun draw(canvas: Canvas) {
        for (b in buttons) {
            val baseAlpha = if (b.pressed) 210 else 110
            fill.color = withAlpha(b.color, baseAlpha)
            canvas.drawCircle(b.cx, b.cy, b.r, fill)
            canvas.drawCircle(b.cx, b.cy, b.r, stroke)
            drawGlyph(canvas, b)
        }
    }

    private fun drawGlyph(canvas: Canvas, b: Button) {
        val s = b.r * 0.5f
        when (b.kind) {
            Kind.LEFT -> triangle(canvas, b.cx, b.cy, s, left = true)
            Kind.RIGHT -> triangle(canvas, b.cx, b.cy, s, left = false)
            Kind.ACCEL -> canvas.drawText("GAS", b.cx, b.cy + label.textSize / 3f, label)
            Kind.BRAKE -> canvas.drawText("BRK", b.cx, b.cy + label.textSize / 3f, label)
            Kind.ATTACK -> canvas.drawText("HIT", b.cx, b.cy + label.textSize / 3f, label)
        }
    }

    private fun triangle(canvas: Canvas, cx: Float, cy: Float, s: Float, left: Boolean) {
        path.reset()
        if (left) {
            path.moveTo(cx - s, cy)
            path.lineTo(cx + s * 0.7f, cy - s)
            path.lineTo(cx + s * 0.7f, cy + s)
        } else {
            path.moveTo(cx + s, cy)
            path.lineTo(cx - s * 0.7f, cy - s)
            path.lineTo(cx - s * 0.7f, cy + s)
        }
        path.close()
        canvas.drawPath(path, glyph)
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}
