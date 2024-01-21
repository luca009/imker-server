package com.luca009.imker.imkerserver.caching.model

import com.luca009.imker.imkerserver.parser.model.WeatherVariableType

interface WeatherRasterCompositeCache : WeatherRasterCache {
    fun updateCaches()
    fun updateCaches(weatherVariables: Set<WeatherVariableType>)
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