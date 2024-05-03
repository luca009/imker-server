package com.luca009.imker.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "update")
class UpdateProperties {
    var lazyUpdate: Boolean = false
    var updateCheckInterval: Int = 10

    var lazyCaching: Boolean = false
}