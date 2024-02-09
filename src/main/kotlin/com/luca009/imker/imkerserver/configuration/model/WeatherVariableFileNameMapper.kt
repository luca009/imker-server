package com.luca009.imker.imkerserver.configuration.model

import com.luca009.imker.imkerserver.parser.model.WeatherVariableType

interface WeatherVariableFileNameMapper {
    fun getWeatherVariables(): Set<WeatherVariableType>
    fun getWeatherVariables(variableName: String): Set<WeatherVariableType>
    fun getWeatherVariables(variableName: String, filePath: String): Set<WeatherVariableType>
    fun getWeatherVariableFileRule(variable: WeatherVariableType): String?
    fun getWeatherVariableName(variable: WeatherVariableType): String?
    fun getMatchingFileName(variable: WeatherVariableType, availableFiles: Set<String>): String?

    fun containsWeatherVariable(variable: WeatherVariableType): Boolean
}