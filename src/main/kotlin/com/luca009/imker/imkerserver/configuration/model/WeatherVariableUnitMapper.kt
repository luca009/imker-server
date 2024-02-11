package com.luca009.imker.imkerserver.configuration.model

import com.luca009.imker.imkerserver.parser.model.WeatherVariableUnit

interface WeatherVariableUnitMapper {
    fun getUnits(unitString: String): WeatherVariableUnit?
}