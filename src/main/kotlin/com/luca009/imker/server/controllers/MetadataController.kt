package com.luca009.imker.server.controllers

import com.luca009.imker.server.configuration.properties.EndpointProperties
import com.luca009.imker.server.configuration.properties.VersionProperties
import com.luca009.imker.server.queries.model.WeatherDataQueryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/metadata")
class MetadataController(
    val weatherDataQueryService: WeatherDataQueryService,
    val endpointProperties: EndpointProperties,
    val versionProperties: VersionProperties
) {
    @GetMapping("/version")
    fun version(): Map<String, String> {
        return mapOf(
            "version" to versionProperties.versionString.toString(),
            "gitTag" to versionProperties.gitLastTag.toString()
        )
    }

    @GetMapping("/forecast/simple")
    fun simpleVariables(): List<String> {
        return endpointProperties.simpleWeatherVariables.map { it.name }
    }
}