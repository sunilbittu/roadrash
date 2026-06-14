package com.roadrash.game.engine

/**
 * One slice of the road. [p1] is the near edge (at z = index * segmentLength)
 * and [p2] is the far edge (the start of the next segment).
 */
class Segment(val index: Int) {
    val p1 = Point3D()
    val p2 = Point3D()

    /** Horizontal curvature contributed by this segment. */
    var curve = 0f

    var color: ColorSet = ColorSet.LIGHT

    /** True when this segment has wrapped around behind the camera. */
    var looped = false

    /** Fog amount 0 (clear) .. 1 (fully fogged) for this segment. */
    var fog = 1f

    /** The y clip boundary recorded while rendering (top of this segment). */
    var clip = 0f
}
