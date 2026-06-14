package com.roadrash.game

import android.graphics.Canvas
import com.roadrash.game.engine.MathUtil
import com.roadrash.game.engine.MathUtil.increase
import com.roadrash.game.engine.MathUtil.interpolate
import com.roadrash.game.engine.Renderer
import com.roadrash.game.engine.Road
import com.roadrash.game.entities.Opponent
import com.roadrash.game.entities.Player
import com.roadrash.game.entities.Traffic
import com.roadrash.game.input.Controls
import com.roadrash.game.ui.Art
import com.roadrash.game.ui.Hud
import kotlin.math.abs
import kotlin.math.tan

/**
 * Owns all game state and orchestrates update + render each frame. Implements
 * a classic pseudo-3D road racer with Road Rash-style melee combat.
 */
class Game(private val controls: Controls) {

    enum class State { MENU, COUNTDOWN, RACING, RESULT }

    // --- Camera / projection ---
    private val cameraHeight = 1000f
    private val fieldOfView = 100.0
    private val cameraDepth = 1.0 / tan(Math.toRadians(fieldOfView / 2.0))
    private val drawDistance = 300
    private val fogDensity = 5f
    private val roadWidth = 2000f

    private val road = Road()
    private val renderer = Renderer()
    private val hud = Hud()

    private val maxSpeed = road.segmentLength * 60f      // ~1 segment / frame @60fps
    private val playerZ = (cameraHeight * cameraDepth).toFloat()
    private val playerColor = 0xFFE8C547.toInt()

    private val player = Player(maxSpeed)
    private val rivals = ArrayList<Opponent>()
    private val traffic = ArrayList<Traffic>()

    var state = State.MENU
        private set

    private var position = 0f            // wrapped camera z for rendering
    private var playerDistance = 0f      // monotonic total distance
    private var raceDistance = 0f
    private var raceTime = 0f
    private var countdown = 0f
    private var flashText: String? = null
    private var flashTimer = 0f
    private var playerAttackDir = 1f
    private var resultWon = false
    private var finalRank = 1

    private val rivalNames = listOf("Viper", "Slash", "Diesel", "Mad Dog", "Ripper", "Natasha", "Bones")

    init {
        road.build()
        raceDistance = road.trackLength
    }

    // ---------------------------------------------------------------- lifecycle

    private fun startRace() {
        player.reset()
        position = 0f
        playerDistance = 0f
        raceTime = 0f
        countdown = COUNTDOWN_TIME
        flashText = null
        flashTimer = 0f

        rivals.clear()
        for (i in 0 until NUM_RIVALS) {
            val r = Opponent(
                distance = MathUtil.randomFloat(road.segmentLength * 2, road.segmentLength * 16),
                x = MathUtil.randomFloat(-0.7f, 0.7f),
                speed = maxSpeed * 0.65f,
                color = RIVAL_COLORS[i % RIVAL_COLORS.size],
                name = rivalNames[i % rivalNames.size]
            )
            r.place(road.trackLength)
            rivals.add(r)
        }

        traffic.clear()
        for (i in 0 until NUM_TRAFFIC) {
            traffic.add(
                Traffic(
                    z = MathUtil.randomFloat(road.segmentLength * 20, road.trackLength),
                    x = MathUtil.randomFloat(-0.6f, 0.6f),
                    speed = maxSpeed * MathUtil.randomFloat(0.18f, 0.34f),
                    color = Traffic.randomColor()
                )
            )
        }
        state = State.COUNTDOWN
    }

    // ------------------------------------------------------------------- update

    fun update(dtRaw: Float) {
        val dt = dtRaw.coerceAtMost(0.05f)   // clamp big hitches
        val input = controls.input

        when (state) {
            State.MENU -> if (consumeTap()) startRace()

            State.COUNTDOWN -> {
                countdown -= dt
                flashText = when {
                    countdown > 2f -> "3"
                    countdown > 1f -> "2"
                    countdown > 0f -> "1"
                    else -> "GO!"
                }
                if (countdown <= -0.7f) {
                    state = State.RACING
                    flashTimer = 0f
                    flashText = null
                }
            }

            State.RACING -> updateRacing(dt, input)

            State.RESULT -> if (consumeTap()) state = State.MENU
        }

        if (flashTimer > 0f) flashTimer -= dt
    }

    private fun consumeTap(): Boolean {
        if (controls.input.tapDown) {
            controls.input.tapDown = false
            return true
        }
        return false
    }

