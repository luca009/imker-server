package com.luca009.imker.server.controllers.model

import java.time.ZonedDateTime

data class WeatherForecastResponse(
    val variables: List<WeatherVariableForecastResponse>
)

data class WeatherVariableForecastResponse(
    val variableName: String,
    val units: String,
    val values: List<WeatherVariableForecastValueResponse>
) {
    operator fun WeatherVariableForecastResponse.plus(other: WeatherVariableForecastResponse): WeatherVariableForecastResponse {
        return WeatherVariableForecastResponse(
            variableName,
            units,
            values + other.values
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is WeatherVariableForecastResponse) {
            return false
        }

        if (variableName != other.variableName ||
            units != other.units) {
            return false
        }

        return values == other.values
    }
}

data class WeatherVariableForecastValueResponse(
    val weatherModelName: String,
    val date: Long,
    val value: Double
)
object WeatherVariableForecastResponseHelper {
    fun doubleMapToWeatherVariableForecastResponse(data: Map<ZonedDateTime, Double>, variableName: String, units: String, weatherModelName: String): WeatherVariableForecastResponse {
        return WeatherVariableForecastResponse(
            variableName,
            units,
            data.map {
                WeatherVariableForecastValueResponse(
                    weatherModelName,
                    it.key.toEpochSecond(),
                    it.value
                )
            }
        )
    }
}