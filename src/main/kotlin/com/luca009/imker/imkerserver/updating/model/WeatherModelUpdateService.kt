package com.luca009.imker.imkerserver.updating.model

interface WeatherModelUpdateService {
    fun updateWeatherModels(updateSources: Boolean = true)
}