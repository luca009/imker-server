package com.luca009.imker.imkerserver.filemanager

import com.luca009.imker.imkerserver.filemanager.model.DataFileNameManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataFileNameManagerConfiguration {
    @Bean
    fun dataFileNameManagerFactory() = {
        prefix: String, postfix: String, dateFormat: String, updateFrequencyMins: Int -> dataFileNameManager(prefix, postfix, dateFormat, updateFrequencyMins)
    }

    @Bean
    fun dataFileNameManager(
        prefix: String,
        postfix: String,
        dateFormat: String,
        updateFrequencyMins: Int
    ): DataFileNameManager {
        return DataFileNameManagerImpl(prefix, postfix, dateFormat, updateFrequencyMins)
    }
}