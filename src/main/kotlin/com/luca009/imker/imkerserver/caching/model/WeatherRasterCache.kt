 package com.luca009.imker.imkerserver.caching.model

import ucar.nc2.dataset.NetcdfDataset

/**
 * A cache for weather raster data. Supports getting weather variables.
 */
interface WeatherRasterCache {
    fun variableExists(weatherVariable: WeatherVariable): Boolean
    fun variableExistsAtTime(weatherVariable: WeatherVariable, timeIndex: Int): Boolean
    fun variableExistsAtTimeAndPosition(weatherVariable: WeatherVariable, timeIndex: Int, xIndex: Int, yIndex: Int): Boolean

    fun getVariable(weatherVariable: WeatherVariable): WeatherVariableSlice?
    fun getVariableAtTime(weatherVariable: WeatherVariable, timeIndex: Int): WeatherVariable2dRasterSlice?
    fun getVariableAtTimeAndPosition(weatherVariable: WeatherVariable, timeIndex: Int, xIndex: Int, yIndex: Int): Float?
}

/**
 * A cache for weather raster data in memory. Supports getting and setting weather variables.
 */
interface WeatherRasterMemoryCache : WeatherRasterCache {
    fun setVariable(weatherVariable: WeatherVariable, variableData: WeatherVariableSlice)
    fun setVariableAtTime(weatherVariable: WeatherVariable, variable2dData: WeatherVariable2dRasterSlice, timeIndex: Int)
}

/**
 * A cache for weather raster data from a NetCDF file. Supports getting weather variables.
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