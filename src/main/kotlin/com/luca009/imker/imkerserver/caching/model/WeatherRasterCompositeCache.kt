package com.luca009.imker.imkerserver.caching.model

import com.luca009.imker.imkerserver.parser.model.WeatherVariableType

interface WeatherRasterCompositeCache : WeatherRasterCache

/**
 * Configuration for a WeatherRasterCacheManagerService.
 * [variablesInMemory] decides which variables will be stored in memory and therefore fast to access.
 * [ignoredVariables] decides which variables will be ignored.
 */
data class WeatherRasterCompositeCacheConfiguration(
    val variablesInMemory: Array<WeatherVariableType>,
    val ignoredVariables: Array<WeatherVariableType>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WeatherRasterCompositeCacheConfiguration

        if (!variablesInMemory.contentEquals(other.variablesInMemory)) return false
        return ignoredVariables.contentEquals(other.ignoredVariables)
    }

    override fun hashCode(): Int {
        var result = variablesInMemory.contentHashCode()
        result = 31 * result + ignoredVariables.contentHashCode()
        return result
    }
}