package com.luca009.imker.server.controllers

import com.luca009.imker.server.ArgumentNotNullHelper.requireArgumentNotNullOrDefault
import com.luca009.imker.server.controllers.model.WeatherModelResponse
import com.luca009.imker.server.controllers.model.WeatherModelResponseHelper.toWeatherModelResponse
import com.luca009.imker.server.management.models.model.WeatherModelManagerService
import com.luca009.imker.server.parser.model.WeatherVariableType
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.ZoneOffset

@RestController
@RequestMapping("/models")
class WeatherModelManagementController(
    val weatherModelManagerService: WeatherModelManagerService
) {
    /**
     * Returns all registered weather models.
     */
    @GetMapping("/all")
    @Operation(
        summary = "All weather models",
        description = "Gets all registered weather models."
    )
    fun weatherModels(): List<WeatherModelResponse> {
        return weatherModelManagerService.getWeatherModels().values.map {
            it.toWeatherModelResponse()
        }
    }

    /**
     * Returns all available weather models at a specified [lat]/[lon] position and, optionally, at a specified [time] and/or for a specific [variable].
     */
    @GetMapping("/by-location")
    @Operation(
        summary = "Available weather models at location",
        description = "Gets all available weather models at the specified lat/lon location and, optionally, time and/or for a specific variable."
    )
    fun weatherModelsAtLocation(
        @RequestParam lat: Double,
        @RequestParam lon: Double,
        @RequestParam(required = false) time: Long? = null,
        @RequestParam(required = false) variable: WeatherVariableType? = null
    ): List<WeatherModelResponse> {
        val dateTime = requireArgumentNotNullOrDefault(time, null) {
            Instant.ofEpochSecond(it).atZone(ZoneOffset.UTC)
        }

        return weatherModelManagerService.getAvailableWeatherModelsForLatLon(lat, lon, variable, dateTime).values.map {
            it.toWeatherModelResponse()
        }
    }
}