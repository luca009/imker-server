package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.*
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.imkerserver.parser.model.WeatherDataParser
import com.luca009.imker.imkerserver.parser.model.WeatherVariable2dCoordinate
import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WeatherRasterCompositeCacheImpl(
    val configuration: WeatherRasterCompositeCacheConfiguration,
    private val dataParser: WeatherDataParser,
    private val variableMapper: WeatherVariableFileNameMapper,
    private val memoryCache: WeatherRasterMemoryCache,
    private val diskCache: WeatherRasterDiskCache,
    private val weatherRasterCacheHelper: WeatherRasterCacheHelper
) : WeatherRasterCompositeCache {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Updates all the memory cached variables
     */
    override fun updateCaches() {
        configuration.variablesInMemory
            .forEach {
                updateCache(it)
            }
    }

    /**
     * Updates the memory cached variables in [weatherVariables]
     */
    override fun updateCaches(weatherVariables: Set<WeatherVariableType>) {
        configuration.variablesInMemory
            .intersect(weatherVariables)
            .forEach {
                updateCache(it)
            }
    }

    private fun updateCache(weatherVariable: WeatherVariableType) {
        if (!dataParser.getDataSources().contains(variableMapper.getWeatherVariableFileRule(weatherVariable)))
            return

        val variableName = variableMapper.getWeatherVariableName(weatherVariable) ?: return
        if (dataParser.getRawVariable(variableName)?.type != "DOUBLE")
            return // Make sure the variable is actually a double, since that is the only type that can be stored at the moment

        val variableData = weatherRasterCacheHelper.arraysToWeatherVariableSlice(
            dataParser.getGridEntireSlice(variableName) ?: return
        ) ?: return

        memoryCache.setVariable(weatherVariable, variableData)
    }

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

    override fun getVariable(weatherVariableType: WeatherVariableType): WeatherVariableSlice? {
        if (configuration.ignoredVariables.contains(weatherVariableType)) {
            return null
        }

        if (configuration.variablesInMemory.contains(weatherVariableType)) {
            val memoryValue = memoryCache.getVariable(weatherVariableType)

            if (memoryValue == null) {
                logger.warn("Memory cache does not contain variable $weatherVariableType despite being configured to do so. Defaulting to disk cache.")
            }
            else {
                return memoryValue
            }
        }

        return diskCache.getVariable(weatherVariableType)
    }

    override fun getVariableAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): WeatherVariable2dRasterSlice? {
        if (configuration.ignoredVariables.contains(weatherVariableType)) {
            return null
        }

        if (configuration.variablesInMemory.contains(weatherVariableType)) {
            val memoryValue = memoryCache.getVariableAtTime(weatherVariableType, timeIndex)

            if (memoryValue == null) {
                logger.warn("Memory cache does not contain variable $weatherVariableType despite being configured to do so. Defaulting to disk cache.")
            }
            else {
                return memoryValue
            }
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

        if (configuration.variablesInMemory.contains(weatherVariableType)) {
            val memoryValue = memoryCache.getVariableAtTimeAndPosition(weatherVariableType, timeIndex, coordinate)

            if (memoryValue == null) {
                logger.warn("Memory cache does not contain variable $weatherVariableType despite being configured to do so. Defaulting to disk cache.")
            }
            else {
                return memoryValue
            }
        }

        return diskCache.getVariableAtTimeAndPosition(weatherVariableType, timeIndex, coordinate)
    }
}