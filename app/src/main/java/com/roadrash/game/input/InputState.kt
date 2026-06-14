package com.roadrash.game.input

/** Per-frame snapshot of which controls are currently engaged. */
class InputState {
    var left = false
    var right = false
    var accel = false
    var brake = false
    var attack = false

    /** Set true on a fresh touch-down; consumed by menus to start/continue. */
    var tapDown = false

    fun clearMovement() {
        left = false
        right = false
        accel = false
        brake = false
        attack = false
    }
}
