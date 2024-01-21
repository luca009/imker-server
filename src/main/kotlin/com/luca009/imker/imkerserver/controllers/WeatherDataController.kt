package com.luca009.imker.imkerserver.controllers

import com.luca009.imker.imkerserver.management.model.WeatherModelManagerService
import com.luca009.imker.imkerserver.parser.model.WeatherVariable
import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException


@RestController
@RequestMapping("/weather")
class WeatherDataController(
    val weatherModelManagerService: WeatherModelManagerService
) {
    @GetMapping("/forecast")
    fun forecast(@RequestParam lat: Double, @RequestParam lon: Double, @RequestParam(required = false) model: String?): Double? {
        val weatherVariableType = WeatherVariableType.Temperature2m

        val weatherModel = if (model == null) {
            // No weather model was given as an argument, use the preferred one
            val preferredWeatherModel = weatherModelManagerService.getPreferredWeatherModelForLatLon(weatherVariableType, lat, lon)
            requireNotNull(preferredWeatherModel) {
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No weather model available at $lat $lon (lat, lon)")
            }

            preferredWeatherModel
        } else {
            // A specific weather model was requested
            // First, look at all available weather models at the requested location, to see if it is available
            val availableWeatherModels = weatherModelManagerService.getAvailableWeatherModelsForLatLon(weatherVariableType, lat, lon)
            val filteredWeatherModels = availableWeatherModels.filterValues { it.name == model }

            // Our search for the weather model had no results, this is a bad request :(
            if (filteredWeatherModels.isEmpty()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Weather model $model not available at $lat $lon (lat, lon)")
            }

            // Use the first search result and see if it's null. There might be more, but there shouldn't be, as this would be an illegal configuration
            val requestedWeatherModel = requireNotNull(filteredWeatherModels[0]) {
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Weather model $model available at $lat $lon (lat, lon), but defined as null")
            }

            requestedWeatherModel
        }

        val weatherModelCache = weatherModelManagerService.getCompositeCacheForWeatherModel(weatherModel)
        requireNotNull(weatherModelCache) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Weather model ${weatherModel.name} had no cache associated with it")
        }

        val coordinates = weatherModel.parser.latLonToCoordinates("TT", lat, lon)
        requireNotNull(coordinates) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to convert $lat $lon (lat, lon) into valid coordinates for weather model ${weatherModel.name}")
        }

        return weatherModelCache.getVariableAtTimeAndPosition(WeatherVariableType.Temperature2m, 0, coordinates)
    }
}