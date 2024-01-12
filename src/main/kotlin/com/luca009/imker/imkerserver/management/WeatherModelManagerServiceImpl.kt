package com.luca009.imker.imkerserver.management

import com.luca009.imker.imkerserver.configuration.model.WeatherModel
import com.luca009.imker.imkerserver.management.model.WeatherModelManagerService
import java.util.*

class WeatherModelManagerServiceImpl(
    private val availableWeatherModels: SortedMap<Int, WeatherModel>
) : WeatherModelManagerService {
    override fun getWeatherModels() = availableWeatherModels

    override fun getAvailableWeatherModelsForLatLon(variableName: String, latitude: Double, longitude: Double): SortedMap<Int, WeatherModel> {
        return availableWeatherModels
            .filter { it.value.parser.containsLatLon(variableName, latitude, longitude) }
            .toSortedMap()
    }

    override fun getPreferredWeatherModelForLatLon(variableName: String, latitude: Double, longitude: Double): WeatherModel? {
        val filteredMap = availableWeatherModels
            .filter { it.value.parser.containsLatLon(variableName, latitude, longitude) }
            .toSortedMap()

        if (filteredMap.isEmpty()) {
            return null
        }

        return availableWeatherModels[filteredMap.firstKey()]
    }
}