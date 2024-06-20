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

    private inline fun <T> autoCallFunction(weatherVariable: WeatherVariableType, callback: (WeatherRasterCache) -> T?): T? {
        if (configuration.ignoredVariables.contains(weatherVariable)) {
            return null
        }

        if (configuration.variablesInMemory.contains(weatherVariable) &&
            assertVariableExistsInMemoryCache(weatherVariable)) {
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

    override fun variableExists(weatherVariable: WeatherVariableType) =
        autoCallFunction(weatherVariable) {
            it.variableExists(weatherVariable)
        } ?: false

    override fun variableExistsAtTime(weatherVariable: WeatherVariableType, time: ZonedDateTime) =
        autoCallFunction(weatherVariable) {
            it.variableExistsAtTime(weatherVariable, time)
        } ?: false

    override fun variableExistsAtTimeAndPosition(
        weatherVariable: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable2dCoordinate
    ) = autoCallFunction(weatherVariable) {
        it.variableExistsAtTimeAndPosition(weatherVariable, time, coordinate)
    } ?: false

    override fun getVariable(weatherVariable: WeatherVariableType) =
        autoCallFunction(weatherVariable) {
            it.getVariable(weatherVariable)
        }

    override fun getVariableAtTime(
        weatherVariable: WeatherVariableType,
        time: ZonedDateTime
    ) = autoCallFunction(weatherVariable) {
        it.getVariableAtTime(weatherVariable, time)
    }

    override fun getVariableAtPosition(
        weatherVariable: WeatherVariableType,
        coordinate: WeatherVariable2dCoordinate,
        timeLimit: Int
    ) = autoCallFunction(weatherVariable) {
        it.getVariableAtPosition(weatherVariable, coordinate, timeLimit)
    }

    override fun getVariableAtTimeAndPosition(
        weatherVariable: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable2dCoordinate
    ) = autoCallFunction(weatherVariable) {
        it.getVariableAtTimeAndPosition(weatherVariable, time, coordinate)
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

    override fun containsLatLon(
        weatherVariable: WeatherVariableType?,
        latitude: Double,
        longitude: Double
    ): Boolean {
        if (configuration.ignoredVariables.contains(weatherVariable)) {
            return false
        }

        // TODO: caching of coordinates?
        return diskCache.containsLatLon(weatherVariable, latitude, longitude)
    }


    override fun latLonToCoordinates(
        weatherVariable: WeatherVariableType,
        latitude: Double,
        longitude: Double
    ): WeatherVariable2dCoordinate? {
        if (configuration.ignoredVariables.contains(weatherVariable)) {
            return null
        }

        // TODO: caching of coordinates?
        return diskCache.latLonToCoordinates(weatherVariable, latitude, longitude)
    }
}