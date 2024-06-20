package com.luca009.imker.server.controllers.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.luca009.imker.server.configuration.model.WeatherModel

/**
 * Data class representing a list of weather variables and their data ([WeatherVariableForecastResponse]).
 */
data class WeatherForecastResponse(
    val variables: List<WeatherVariableForecastResponse>,
    val models: List<WeatherModelDetailsResponse>
) {
    constructor(variables: List<WeatherVariableForecastResponse>, usedModels: Set<WeatherModel>) : this(
        variables,
        usedModels.map {
            WeatherModelDetailsResponse(
                it.name,
                it.friendlyName,
                it.copyright,
                it.parser.getDataSources().minOf { date -> date.value }.toEpochSecond()
            )
        }
    )

    constructor(variables: List<WeatherVariableForecastResponse>) : this(
        variables,
        variables.fold(setOf()) { usedModels, forecast ->
            usedModels.plus(forecast.usedModels)
        }
    )
}

/**
 * Data class representing a list of weather variable values ([WeatherVariableForecastValueResponse]).
 */
data class WeatherVariableForecastResponse(
    val variable: String,
    val units: String?,
    val values: List<WeatherVariableForecastValueResponse>,
    @JsonIgnore
    val usedModels: Set<WeatherModel>
) {
    operator fun WeatherVariableForecastResponse.plus(other: WeatherVariableForecastResponse): WeatherVariableForecastResponse {
        return WeatherVariableForecastResponse(
            variable,
            units,
            values + other.values,
            usedModels
        )
    }
}

/**
 * Data class representing a weather variable value at a single point and time.
 */
data class WeatherVariableForecastValueResponse(
    val model: String,
    val date: Long,
    val value: Double
)

data class WeatherModelDetailsResponse(
    val name: String,
    val friendlyName: String,
    val copyright: String,
    val lastUpdated: Long
)