    private fun updateRacing(dt: Float, input: com.roadrash.game.input.InputState) {
        raceTime += dt
        val playerTrackZ = playerDistance + playerZ

        // Player movement & combat input
        if (player.state == Player.State.RACING && input.attack) {
            if (player.tryAttack()) resolvePlayerAttack()
        }
        player.update(dt, input, road, playerTrackZ % road.trackLength)

        // health regen when not recently hurt
        if (player.state == Player.State.RACING && player.hurtTimer <= 0f) {
            player.health = (player.health + dt * 2.5f).coerceAtMost(100f)
        }

        // advance the player along the track
        playerDistance += dt * player.speed
        position = increase(position, dt * player.speed, road.trackLength)

        // Update rivals (they may punch back)
        for (r in rivals) {
            val dmg = r.update(dt, road.trackLength, playerDistance + playerZ, player.x, maxSpeed)
            if (dmg > 0f && player.state == Player.State.RACING) {
                player.takeHit(dmg)
                flash("OUCH!", 0xFFE74C3C.toInt(), 0.5f)
            }
        }

        // Update traffic and check collisions
        for (t in traffic) {
            t.update(dt, road.trackLength)
            checkTrafficCollision(t)
        }

        updateRank()

        // Win / lose conditions
        if (player.health <= 0f && player.state != Player.State.FINISHED) {
            resultWon = false
            state = State.RESULT
        } else if (playerDistance >= raceDistance && player.state != Player.State.FINISHED) {
            player.state = Player.State.FINISHED
            resultWon = finalRank <= NUM_RIVALS / 2 + 1
            state = State.RESULT
        }
    }

    private fun resolvePlayerAttack() {
        var best: Opponent? = null
        var bestGap = Float.MAX_VALUE
        for (r in rivals) {
            if (r.isKnocked) continue
            val gap = abs((r.distance) - playerDistance)
            if (gap < ATTACK_RANGE_Z && abs(r.x - player.x) < ATTACK_RANGE_X && gap < bestGap) {
                best = r
                bestGap = gap
            }
        }
        best?.let {
            playerAttackDir = if (it.x >= player.x) 1f else -1f
            it.takeHit(PLAYER_HIT_DAMAGE)
            if (it.isKnocked) flash("${it.name} DOWN!", 0xFF2ECC71.toInt(), 1.2f)
            else flash("WHACK!", 0xFFF1C40F.toInt(), 0.4f)
        }
    }

    private fun checkTrafficCollision(t: Traffic) {
        if (player.state != Player.State.RACING) return
        var gap = t.z - (playerDistance % road.trackLength) - playerZ
        if (gap > road.trackLength / 2f) gap -= road.trackLength
        if (gap < -road.trackLength / 2f) gap += road.trackLength
        if (gap in 0f..COLLIDE_Z && abs(player.x - t.x) < COLLIDE_X && player.speed > t.speed) {
            player.crash()
            player.health = (player.health - 22f).coerceAtLeast(0f)
            player.speed = t.speed * 0.5f
            flash("CRASH!", 0xFFE74C3C.toInt(), 1.0f)
        }
    }

    private fun updateRank() {
        var ahead = 0
        for (r in rivals) if (r.distance > playerDistance) ahead++
        finalRank = ahead + 1
    }

    private fun flash(text: String, color: Int, time: Float) {
        flashText = text
        flashColor = color
        flashTimer = time
    }

    private var flashColor = 0xFFFFFFFF.toInt()

    // ------------------------------------------------------------------- render

    fun render(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height
        renderer.drawSky(canvas, width, height)

        renderRoad(canvas, width, height)
        renderSprites(canvas)
        renderPlayer(canvas, width, height)

        when (state) {
            State.MENU -> {
                hud.drawCenter(
                    canvas, width, height, "ROAD RASH",
                    "Tap to race • punch your rivals off their bikes", playerColor
                )
            }
            State.COUNTDOWN -> {
                renderRaceHud(canvas, width, height)
                flashText?.let {
                    hud.drawFlash(canvas, width, height, it, playerColor, height * 0.2f)
                }
            }
            State.RACING -> {
                renderRaceHud(canvas, width, height)
                if (flashTimer > 0f) flashText?.let {
                    hud.drawFlash(canvas, width, height, it, flashColor, height * 0.12f)
                }
            }
            State.RESULT -> {
                renderRaceHud(canvas, width, height)
                val title = if (resultWon) "YOU WIN!" else if (player.health <= 0f) "WASTED" else "RACE OVER"
                val sub = if (resultWon) "Finished P$finalRank • tap to continue"
                else if (player.health <= 0f) "You got knocked out • tap to retry"
                else "Finished P$finalRank • tap to continue"
                val accent = if (resultWon) 0xFF2ECC71.toInt() else 0xFFE74C3C.toInt()
                hud.drawCenter(canvas, width, height, title, sub, accent)
            }
        }

        controls.draw(canvas)
    }

    private fun renderRaceHud(canvas: Canvas, width: Int, height: Int) {
        val mph = (player.speed / maxSpeed * 200f).toInt()
        hud.drawRace(
            canvas, width, height,
            speedMph = mph,
            rank = finalRank,
            totalRacers = NUM_RIVALS + 1,
            rivalsLeft = rivals.count { !it.isKnocked },
            progressPercent = (playerDistance / raceDistance),
            healthPercent = player.health / 100f,
            timeSeconds = raceTime
        )
    }

