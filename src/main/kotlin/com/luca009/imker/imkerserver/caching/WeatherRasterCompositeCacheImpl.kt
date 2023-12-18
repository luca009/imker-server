package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.*
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.imkerserver.parser.model.WeatherDataParser
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

    init {
        updateCaches()
    }

    private fun updateCaches() {
        // This is for loading all the variables that should be in memory into memory
        configuration.variablesInMemory
            .forEach {
                if (!dataParser.getDataSources().contains(variableMapper.getWeatherVariableFile(it)))
                    return@forEach

                val variableName = variableMapper.getWeatherVariableName(it) ?: return@forEach
                if (dataParser.getRawVariable(variableName)?.type != "DOUBLE")
                    return@forEach // Make sure the variable is actually a double, since that is the only type that can be stored at the moment

                val variableData = weatherRasterCacheHelper.arraysToWeatherVariableSlice(
                    dataParser.getGridEntireSlice(variableName) ?: return@forEach
                ) ?: return@forEach

                memoryCache.setVariable(it, variableData)
            }
        
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
        xIndex: Int,
        yIndex: Int
    ): Boolean {
        if (configuration.ignoredVariables.contains(weatherVariableType)) {
            return false
        }

        return memoryCache.variableExistsAtTimeAndPosition(weatherVariableType, timeIndex, xIndex, yIndex) ||
                diskCache.variableExistsAtTimeAndPosition(weatherVariableType, timeIndex, xIndex, yIndex)
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
        xIndex: Int,
        yIndex: Int
    ): Double? {
        if (configuration.ignoredVariables.contains(weatherVariableType)) {
            return null
        }

        if (configuration.variablesInMemory.contains(weatherVariableType)) {
            val memoryValue = memoryCache.getVariableAtTimeAndPosition(weatherVariableType, timeIndex, xIndex, yIndex)

            if (memoryValue == null) {
                logger.warn("Memory cache does not contain variable $weatherVariableType despite being configured to do so. Defaulting to disk cache.")
            }
            else {
                return memoryValue
            }
        }

        return diskCache.getVariableAtTimeAndPosition(weatherVariableType, timeIndex, xIndex, yIndex)
    }
}