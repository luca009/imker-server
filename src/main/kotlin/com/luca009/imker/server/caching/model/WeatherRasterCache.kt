 package com.luca009.imker.server.caching.model

 import com.luca009.imker.server.parser.model.*
 import java.time.ZonedDateTime

 /**
 * A cache for weather raster data. Supports getting weather variables.
 */
interface WeatherRasterCache {
    fun variableExists(weatherVariable: WeatherVariableType): Boolean
    fun variableExistsAtTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean
    fun variableExistsAtTimeAndPosition(weatherVariable: WeatherVariableType, time: ZonedDateTime, coordinate: WeatherVariable2dCoordinate): Boolean

    fun getVariable(weatherVariable: WeatherVariableType): WeatherVariableTimeRasterSlice?
    fun getVariableAtTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): WeatherVariableRasterSlice?
    fun getVariableAtPosition(weatherVariable: WeatherVariableType, coordinate: WeatherVariable2dCoordinate, timeLimit: Int = -1): WeatherVariableTimeSlice?
    fun getVariableAtTimeAndPosition(weatherVariable: WeatherVariableType, time: ZonedDateTime, coordinate: WeatherVariable2dCoordinate): Double?

    fun getTimes(weatherVariable: WeatherVariableType): List<ZonedDateTime>?

     /**
      * Get the nearest time to the specified [time] based on the [timeSnappingType]
      */
     fun getSnappedTime(weatherVariable: WeatherVariableType, time: ZonedDateTime, timeSnappingType: WeatherRasterTimeSnappingType): ZonedDateTime?

     /**
      * Gets whether the [time] is within the defined range of the [weatherVariable]
      */
     fun containsTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean

     /**
      * Gets whether the exact [time] is contained within the [weatherVariable]
      */
     fun containsExactTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean

     /**
      * Gets whether the [weatherVariable] (may be null for any) contains the specified [latitude] and [longitude]
      */
     fun containsLatLon(weatherVariable: WeatherVariableType?, latitude: Double, longitude: Double): Boolean

     /**
      * Gets the [WeatherVariable2dCoordinate] representing the closest point to the specified [latitude] and [longitude] within the [weatherVariable]
      */
     fun latLonToCoordinates(weatherVariable: WeatherVariableType, latitude: Double, longitude: Double): WeatherVariable2dCoordinate?
 }

/**
 * A cache for weather raster data in memory. Supports getting and setting weather variables.
 */
interface WeatherRasterMemoryCache : WeatherRasterCache {
    fun setVariable(weatherVariable: WeatherVariableType, variableData: WeatherVariableTimeRasterSlice)
    fun setVariableAtTime(weatherVariable: WeatherVariableType, variableData: WeatherVariableRasterSlice, time: ZonedDateTime)
}

/**
 * A cache for weather raster data from disk. Supports getting weather variables.
 */
interface WeatherRasterDiskCache : WeatherRasterCache