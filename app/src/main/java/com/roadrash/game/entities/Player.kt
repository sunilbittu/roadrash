package com.roadrash.game.entities

import com.roadrash.game.engine.MathUtil.accelerate
import com.roadrash.game.engine.MathUtil.limit
import com.roadrash.game.engine.Road
import com.roadrash.game.input.InputState
import kotlin.math.min

/**
 * The rider the player controls. Owns lateral position, speed, health and the
 * combat/crash timers. The camera position along the track lives in the Game;
 * the player's own track position is always camera + playerZ.
 */
class Player(val maxSpeed: Float) {

    enum class State { RACING, CRASHED, FINISHED }

    var x = 0f              // lateral offset, -1..1 on road, beyond = off-road
    var speed = 0f
    var health = 100f
    var state = State.RACING

    var lean = 0f           // smoothed visual lean, -1 (left) .. 1 (right)
    var bob = 0f            // engine bob phase

    var crashTimer = 0f
    var attackTimer = 0f    // > 0 while a punch swing is animating
    var attackCooldown = 0f
    var hurtTimer = 0f      // > 0 briefly after taking a hit

    private val accel = maxSpeed / 4f
    private val braking = -maxSpeed
    private val decel = -maxSpeed / 5f
    private val offRoadDecel = -maxSpeed / 2f
    private val offRoadLimit = maxSpeed / 4f
    private val centrifugal = 0.3f

    val isAttacking: Boolean get() = attackTimer > 0f
    val isOffRoad: Boolean get() = x < -1f || x > 1f

    fun reset() {
        x = 0f
        speed = 0f
        health = 100f
        state = State.RACING
        lean = 0f
        crashTimer = 0f
        attackTimer = 0f
        attackCooldown = 0f
        hurtTimer = 0f
    }

    /** Trigger a punch if not on cooldown. Returns true if a swing started. */
    fun tryAttack(): Boolean {
        if (attackCooldown <= 0f && state == State.RACING) {
            attackTimer = ATTACK_DURATION
            attackCooldown = ATTACK_COOLDOWN
            return true
        }
        return false
    }

    /** Apply incoming damage from a rival or hazard. */
    fun takeHit(amount: Float) {
        health = (health - amount).coerceAtLeast(0f)
        hurtTimer = 0.4f
        if (health <= 0f) crash()
    }

    fun crash() {
        if (state == State.RACING) {
            state = State.CRASHED
            crashTimer = CRASH_DURATION
            speed = 0f
        }
    }

    fun update(dt: Float, input: InputState, road: Road, trackZ: Float) {
        bob += dt * (4f + 8f * speed / maxSpeed)

        // tick down timers regardless of state
        if (attackTimer > 0f) attackTimer -= dt
        if (attackCooldown > 0f) attackCooldown -= dt
        if (hurtTimer > 0f) hurtTimer -= dt

        if (state == State.CRASHED) {
            crashTimer -= dt
            speed *= (1f - min(1f, dt * 4f))
            if (crashTimer <= 0f && health > 0f) {
                state = State.RACING
            }
            updateLean(0f, dt)
            return
        }
        if (state == State.FINISHED) {
            speed = accelerate(speed, decel, dt)
            speed = limit(speed, 0f, maxSpeed)
            updateLean(0f, dt)
            return
        }

        val speedPercent = speed / maxSpeed
        val dx = dt * 2.2f * speedPercent

        if (input.left) x -= dx
        else if (input.right) x += dx

        // centrifugal force pushes you to the outside of curves
        val seg = road.findSegment(trackZ)
        x -= dx * speedPercent * seg.curve * centrifugal

        when {
            input.accel -> speed = accelerate(speed, accel, dt)
            input.brake -> speed = accelerate(speed, braking, dt)
            else -> speed = accelerate(speed, decel, dt)
        }

        if (isOffRoad && speed > offRoadLimit) {
            speed = accelerate(speed, offRoadDecel, dt)
        }

        x = limit(x, -2f, 2f)
        speed = limit(speed, 0f, maxSpeed)

        val target = when {
            input.left -> -1f
            input.right -> 1f
            else -> -seg.curve * 0.15f
        }
        updateLean(target, dt)
    }

    private fun updateLean(target: Float, dt: Float) {
        lean += (target - lean) * min(1f, dt * 8f)
        lean = limit(lean, -1.2f, 1.2f)
    }

    companion object {
        const val ATTACK_DURATION = 0.28f
        const val ATTACK_COOLDOWN = 0.5f
        const val CRASH_DURATION = 2.2f
    }
}
