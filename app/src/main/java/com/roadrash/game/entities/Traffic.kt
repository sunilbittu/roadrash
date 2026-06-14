package com.roadrash.game.entities

import com.roadrash.game.engine.MathUtil
import com.roadrash.game.engine.MathUtil.increase

/**
 * Slow civilian traffic that shares the road. Crashing into the back of one
 * wipes the player out, so they double as moving hazards.
 */
class Traffic(
    var z: Float,
    var x: Float,
    var speed: Float,
    val color: Int
) {
    fun update(dt: Float, trackLength: Float) {
        z = increase(z, dt * speed, trackLength)
    }

    companion object {
        val CAR_COLORS = listOf(
            0xFFB03A2E.toInt(), // red
            0xFF2471A3.toInt(), // blue
            0xFFD4AC0D.toInt(), // yellow
            0xFF6C3483.toInt(), // purple
            0xFF34495E.toInt()  // slate
        )

        fun randomColor(): Int = MathUtil.randomChoice(CAR_COLORS)
    }
}
