package com.luca009.imker.imkerserver.filemanager.model

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
interface LocalFileManagerService {
    @Bean
    public fun getWeatherDataLocation(subFolder: String): Path
}