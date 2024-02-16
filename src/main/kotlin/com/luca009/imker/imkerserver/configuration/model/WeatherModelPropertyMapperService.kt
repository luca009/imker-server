package com.luca009.imker.imkerserver.configuration.model

import java.util.SortedMap

interface WeatherModelPropertyMapperService {
    fun getWeatherModels(): SortedMap<Int, WeatherModel>
}