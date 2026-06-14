package com.roadrash.game.engine

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Small helper functions used throughout the pseudo-3D engine and game logic.
 */
object MathUtil {

    /** Linear interpolation between [a] and [b] by [percent] (0..1). */
    fun interpolate(a: Float, b: Float, percent: Float): Float = a + (b - a) * percent

    /** Apply acceleration [accel] over time [dt] to a velocity [v]. */
    fun accelerate(v: Float, accel: Float, dt: Float): Float = v + accel * dt

    /** Clamp [value] into the inclusive range [min]..[max]. */
    fun limit(value: Float, min: Float, max: Float): Float = max(min, min(value, max))

    /** Ease-in curve (quadratic). */
    fun easeIn(a: Float, b: Float, percent: Float): Float = a + (b - a) * percent * percent

    /** Smooth ease-in-out curve. */
    fun easeInOut(a: Float, b: Float, percent: Float): Float =
        a + (b - a) * (-cos(percent * Math.PI).toFloat() / 2f + 0.5f)

    /** Width of the rumble strips relative to the projected road width. */
    fun rumbleWidth(projectedRoadWidth: Float, lanes: Int): Float =
        projectedRoadWidth / max(6, 2 * lanes)

    /** Width of the dashed lane markers relative to the projected road width. */
    fun laneMarkerWidth(projectedRoadWidth: Float, lanes: Int): Float =
        projectedRoadWidth / max(32, 8 * lanes)

    /** Advance a looping track position by [increment], wrapping at [max]. */
    fun increase(start: Float, increment: Float, max: Float): Float {
        var result = start + increment
        while (result >= max) result -= max
        while (result < 0) result += max
        return result
    }

    fun randomInt(min: Int, max: Int): Int = Random.nextInt(min, max + 1)

    fun randomFloat(min: Float, max: Float): Float = min + Random.nextFloat() * (max - min)

    fun <T> randomChoice(items: List<T>): T = items[Random.nextInt(items.size)]
}
