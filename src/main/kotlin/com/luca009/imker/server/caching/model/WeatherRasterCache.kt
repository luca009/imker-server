 package com.luca009.imker.server.caching.model

 import com.luca009.imker.server.parser.model.WeatherVariable2dCoordinate
 import com.luca009.imker.server.parser.model.WeatherVariableRasterSlice
 import com.luca009.imker.server.parser.model.WeatherVariableSlice
 import com.luca009.imker.server.parser.model.WeatherVariableType

 /**
 * A cache for weather raster data. Supports getting weather variables.
 */
interface WeatherRasterCache {
    fun variableExists(weatherVariableType: WeatherVariableType): Boolean
    fun variableExistsAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): Boolean
    fun variableExistsAtTimeAndPosition(weatherVariableType: WeatherVariableType, timeIndex: Int, coordinate: WeatherVariable2dCoordinate): Boolean

    fun getVariable(weatherVariableType: WeatherVariableType): WeatherVariableSlice?
    fun getVariableAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): WeatherVariableRasterSlice?
    fun getVariableAtTimeAndPosition(weatherVariableType: WeatherVariableType, timeIndex: Int, coordinate: WeatherVariable2dCoordinate): Double?

    fun latLonToCoordinates(weatherVariableType: WeatherVariableType, latitude: Double, longitude: Double): WeatherVariable2dCoordinate?
 }

/**
 * A cache for weather raster data in memory. Supports getting and setting weather variables.
 */
interface WeatherRasterMemoryCache : WeatherRasterCache {
    fun setVariable(weatherVariableType: WeatherVariableType, variableData: WeatherVariableSlice)
    fun setVariableAtTime(weatherVariableType: WeatherVariableType, variableData: WeatherVariableRasterSlice, timeIndex: Int)
}

/**
 * A cache for weather raster data from disk. Supports getting weather variables.
 */
interface WeatherRasterDiskCache : WeatherRasterCache