package com.luca009.imker.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConfigurationProperties(prefix = "update")
class UpdateProperties {
    var lazyUpdate: Boolean = false
    var updateCheckInterval: Duration = Duration.ofMinutes(10)

    var lazyCaching: Boolean = false
}