package com.luca009.imker.server.controllers

import com.luca009.imker.server.configuration.properties.EndpointProperties
import com.luca009.imker.server.configuration.properties.VersionProperties
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/metadata")
class MetadataController(
    val endpointProperties: EndpointProperties,
    val versionProperties: VersionProperties
) {
    /**
     * Returns the version of Imker being used, as well as the latest git commit tag.
     */
    @GetMapping("/version")
    fun version(): Map<String, String> {
        return mapOf(
            "version" to versionProperties.versionString.toString(),
            "gitTag" to versionProperties.gitLastTag.toString()
        )
    }

    /**
     * Returns the variables that are used for the simple forecast.
     */
    @GetMapping("/forecast/simple")
    @Operation(
        summary = "Get variables for simple forecast",
        description = "Gets the variables that are used for the simple forecast."
    )
    fun simpleVariables(): List<String> {
        return endpointProperties.simpleWeatherVariables.map { it.name }
    }
}