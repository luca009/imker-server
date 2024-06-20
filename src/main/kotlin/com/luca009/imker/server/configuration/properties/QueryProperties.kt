package com.luca009.imker.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "query")
class QueryProperties {
    /**
     * The maximum number of results to output per query.
     */
    var maxResultLimit: UInt = 100u
}