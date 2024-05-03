package com.luca009.imker.server.queries

import com.luca009.imker.server.caching.model.WeatherRasterTimeSnappingType
import com.luca009.imker.server.configuration.model.WeatherModel
import com.luca009.imker.server.configuration.properties.QueryProperties
import com.luca009.imker.server.controllers.model.WeatherForecastResponse
import com.luca009.imker.server.controllers.model.WeatherVariableForecastResponse
import com.luca009.imker.server.controllers.model.WeatherVariableForecastValueResponse
import com.luca009.imker.server.management.models.model.WeatherModelManagerService
import com.luca009.imker.server.parser.model.WeatherVariableTimeSlice
import com.luca009.imker.server.parser.model.WeatherVariableType
import com.luca009.imker.server.queries.model.PreferredWeatherModelMode
import com.luca009.imker.server.queries.model.WeatherDataQueryService
import com.luca009.imker.server.queries.model.WeatherVariableProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Service
class WeatherDataQueryServiceImpl(
    val weatherModelManagerService: WeatherModelManagerService,
    val queryProperties: QueryProperties
) : WeatherDataQueryService {
    private fun getWeatherVariableProperties(weatherModel: WeatherModel, weatherVariable: WeatherVariableType, lat: Double, lon: Double, time: ZonedDateTime, timeSnapping: WeatherRasterTimeSnappingType, checkWeatherModelExists: Boolean): WeatherVariableProperties {
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

        val weatherModelCache = weatherModelManagerService.getCompositeCacheForWeatherModel(weatherModel)
        requireNotNull(weatherModelCache) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Weather model ${weatherModel.name} had no cache associated with it")
        }

        val coordinates = weatherModel.parser.latLonToCoordinates(weatherVariable, lat, lon)
        requireNotNull(coordinates) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to convert $lat $lon (lat, lon) into valid coordinates for weather model ${weatherModel.name}")
        }

        val snappedTime = weatherModelCache.getSnappedTime(weatherVariable, time, timeSnapping)
        requireNotNull(snappedTime) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$time is not in the range of the weather model ${weatherModel.name}")
        }

        val units = weatherModelCache.getUnits(weatherVariable)

        return WeatherVariableProperties(
            weatherModelCache,
            coordinates,
            snappedTime,
            units
        )
    }

    private fun limitForecastList(forecasts: List<WeatherVariableForecastValueResponse>, limit: Int): List<WeatherVariableForecastValueResponse> {
        // Slice the forecast if necessary to hit the limit
        return if (forecasts.count() > limit) {
            forecasts.subList(0, limit)
        } else {
            forecasts
        }
    }

    private fun getSafeLimit(limit: Int?): Int {
        val realLimit = (limit ?: queryProperties.maxResultLimit.toInt())
        if (realLimit < 1) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Query limit was outside of the accepted range (1 to 32-bit signed integer limit)")
        }

        return realLimit
    }

    override fun findWeatherModel(name: String): WeatherModel? {
        val availableWeatherModels = weatherModelManagerService.getWeatherModels()
        return availableWeatherModels.values.firstOrNull { it.name == name }
    }

    /**
     * Gets a forecast for multiple [weatherVariables] with the defined [query]. Catches any errors that may happen.
     * If the final response is empty, a common error gets thrown, otherwise, any erroneous queries get ignored.
     */
    private inline fun <T> tryGetForecast(
        weatherVariables: Set<T>,
        query: (T) -> WeatherVariableForecastResponse
    ): WeatherForecastResponse {
        if (weatherVariables.isEmpty()) {
            return WeatherForecastResponse(emptyList())
        }

        val errors: MutableSet<ResponseStatusException> = mutableSetOf()

        val forecastData = weatherVariables.mapNotNull {
            try {
                query(it)
            } catch (e: ResponseStatusException) {
                errors.add(e)
                null
            }
        }

        // We did not get any successful query responses :(
        if (forecastData.isEmpty()) {
            if (errors.count() == 1) {
                // Only one unique error occurred, throw that one
                throw errors.first()
            }

            // Multiple unique errors occurred, throw generic message
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Multiple errors occurred while processing request")
        }

        return WeatherForecastResponse(
            forecastData
        )
    }

    override fun getForecast(
        weatherVariables: Set<WeatherVariableType>,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int?,
        preferredWeatherModelMode: PreferredWeatherModelMode
    ) = tryGetForecast(weatherVariables) {
        getVariableForecast(it, lat, lon, time, limit, preferredWeatherModelMode)
    }

    override fun getForecast(
        weatherVariables: Set<WeatherVariableType>,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int?,
        weatherModel: WeatherModel
    ) = tryGetForecast(weatherVariables) {
        getVariableForecast(it, lat, lon, time, limit, weatherModel)
    }

    override fun getForecast(
        weatherVariables: Map<WeatherVariableType, WeatherModel>,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int?
    ) = tryGetForecast(weatherVariables.entries) {
        getVariableForecast(it.key, lat, lon, time, limit, it.value)
    }

    override fun getVariableForecast(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int?,
        preferredWeatherModelMode: PreferredWeatherModelMode
    ): WeatherVariableForecastResponse {
        return when (preferredWeatherModelMode) {
            PreferredWeatherModelMode.Static -> getFixedPreferredWeatherModelVariableForecast(weatherVariable, lat, lon, time, WeatherRasterTimeSnappingType.Earliest, limit)
            PreferredWeatherModelMode.Dynamic -> getDynamicPreferredWeatherModelVariableForecast(weatherVariable, lat, lon, time, limit)
            PreferredWeatherModelMode.All -> getAllWeatherModelVariableForecast(weatherVariable, lat, lon, time, limit)
        }
    }

    private fun getFixedPreferredWeatherModelVariableForecast(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        timeSnapping: WeatherRasterTimeSnappingType,
        limit: Int?
    ): WeatherVariableForecastResponse {
        // No weather model was given as an argument, use the preferred one
        val preferredWeatherModel = weatherModelManagerService.getPreferredWeatherModelForLatLon(weatherVariable, lat, lon, time)
        requireNotNull(preferredWeatherModel) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No weather model available at $lat $lon (lat, lon)")
        }

        return getVariableForecast(weatherVariable, lat, lon, time, timeSnapping, limit, preferredWeatherModel)
    }

    private fun getFixedPreferredWeatherModelVariableForecastOrNull(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        timeSnapping: WeatherRasterTimeSnappingType,
        limit: Int?
    ): WeatherVariableForecastResponse? {
        return try {
            getFixedPreferredWeatherModelVariableForecast(weatherVariable, lat, lon, time, timeSnapping, limit)
        } catch (e: Exception) {
            null
        }
    }

    private fun getDynamicPreferredWeatherModelVariableForecast(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int?
    ): WeatherVariableForecastResponse {
        val safeLimit = getSafeLimit(limit)

        val usedUnits: HashSet<String> = hashSetOf()
        val weatherVariableForecasts: MutableList<WeatherVariableForecastValueResponse> = mutableListOf()

        var lastTime = time
        do {
            // Get the fixed preferred weather model forecast
            val fixedWeatherForecast = if (weatherVariableForecasts.isEmpty()) {
                // First iteration, use the earliest snapping type
                getFixedPreferredWeatherModelVariableForecastOrNull(weatherVariable, lat, lon, lastTime, WeatherRasterTimeSnappingType.Earliest, limit) ?: break
            } else {
                // For the following iterations, use the latest snapping type, to make sure no timestamps pop up twice
                getFixedPreferredWeatherModelVariableForecastOrNull(weatherVariable, lat, lon, lastTime, WeatherRasterTimeSnappingType.Latest, limit) ?: break
            }

            if (fixedWeatherForecast.values.isEmpty()) {
                continue
            }

            weatherVariableForecasts.addAll(fixedWeatherForecast.values)
            usedUnits.add(fixedWeatherForecast.units)

            // Early break, because we're going to do a more expensive operation now
            if (weatherVariableForecasts.count() >= safeLimit) {
                break
            }

            // Get the maximum time of the forecast...
            val highestDate = fixedWeatherForecast.values.maxBy { it.date }.date

            // ...and add one second, so it doesn't come up again (TODO: maybe fix this janky workaround)
            lastTime = Instant.ofEpochSecond(highestDate + 1).atZone(ZoneOffset.UTC)
        } while (weatherVariableForecasts.count() < safeLimit) // Have we reached the limit yet? If so, great, we're done!

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
            limitForecastList(weatherVariableForecasts, safeLimit)
        )
    }

    private fun getAllWeatherModelVariableForecast(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int?
    ): WeatherVariableForecastResponse {
        val safeLimit = getSafeLimit(limit)

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
            limitForecastList(weatherVariableForecasts, safeLimit)
        )
    }

    override fun getVariableForecast(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        limit: Int?,
        weatherModel: WeatherModel
    ) = getVariableForecast(
        weatherVariable,
        lat,
        lon,
        time,
        WeatherRasterTimeSnappingType.Earliest,
        limit,
        weatherModel
    )

    private fun WeatherVariableTimeSlice.toWeatherVariableForecastResponse(variableName: String, variableUnits: String, weatherModelName: String): WeatherVariableForecastResponse? {
        if (!this.isDouble()) {
            return null
        }

        return WeatherVariableForecastResponse(
            variableName,
            variableUnits,
            this.mapNotNull {
                WeatherVariableForecastValueResponse(
                    weatherModelName,
                    it.key.toEpochSecond(),
                    it.value as Double // theoretically allowed unsafe cast, since we're checking for isDouble() at the top
                )
            }
        )
    }

    private fun getVariableForecast(
        weatherVariable: WeatherVariableType,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        timeSnapping: WeatherRasterTimeSnappingType,
        limit: Int?,
        weatherModel: WeatherModel
    ): WeatherVariableForecastResponse {
        val properties = getWeatherVariableProperties(weatherModel, weatherVariable, lat, lon, time, timeSnapping, true)
        val safeLimit = getSafeLimit(limit)
        val values = properties.weatherModelCache.getVariableAtPosition(weatherVariable, properties.coordinates, safeLimit)

        val response = values?.toWeatherVariableForecastResponse(weatherVariable.name, properties.units.toString(), weatherModel.name)

        return requireNotNull(response) {
            throw ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "Response from model ${weatherModel.name} would have been in a different data type than Double: ${values?.dataType?.simpleName}"
            )
        }
    }

    override fun getForecastAtTimePoint(
        weatherVariables: Set<WeatherVariableType>,
        lat: Double,
        lon: Double,
        time: ZonedDateTime,
        ignoreUnknownVariables: Boolean
    ): WeatherForecastResponse {
        val preferredWeatherModelMap =
            weatherVariables.mapNotNull {
                // No weather model was given as an argument, use the preferred one
                val preferredWeatherModel = weatherModelManagerService.getPreferredWeatherModelForLatLon(it, lat, lon, time)

                if (preferredWeatherModel == null) {
                    if (ignoreUnknownVariables) {
                        return@mapNotNull null
                    }

                    throw ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No weather model available at $lat $lon (lat, lon)"
                    )
                }

                Pair(it, preferredWeatherModel)
            }.toMap()

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
                val properties = getWeatherVariableProperties(it.value, it.key, lat, lon, time, WeatherRasterTimeSnappingType.Earliest,true)

                val value = properties.weatherModelCache.getVariableAtTimeAndPosition(it.key, properties.snappedTime, properties.coordinates) ?: return@mapNotNull null
                val epochTime = properties.snappedTime.toEpochSecond()

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
        val properties = getWeatherVariableProperties(weatherModel, weatherVariable, lat, lon, time, WeatherRasterTimeSnappingType.Earliest, true)

        val value = properties.weatherModelCache.getVariableAtTimeAndPosition(weatherVariable, properties.snappedTime, properties.coordinates)
        requireNotNull(value) {
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
                    properties.snappedTime.toEpochSecond(),
                    value
                )
            )
        )
    }

}

