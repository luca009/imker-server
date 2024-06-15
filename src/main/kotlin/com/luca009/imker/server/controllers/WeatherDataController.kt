package com.luca009.imker.server.controllers

import com.luca009.imker.server.configuration.properties.EndpointProperties
import com.luca009.imker.server.controllers.model.WeatherForecastResponse
import com.luca009.imker.server.parser.model.WeatherVariableType
import com.luca009.imker.server.queries.model.PreferredWeatherModelMode
import com.luca009.imker.server.queries.model.WeatherDataQueryService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime


@RestController
@RequestMapping("/weather")
class WeatherDataController(
    val weatherDataQueryService: WeatherDataQueryService,
    val endpointProperties: EndpointProperties
) {
    fun forecast(
        lat: Double,
        lon: Double,
        date: Long?,
        limit: Int?,
        weatherVariables: Set<WeatherVariableType>,
        model: String?
    ): WeatherForecastResponse {
        val dateTime = if (date == null) {
            ZonedDateTime.now(ZoneOffset.UTC) // No time specified, use current time
        } else {
            Instant.ofEpochSecond(date).atZone(ZoneOffset.UTC) // Time specified, convert epoch to ZonedDateTime at UTC
        }

        if (model == null) {
            // No model specified, query with PreferredWeatherModelMode.Dynamic
            return weatherDataQueryService.getForecast(
                weatherVariables,
                lat,
                lon,
                dateTime,
                limit,
                PreferredWeatherModelMode.Dynamic
            )
        }

        val weatherModel = weatherDataQueryService.findWeatherModel(model)
        requireNotNull(weatherModel) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Weather model \"$model\" not available at $lat $lon (lat, lon)")
        }

        return weatherDataQueryService.getForecast(
            weatherVariables,
            lat,
            lon,
            dateTime,
            limit,
            weatherModel
        )
    }

    /**
     * Returns a forecast at the specified [lat]/[lon] coordinates, only including simple weather variables.
     * Optionally, a [date] (epoch seconds, start of the forecast), a [limit] for the amount of values per variable, and/or a specific weather [model] can be specified.
     */
    @GetMapping("/forecast/simple")
    @Operation(
        summary = "Simple Forecast",
        description = "Get a forecast at the specified lat/lon coordinates, only including simple weather variables.\n" +
                "Optionally, a date (epoch seconds, start of the forecast), a limit for the amount of values per variable, and/or a specific weather model can be specified."
    )
    fun simpleForecast(
        @RequestParam lat: Double,
        @RequestParam lon: Double,
        @RequestParam(required = false) date: Long?,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) model: String?
    ): WeatherForecastResponse {
        val weatherVariables = endpointProperties.simpleWeatherVariables

        return forecast(
            lat, lon, date, limit, weatherVariables, model
        )
    }

    /**
     * Returns a forecast at the specified [lat]/[lon] coordinates, including all available weather variables.
     * Optionally, a [date] (epoch seconds, start of the forecast), a [limit] for the amount of values per variable, and/or a specific weather [model] can be specified.
     */
    @GetMapping("/forecast/complete")
    @Operation(
        summary = "Complete Forecast",
        description = "Get a forecast at the specified lat/lon coordinates, including all available weather variables.\n" +
                "Optionally, a date (epoch seconds, start of the forecast), a limit for the amount of values per variable, and/or a specific weather model can be specified."
    )
    fun completeForecast(
        @RequestParam lat: Double,
        @RequestParam lon: Double,
        @RequestParam(required = false) date: Long?,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) model: String?
    ): WeatherForecastResponse {
        val weatherVariables = WeatherVariableType.values().toSet()

        return forecast(
            lat, lon, date, limit, weatherVariables, model
        )
    }
}