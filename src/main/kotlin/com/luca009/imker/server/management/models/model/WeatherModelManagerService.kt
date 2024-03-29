package com.luca009.imker.server.management.models.model

import com.luca009.imker.server.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.server.configuration.model.WeatherModel
import com.luca009.imker.server.parser.model.WeatherVariableType
import java.time.ZonedDateTime
import java.util.SortedMap

interface WeatherModelManagerService {
    // TODO: A bunch more functions for managing available weather models (available variables, etc.)

    fun getWeatherModels(): SortedMap<Int, WeatherModel>

    fun queueUpdateWeatherModel(weatherModel: WeatherModel, updateSource: Boolean = true, forceUpdateParser: Boolean = false)
    suspend fun beginUpdateWeatherModels(updateSources: Boolean = true, forceUpdateParsers: Boolean = false)

    fun cleanupDataStorageLocations()
    fun cleanupDataStorageLocations(weatherModels: Set<WeatherModel>)

    fun getAvailableWeatherModelsForLatLon(variable: WeatherVariableType, latitude: Double, longitude: Double): SortedMap<Int, WeatherModel>
    fun getAvailableWeatherModelsForLatLon(variable: WeatherVariableType, latitude: Double, longitude: Double, time: ZonedDateTime): SortedMap<Int, WeatherModel>

    fun getPreferredWeatherModelForLatLon(variable: WeatherVariableType, latitude: Double, longitude: Double, time: ZonedDateTime): WeatherModel?
    fun getPreferredWeatherModelForLatLon(variable: WeatherVariableType, latitude: Double, longitude: Double): WeatherModel?

    fun getCompositeCacheForWeatherModel(weatherModel: WeatherModel): WeatherRasterCompositeCache?
}