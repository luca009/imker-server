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
    private val diskCache: WeatherRasterDiskCache,
    private val timeCache: WeatherTimeCache
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

        weatherVariables.forEach {
            val times = dataParser.getTimes(it)
            if (times != null) {
                timeCache.setTimes(it, times)
            }
        }
    }

    override fun getTimeIndex(
        weatherVariable: WeatherVariableType,
        time: ZonedDateTime,
        timeSnappingType: WeatherRasterTimeSnappingType
    ) = when (timeSnappingType) {
        WeatherRasterTimeSnappingType.Earliest -> timeCache.getEarliestIndex(weatherVariable, time)
        WeatherRasterTimeSnappingType.Closest -> timeCache.getClosestIndex(weatherVariable, time)
        WeatherRasterTimeSnappingType.Latest -> timeCache.getLatestIndex(weatherVariable, time)
    }

    override fun getSnappedTime(
        weatherVariable: WeatherVariableType,
        time: ZonedDateTime,
        timeSnappingType: WeatherRasterTimeSnappingType
    ) = when (timeSnappingType) {
        WeatherRasterTimeSnappingType.Earliest -> timeCache.getEarliestTime(weatherVariable, time)
        WeatherRasterTimeSnappingType.Closest -> timeCache.getClosestTime(weatherVariable, time)
        WeatherRasterTimeSnappingType.Latest -> timeCache.getLatestTime(weatherVariable, time)
    }

    override fun getTime(weatherVariable: WeatherVariableType, index: Int): ZonedDateTime? = timeCache.getTime(weatherVariable, index)
    override fun containsTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean = timeCache.containsTime(weatherVariable, time)
    override fun containsTimeIndex(weatherVariable: WeatherVariableType, index: Int): Boolean = timeCache.containsTimeIndex(weatherVariable, index)

    override fun getUnits(weatherVariable: WeatherVariableType): WeatherVariableUnit? = dataParser.getVariable(weatherVariable)?.unitType

    override fun variableExists(weatherVariableType: WeatherVariableType): Boolean {
        if (configuration.ignoredVariables.contains(weatherVariableType)) {
            return false
        }

        return memoryCache.variableExists(weatherVariableType) ||
                diskCache.variableExists(weatherVariableType)
    }

    override fun variableExistsAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): Boolean {
        if (configuration.ignoredVariables.contains(weatherVariableType)) {
            return false
        }

        return memoryCache.variableExistsAtTime(weatherVariableType, timeIndex) ||
                diskCache.variableExistsAtTime(weatherVariableType, timeIndex)
    }

    override fun variableExistsAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ): Boolean {
        if (configuration.ignoredVariables.contains(weatherVariableType)) {
            return false
        }

        return memoryCache.variableExistsAtTimeAndPosition(weatherVariableType, timeIndex, coordinate) ||
                diskCache.variableExistsAtTimeAndPosition(weatherVariableType, timeIndex, coordinate)
    }

    private fun assertVariableExistsInMemoryCache(variableType: WeatherVariableType): Boolean {
        return if (!memoryCache.variableExists(variableType)) {
            logger.warn("Memory cache does not contain variable $variableType despite being configured to do so. Defaulting to disk cache.")
            false
        } else {
            true
        }
    }

    override fun getVariable(weatherVariableType: WeatherVariableType): WeatherVariableTimeRasterSlice? {
        if (configuration.ignoredVariables.contains(weatherVariableType)) {
            return null
        }

        if (configuration.variablesInMemory.contains(weatherVariableType) &&
            assertVariableExistsInMemoryCache(weatherVariableType)) {
            return memoryCache.getVariable(weatherVariableType)
        }

        return diskCache.getVariable(weatherVariableType)
    }

    override fun getVariableAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): WeatherVariableRasterSlice? {
        if (configuration.ignoredVariables.contains(weatherVariableType)) {
            return null
        }

        if (configuration.variablesInMemory.contains(weatherVariableType) &&
            assertVariableExistsInMemoryCache(weatherVariableType)) {
            return memoryCache.getVariableAtTime(weatherVariableType, timeIndex)
        }

        return diskCache.getVariableAtTime(weatherVariableType, timeIndex)
    }

    override fun getVariableAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ): Double? {
        if (configuration.ignoredVariables.contains(weatherVariableType)) {
            return null
        }

        if (configuration.variablesInMemory.contains(weatherVariableType) &&
            assertVariableExistsInMemoryCache(weatherVariableType)) {
            return memoryCache.getVariableAtTimeAndPosition(weatherVariableType, timeIndex, coordinate)
        }
        return diskCache.getVariableAtTimeAndPosition(weatherVariableType, timeIndex, coordinate)
    }

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