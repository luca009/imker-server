package com.luca009.imker.server.configuration.model

import com.luca009.imker.server.parser.model.WeatherVariableType

/**
 * Mapper interface for associating variable names/identifiers with [WeatherVariableType]s.
 */
interface WeatherVariableTypeMapper {
    /**
     * Get all [WeatherVariableType]s specified in the configuration.
     */
    fun getWeatherVariables(): Set<WeatherVariableType>

    /**
     * Get all [WeatherVariableType]s associated with the specified [variableName] (variable identifier).
     */
    fun getWeatherVariables(variableName: String): Set<WeatherVariableType>

    /**
     * Get the variable name/identifier associated with the specified [variable].
     */
    fun getWeatherVariableName(variable: WeatherVariableType): String?

    /**
     * Get whether the configuration contains the specified [variable].
     */
    fun containsWeatherVariable(variable: WeatherVariableType): Boolean
}