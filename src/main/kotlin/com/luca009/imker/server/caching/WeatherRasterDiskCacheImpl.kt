package com.luca009.imker.server.caching

import com.luca009.imker.server.caching.model.WeatherRasterDiskCache
import com.luca009.imker.server.caching.model.WeatherRasterTimeSnappingType
import com.luca009.imker.server.parser.model.*
import com.luca009.imker.server.queries.TimeQueryHelper.getClosest
import com.luca009.imker.server.queries.TimeQueryHelper.getEarliest
import com.luca009.imker.server.queries.TimeQueryHelper.getLatest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

class WeatherRasterDiskCacheImpl(
    val dataParser: WeatherDataParser
) : WeatherRasterDiskCache {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun variableExists(weatherVariableType: WeatherVariableType): Boolean {
        return dataParser.getVariable(weatherVariableType) != null
    }

    override fun variableExistsAtTime(weatherVariableType: WeatherVariableType, time: ZonedDateTime): Boolean {
        return dataParser.gridTimeSliceExists(weatherVariableType, time)
    }

    override fun variableExistsAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable2dCoordinate
    ): Boolean {
        return dataParser.gridTimeAnd2dPositionSliceExists(weatherVariableType, time, coordinate)
    }

    override fun getVariable(weatherVariableType: WeatherVariableType): WeatherVariableTimeRasterSlice? {
        val raster = dataParser.getGridEntireSlice(weatherVariableType)
        require(raster?.isDouble() == true) {
            logger.warn("Data type of $weatherVariableType was not double. This is currently not supported. Returning null.")
            return null
        }

        return raster
    }

    override fun getVariableAtTime(
        weatherVariableType: WeatherVariableType,
        time: ZonedDateTime
    ): WeatherVariableRasterSlice? {
        val raster = dataParser.getGridRasterSlice(weatherVariableType, time)
        require(raster?.isDouble() == true) {
            logger.warn("Data type of $weatherVariableType was not double. This is currently not supported. Returning null.")
            return null
        }

        return raster
    }

    override fun getVariableAtPosition(
        weatherVariableType: WeatherVariableType,
        coordinate: WeatherVariable2dCoordinate,
        timeLimit: Int
    ): WeatherVariableTimeSlice? {
        val series = dataParser.getGridTimeSeriesAt2dPosition(weatherVariableType, coordinate, timeLimit)
        require(series?.isDouble() == true) {
            logger.warn("Data type of $weatherVariableType was not double. This is currently not supported. Returning null.")
            return null
        }

        return series
    }

    override fun getVariableAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable2dCoordinate
    ): Double? {
        val value = dataParser.getGridTimeAnd2dPositionSlice(weatherVariableType, time, coordinate)
        requireNotNull(value) { return null }

        require(value is Double) {
            logger.warn("Data type of $weatherVariableType was not double. This is currently not supported. Returning null.")
            return null
        }

        return value
    }

    override fun getTimes(weatherVariable: WeatherVariableType): List<ZonedDateTime>? {
        return dataParser.getTimes(weatherVariable)
    }

    override fun getSnappedTime(
        weatherVariable: WeatherVariableType,
        time: ZonedDateTime,
        timeSnappingType: WeatherRasterTimeSnappingType
    ): ZonedDateTime? {
        val times = dataParser.getTimes(weatherVariable) ?: return null

        return when (timeSnappingType) {
            WeatherRasterTimeSnappingType.Earliest -> times.getEarliest(time)
            WeatherRasterTimeSnappingType.Closest -> times.getClosest(time)
            WeatherRasterTimeSnappingType.Latest -> times.getLatest(time)
        }
    }

    override fun containsTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean {
        val times = dataParser.getTimes(weatherVariable) ?: return false
        val min = times.minOrNull() ?: return false
        val max = times.maxOrNull() ?: return false

        return time in min..max
    }

    override fun containsExactTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean {
        val times = dataParser.getTimes(weatherVariable) ?: return false
        return times.contains(time)
    }

    override fun latLonToCoordinates(weatherVariableType: WeatherVariableType, latitude: Double, longitude: Double): WeatherVariable2dCoordinate? {
        val coordinate = dataParser.latLonToCoordinates(weatherVariableType, latitude, longitude)
        requireNotNull(coordinate) { return null }

        return coordinate
    }
}