package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.WeatherRasterMemoryCache
import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import com.luca009.imker.imkerserver.caching.model.WeatherVariable2dRasterSlice
import com.luca009.imker.imkerserver.caching.model.WeatherVariableSlice

class WeatherRasterMemoryCacheImpl : WeatherRasterMemoryCache {
    val store: MutableMap<WeatherVariableType, WeatherVariableSlice> = mutableMapOf()

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
        return timeIndex < (store[weatherVariableType]?.variableSlices?.count() ?: return false)
    }

    override fun variableExistsAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        timeIndex: Int,
        xIndex: Int,
        yIndex: Int
    ): Boolean {
        val raster = store[weatherVariableType]?.variableSlices?.get(timeIndex)?.raster
        requireNotNull(raster) { return false }

        return yIndex < raster.count() && xIndex < raster[yIndex].count()
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
        xIndex: Int,
        yIndex: Int
    ): Double? {
        return store[weatherVariableType]?.variableSlices?.get(timeIndex)?.raster?.get(yIndex)?.get(xIndex)
    }
}