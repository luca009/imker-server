package com.luca009.imker.server.management.files

import com.luca009.imker.server.management.files.model.DataFileNameManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class DataFileNameManagerConfiguration {
    @Bean
    fun dataFileNameManagerFactory() = {
        prefix: String, postfix: String, dateFormat: String, updateFrequency: Duration -> dataFileNameManager(prefix, postfix, dateFormat, updateFrequency)
    }

    fun dataFileNameManager(
        prefix: String,
        postfix: String,
        dateFormat: String,
        updateFrequency: Duration
    ): DataFileNameManager {
        return DataFileNameManagerImpl(prefix, postfix, dateFormat, updateFrequency)
    }
}