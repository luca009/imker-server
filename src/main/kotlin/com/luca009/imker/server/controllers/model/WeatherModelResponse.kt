package com.luca009.imker.server.controllers.model

import com.luca009.imker.server.configuration.model.WeatherModel

data class WeatherModelResponse(
    val name: String,
    val friendlyName: String,
    val copyright: String
)

object WeatherModelResponseHelper {
    fun WeatherModel.toWeatherModelResponse(): WeatherModelResponse {
        return WeatherModelResponse(
            this.name,
            this.friendlyName,
            this.copyright
        )
    }
}