package com.luca009.imker.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "storage")
class StorageProperties {
    var storageLocations: MutableMap<String, String> = mutableMapOf(
        "default" to "/imkerstorage"
    )
}