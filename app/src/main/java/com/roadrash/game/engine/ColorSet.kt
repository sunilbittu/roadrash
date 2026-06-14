package com.roadrash.game.engine

/**
 * Colors used to render one road segment band. [lane] of 0 means "no lane
 * markers" for this band (used for the dark alternating bands).
 */
data class ColorSet(
    val road: Int,
    val grass: Int,
    val rumble: Int,
    val lane: Int = 0
) {
    companion object {
        val LIGHT = ColorSet(
            road = 0xFF6B6B6B.toInt(),
            grass = 0xFF10AA10.toInt(),
            rumble = 0xFFFFFFFF.toInt(),
            lane = 0xFFCCCCCC.toInt()
        )
        val DARK = ColorSet(
            road = 0xFF606060.toInt(),
            grass = 0xFF009A00.toInt(),
            rumble = 0xFFBBBBBB.toInt()
        )
        val START = ColorSet(
            road = 0xFFFFFFFF.toInt(),
            grass = 0xFF10AA10.toInt(),
            rumble = 0xFFFFFFFF.toInt()
        )
        val FINISH = ColorSet(
            road = 0xFF111111.toInt(),
            grass = 0xFF10AA10.toInt(),
            rumble = 0xFF111111.toInt()
        )

        const val FOG = 0xFF005A20.toInt()
        const val SKY_TOP = 0xFF1E5FA8.toInt()
        const val SKY_BOTTOM = 0xFF72C7E8.toInt()
    }
}
