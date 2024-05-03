package com.luca009.imker.server.parser.model

interface WeatherVariableCoordinate {
    val xIndex: Int
    val yIndex: Int

    fun isInRange(xMax: Int, yMax: Int): Boolean {
        return xIndex >= 0 && yIndex >= 0 &&
                xIndex < xMax && yIndex < yMax
    }
}

data class WeatherVariable2dCoordinate(
    override val xIndex: Int,
    override val yIndex: Int
) : WeatherVariableCoordinate

data class WeatherVariable3dCoordinate(
    override val xIndex: Int,
    override val yIndex: Int,
    val zIndex: Int
) : WeatherVariableCoordinate {
    fun isInRange(xMax: Int, yMax: Int, zMax: Int): Boolean {
        return isInRange(xMax, yMax) &&
                zIndex > 0 && zIndex < zMax
    }
}