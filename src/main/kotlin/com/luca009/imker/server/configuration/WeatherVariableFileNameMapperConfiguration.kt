package com.luca009.imker.server.configuration

import com.luca009.imker.server.configuration.model.WeatherVariableFileNameMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class WeatherVariableFileNameMapperConfiguration {
    @Bean
    fun weatherVariableFileNameMapperFactory() = {
            mappingFile: File -> weatherVariableFileNameMapper(mappingFile)
    }

    fun weatherVariableFileNameMapper(mappingFile: File): WeatherVariableFileNameMapper {
        return WeatherVariableFileNameMapperImpl(
            mappingFile
        )
    }
}