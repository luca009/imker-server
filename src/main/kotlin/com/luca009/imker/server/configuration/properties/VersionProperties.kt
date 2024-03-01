package com.luca009.imker.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@ConfigurationProperties(prefix = "version")
@PropertySource("\${classpath:version.properties}")
class VersionProperties {
    var versionString: String? = null
    var gitLastTag: String? = null
}