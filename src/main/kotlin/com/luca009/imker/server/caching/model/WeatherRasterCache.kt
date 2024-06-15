 package com.luca009.imker.server.caching.model

 import com.luca009.imker.server.parser.model.*
 import java.time.ZonedDateTime

 /**
 * A cache for weather raster data. Supports getting weather variables.
 */
interface WeatherRasterCache {
    fun variableExists(weatherVariableType: WeatherVariableType): Boolean
    fun variableExistsAtTime(weatherVariableType: WeatherVariableType, time: ZonedDateTime): Boolean
    fun variableExistsAtTimeAndPosition(weatherVariableType: WeatherVariableType, time: ZonedDateTime, coordinate: WeatherVariable2dCoordinate): Boolean

    fun getVariable(weatherVariableType: WeatherVariableType): WeatherVariableTimeRasterSlice?
    fun getVariableAtTime(weatherVariableType: WeatherVariableType, time: ZonedDateTime): WeatherVariableRasterSlice?
    fun getVariableAtPosition(weatherVariableType: WeatherVariableType, coordinate: WeatherVariable2dCoordinate, timeLimit: Int = -1): WeatherVariableTimeSlice?
    fun getVariableAtTimeAndPosition(weatherVariableType: WeatherVariableType, time: ZonedDateTime, coordinate: WeatherVariable2dCoordinate): Double?

    fun getTimes(weatherVariable: WeatherVariableType): List<ZonedDateTime>?

     /**
      * Get the nearest time to the specified [time] based on the [timeSnappingType].
      */
     fun getSnappedTime(weatherVariable: WeatherVariableType, time: ZonedDateTime, timeSnappingType: WeatherRasterTimeSnappingType): ZonedDateTime?

     /**
      * Gets whether the [time] is within the defined range of the [weatherVariable].
      */
     fun containsTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean

     /**
      * Gets whether the exact [time] is contained within the [weatherVariable].
      */
     fun containsExactTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean

     fun containsLatLon(weatherVariableType: WeatherVariableType?, latitude: Double, longitude: Double): Boolean
     fun latLonToCoordinates(weatherVariableType: WeatherVariableType, latitude: Double, longitude: Double): WeatherVariable2dCoordinate?
 }

/**
 * A cache for weather raster data in memory. Supports getting and setting weather variables.
 */
interface WeatherRasterMemoryCache : WeatherRasterCache {
    fun setVariable(weatherVariableType: WeatherVariableType, variableData: WeatherVariableTimeRasterSlice)
    fun setVariableAtTime(weatherVariableType: WeatherVariableType, variableData: WeatherVariableRasterSlice, time: ZonedDateTime)
}

/**
 * A cache for weather raster data from disk. Supports getting weather variables.
 */
interface WeatherRasterDiskCache : WeatherRasterCache