package com.roadrash.game.entities

import com.roadrash.game.engine.MathUtil
import com.roadrash.game.engine.MathUtil.limit
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

/**
 * A rival racer. Rivals drive the track, jostle for the racing line, and trade
 * punches with the player when riding alongside them — the heart of Road Rash.
 *
 * [distance] is monotonic total distance travelled (used for race ranking);
 * [z] is the wrapped position along the track used for rendering.
 */
class Opponent(
    distance: Float,
    var x: Float,
    var speed: Float,
    val color: Int,
    val name: String
) {
    enum class State { RACING, KNOCKED }

    var distance = distance
        private set
    var z = 0f
        private set

    var health = 100f
    var state = State.RACING
    var hurtTimer = 0f
    var swingTimer = 0f       // > 0 while throwing a punch at the player
    var attackCooldown = MathUtil.randomFloat(0.5f, 2f)
    var knockTimer = 0f
    var lean = 0f

    private val aggression = MathUtil.randomFloat(0.3f, 1f)
    private var preferredLane = MathUtil.randomFloat(-0.6f, 0.6f)
    private var laneTimer = MathUtil.randomFloat(2f, 5f)

    val isSwinging: Boolean get() = swingTimer > 0f
    val isKnocked: Boolean get() = state == State.KNOCKED

    private fun syncZ(trackLength: Float) {
        z = ((distance % trackLength) + trackLength) % trackLength
    }

    fun place(trackLength: Float) = syncZ(trackLength)

    fun takeHit(amount: Float) {
        if (state == State.KNOCKED) return
        health = (health - amount).coerceAtLeast(0f)
        hurtTimer = 0.35f
        x += sign(if (x == 0f) 1f else x) * 0.12f
        speed *= 0.9f
        if (health <= 0f) knockOut()
    }

    private fun knockOut() {
        state = State.KNOCKED
        knockTimer = 3.5f
        speed *= 0.3f
    }

    /**
     * @return the damage this rival deals to the player this frame (0 if none).
     */
    fun update(
        dt: Float,
        trackLength: Float,
        playerDistance: Float,
        playerX: Float,
        maxSpeed: Float
    ): Float {
        if (hurtTimer > 0f) hurtTimer -= dt
        if (swingTimer > 0f) swingTimer -= dt
        if (attackCooldown > 0f) attackCooldown -= dt

        if (state == State.KNOCKED) {
            knockTimer -= dt
            speed *= (1f - min(1f, dt * 1.5f))
            distance += dt * speed
            syncZ(trackLength)
            if (knockTimer <= 0f) {
                state = State.RACING
                health = 45f
                speed = maxSpeed * 0.5f
            }
            return 0f
        }

        laneTimer -= dt
        if (laneTimer <= 0f) {
            preferredLane = MathUtil.randomFloat(-0.7f, 0.7f)
            laneTimer = MathUtil.randomFloat(2f, 5f)
        }

        val gap = playerDistance - distance     // +: player ahead, -: rival ahead
        val alongside = abs(gap) < ALONGSIDE_Z
        val sameLane = abs(playerX - x) < ALONGSIDE_X

        var targetX = preferredLane
        if (alongside && abs(playerX - x) < 0.6f) {
            targetX = x + sign(playerX - x) * 0.4f * aggression
        }
        targetX = limit(targetX, -0.9f, 0.9f)
        x += (targetX - x) * min(1f, dt * 1.5f)
        lean += (limit(targetX - x, -1f, 1f) - lean) * min(1f, dt * 6f)

        val cruise = maxSpeed * (0.78f + 0.18f * aggression)
        speed += (cruise - speed) * min(1f, dt * 0.6f)
        speed = limit(speed, 0f, maxSpeed)
        distance += dt * speed
        syncZ(trackLength)

        if (alongside && sameLane && attackCooldown <= 0f) {
            swingTimer = 0.3f
            attackCooldown = MathUtil.randomFloat(0.8f, 2.2f)
            return HIT_DAMAGE * aggression
        }
        return 0f
    }

    companion object {
        const val ALONGSIDE_Z = 220f
        const val ALONGSIDE_X = 0.45f
        const val HIT_DAMAGE = 9f
    }
}
