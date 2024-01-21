package com.luca009.imker.imkerserver.configuration

import com.luca009.imker.imkerserver.configuration.model.WeatherVariableFileNameMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import java.io.File

@Configuration
class WeatherVariableFileNameMapperConfiguration {
    @Bean
    fun weatherVariableFileNameMapperFactory() = {
            mappingFile: File -> weatherVariableFileNameMapper(mappingFile)
    }

    @Bean
    @Scope("prototype")
    fun weatherVariableFileNameMapper(mappingFile: File): WeatherVariableFileNameMapper {
        return WeatherVariableFileNameMapperImpl(
            mappingFile
        )
    }
}