package com.luca009.imker.server.caching.model

import com.luca009.imker.server.parser.model.WeatherVariableType
import com.luca009.imker.server.parser.model.WeatherVariableUnit

interface WeatherRasterCompositeCache : WeatherRasterCache {
    /**
     * Updates all the memory cached variables and the weather time cache.
     */
    fun updateCaches()

    /**
     * Updates the memory cached variables in [weatherVariables].
     */
    fun updateCaches(weatherVariables: Set<WeatherVariableType>)

    /**
     * Get the units used by a specified [weatherVariable].
     */
    fun getUnits(weatherVariable: WeatherVariableType): WeatherVariableUnit?
}

enum class WeatherRasterTimeSnappingType {
    /**
     * The nearest time index that is smaller than the specified time.
     */
    Earliest,

    /**
     * The nearest time index to the specified time.
     */
    Closest,

    /**
     * The nearest time index that is larger than the specified time.
     */
    Latest
}

/**
 * Configuration for a WeatherRasterCacheManagerService.
 */
data class WeatherRasterCompositeCacheConfiguration(
    /**
     * Variables to be stored in memory and therefore fast to access.
     */
    val variablesInMemory: Set<WeatherVariableType>,

    /**
     * Variables to be ignored.
     */
    val ignoredVariables: Set<WeatherVariableType>
)