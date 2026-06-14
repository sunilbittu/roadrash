package com.roadrash.game

import android.graphics.Canvas
import android.view.SurfaceHolder

/**
 * Dedicated render/update loop thread. Drives the [Game] at a target frame rate
 * using a fixed-step accumulator so physics stay stable regardless of device.
 */
class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val game: Game
) : Thread() {

    @Volatile
    var running = false

    override fun run() {
        var lastTime = System.nanoTime()
        var accumulator = 0f

        while (running) {
            val now = System.nanoTime()
            var frame = (now - lastTime) / 1_000_000_000f
            lastTime = now
            if (frame > 0.1f) frame = 0.1f      // avoid spiral of death
            accumulator += frame

            while (accumulator >= STEP) {
                game.update(STEP)
                accumulator -= STEP
            }

            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        game.render(canvas)
                    }
                }
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {
                        // surface may have been destroyed mid-frame
                    }
                }
            }

            // small sleep to cap CPU usage / target ~60fps
            val elapsed = (System.nanoTime() - now) / 1_000_000f
            val sleep = (FRAME_MS - elapsed).toLong()
            if (sleep > 0) {
                try {
                    sleep(sleep)
                } catch (_: InterruptedException) {
                }
            }
        }
    }

    companion object {
        const val STEP = 1f / 60f
        const val FRAME_MS = 1000f / 60f
    }
}
