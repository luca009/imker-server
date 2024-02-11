package com.luca009.imker.imkerserver.caching.model

import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import com.luca009.imker.imkerserver.parser.model.WeatherVariableUnit
import java.time.ZonedDateTime

interface WeatherRasterCompositeCache : WeatherRasterCache {
    /**
     * Updates all the memory cached variables and the weather time cache
     */
    fun updateCaches()

    /**
     * Updates the memory cached variables in [weatherVariables]
     */
    fun updateCaches(weatherVariables: Set<WeatherVariableType>)

    /**
     * Get the nearest time index that is smaller than the specified [time]
     */
    fun getEarliestTimeIndex(weatherVariable: WeatherVariableType, time: ZonedDateTime): Int?

    /**
     * Get the nearest time index to the specified [time]
     */
    fun getClosestTimeIndex(weatherVariable: WeatherVariableType, time: ZonedDateTime): Int?

    /**
     * Get the nearest time index that is larger than the specified [time]
     */
    fun getLatestTimeIndex(weatherVariable: WeatherVariableType, time: ZonedDateTime): Int?

    /**
     * Get the nearest time that is smaller than the specified [time]
     */
    fun getEarliestTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): ZonedDateTime?

    /**
     * Get the nearest time to the specified [time]
     */
    fun getClosestTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): ZonedDateTime?

    /**
     * Get the nearest time that is larger than the specified [time]
     */
    fun getLatestTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): ZonedDateTime?

    /**
     * Get the ZonedDateTime associated with the [index]
     */
    fun getTime(weatherVariable: WeatherVariableType, index: Int): ZonedDateTime?

    fun getUnits(weatherVariable: WeatherVariableType): WeatherVariableUnit?

    fun containsTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean
    fun containsTimeIndex(weatherVariable: WeatherVariableType, index: Int): Boolean
}

/**
 * Configuration for a WeatherRasterCacheManagerService.
 * [variablesInMemory] decides which variables will be stored in memory and therefore fast to access.
 * [ignoredVariables] decides which variables will be ignored.
 */
data class WeatherRasterCompositeCacheConfiguration(
    val variablesInMemory: Set<WeatherVariableType>,
    val ignoredVariables: Set<WeatherVariableType>
)