package com.luca009.imker.server.configuration.model

import com.luca009.imker.server.parser.model.WeatherVariableType
import java.nio.file.Path

interface WeatherVariableFileNameMapper {
    fun getWeatherVariables(): Set<WeatherVariableType>
    fun getWeatherVariables(variableName: String): Set<WeatherVariableType>
    fun getWeatherVariables(variableName: String, filePath: Path): Set<WeatherVariableType>
    fun getWeatherVariableFileRule(variable: WeatherVariableType): String?
    fun getWeatherVariableName(variable: WeatherVariableType): String?
    fun getMatchingFilePath(variable: WeatherVariableType, availableFiles: Set<Path>): Path?

    fun containsWeatherVariable(variable: WeatherVariableType): Boolean
}