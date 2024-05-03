package com.luca009.imker.server.configuration.model

import com.luca009.imker.server.parser.model.WeatherVariableType

interface WeatherVariableTypeMapper {
    fun getWeatherVariables(): Set<WeatherVariableType>
    fun getWeatherVariables(variableName: String): Set<WeatherVariableType>
    fun getWeatherVariableName(variable: WeatherVariableType): String?
    fun containsWeatherVariable(variable: WeatherVariableType): Boolean
}