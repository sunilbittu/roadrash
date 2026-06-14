package com.roadrash.game

import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.roadrash.game.input.Controls

/**
 * The SurfaceView that hosts the game. Wires touch input to [Controls], owns
 * the [Game] and manages the [GameThread] across the surface lifecycle.
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private val controls = Controls()
    private val game = Game(controls)
    private var thread: GameThread? = null

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        controls.layout(width, height)
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        controls.layout(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
    }

    private fun startLoop() {
        if (thread?.running == true) return
        thread = GameThread(holder, game).apply {
            running = true
            start()
        }
    }

    private fun stopLoop() {
        val t = thread ?: return
        t.running = false
        var retry = true
        while (retry) {
            try {
                t.join()
                retry = false
            } catch (_: InterruptedException) {
            }
        }
        thread = null
    }

    fun pause() = stopLoop()

    fun resume() {
        if (holder.surface.isValid) startLoop()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return controls.onTouch(event)
    }
}
