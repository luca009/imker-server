package com.luca009.imker.imkerserver.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "query")
class QueryProperties {
    var maxResultLimit: UInt = 100u
}