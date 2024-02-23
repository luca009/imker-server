package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.WeatherRasterMemoryCache
import com.luca009.imker.imkerserver.parser.model.*
import java.util.EnumMap

class WeatherRasterMemoryCacheImpl : WeatherRasterMemoryCache {
    // This is just a basic map of all weather variables that are loaded into memory, storing their data as well
    // The other functions in this class aren't really important, since they essentially just wrap this map
    private val store: EnumMap<WeatherVariableType, WeatherVariableSlice> = EnumMap(WeatherVariableType::class.java)

    override fun setVariable(weatherVariableType: WeatherVariableType, variableData: WeatherVariableSlice) {
        store[weatherVariableType] = variableData
    }

    override fun setVariableAtTime(
        weatherVariableType: WeatherVariableType,
        variableData: WeatherVariableRasterSlice,
        timeIndex: Int
    ) {
        store[weatherVariableType]?.setSlice(timeIndex, variableData)
    }

    override fun variableExists(weatherVariableType: WeatherVariableType): Boolean {
        return store.containsKey(weatherVariableType)
    }

    override fun variableExistsAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): Boolean {
        return timeIndex >= 0 &&
                timeIndex < (store[weatherVariableType]?.variableSlices?.count() ?: return false)
    }

    override fun variableExistsAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ): Boolean {
        val raster = store[weatherVariableType]?.variableSlices?.getOrNull(timeIndex)
        requireNotNull(raster) { return false }

        val xMax = raster.dimensions[WeatherVariableRasterDimensionType.x]?.size ?: return false
        val yMax = raster.dimensions[WeatherVariableRasterDimensionType.y]?.size ?: return false

        return coordinate.isInRange(xMax, yMax)
    }

    override fun getVariable(weatherVariableType: WeatherVariableType): WeatherVariableSlice? {
        return store[weatherVariableType]
    }

    override fun getVariableAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): WeatherVariableRasterSlice? {
        return store[weatherVariableType]?.variableSlices?.get(timeIndex)
    }

    override fun getVariableAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ): Double? {
        val slices = store[weatherVariableType]?.variableSlices
        requireNotNull(slices) {
            // We don't have the correct slice
            return null
        }

        val raster = slices.getOrNull(timeIndex)
        requireNotNull(raster) {
            // timeIndex is out of range
            return null
        }

        return raster.getDoubleOrNull(coordinate.xIndex, coordinate.yIndex)
    }

    override fun latLonToCoordinates(
        weatherVariableType: WeatherVariableType,
        latitude: Double,
        longitude: Double
    ): WeatherVariable2dCoordinate? {
        // TODO: caching of coordinates?
        return null
    }
}