    private fun renderRoad(canvas: Canvas, width: Int, height: Int) {
        val baseSegment = road.findSegment(position)
        val basePercent = (position % road.segmentLength) / road.segmentLength
        val playerSegment = road.findSegment(position + playerZ)
        val playerPercent = ((position + playerZ) % road.segmentLength) / road.segmentLength
        val playerY = interpolate(playerSegment.p1.worldY, playerSegment.p2.worldY, playerPercent)

        var maxY = height.toFloat()
        var x = 0f
        var dx = -(baseSegment.curve * basePercent)
        val segs = road.segments
        val n = segs.size

        for (i in 0 until drawDistance) {
            val seg = segs[(baseSegment.index + i) % n]
            seg.looped = seg.index < baseSegment.index
            seg.fog = Renderer.fogFactor(i, drawDistance, fogDensity)
            seg.clip = maxY

            val camZ = position - (if (seg.looped) road.trackLength else 0f)
            val camX = player.x * roadWidth - x
            seg.p1.project(camX, playerY + cameraHeight, camZ, cameraDepth.toFloat(), width, height, roadWidth)
            seg.p2.project(camX - dx, playerY + cameraHeight, camZ, cameraDepth.toFloat(), width, height, roadWidth)

            x += dx
            dx += seg.curve

            if (seg.p1.cameraZ <= cameraDepth || seg.p2.screenY >= seg.p1.screenY || seg.p2.screenY >= maxY) {
                continue
            }
            renderer.drawSegment(
                canvas, width, road.lanes,
                seg.p1.screenX, seg.p1.screenY, seg.p1.screenW,
                seg.p2.screenX, seg.p2.screenY, seg.p2.screenW,
                seg.fog, seg.color
            )
            maxY = seg.p2.screenY
        }
    }

    private class Drawable(val rel: Int, val render: () -> Unit)

    private fun renderSprites(canvas: Canvas) {
        val segs = road.segments
        val n = segs.size
        val baseIndex = road.findSegment(position).index
        val drawables = ArrayList<Drawable>()

        fun add(z: Float, render: (seg: com.roadrash.game.engine.Segment) -> Unit) {
            val segIndex = road.findSegment(z).index
            val rel = ((segIndex - baseIndex) % n + n) % n
            if (rel <= 0 || rel >= drawDistance) return
            val seg = segs[segIndex]
            if (seg.p1.scale <= 0f || seg.p1.screenW <= 0f) return
            drawables.add(Drawable(rel) { render(seg) })
        }

        for (t in traffic) add(t.z) { seg ->
            val sx = seg.p1.screenX + t.x * seg.p1.screenW
            val w = seg.p1.screenW * CAR_W
            drawClipped(canvas, seg.clip) {
                Art.drawTrafficCar(canvas, sx, seg.p1.screenY, w, t.color)
            }
        }

        for (r in rivals) add(r.z) { seg ->
            val sx = seg.p1.screenX + r.x * seg.p1.screenW
            val w = seg.p1.screenW * BIKE_W
            drawClipped(canvas, seg.clip) {
                Art.drawRivalBike(
                    canvas, sx, seg.p1.screenY, w, r.color,
                    r.lean, r.hurtTimer > 0f, r.isSwinging, r.isKnocked
                )
            }
        }

        // far to near so nearer sprites draw on top
        drawables.sortByDescending { it.rel }
        for (d in drawables) d.render()
    }

    private inline fun drawClipped(canvas: Canvas, clipBottom: Float, body: () -> Unit) {
        canvas.save()
        canvas.clipRect(0f, 0f, canvas.width.toFloat(), clipBottom)
        body()
        canvas.restore()
    }

    private fun renderPlayer(canvas: Canvas, width: Int, height: Int) {
        if (state == State.MENU) return
        val w = height * 0.34f
        val groundY = height * 0.90f
        val attackPhase =
            if (player.isAttacking) {
                val t = 1f - (player.attackTimer / Player.ATTACK_DURATION)
                // ramp up then back down
                (1f - abs(t - 0.5f) * 2f).coerceIn(0f, 1f)
            } else 0f
        Art.drawPlayerBike(
            canvas, width / 2f, groundY, w, playerColor,
            player.lean, player.bob, player.hurtTimer > 0f, attackPhase, playerAttackDir
        )
    }

    companion object {
        const val NUM_RIVALS = 5
        const val NUM_TRAFFIC = 12
        const val COUNTDOWN_TIME = 3f

        const val BIKE_W = 0.5f
        const val CAR_W = 0.85f

        const val ATTACK_RANGE_Z = 260f
        const val ATTACK_RANGE_X = 0.5f
        const val PLAYER_HIT_DAMAGE = 26f

        const val COLLIDE_Z = 220f
        const val COLLIDE_X = 0.45f

        val RIVAL_COLORS = listOf(
            0xFF8E44AD.toInt(),
            0xFF16A085.toInt(),
            0xFFD35400.toInt(),
            0xFF2980B9.toInt(),
            0xFFC0392B.toInt()
        )
    }
}
