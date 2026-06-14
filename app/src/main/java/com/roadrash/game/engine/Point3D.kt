package com.roadrash.game.engine

/**
 * A single point in the pseudo-3D world that knows how to project itself
 * from world space, through camera space, and onto the 2D screen.
 */
class Point3D {
    // World space
    var worldX = 0f
    var worldY = 0f
    var worldZ = 0f

    // Camera space (relative to camera)
    var cameraX = 0f
    var cameraY = 0f
    var cameraZ = 0f

    // Screen space (pixels)
    var screenX = 0f
    var screenY = 0f
    var screenW = 0f
    var scale = 0f

    /**
     * Project this point onto the screen given the camera position/depth and
     * the viewport dimensions. [roadWidth] sets the half-width used for the
     * projected screen width.
     */
    fun project(
        cameraXPos: Float,
        cameraYPos: Float,
        cameraZPos: Float,
        cameraDepth: Float,
        width: Int,
        height: Int,
        roadWidth: Float
    ) {
        cameraX = worldX - cameraXPos
        cameraY = worldY - cameraYPos
        cameraZ = worldZ - cameraZPos
        scale = if (cameraZ != 0f) cameraDepth / cameraZ else 0f
        screenX = (width / 2f) + (scale * cameraX * width / 2f)
        screenY = (height / 2f) - (scale * cameraY * height / 2f)
        screenW = scale * roadWidth * width / 2f
    }
}
