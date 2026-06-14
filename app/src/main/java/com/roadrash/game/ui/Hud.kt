package com.roadrash.game.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/** Heads-up display: speed, race position, lap, health and centre messages. */
class Hud {

    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val textShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 0, 0, 0)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val box = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val rect = RectF()

    fun drawRace(
        canvas: Canvas,
        w: Int,
        h: Int,
        speedMph: Int,
        rank: Int,
        totalRacers: Int,
        rivalsLeft: Int,
        progressPercent: Float,
        healthPercent: Float,
        timeSeconds: Float
    ) {
        val unit = h * 0.045f
        val pad = h * 0.03f

        // Speed (bottom-centre-ish, large)
        label(canvas, "${speedMph}", pad, h - pad * 0.6f, unit * 1.7f, Color.WHITE)
        label(canvas, "MPH", pad + unit * 2.3f, h - pad * 0.6f, unit * 0.7f, 0xFFBDC3C7.toInt())

        // Position
        label(canvas, "POS ${rank}/${totalRacers}", pad, pad + unit, unit, 0xFFF1C40F.toInt())

        // Time
        val mins = (timeSeconds / 60).toInt()
        val secs = (timeSeconds % 60).toInt()
        val ms = ((timeSeconds * 100) % 100).toInt()
        val tStr = String.format("%d:%02d.%02d", mins, secs, ms)
        text.textAlign = Paint.Align.RIGHT
        textShadow.textAlign = Paint.Align.RIGHT
        label(canvas, tStr, w - pad, pad + unit, unit, Color.WHITE)
        label(canvas, "RIVALS $rivalsLeft", w - pad, pad + unit * 2.2f, unit * 0.7f, 0xFFE67E22.toInt())
        text.textAlign = Paint.Align.LEFT
        textShadow.textAlign = Paint.Align.LEFT

        // Health bar (top-centre)
        val barW = w * 0.34f
        val barH = unit * 0.7f
        val barX = (w - barW) / 2f
        val barY = pad
        box.color = 0xAA000000.toInt()
        rect.set(barX - 3, barY - 3, barX + barW + 3, barY + barH + 3)
        canvas.drawRoundRect(rect, barH / 2, barH / 2, box)
        val hp = healthPercent.coerceIn(0f, 1f)
        box.color = when {
            hp > 0.5f -> 0xFF2ECC71.toInt()
            hp > 0.25f -> 0xFFF39C12.toInt()
            else -> 0xFFE74C3C.toInt()
        }
        rect.set(barX, barY, barX + barW * hp, barY + barH)
        canvas.drawRoundRect(rect, barH / 2, barH / 2, box)
        label(canvas, "DAMAGE", barX, barY + barH + unit * 0.7f, unit * 0.55f, 0xFFBDC3C7.toInt())

        // Progress bar (bottom)
        val pBarW = w * 0.5f
        val pBarX = (w - pBarW) / 2f
        val pBarY = h - barH - pad * 0.4f
        box.color = 0x88000000.toInt()
        rect.set(pBarX, pBarY, pBarX + pBarW, pBarY + barH * 0.6f)
        canvas.drawRoundRect(rect, barH / 2, barH / 2, box)
        box.color = 0xFF3498DB.toInt()
        rect.set(pBarX, pBarY, pBarX + pBarW * progressPercent.coerceIn(0f, 1f), pBarY + barH * 0.6f)
        canvas.drawRoundRect(rect, barH / 2, barH / 2, box)
    }

    fun drawCenter(canvas: Canvas, w: Int, h: Int, title: String, subtitle: String?, accent: Int = Color.WHITE) {
        // dim backdrop
        box.color = 0x99000000.toInt()
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), box)
        text.textAlign = Paint.Align.CENTER
        textShadow.textAlign = Paint.Align.CENTER
        label(canvas, title, w / 2f, h * 0.42f, h * 0.11f, accent)
        if (subtitle != null) {
            label(canvas, subtitle, w / 2f, h * 0.58f, h * 0.05f, Color.WHITE)
        }
        text.textAlign = Paint.Align.LEFT
        textShadow.textAlign = Paint.Align.LEFT
    }

    fun drawFlash(canvas: Canvas, w: Int, h: Int, text: String, accent: Int, size: Float) {
        this.text.textAlign = Paint.Align.CENTER
        textShadow.textAlign = Paint.Align.CENTER
        label(canvas, text, w / 2f, h * 0.4f, size, accent)
        this.text.textAlign = Paint.Align.LEFT
        textShadow.textAlign = Paint.Align.LEFT
    }

    private fun label(canvas: Canvas, str: String, x: Float, y: Float, size: Float, color: Int) {
        text.textSize = size
        textShadow.textSize = size
        val off = size * 0.04f + 2f
        canvas.drawText(str, x + off, y + off, textShadow)
        text.color = color
        canvas.drawText(str, x, y, text)
    }
}
