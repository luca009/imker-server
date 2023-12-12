 package com.luca009.imker.imkerserver.caching.model

 import com.luca009.imker.imkerserver.parser.model.WeatherVariableType

 /**
 * A cache for weather raster data. Supports getting weather variables.
 */
interface WeatherRasterCache {
    fun variableExists(weatherVariableType: WeatherVariableType): Boolean
    fun variableExistsAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): Boolean
    fun variableExistsAtTimeAndPosition(weatherVariableType: WeatherVariableType, timeIndex: Int, xIndex: Int, yIndex: Int): Boolean

    fun getVariable(weatherVariableType: WeatherVariableType): WeatherVariableSlice?
    fun getVariableAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): WeatherVariable2dRasterSlice?
    fun getVariableAtTimeAndPosition(weatherVariableType: WeatherVariableType, timeIndex: Int, xIndex: Int, yIndex: Int): Float?
}

/**
 * A cache for weather raster data in memory. Supports getting and setting weather variables.
 */
interface WeatherRasterMemoryCache : WeatherRasterCache {
    fun setVariable(weatherVariableType: WeatherVariableType, variableData: WeatherVariableSlice)
    fun setVariableAtTime(weatherVariableType: WeatherVariableType, variable2dData: WeatherVariable2dRasterSlice, timeIndex: Int)
}

/**
 * A cache for weather raster data from disk. Supports getting weather variables.
 */
interface WeatherRasterDiskCache : WeatherRasterCache

/**
 * A slice of a weather variable, containing multiple [variableSlices] at different time points.
 */
data class WeatherVariableSlice(
    val variableSlices: Array<WeatherVariable2dRasterSlice>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WeatherVariableSlice

        return variableSlices.contentEquals(other.variableSlices)
    }

    override fun hashCode(): Int {
        return variableSlices.contentHashCode()
    }
}

/**
 * A 2d slice of a weather variable at a specified time point.
 */
data class WeatherVariable2dRasterSlice(
    val raster: Array<FloatArray>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WeatherVariable2dRasterSlice

        return raster.contentDeepEquals(other.raster)
    }

    override fun hashCode(): Int {
        return raster.contentDeepHashCode()
    }
}