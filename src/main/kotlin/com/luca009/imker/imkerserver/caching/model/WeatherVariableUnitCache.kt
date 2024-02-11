package com.luca009.imker.imkerserver.caching.model

import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import com.luca009.imker.imkerserver.parser.model.WeatherVariableUnit

interface WeatherVariableUnitCache {
    fun getUnits(weatherVariableType: WeatherVariableType): WeatherVariableUnit?
    fun containsUnits(weatherVariableType: WeatherVariableType): Boolean
    fun setUnits(weatherVariableType: WeatherVariableType, newUnits: WeatherVariableUnit)
}