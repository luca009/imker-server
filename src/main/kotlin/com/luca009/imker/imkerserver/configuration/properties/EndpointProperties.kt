package com.luca009.imker.imkerserver.configuration.properties

import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "endpoints")
class EndpointProperties {
    var simpleWeatherVariables: Set<WeatherVariableType> = WeatherVariableType.values().toSet()
}