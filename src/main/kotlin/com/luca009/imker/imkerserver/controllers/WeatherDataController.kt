package com.luca009.imker.imkerserver.controllers

import com.luca009.imker.imkerserver.controllers.model.WeatherForecastResponse
import com.luca009.imker.imkerserver.management.model.WeatherModelManagerService
import com.luca009.imker.imkerserver.parser.model.WeatherVariable
import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import com.luca009.imker.imkerserver.queries.model.PreferredWeatherModelMode
import com.luca009.imker.imkerserver.queries.model.WeatherDataQueryService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime


@RestController
@RequestMapping("/weather")
class WeatherDataController(
    val weatherModelManagerService: WeatherModelManagerService,
    val weatherDataQueryService: WeatherDataQueryService
) {
    @GetMapping("/forecast")
    fun forecast(@RequestParam lat: Double, @RequestParam lon: Double, @RequestParam(required = false) date: Long?, @RequestParam(required = false) limit: Int?, @RequestParam(required = false) model: String?): WeatherForecastResponse {
        // TODO: make this configurable
        val weatherVariables = listOf(WeatherVariableType.Temperature2m, WeatherVariableType.WindSpeed10m)

        val dateTime = if (date == null) {
            ZonedDateTime.now(ZoneOffset.UTC)
        } else {
            Instant.ofEpochSecond(date).atZone(ZoneOffset.UTC)
        }

        return if (model == null) {
            weatherDataQueryService.getForecast(
                weatherVariables,
                lat,
                lon,
                dateTime,
                limit,
                PreferredWeatherModelMode.Dynamic
            )
        } else {
            val weatherModel = weatherDataQueryService.findWeatherModel(model)
            requireNotNull(weatherModel) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Weather model \"$model\" not available at $lat $lon (lat, lon)")
            }

           weatherDataQueryService.getForecast(
               weatherVariables,
               lat,
               lon,
               dateTime,
               limit,
               weatherModel
           )
        }
    }
}