package com.luca009.imker.server.caching

import com.luca009.imker.server.caching.model.*
import com.luca009.imker.server.parser.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

class WeatherRasterCompositeCacheImpl(
    val configuration: WeatherRasterCompositeCacheConfiguration,
    private val dataParser: WeatherDataParser,
    private val memoryCache: WeatherRasterMemoryCache,
    private val diskCache: WeatherRasterDiskCache
) : WeatherRasterCompositeCache {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private fun updateCache(weatherVariable: WeatherVariableType) {
        val variableData = dataParser.getGridEntireSlice(weatherVariable) ?: return
        memoryCache.setVariable(weatherVariable, variableData)
    }

    override fun updateCaches() {
        updateCaches(dataParser.getAvailableVariableTypes())
    }

    override fun updateCaches(weatherVariables: Set<WeatherVariableType>) {
        configuration.variablesInMemory
            .intersect(weatherVariables)
            .forEach {
                updateCache(it)
            }
    }

    private fun assertVariableExistsInMemoryCache(variableType: WeatherVariableType): Boolean {
        return if (!memoryCache.variableExists(variableType)) {
            logger.warn("Memory cache does not contain variable $variableType despite being configured to do so. Defaulting to disk cache.")
            false
        } else {
            true
        }
    }

    private inline fun <T> autoCallFunction(weatherVariableType: WeatherVariableType, callback: (WeatherRasterCache) -> T?): T? {
        if (configuration.ignoredVariables.contains(weatherVariableType)) {
            return null
        }

        if (configuration.variablesInMemory.contains(weatherVariableType) &&
            assertVariableExistsInMemoryCache(weatherVariableType)) {
            return callback(memoryCache)
        }

        return callback(diskCache)
    }

    // The functions below aren't really interesting - they merely map all the required functions to the autoCallFunction above
    override fun getSnappedTime(
        weatherVariable: WeatherVariableType,
        time: ZonedDateTime,
        timeSnappingType: WeatherRasterTimeSnappingType
    ) = autoCallFunction(weatherVariable) {
        it.getSnappedTime(weatherVariable, time, timeSnappingType)
    }

    override fun getUnits(weatherVariable: WeatherVariableType): WeatherVariableUnit? = dataParser.getVariable(weatherVariable)?.unitType

    override fun variableExists(weatherVariableType: WeatherVariableType) =
        autoCallFunction(weatherVariableType) {
            it.variableExists(weatherVariableType)
        } ?: false

    override fun variableExistsAtTime(weatherVariableType: WeatherVariableType, time: ZonedDateTime) =
        autoCallFunction(weatherVariableType) {
            it.variableExistsAtTime(weatherVariableType, time)
        } ?: false

    override fun variableExistsAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable2dCoordinate
    ) = autoCallFunction(weatherVariableType) {
        it.variableExistsAtTimeAndPosition(weatherVariableType, time, coordinate)
    } ?: false

    override fun getVariable(weatherVariableType: WeatherVariableType) =
        autoCallFunction(weatherVariableType) {
            it.getVariable(weatherVariableType)
        }

    override fun getVariableAtTime(
        weatherVariableType: WeatherVariableType,
        time: ZonedDateTime
    ) = autoCallFunction(weatherVariableType) {
        it.getVariableAtTime(weatherVariableType, time)
    }

    override fun getVariableAtPosition(
        weatherVariableType: WeatherVariableType,
        coordinate: WeatherVariable2dCoordinate,
        timeLimit: Int
    ) = autoCallFunction(weatherVariableType) {
        it.getVariableAtPosition(weatherVariableType, coordinate, timeLimit)
    }

    override fun getVariableAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable2dCoordinate
    ) = autoCallFunction(weatherVariableType) {
        it.getVariableAtTimeAndPosition(weatherVariableType, time, coordinate)
    }

    override fun getTimes(weatherVariable: WeatherVariableType) =
        autoCallFunction(weatherVariable) {
            it.getTimes(weatherVariable)
        }

    override fun containsTime(weatherVariable: WeatherVariableType, time: ZonedDateTime) =
        autoCallFunction(weatherVariable) {
            it.containsTime(weatherVariable, time)
        } ?: false

    override fun containsExactTime(weatherVariable: WeatherVariableType, time: ZonedDateTime) =
        autoCallFunction(weatherVariable) {
            it.containsExactTime(weatherVariable, time)
        } ?: false


    override fun latLonToCoordinates(
        weatherVariableType: WeatherVariableType,
        latitude: Double,
        longitude: Double
    ): WeatherVariable2dCoordinate? {
        if (configuration.ignoredVariables.contains(weatherVariableType)) {
            return null
        }

        // TODO: caching of coordinates?
        return diskCache.latLonToCoordinates(weatherVariableType, latitude, longitude)
    }
}