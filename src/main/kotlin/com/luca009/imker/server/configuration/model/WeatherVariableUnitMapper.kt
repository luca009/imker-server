package com.luca009.imker.server.configuration.model

import com.luca009.imker.server.parser.model.WeatherVariableUnit

interface WeatherVariableUnitMapper {
    fun getUnits(unitString: String): WeatherVariableUnit?
}