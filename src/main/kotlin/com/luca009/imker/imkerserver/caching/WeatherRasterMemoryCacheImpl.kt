package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.WeatherRasterMemoryCache
import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import com.luca009.imker.imkerserver.caching.model.WeatherVariable2dRasterSlice
import com.luca009.imker.imkerserver.caching.model.WeatherVariableSlice
import com.luca009.imker.imkerserver.parser.model.WeatherVariable2dCoordinate

class WeatherRasterMemoryCacheImpl : WeatherRasterMemoryCache {
    // This is just a basic map of all weather variables that are loaded into memory, storing their data as well
    // The other functions in this class aren't really important, since they essentially just wrap this map
    private val store: MutableMap<WeatherVariableType, WeatherVariableSlice> = mutableMapOf()

    override fun setVariable(weatherVariableType: WeatherVariableType, variableData: WeatherVariableSlice) {
        store[weatherVariableType] = variableData
    }

    override fun setVariableAtTime(
        weatherVariableType: WeatherVariableType,
        variable2dData: WeatherVariable2dRasterSlice,
        timeIndex: Int
    ) {
        store[weatherVariableType]?.setSlice(timeIndex, variable2dData)
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
        val raster = store[weatherVariableType]?.variableSlices?.get(timeIndex)?.raster
        requireNotNull(raster) { return false }

        // Get if coordinate is in range. Since the yIndex might not be in range, but we don't know this when looking it up in the array, use a getOrNull just in case.
        return coordinate.isInRange(raster.getOrNull(coordinate.yIndex)?.count() ?: return false, raster.count())
    }

    override fun getVariable(weatherVariableType: WeatherVariableType): WeatherVariableSlice? {
        return store[weatherVariableType]
    }

    override fun getVariableAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): WeatherVariable2dRasterSlice? {
        return store[weatherVariableType]?.variableSlices?.get(timeIndex)
    }

    override fun getVariableAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ): Double? {
        // Looks confusing, but this is just a lookup in the store for the specified coordinates, just looks a bit ugly chained together like this
        return store[weatherVariableType]?.variableSlices?.get(timeIndex)?.raster?.get(coordinate.yIndex)?.get(coordinate.xIndex)
    }
}