package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.WeatherRasterDiskCache
import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import com.luca009.imker.imkerserver.caching.model.WeatherVariable2dRasterSlice
import com.luca009.imker.imkerserver.caching.model.WeatherVariableSlice
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.imkerserver.parser.model.WeatherDataParser

class WeatherRasterDiskCacheImpl(
    val dataParser: WeatherDataParser,
    val fileNameMapper: WeatherVariableFileNameMapper,
    val weatherRasterCacheHelper: WeatherRasterCacheHelper
) : WeatherRasterDiskCache {
    private fun getSafeVariableName(weatherVariableType: WeatherVariableType): String? {
        val variableName = fileNameMapper.getWeatherVariableName(weatherVariableType)
        val variableFile = fileNameMapper.getWeatherVariableFile(weatherVariableType)

        requireNotNull(variableName) { return null }
        requireNotNull(variableFile) { return null }

        if (!dataParser.getDataSources().contains(variableFile))
            return null

        return variableName
    }

    override fun variableExists(weatherVariableType: WeatherVariableType): Boolean {
        val variableName = getSafeVariableName(weatherVariableType)
        requireNotNull(variableName) { return false }

        return dataParser.getRawVariable(variableName) != null
    }

    override fun variableExistsAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): Boolean {
        val variableName = getSafeVariableName(weatherVariableType)
        requireNotNull(variableName) { return false }

        return dataParser.gridTimeSliceExists(variableName, timeIndex)
    }

    override fun variableExistsAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        timeIndex: Int,
        xIndex: Int,
        yIndex: Int
    ): Boolean {
        val variableName = getSafeVariableName(weatherVariableType)
        requireNotNull(variableName) { return false }

        return dataParser.gridTimeAnd2dPositionSliceExists(variableName, timeIndex, xIndex, yIndex)
    }

    override fun getVariable(weatherVariableType: WeatherVariableType): WeatherVariableSlice? {
        val variableName = getSafeVariableName(weatherVariableType)
        requireNotNull(variableName) { return null }

        val slice = dataParser.getGridEntireSlice(variableName)
        requireNotNull(slice) { return null }

        return weatherRasterCacheHelper.arraysToWeatherVariableSlice(slice)
    }

    override fun getVariableAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): WeatherVariable2dRasterSlice? {
        val variableName = getSafeVariableName(weatherVariableType)
        requireNotNull(variableName) { return null }

        val slice = dataParser.getGridTimeSlice(variableName, timeIndex)
        requireNotNull(slice) { return null }

        val firstSliceObject = slice.firstOrNull()
        if (firstSliceObject !is List<*>)
            return null
        if (firstSliceObject.firstOrNull() !is Double)
            return null

        return WeatherVariable2dRasterSlice(slice as? List<List<Double>> ?: return null)
    }

    override fun getVariableAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        timeIndex: Int,
        xIndex: Int,
        yIndex: Int
    ): Double? {
        val variableName = getSafeVariableName(weatherVariableType)
        requireNotNull(variableName) { return null }

        val value = dataParser.getGridTimeAndPositionSlice(variableName, timeIndex, xIndex, yIndex, 0)
        requireNotNull(value) { return null }

        return value as? Double
    }
}