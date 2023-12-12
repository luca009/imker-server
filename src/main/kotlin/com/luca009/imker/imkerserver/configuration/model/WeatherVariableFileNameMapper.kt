package com.luca009.imker.imkerserver.configuration.model

import com.luca009.imker.imkerserver.parser.model.WeatherVariableType

interface WeatherVariableFileNameMapper {
    fun getWeatherVariables(variableName: String, filePath: String): Set<WeatherVariableType>
    fun getWeatherVariableFile(variable: WeatherVariableType): String?
    fun getWeatherVariableName(variable: WeatherVariableType): String?

    fun containsWeatherVariable(variable: WeatherVariableType): Boolean
}