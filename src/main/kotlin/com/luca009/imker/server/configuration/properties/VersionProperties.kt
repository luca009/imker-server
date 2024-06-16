package com.luca009.imker.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@ConfigurationProperties(prefix = "version")
@PropertySource("\${classpath:version.properties}")
class VersionProperties {
    /**
     * The current version of Imker
     */
    var versionString: String? = null

    /**
     * The last git tag associated with this version of Imker
     */
    var gitLastTag: String? = null
}