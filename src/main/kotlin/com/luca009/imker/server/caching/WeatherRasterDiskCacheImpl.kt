package com.luca009.imker.server.caching

import com.luca009.imker.server.caching.model.WeatherRasterDiskCache
import com.luca009.imker.server.parser.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WeatherRasterDiskCacheImpl(
    val dataParser: WeatherDataParser
) : WeatherRasterDiskCache {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun variableExists(weatherVariableType: WeatherVariableType): Boolean {
        return dataParser.getVariable(weatherVariableType) != null
    }

    override fun variableExistsAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): Boolean {
        return dataParser.gridTimeSliceExists(weatherVariableType, timeIndex)
    }

    override fun variableExistsAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ): Boolean {
        return dataParser.gridTimeAnd2dPositionSliceExists(weatherVariableType, timeIndex, coordinate)
    }

    override fun getVariable(weatherVariableType: WeatherVariableType): WeatherVariableTimeRasterSlice? {
        val raster = dataParser.getGridEntireSlice(weatherVariableType)
        require(raster?.isDouble() == true) {
            logger.warn("Data type of $weatherVariableType was not double. This is currently not supported. Returning null.")
            return null
        }

        return raster
    }

    override fun getVariableAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): WeatherVariableRasterSlice? {
        val raster = dataParser.getGridTimeSlice(weatherVariableType, timeIndex)
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
        val value = dataParser.getGridTimeAnd2dPositionSlice(weatherVariableType, timeIndex, coordinate)
        requireNotNull(value) { return null }

        require(value is Double) {
            logger.warn("Data type of $weatherVariableType was not double. This is currently not supported. Returning null.")
            return null
        }

        return value
    }

    override fun latLonToCoordinates(weatherVariableType: WeatherVariableType, latitude: Double, longitude: Double): WeatherVariable2dCoordinate? {
        val coordinate = dataParser.latLonToCoordinates(weatherVariableType, latitude, longitude)
        requireNotNull(coordinate) { return null }

        return coordinate
    }
}