package com.luca009.imker.imkerserver.management.model

import com.luca009.imker.imkerserver.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.imkerserver.configuration.model.WeatherModel
import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import java.util.SortedMap

interface WeatherModelManagerService {
    // TODO: A bunch more functions for managing available weather models (available variables, etc.)

    fun getWeatherModels(): SortedMap<Int, WeatherModel>

    fun updateWeatherModelCaches()
    fun updateWeatherModelCaches(weatherModels: Set<WeatherModel>)

    fun updateDataParsers()
    fun updateDataParsers(weatherModels: Set<WeatherModel>)

    fun getAvailableWeatherModelsForLatLon(variable: WeatherVariableType, latitude: Double, longitude: Double): SortedMap<Int, WeatherModel>
    fun getPreferredWeatherModelForLatLon(variable: WeatherVariableType, latitude: Double, longitude: Double): WeatherModel?

    fun getCompositeCacheForWeatherModel(weatherModel: WeatherModel): WeatherRasterCompositeCache?
}