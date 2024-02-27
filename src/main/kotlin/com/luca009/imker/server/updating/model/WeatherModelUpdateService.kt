package com.luca009.imker.server.updating.model

interface WeatherModelUpdateService {
    fun updateWeatherModels(updateSources: Boolean = true, forceUpdateParsers: Boolean = false)
}