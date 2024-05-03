package com.luca009.imker.server.configuration

import com.luca009.imker.server.parser.model.WeatherVariableType
import com.luca009.imker.server.configuration.model.WeatherVariableTypeMapper

class WeatherVariableTypeMapperImpl(
    private val map: Map<WeatherVariableType, String>
) : WeatherVariableTypeMapper {

    override fun getWeatherVariables(): Set<WeatherVariableType> {
        return map.keys
    }

    override fun getWeatherVariables(variableName: String): Set<WeatherVariableType> {
        return map.filterValues { it == variableName }.keys
    }

    override fun getWeatherVariableName(variable: WeatherVariableType): String? {
        return map[variable]
    }

    override fun containsWeatherVariable(variable: WeatherVariableType): Boolean {
        return map.containsKey(variable)
    }
}