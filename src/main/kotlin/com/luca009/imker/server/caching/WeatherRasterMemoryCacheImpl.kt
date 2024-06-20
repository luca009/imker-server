package com.luca009.imker.server.caching

import com.luca009.imker.server.caching.model.WeatherRasterMemoryCache
import com.luca009.imker.server.caching.model.WeatherRasterTimeSnappingType
import com.luca009.imker.server.parser.model.*
import com.luca009.imker.server.queries.TimeQueryHelper.getClosest
import com.luca009.imker.server.queries.TimeQueryHelper.getEarliest
import com.luca009.imker.server.queries.TimeQueryHelper.getLatest
import java.time.ZonedDateTime
import java.util.EnumMap
import kotlin.reflect.KClass

class WeatherRasterMemoryCacheImpl : WeatherRasterMemoryCache {
    // This is just a basic map of all weather variables that are loaded into memory, storing their data as well
    // The other functions in this class aren't really important, since they essentially just wrap this map
    private val store: EnumMap<WeatherVariableType, WeatherVariableTimeRasterSlice> =
        EnumMap(WeatherVariableType::class.java)

    override fun setVariable(weatherVariable: WeatherVariableType, variableData: WeatherVariableTimeRasterSlice) {
        store[weatherVariable] = variableData
    }

    override fun setVariableAtTime(
        weatherVariable: WeatherVariableType,
        variableData: WeatherVariableRasterSlice,
        time: ZonedDateTime
    ) {
        store[weatherVariable]?.setSlice(time, variableData)
    }

    override fun variableExists(weatherVariable: WeatherVariableType): Boolean {
        return store.containsKey(weatherVariable)
    }

    override fun variableExistsAtTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean {
        return store[weatherVariable]?.variableSlices?.containsKey(time) == true
    }

    override fun variableExistsAtTimeAndPosition(
        weatherVariable: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable2dCoordinate
    ): Boolean {
        val raster = store[weatherVariable]?.variableSlices?.get(time)
        requireNotNull(raster) { return false }

        val xMax = raster.dimensions[WeatherVariableRasterDimensionType.X]?.size ?: return false
        val yMax = raster.dimensions[WeatherVariableRasterDimensionType.Y]?.size ?: return false

        return coordinate.isInRange(xMax, yMax)
    }

    override fun getVariable(weatherVariable: WeatherVariableType): WeatherVariableTimeRasterSlice? {
        return store[weatherVariable]
    }

    override fun getVariableAtTime(
        weatherVariable: WeatherVariableType,
        time: ZonedDateTime
    ): WeatherVariableRasterSlice? {
        return store[weatherVariable]?.variableSlices?.get(time)
    }

    override fun getVariableAtPosition(
        weatherVariable: WeatherVariableType,
        coordinate: WeatherVariable2dCoordinate,
        timeLimit: Int
    ): WeatherVariableTimeSlice? {
        val variableSlice = store[weatherVariable]
        requireNotNull(variableSlice) { return null }

        return variableSlice.subSliceAt2dPosition(0, timeLimit, coordinate)
    }

    override fun getVariableAtTimeAndPosition(
        weatherVariable: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable2dCoordinate
    ): Double? {
        val slices = store[weatherVariable]?.variableSlices
        requireNotNull(slices) {
            // We don't have the correct slice
            return null
        }

        val raster = slices[time]
        requireNotNull(raster) {
            // time is out of range
            return null
        }

        return raster.getDoubleOrNull(coordinate.xIndex, coordinate.yIndex)
    }

    override fun getTimes(weatherVariable: WeatherVariableType): List<ZonedDateTime>? {
        val variableSlices = store[weatherVariable]?.variableSlices
        requireNotNull(variableSlices) { return null }

        return variableSlices.keys.sorted()
    }

    override fun getSnappedTime(
        weatherVariable: WeatherVariableType,
        time: ZonedDateTime,
        timeSnappingType: WeatherRasterTimeSnappingType
    ): ZonedDateTime? {
        val variableSlices = store[weatherVariable]?.variableSlices
        requireNotNull(variableSlices) { return null }

        return when (timeSnappingType) {
            WeatherRasterTimeSnappingType.Earliest -> variableSlices.keys.getEarliest(time)
            WeatherRasterTimeSnappingType.Closest -> variableSlices.keys.getClosest(time)
            WeatherRasterTimeSnappingType.Latest -> variableSlices.keys.getLatest(time)
        }
    }

    override fun containsTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean {
        val times = store[weatherVariable]?.variableSlices?.keys
        requireNotNull(times) { return false }

        val min = times.minOrNull() ?: return false
        val max = times.maxOrNull() ?: return false

        return time in min..max
    }

    override fun containsExactTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean {
        val variableSlices = store[weatherVariable]?.variableSlices
        requireNotNull(variableSlices) { return false }

        return variableSlices.containsKey(time)
    }

    override fun containsLatLon(
        weatherVariable: WeatherVariableType?,
        latitude: Double,
        longitude: Double
    ): Boolean {
        // TODO: caching of coordinates?
        return false
    }

    override fun latLonToCoordinates(
        weatherVariable: WeatherVariableType,
        latitude: Double,
        longitude: Double
    ): WeatherVariable2dCoordinate? {
        // TODO: caching of coordinates?
        return null
    }
}
