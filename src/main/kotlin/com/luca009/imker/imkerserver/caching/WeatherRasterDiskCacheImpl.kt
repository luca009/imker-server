package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.WeatherRasterDiskCache
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.imkerserver.parser.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WeatherRasterDiskCacheImpl(
    val dataParser: WeatherDataParser,
    val fileNameMapper: WeatherVariableFileNameMapper
) : WeatherRasterDiskCache {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private fun getSafeVariableName(weatherVariableType: WeatherVariableType): String? {
        val variableName = fileNameMapper.getWeatherVariableName(weatherVariableType)
        val variableFile = fileNameMapper.getMatchingFileName(weatherVariableType, dataParser.getDataSources())

        requireNotNull(variableName) { return null }
        requireNotNull(variableFile) { return null }

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
        coordinate: WeatherVariable2dCoordinate
    ): Boolean {
        val variableName = getSafeVariableName(weatherVariableType)
        requireNotNull(variableName) { return false }

        return dataParser.gridTimeAnd2dPositionSliceExists(variableName, timeIndex, coordinate)
    }

    override fun getVariable(weatherVariableType: WeatherVariableType): WeatherVariableSlice? {
        val variableName = getSafeVariableName(weatherVariableType)
        requireNotNull(variableName) { return null }

        val raster = dataParser.getGridEntireSlice(variableName)
        require(raster?.isDouble() == true) {
            logger.warn("Data type of $weatherVariableType was not double. This is currently not supported. Returning null.")
            return null
        }

        return raster
    }

    override fun getVariableAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): WeatherVariableRasterSlice? {
        val variableName = getSafeVariableName(weatherVariableType)
        requireNotNull(variableName) { return null }

        val raster = dataParser.getGridTimeSlice(variableName, timeIndex)
        require(raster?.isDouble() == true) {
            logger.warn("Data type of $weatherVariableType was not double. This is currently not supported. Returning null.")
            return null
        }

        return raster
    }

    override fun getVariableAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ): Double? {
        val variableName = getSafeVariableName(weatherVariableType)
        requireNotNull(variableName) { return null }

        val value = dataParser.getGridTimeAnd2dPositionSlice(variableName, timeIndex, coordinate)
        requireNotNull(value) { return null }

        require(value is Double) {
            logger.warn("Data type of $weatherVariableType was not double. This is currently not supported. Returning null.")
            return null
        }

        return value
    }

    override fun latLonToCoordinates(weatherVariableType: WeatherVariableType, latitude: Double, longitude: Double): WeatherVariable2dCoordinate? {
        val variableName = getSafeVariableName(weatherVariableType)
        requireNotNull(variableName) { return null }

        val coordinate = dataParser.latLonToCoordinates(variableName, latitude, longitude)
        requireNotNull(coordinate) { return null }

        return coordinate
    }
}