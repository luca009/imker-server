package com.luca009.imker.imkerserver.queries

import com.luca009.imker.imkerserver.configuration.model.WeatherModel
import com.luca009.imker.imkerserver.controllers.model.WeatherForecastResponse
import com.luca009.imker.imkerserver.controllers.model.WeatherVariableForecastResponse
import com.luca009.imker.imkerserver.controllers.model.WeatherVariableForecastResponseHelper
import com.luca009.imker.imkerserver.controllers.model.WeatherVariableForecastValueResponse
import com.luca009.imker.imkerserver.management.model.WeatherModelManagerService
import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import com.luca009.imker.imkerserver.parser.model.WeatherVariableUnit
import com.luca009.imker.imkerserver.queries.model.PreferredWeatherModelMode
import com.luca009.imker.imkerserver.queries.model.WeatherDataQueryService
import com.luca009.imker.imkerserver.queries.model.WeatherVariableProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Service
class WeatherDataQueryServiceImpl(
    val weatherModelManagerService: WeatherModelManagerService
) : WeatherDataQueryService {
    fun getWeatherVariableProperties(weatherModel: WeatherModel, weatherVariable: WeatherVariableType, lat: Double, lon: Double, time: ZonedDateTime, checkWeatherModelExists: Boolean): WeatherVariableProperties {
        if (checkWeatherModelExists) {
            // A specific weather model was requested
            // First, look at all available weather models at the requested location, to see if it is available
            val availableWeatherModels = weatherModelManagerService.getAvailableWeatherModelsForLatLon(weatherVariable, lat, lon, time)
            val weatherModelAvailable = availableWeatherModels.containsValue(weatherModel)

            // Our search for the weather model had no results, this is a bad request :(
            if (!weatherModelAvailable) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Weather model ${weatherModel.name} not available at $lat $lon (lat, lon)")
            }
        }

        val variableName = weatherModel.mapper.getWeatherVariableName(weatherVariable)
        requireNotNull(variableName) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Weather model ${weatherModel.name} had no mapping for variable $weatherVariable")
        }

        val rawWeatherVariable = weatherModel.parser.getRawVariable(variableName)
        requireNotNull(rawWeatherVariable) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Weather model ${weatherModel.name} did not contain variable $weatherVariable, despite the mapping suggesting it")
        }

        val weatherModelCache = weatherModelManagerService.getCompositeCacheForWeatherModel(weatherModel)
        requireNotNull(weatherModelCache) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Weather model ${weatherModel.name} had no cache associated with it")
        }

        val coordinates = weatherModel.parser.latLonToCoordinates("TT", lat, lon)
        requireNotNull(coordinates) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to convert $lat $lon (lat, lon) into valid coordinates for weather model ${weatherModel.name}")
        }

        val timeIndex = weatherModelCache.getEarliestTimeIndex(weatherVariable, time)
        requireNotNull(timeIndex) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$time is not in the range of the weather model ${weatherModel.name}")
        }

        val units = weatherModelCache.getUnits(weatherVariable)

        return WeatherVariableProperties(
            rawWeatherVariable,
            weatherModelCache,
            coordinates,
            timeIndex,
            units
        )
    }

    fun limitForecastList(forecasts: List<WeatherVariableForecastValueResponse>, limit: Int): List<WeatherVariableForecastValueResponse> {
        // Slice the forecast if necessary to hit the limit
        return if (forecasts.count() > limit) {
            forecasts.subList(0, limit)
        } else {
            forecasts
        }
    }

    override fun findWeatherModel(name: String): WeatherModel? {
        val availableWeatherModels = weatherModelManagerService.getWeatherModels()
        return availableWeatherModels.values.firstOrNull { it.name == name }
    }

    override fun getForecast(
        weatherVariables: List<WeatherVariableType>,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int,
        preferredWeatherModelMode: PreferredWeatherModelMode
    ): WeatherForecastResponse {
        return WeatherForecastResponse(
            weatherVariables.map {
                getVariableForecast(it, lat, lon, time, limit, preferredWeatherModelMode)
            }
        )
    }

    override fun getForecast(
        weatherVariables: List<WeatherVariableType>,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int,
        weatherModel: WeatherModel
    ): WeatherForecastResponse {
        return WeatherForecastResponse(
            weatherVariables.map {
                getVariableForecast(it, lat, lon, time, limit, weatherModel)
            }
        )
    }

    override fun getForecast(
        weatherVariables: Map<WeatherVariableType, WeatherModel>,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int
    ): WeatherForecastResponse {
        return WeatherForecastResponse(
            weatherVariables.map {
                getVariableForecast(it.key, lat, lon, time, limit, it.value)
            }
        )
    }

    override fun getVariableForecast(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int,
        preferredWeatherModelMode: PreferredWeatherModelMode
    ): WeatherVariableForecastResponse {
        return when (preferredWeatherModelMode) {
            PreferredWeatherModelMode.Static -> getFixedPreferredWeatherModelVariableForecast(weatherVariable, lat, lon, time, limit)
            PreferredWeatherModelMode.Dynamic -> getDynamicPreferredWeatherModelVariableForecast(weatherVariable, lat, lon, time, limit)
            PreferredWeatherModelMode.All -> getAllWeatherModelVariableForecast(weatherVariable, lat, lon, time, limit)
        }
    }

    fun getFixedPreferredWeatherModelVariableForecast(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int
    ): WeatherVariableForecastResponse {
        // No weather model was given as an argument, use the preferred one
        val preferredWeatherModel = weatherModelManagerService.getPreferredWeatherModelForLatLon(weatherVariable, lat, lon, time)
        requireNotNull(preferredWeatherModel) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No weather model available at $lat $lon (lat, lon)")
        }

        return getVariableForecast(weatherVariable, lat, lon, time, limit, preferredWeatherModel)
    }

    fun getFixedPreferredWeatherModelVariableForecastOrNull(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int
    ): WeatherVariableForecastResponse? {
        return try {
            getFixedPreferredWeatherModelVariableForecast(weatherVariable, lat, lon, time, limit)
        } catch (e: Exception) {
            null
        }
    }

    fun getDynamicPreferredWeatherModelVariableForecast(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int
    ): WeatherVariableForecastResponse {
        val usedUnits: HashSet<String> = hashSetOf()
        val weatherVariableForecasts: MutableList<WeatherVariableForecastValueResponse> = mutableListOf()

        var lastTime = time
        do {
            // Get the fixed preferred weather model forecast
            val fixedWeatherForecast = getFixedPreferredWeatherModelVariableForecastOrNull(weatherVariable, lat, lon, lastTime, limit) ?: break

            if (fixedWeatherForecast.values.isEmpty()) {
                continue
            }

            weatherVariableForecasts.addAll(fixedWeatherForecast.values)
            usedUnits.add(fixedWeatherForecast.units)

            // Early break, because we're going to do a more expensive operation now
            if (weatherVariableForecasts.count() >= limit) {
                break
            }

            // Get the maximum time of the forecast and add one second, so it doesn't come up again (TODO: maybe fix this janky workaround)
            lastTime = Instant.ofEpochSecond(fixedWeatherForecast.values.maxBy { it.date }.date + 1).atZone(ZoneOffset.UTC)
        } while (weatherVariableForecasts.count() < limit) // Have we reached the limit yet? If so, great, we're done!

        if (weatherVariableForecasts.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No weather model available at $lat $lon (lat, lon)")
        }

        val finalUnits = if (usedUnits.count() != 1) {
            "null" // Too many or no units defined (TODO: implement unit conversion)
        } else {
            usedUnits.elementAtOrNull(0) ?: "null"
        }

        return WeatherVariableForecastResponse(
            weatherVariable.name,
            finalUnits,
            limitForecastList(weatherVariableForecasts, limit)
        )
    }

    fun getAllWeatherModelVariableForecast(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int
    ): WeatherVariableForecastResponse {
        val weatherVariableForecasts: MutableList<WeatherVariableForecastValueResponse> = mutableListOf()
        val usedUnits: HashSet<String> = hashSetOf()
        val availableModels = weatherModelManagerService.getAvailableWeatherModelsForLatLon(weatherVariable, lat, lon).values.filterNotNull()

        for (model in availableModels) {
            val forecast = getVariableForecast(weatherVariable, lat, lon, time, limit, model)
            weatherVariableForecasts.addAll(forecast.values)
            usedUnits.add(forecast.units)
        }

        val finalUnits = if (usedUnits.count() != 1) {
            "null" // Too many or no units defined (TODO: implement unit conversion)
        } else {
            usedUnits.elementAtOrNull(0) ?: "null"
        }

        weatherVariableForecasts.sortBy { it.date }

        return WeatherVariableForecastResponse(
            weatherVariable.name,
            finalUnits,
            limitForecastList(weatherVariableForecasts, limit)
        )
    }

    override fun getVariableForecast(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int,
        weatherModel: WeatherModel
    ): WeatherVariableForecastResponse {
        val properties = getWeatherVariableProperties(weatherModel, weatherVariable, lat, lon, time, true)

        val variableValues: MutableMap<ZonedDateTime, Double> = mutableMapOf()
        for (i in 0 until limit) {
            val value = properties.weatherModelCache.getVariableAtTimeAndPosition(weatherVariable, properties.timeIndex + i, properties.coordinates)
                ?: continue
            val date = properties.weatherModelCache.getTime(weatherVariable, properties.timeIndex + i) ?: continue

            variableValues[date] = value
        }

        return WeatherVariableForecastResponseHelper.doubleMapToWeatherVariableForecastResponse(variableValues, weatherVariable.name, properties.units.toString(), weatherModel.name)
    }

    override fun getForecastAtTimePoint(
        weatherVariables: List<WeatherVariableType>,
        lat: Double,
        lon: Double,
        time: ZonedDateTime
    ): WeatherForecastResponse {
        val preferredWeatherModelMap =
            weatherVariables.associateWith {
                // No weather model was given as an argument, use the preferred one
                val preferredWeatherModel = weatherModelManagerService.getPreferredWeatherModelForLatLon(it, lat, lon, time)

                requireNotNull(preferredWeatherModel) {
                    throw ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No weather model available at $lat $lon (lat, lon)"
                    )
                }

                preferredWeatherModel
            }

        return getForecastAtTimePoint(preferredWeatherModelMap, lat, lon, time)
    }

    override fun getForecastAtTimePoint(
        weatherVariables: Map<WeatherVariableType, WeatherModel>,
        lat: Double,
        lon: Double,
        time: ZonedDateTime
    ): WeatherForecastResponse {
        return WeatherForecastResponse(
            weatherVariables.mapNotNull {
                val properties = getWeatherVariableProperties(it.value, it.key, lat, lon, time, true)

                val value = properties.weatherModelCache.getVariableAtTimeAndPosition(it.key, properties.timeIndex, properties.coordinates) ?: return@mapNotNull null
                val epochTime = properties.weatherModelCache.getTime(it.key, properties.timeIndex)?.toEpochSecond() ?: return@mapNotNull null

                WeatherVariableForecastResponse(
                    it.key.name,
                    properties.units.toString(),
                    listOf(WeatherVariableForecastValueResponse(
                        it.value.name,
                        epochTime,
                        value
                    ))
                )
            }
        )
    }

    override fun getVariableForecastAtTimePoint(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime
    ): WeatherVariableForecastResponse {
        // No weather model was given as an argument, use the preferred one
        val preferredWeatherModel = weatherModelManagerService.getPreferredWeatherModelForLatLon(weatherVariable, lat, lon, time)
        requireNotNull(preferredWeatherModel) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No weather model available at $lat $lon (lat, lon)")
        }

        return getVariableForecastAtTimePoint(
            weatherVariable,
            lat,
            lon,
            time,
            preferredWeatherModel
        )
    }

    override fun getVariableForecastAtTimePoint(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        weatherModel: WeatherModel
    ): WeatherVariableForecastResponse {
        val properties = getWeatherVariableProperties(weatherModel, weatherVariable, lat, lon, time, true)

        val value = properties.weatherModelCache.getVariableAtTimeAndPosition(weatherVariable, properties.timeIndex, properties.coordinates)
        requireNotNull(value) {
            return WeatherVariableForecastResponse(
                weatherVariable.name,
                properties.units.toString(),
                listOf()
            )
        }

        val actualTime = properties.weatherModelCache.getTime(weatherVariable, properties.timeIndex)?.toEpochSecond()
        requireNotNull(actualTime) {
            return WeatherVariableForecastResponse(
                weatherVariable.name,
                properties.units.toString(),
                listOf()
            )
        }

        return WeatherVariableForecastResponse(
            weatherVariable.name,
            properties.units.toString(),
            listOf(
                WeatherVariableForecastValueResponse(
                    weatherModel.name,
                    actualTime,
                    value
                )
            )
        )
    }

}