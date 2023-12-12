package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.WeatherRasterMemoryCache
import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import com.luca009.imker.imkerserver.caching.model.WeatherVariable2dRasterSlice
import com.luca009.imker.imkerserver.caching.model.WeatherVariableSlice

class WeatherRasterMemoryCacheImpl : WeatherRasterMemoryCache {
    override fun setVariable(weatherVariableType: WeatherVariableType, variableData: WeatherVariableSlice) {
        TODO("Not yet implemented")
    }

    override fun setVariableAtTime(
        weatherVariableType: WeatherVariableType,
        variable2dData: WeatherVariable2dRasterSlice,
        timeIndex: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun variableExists(weatherVariableType: WeatherVariableType): Boolean {
        TODO("Not yet implemented")
    }

    override fun variableExistsAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun variableExistsAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        timeIndex: Int,
        xIndex: Int,
        yIndex: Int
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun getVariable(weatherVariableType: WeatherVariableType): WeatherVariableSlice? {
        TODO("Not yet implemented")
    }

    override fun getVariableAtTime(weatherVariableType: WeatherVariableType, timeIndex: Int): WeatherVariable2dRasterSlice? {
        TODO("Not yet implemented")
    }

    override fun getVariableAtTimeAndPosition(
        weatherVariableType: WeatherVariableType,
        timeIndex: Int,
        xIndex: Int,
        yIndex: Int
    ): Float? {
        TODO("Not yet implemented")
    }
}