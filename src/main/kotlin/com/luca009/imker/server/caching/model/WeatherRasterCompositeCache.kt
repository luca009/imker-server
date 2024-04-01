package com.luca009.imker.server.caching.model

import com.luca009.imker.server.parser.model.WeatherVariableType
import com.luca009.imker.server.parser.model.WeatherVariableUnit
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
     * Get the nearest time index to the specified [time] based on the [timeSnappingType]
     */
    fun getTimeIndex(weatherVariable: WeatherVariableType, time: ZonedDateTime, timeSnappingType: WeatherRasterTimeSnappingType): Int?

    /**
     * Get the nearest time to the specified [time] based on the [timeSnappingType]
     */
    fun getSnappedTime(weatherVariable: WeatherVariableType, time: ZonedDateTime, timeSnappingType: WeatherRasterTimeSnappingType): ZonedDateTime?

    /**
     * Get the ZonedDateTime associated with the [index]
     */
    fun getTime(weatherVariable: WeatherVariableType, index: Int): ZonedDateTime?

    fun getUnits(weatherVariable: WeatherVariableType): WeatherVariableUnit?

    fun containsTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean
    fun containsTimeIndex(weatherVariable: WeatherVariableType, index: Int): Boolean
}

enum class WeatherRasterTimeSnappingType {
    /**
     * The nearest time index that is smaller than the specified time
     */
    Earliest,

    /**
     * The nearest time index to the specified time
     */
    Closest,

    /**
     * The nearest time index that is larger than the specified time
     */
    Latest
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