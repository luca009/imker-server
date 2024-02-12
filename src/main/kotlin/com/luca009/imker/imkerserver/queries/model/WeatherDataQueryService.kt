package com.luca009.imker.imkerserver.queries.model

import com.luca009.imker.imkerserver.configuration.model.WeatherModel
import com.luca009.imker.imkerserver.controllers.model.WeatherForecastResponse
import com.luca009.imker.imkerserver.controllers.model.WeatherVariableForecastResponse
import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import java.time.ZonedDateTime

interface WeatherDataQueryService {
    fun findWeatherModel(name: String): WeatherModel?

    /**
     * Get a weather forecast for the preferred weather model for each variable in [weatherVariables] with a maximum amount of results of [limit] (per variable).
     */
    fun getForecast(weatherVariables: List<WeatherVariableType>, lat: Double, lon: Double, time: ZonedDateTime, limit: Int?, preferredWeatherModelMode: PreferredWeatherModelMode): WeatherForecastResponse
    /**
     * Get a weather forecast for the specified [weatherModel] for each variable in [weatherVariables] with a maximum amount of results of [limit] (per variable).
     */
    fun getForecast(weatherVariables: List<WeatherVariableType>, lat: Double, lon: Double, time: ZonedDateTime, limit: Int?, weatherModel: WeatherModel): WeatherForecastResponse
    /**
     * Get a weather forecast for each variable and its corresponding weather model in [weatherVariables] with a maximum amount of results of [limit].
     */
    fun getForecast(weatherVariables: Map<WeatherVariableType, WeatherModel>, lat: Double, lon: Double, time: ZonedDateTime, limit: Int?): WeatherForecastResponse

    /**
     * Get a weather forecast for the preferred weather model (according to [preferredWeatherModelMode]) for the variable [weatherVariable] with a maximum amount of results of [limit].
     */
    fun getVariableForecast(weatherVariable: WeatherVariableType, lat: Double, lon: Double, time: ZonedDateTime, limit: Int?, preferredWeatherModelMode: PreferredWeatherModelMode): WeatherVariableForecastResponse
    /**
     * Get a weather forecast for the specified [weatherModel] for the variable [weatherVariable] with a maximum amount of results of [limit].
     */
    fun getVariableForecast(weatherVariable: WeatherVariableType, lat: Double, lon: Double, time: ZonedDateTime, limit: Int?, weatherModel: WeatherModel): WeatherVariableForecastResponse

    /**
     * Get a weather forecast at the specified [time] for the preferred weather model for each variable in [weatherVariables].
     */
    fun getForecastAtTimePoint(weatherVariables: List<WeatherVariableType>, lat: Double, lon: Double, time: ZonedDateTime): WeatherForecastResponse
    /**
     * Get a weather forecast at the specified [time] for each variable and its corresponding weather model in [weatherVariables].
     */
    fun getForecastAtTimePoint(weatherVariables: Map<WeatherVariableType, WeatherModel>, lat: Double, lon: Double, time: ZonedDateTime): WeatherForecastResponse

    /**
     * Get a weather forecast at the specified [time] for the preferred weather model for the variable [weatherVariable].
     */
    fun getVariableForecastAtTimePoint(weatherVariable: WeatherVariableType, lat: Double, lon: Double, time: ZonedDateTime): WeatherVariableForecastResponse
    /**
     * Get a weather forecast at the specified [time] for the specified [weatherModel] for the variable [weatherVariable].
     */
    fun getVariableForecastAtTimePoint(weatherVariable: WeatherVariableType, lat: Double, lon: Double, time: ZonedDateTime, weatherModel: WeatherModel): WeatherVariableForecastResponse
}

enum class PreferredWeatherModelMode {
    /**
     * Get the preferred weather model at the start of the search and do not update it any further
     *
     * Example:
     * Available weather models
     * a---b---a---c-b-a---b---c
     * Used weather model
     * a-------a-------a--------
     */
    Static,

    /**
     * Get the preferred weather model at the start and stick to it until it no longer provides data, then switch to the next best weather model
     *
     * Example:
     * Available weather models
     * a---b---a---c-b-a---b---c
     * Used weather model
     * a-------a-------a---b---c
     */
    Dynamic,

    /**
     * Get all weather models for every time step
     *
     * Example:
     * Available weather models
     * a---b---a---c-b-a---b---c
     * Used weather model
     * a---b---a---c-b-a---b---c
     */
    All
}