package com.luca009.imker.server.parser.model

interface WeatherVariableCoordinate {
    val xIndex: Int
    val yIndex: Int

    /**
     * Determines if this [WeatherVariableCoordinate] is in range of [xMax] and [yMax].
     */
    fun isInRange(xMax: Int, yMax: Int): Boolean {
        return xIndex >= 0 && yIndex >= 0 &&
                xIndex < xMax && yIndex < yMax
    }
}

/**
 * A 2d integer coordinate.
 */
data class WeatherVariable2dCoordinate(
    override val xIndex: Int,
    override val yIndex: Int
) : WeatherVariableCoordinate

/**
 * A 3d integer coordinate.
 */
data class WeatherVariable3dCoordinate(
    override val xIndex: Int,
    override val yIndex: Int,
    val zIndex: Int
) : WeatherVariableCoordinate {
    /**
     * Determines if this [WeatherVariable3dCoordinate] is in range of [xMax], [yMax] and [zMax].
     */
    fun isInRange(xMax: Int, yMax: Int, zMax: Int): Boolean {
        return isInRange(xMax, yMax) &&
                zIndex > 0 && zIndex < zMax
    }
}