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

    fun getUnits(weatherVariable: WeatherVariableType): WeatherVariableUnit?
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