package com.luca009.imker.server.configuration.model

import java.util.SortedMap

/**
 * Mapper interface for creating [WeatherModel]s from the local configuration.
 */
interface WeatherModelPropertyMapperService {
    /**
     * Get the weather models assembled from the configuration.
     */
    fun getWeatherModels(): SortedMap<Int, WeatherModel>
}