package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.*
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.imkerserver.parser.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

class WeatherRasterCompositeCacheImpl(
    val configuration: WeatherRasterCompositeCacheConfiguration,
    private val dataParser: WeatherDataParser,
    private val variableMapper: WeatherVariableFileNameMapper,
    private val unitMapper: WeatherVariableUnitMapper,
    private val memoryCache: WeatherRasterMemoryCache,
    private val diskCache: WeatherRasterDiskCache,
    private val weatherRasterCacheHelper: WeatherRasterCacheHelper,
    private val timeCache: WeatherTimeCache,
    private val unitCache: WeatherVariableUnitCache
) : WeatherRasterCompositeCache {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private fun updateCache(weatherVariable: WeatherVariableType) {
        if (variableMapper.getMatchingFileName(weatherVariable, dataParser.getDataSources()) == null)
            return

        val variableName = variableMapper.getWeatherVariableName(weatherVariable) ?: return
        if (dataParser.getRawVariable(variableName)?.type != "DOUBLE")
            return // Make sure the variable is actually a double, since that is the only type that can be stored at the moment

        val variableData = weatherRasterCacheHelper.arraysToWeatherVariableSlice(
            dataParser.getGridEntireSlice(variableName) ?: return
        ) ?: return

        memoryCache.setVariable(weatherVariable, variableData)
    }

    override fun updateCaches() {
        updateCaches(variableMapper.getWeatherVariables())
    }

    override fun updateCaches(weatherVariables: Set<WeatherVariableType>) {
        configuration.variablesInMemory
            .intersect(weatherVariables)
            .forEach {
                updateCache(it)
            }

        weatherVariables.forEach {
            val variableName = variableMapper.getWeatherVariableName(it) ?: return@forEach

            val times = dataParser.getTimes(variableName)
            if (times != null) {
                timeCache.setTimes(it, times)
            }

            val rawVariable = dataParser.getRawVariable(variableName) ?: return@forEach
            val unitEnum = unitMapper.getUnits(rawVariable.unitType) ?: return@forEach
            unitCache.setUnits(it, unitEnum)
        }
    }

    override fun getEarliestTimeIndex(weatherVariable: WeatherVariableType, time: ZonedDateTime): Int? = timeCache.getEarliestIndex(weatherVariable, time)
    override fun getClosestTimeIndex(weatherVariable: WeatherVariableType, time: ZonedDateTime): Int? = timeCache.getClosestIndex(weatherVariable, time)
    override fun getLatestTimeIndex(weatherVariable: WeatherVariableType, time: ZonedDateTime): Int? = timeCache.getLatestIndex(weatherVariable, time)
    override fun getEarliestTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): ZonedDateTime? = timeCache.getEarliestTime(weatherVariable, time)
    override fun getClosestTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): ZonedDateTime? = timeCache.getClosestTime(weatherVariable, time)
    override fun getLatestTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): ZonedDateTime? = timeCache.getLatestTime(weatherVariable, time)
    override fun getTime(weatherVariable: WeatherVariableType, index: Int): ZonedDateTime? = timeCache.getTime(weatherVariable, index)
    override fun containsTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean = timeCache.containsTime(weatherVariable, time)
    override fun containsTimeIndex(weatherVariable: WeatherVariableType, index: Int): Boolean = timeCache.containsTimeIndex(weatherVariable, index)

    override fun getUnits(weatherVariable: WeatherVariableType): WeatherVariableUnit? = unitCache.getUnits(weatherVariable)

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

    override fun getVariable(weatherVariableType: WeatherVariableType): WeatherVariableSlice? {
        if (configuration.ignoredVariables.contains(weatherVariableType)) {
            return null
        }

        if (configuration.variablesInMemory.contains(weatherVariableType) &&
            assertVariableExistsInMemoryCache(weatherVariableType)) {
            return memoryCache.getVariable(weatherVariableType)
        }

        return diskCache.getVariable(weatherVariableType)
    }

    override fun getVariableAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): WeatherVariable2dRasterSlice? {
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